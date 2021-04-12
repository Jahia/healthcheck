package org.jahia.modules.healthcheck.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.healthcheck.HealthcheckConstants;
import org.jahia.modules.healthcheck.config.HealthcheckConfigProvider;
import org.jahia.modules.healthcheck.interfaces.HealthcheckProbeService;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.jahia.osgi.FrameworkService;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class HealthcheckJSONProducer extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthcheckJSONProducer.class);
    private static final int DEFAULT_HTTP_CODE_ON_ERROR = 500;
    private static final String DEFAULT_HTTP_CODE_ON_ERROR_PARAMETER = "http_code_on_error";
    private static final int DEFAULT_SEVERITY_THRESHOLD = HealthcheckConstants.PROBE_SEVERITY_CRITICAL; // by default, only critical probes are displayed
    private static final HashMap<String, Integer> PROBE_SEVERITY_LEVELS = new HashMap<String, Integer>() {
        {
            put(HealthcheckConstants.PROBE_SEVERITY_CRITICAL_LABEL, HealthcheckConstants.PROBE_SEVERITY_CRITICAL);
            put(HealthcheckConstants.PROBE_SEVERITY_HIGH_LABEL, HealthcheckConstants.PROBE_SEVERITY_HIGH);
            put(HealthcheckConstants.PROBE_SEVERITY_MEDIUM_LABEL, HealthcheckConstants.PROBE_SEVERITY_MEDIUM);
            put(HealthcheckConstants.PROBE_SEVERITY_LOW_LABEL, HealthcheckConstants.PROBE_SEVERITY_LOW);
        }
    };
    private static final HashMap<String, String> DEFAULT_PROBES_SEVERITY = new HashMap<String, String>() {
        {
            put("Datastore", HealthcheckConstants.PROBE_SEVERITY_CRITICAL_LABEL);
            put("DBConnectivity", HealthcheckConstants.PROBE_SEVERITY_CRITICAL_LABEL);
            put("ServerLoad", HealthcheckConstants.PROBE_SEVERITY_HIGH_LABEL);
        }
    };
    private static final String KEY_SEPARATOR = ", ";
    private final List<SubnetUtils> onlyForSubnets = new ArrayList<>();
    private boolean useFirstRemoteAddress = true;
    private boolean allowUnauthenticatedAccess = false;
    private String configurationToken = "";
    private String remoteAddressHeader;

    public HealthcheckJSONProducer() {
    }

    public void postConstruct() {
    }

    public void preDestroy() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        resp.addHeader("Content-Type", "application/json");

        try ( PrintWriter writer = resp.getWriter()) {
            final JSONObject result = new JSONObject();

            final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            final String token = req.getParameter(HealthcheckConstants.PARAM_TOKEN);
            boolean accessGranted = false;

            if (allowUnauthenticatedAccess) {
                if (onlyForSubnets.isEmpty()) {
                    HealthcheckJSONProducer.putHealtcheckErrorStatus(result);
                } else {
                    String remoteAddress = req.getHeader(remoteAddressHeader);
                    if (remoteAddress == null) {
                        remoteAddress = req.getRemoteAddr();
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("remoteAddress:{}", remoteAddress);
                    }

                    if (HealthcheckJSONProducer.isRemoteAddressWhitelisted(remoteAddress, onlyForSubnets, useFirstRemoteAddress)) {
                        accessGranted = true;
                    }
                }
            }

            if (!accessGranted && isUserAllowed(session, token)) {
                accessGranted = true;
            }

            if (accessGranted) {
                String severityThresholdParam = req.getParameter(HealthcheckConstants.PARAM_SEVERITY);
                if (severityThresholdParam != null) {
                    severityThresholdParam = severityThresholdParam.toUpperCase();
                }
                int severityThreshold = PROBE_SEVERITY_LEVELS.getOrDefault(severityThresholdParam, DEFAULT_SEVERITY_THRESHOLD);
                HealthcheckJSONProducer.computeHealthcheck(result, severityThreshold, resp);
            } else {
                HealthcheckJSONProducer.putHealtcheckErrorStatus(result);
            }
            writer.println(result.toString());
        } catch (RepositoryException ex) {
            LOGGER.error("Impossible to retrieve the JCR session", ex);
        } catch (IOException ex) {
            LOGGER.error("Impossible to write response", ex);
        }

    }

    private boolean isUserAllowed(JCRSessionWrapper session, String token) throws RepositoryException {
        HealthcheckConfigProvider healthcheckConfig = (HealthcheckConfigProvider) SpringContextSingleton.getBean("healthcheckConfig");

        String karafToken = healthcheckConfig.getProperty("token");
        // In case a token is deployed as karaf configuration, we use this one. The other method is kept for backward compatibility purpose only
        if (karafToken != null) {
            configurationToken = karafToken;
        }

        if (token != null) {
            // checking if the token passed in the configuration matches this one
            if (token.equals(configurationToken)) {
                return true;
            }
            JCRSessionWrapper systemSession = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH);
            if (systemSession.nodeExists(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS)) {
                if (systemSession.getNode(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS).hasProperty(HealthcheckConstants.PROP_TOKENS)) {
                    if (systemSession.getNode(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS).getPropertyAsString(HealthcheckConstants.PROP_TOKENS).contains(token)) {
                        return true;
                    }
                }
            }
        }

        if (session.getUser().getUsername().equals("guest")) {
            return false;
        }
        try {
            return session.getNode("/sites/systemsite").hasPermission("healthcheck");
        } catch (PathNotFoundException ignored) {
            return false;
        } catch (RepositoryException e) {
            LOGGER.error("", e);
            return false;
        }
    }

    public void setOnlyForSubnets(String onlyForSubnets) {
        this.onlyForSubnets.clear();
        this.onlyForSubnets.addAll(getSubnetUtilsList(onlyForSubnets));
    }

    public void setRemoteAddressHeader(String remoteAddressHeader) {
        this.remoteAddressHeader = remoteAddressHeader;
    }

    public void setUseFirstRemoteAddress(boolean useFirstRemoteAddress) {
        this.useFirstRemoteAddress = useFirstRemoteAddress;
    }

    protected static boolean isRemoteAddressWhitelisted(String remoteAddress, List<SubnetUtils> onlyForSubnets) {
        return isRemoteAddressWhitelisted(remoteAddress, onlyForSubnets, true);
    }

    protected static boolean isRemoteAddressWhitelisted(String remoteAddress, List<SubnetUtils> onlyForSubnets, boolean useFirstRemoteAddress) {
        boolean isInRange = false;
        int index = 0;
        final String[] remoteAddresses = remoteAddress.split(KEY_SEPARATOR);
        final String remoteAddressToCheck;
        if (useFirstRemoteAddress) {
            remoteAddressToCheck = remoteAddresses[0];
        } else {
            remoteAddressToCheck = remoteAddresses[remoteAddresses.length - 1];
        }
        while (!isInRange && index < onlyForSubnets.size()) {
            final SubnetUtils utils = onlyForSubnets.get(index);
            try {
                isInRange = utils.getInfo().isInRange(remoteAddressToCheck);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Impossible to check remote address", ex);
            }
            index++;
        }
        return isInRange;
    }

    protected static List<SubnetUtils> getSubnetUtilsList(String onlyForSubnets) {
        if (StringUtils.isBlank(onlyForSubnets)) {
            return Collections.emptyList();
        }
        final List<SubnetUtils> subnets = new ArrayList<>();
        for (String subnet : StringUtils.split(onlyForSubnets, KEY_SEPARATOR)) {
            if (StringUtils.isNotEmpty(subnet)) {
                final SubnetUtils subnetUtils = new SubnetUtils(subnet);
                subnetUtils.setInclusiveHostCount(true);
                subnets.add(subnetUtils);
            }
        }
        return subnets;
    }

    private static void computeHealthcheck(JSONObject result, int severityThreshold, HttpServletResponse resp) {
        BundleContext bundleContext = FrameworkService.getBundleContext();
        List<ServiceReference> serviceReferences;
        try {
            ServiceReference[] refs = bundleContext.getAllServiceReferences(HealthcheckProbeService.class.getName(), null);
            serviceReferences = refs != null && refs.length > 0 ? Arrays.asList(refs) : null;
        } catch (InvalidSyntaxException var7) {
            throw new JahiaRuntimeException(var7);
        }

        long startTime = System.currentTimeMillis();
        int probesCount = 0;

        try {
            String currentStatus = HealthcheckConstants.STATUS_GREEN;
            if (serviceReferences != null) {
                for (ServiceReference serviceReference : serviceReferences) {
                    HealthcheckProbeService healthcheckProbeService = (HealthcheckProbeService) bundleContext.getService(serviceReference);
                    if (healthcheckProbeService != null) {
                        List<Probe> probes = healthcheckProbeService.getProbes();
                        HealthcheckConfigProvider healthcheckConfig = (HealthcheckConfigProvider) SpringContextSingleton.getBean("healthcheckConfig");
                        for (Probe probe : probes) {
                            JSONObject healthcheckerJSON = new JSONObject();
                            String probeSeverity = healthcheckConfig.getProperty(String.format(HealthcheckConstants.PROP_HEALTHCHECK_PROBE_SEVERITY_PARAMETER, probe.getName()));
                            if (probeSeverity == null) {
                                probeSeverity = DEFAULT_PROBES_SEVERITY.getOrDefault(probe.getName(), HealthcheckConstants.PROBE_SEVERITY_LOW_LABEL);
                            }
                            probeSeverity = probeSeverity.toUpperCase();
                            int probeSeverityInt = PROBE_SEVERITY_LEVELS.get(probeSeverity);

                            LOGGER.debug("probe {} severity is {} while the threshold is '{}'", probe.getName(), probeSeverityInt, severityThreshold);
                            if (severityThreshold < probeSeverityInt) {
                                // skip this probe since it is above the requested severity threshold
                                LOGGER.debug("skipping the logger {} (it is below the requested threshold)", probe.getName());
                                continue;
                            }

                            healthcheckerJSON.put("severity", probeSeverity);
                            healthcheckerJSON.put("status", probe.getStatus());

                            if (probe.getStatus().equals(HealthcheckConstants.STATUS_YELLOW) && currentStatus.equals(HealthcheckConstants.STATUS_GREEN)) {
                                currentStatus = HealthcheckConstants.STATUS_YELLOW;
                            }
                            if (probe.getStatus().equals(HealthcheckConstants.STATUS_RED) && (currentStatus.equals(HealthcheckConstants.STATUS_GREEN) || currentStatus.equals(HealthcheckConstants.STATUS_YELLOW))) {
                                currentStatus = HealthcheckConstants.STATUS_RED;

                                String customCodeOnError = healthcheckConfig.getProperty(DEFAULT_HTTP_CODE_ON_ERROR_PARAMETER);

                                int errorCode = DEFAULT_HTTP_CODE_ON_ERROR;

                                if (customCodeOnError != null) {
                                    errorCode = Integer.parseInt(customCodeOnError);
                                }

                                resp.setStatus(errorCode);
                            }
                            healthcheckerJSON.put("data", probe.getData());
                            JSONObject checkers;
                            if (!result.has("probes")) {
                                LOGGER.debug("creating checkers");
                                checkers = new JSONObject();
                                result.put("probes", checkers);
                            } else {
                                checkers = result.getJSONObject("probes");
                            }
                            checkers.put(probe.getName(), healthcheckerJSON);
                            LOGGER.debug("putting checkers " + probe.getName());
                            probesCount++;
                        }
                    }
                }
            }

            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;

            result.put("registeredProbes", probesCount);
            result.put("duration", elapsedTime + " ms");
            result.put("status", currentStatus);

            if (result.get("status").equals(HealthcheckConstants.STATUS_RED)) {
                LOGGER.error("The healthcheck returned a RED status: {}", result.toString());
            }

        } catch (JSONException ex) {
            LOGGER.error("Impossible to generate the JSON", ex);
        }
    }

    private static void putHealtcheckErrorStatus(JSONObject result) {
        try {
            result.put("error", "Insufficient privilege");
        } catch (JSONException ex) {
            LOGGER.error("Impossible to generate the JSON", ex);
        }
    }

    public void setAllowUnauthenticatedAccess(boolean allowUnauthenticatedAccess) {
        this.allowUnauthenticatedAccess = allowUnauthenticatedAccess;
    }

    public boolean isAllowUnauthenticatedAccess() {
        return allowUnauthenticatedAccess;
    }

    public void setConfigurationToken(String configurationToken) {
        this.configurationToken = configurationToken;
    }

    public String getConfigurationToken() {
        return configurationToken;
    }

}
