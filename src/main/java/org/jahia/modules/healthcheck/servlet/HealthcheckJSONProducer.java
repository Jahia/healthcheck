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
import org.jahia.settings.SettingsBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jahia.modules.healthcheck.HealthcheckConstants.PROP_HEALTHCHECK_PROBE_SEVERITY_PARAMETER;

/**
 */
public class HealthcheckJSONProducer extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthcheckJSONProducer.class);
    private static final int DEFAULT_HTTP_CODE_ON_ERROR = 500;
    private static final String DEFAULT_HTTP_CODE_ON_ERROR_PARAMETER = "http_code_on_error";
    private static final int DEFAULT_SEVERITY_THRESHOLD = HealthcheckConstants.PROBE_SEVERITY_CRITICAL; // by default, only critical probes are displayed
    private static final HashMap<String, Integer> PROBE_SEVERITY_LEVELS = new HashMap<String, Integer>()  {{
        put("critical", HealthcheckConstants.PROBE_SEVERITY_CRITICAL);
        put("high", HealthcheckConstants.PROBE_SEVERITY_HIGH);
        put("medium", HealthcheckConstants.PROBE_SEVERITY_MEDIUM);
        put("low", HealthcheckConstants.PROBE_SEVERITY_LOW);
    }};
    private static final ArrayList<String> DEFAULT_CRITICAL_PROBES = new ArrayList<String>() {{
        add("Datastore");
        add("ServerLoad");
        add("DBConnectivity");
    }};

    public SettingsBean settingBean;

    public SettingsBean getSettingBean() {
        return settingBean;
    }

    public void setSettingBean(SettingsBean settingBean) {
        this.settingBean = settingBean;
    }

    public HealthcheckJSONProducer() {
    }

    public void postConstruct() {
    }

    public void preDestroy() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Content-Type", "application/json");

        JSONObject result = new JSONObject();
        PrintWriter writer = resp.getWriter();

        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            String token = req.getParameter(HealthcheckConstants.PARAM_TOKEN);
            String severityThresholdParam = req.getParameter(HealthcheckConstants.PARAM_SEVERITY);
            int severityThreshold = PROBE_SEVERITY_LEVELS.getOrDefault(severityThresholdParam, DEFAULT_SEVERITY_THRESHOLD);
            final boolean allowUnauthenticatedAccess = Boolean.parseBoolean(SettingsBean.getInstance().getPropertiesFile().getProperty("modules.healthcheck.allowUnauthenticatedAccess", "false"));
            if (!allowUnauthenticatedAccess && !isUserAllowed(session, token)) {
                result.put("error", "Insufficient privilege");
            } else {
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
                                    String probeSeverity = healthcheckConfig.getProperty(String.format(PROP_HEALTHCHECK_PROBE_SEVERITY_PARAMETER, probe.getName()));
                                    int probeSeverityInt = HealthcheckConstants.PROBE_SEVERITY_LOW;
                                    if (DEFAULT_CRITICAL_PROBES.contains(probe.getName())) {
                                        // we still look at a possible severity override for default probes
                                        probeSeverityInt = PROBE_SEVERITY_LEVELS.getOrDefault(probeSeverity, HealthcheckConstants.PROBE_SEVERITY_CRITICAL);
                                    } else {
                                        probeSeverityInt = PROBE_SEVERITY_LEVELS.getOrDefault(probeSeverity, HealthcheckConstants.PROBE_SEVERITY_LOW);
                                    }
                                    LOGGER.debug("probe {} severity is {} while the threshold is '{}'", probe.getName(), probeSeverityInt, severityThreshold);
                                    if (severityThreshold < probeSeverityInt) {
                                        // skip this probe since it is above the requested severity threshold
                                        LOGGER.debug("skipping the logger {} (it is below the requested threshold)", probe.getName());
                                        continue;
                                    }
                                    JSONObject healthcheckerJSON = new JSONObject();
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

                    if (result.get("status").equals("RED")) {
                        LOGGER.error("The healthcheck returned a RED status: {}", result.toString());
                    }

                    result.put("registeredProbes", probesCount);
                    result.put("duration", elapsedTime + " ms");
                    result.put("status", currentStatus);

                } catch (JSONException ex) {
                    LOGGER.error("Impossible to generate the JSON", ex);
                }
            }
        } catch (RepositoryException ex) {
            LOGGER.error("Impossible to retrieve the JCR session", ex);
        } catch (JSONException ex) {
            LOGGER.error("Impossible to generate the JSON", ex);
        }

        writer.println(result.toString());
    }

    private boolean isUserAllowed(JCRSessionWrapper session, String token) throws RepositoryException {
        String configurationToken = settingBean.getString(HealthcheckConstants.PROP_HEALTHCHECK_TOKEN, null);

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

}
