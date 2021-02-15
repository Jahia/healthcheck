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
import org.jahia.modules.healthcheck.HealthcheckConstants;
import org.jahia.modules.healthcheck.config.HealthcheckConfigProvider;
import org.jahia.modules.healthcheck.interfaces.HealthcheckProbeService;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.settings.SettingsBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class HealthcheckJSONProducer extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthcheckJSONProducer.class);
    private static final int DEFAULT_HTTP_CODE_ON_ERROR = 500;
    private static final String DEFAULT_HTTP_CODE_ON_ERROR_PARAMETER = "http_code_on_error";
    private static final int DEFAULT_SEVERITY_THRESHOLD = HealthcheckConstants.PROBE_SEVERITY_CRITICAL; // by default, only critical probes are displayed
    private static final HashMap<String, Integer> PROBE_SEVERITY_LEVELS = new HashMap<String, Integer>()  {{
        put(HealthcheckConstants.PROBE_SEVERITY_CRITICAL_LABEL, HealthcheckConstants.PROBE_SEVERITY_CRITICAL);
        put(HealthcheckConstants.PROBE_SEVERITY_HIGH_LABEL, HealthcheckConstants.PROBE_SEVERITY_HIGH);
        put(HealthcheckConstants.PROBE_SEVERITY_MEDIUM_LABEL, HealthcheckConstants.PROBE_SEVERITY_MEDIUM);
        put(HealthcheckConstants.PROBE_SEVERITY_LOW_LABEL, HealthcheckConstants.PROBE_SEVERITY_LOW);
    }};
    private static final HashMap<String, String> DEFAULT_PROBES_SEVERITY = new HashMap<String, String>() {{
        put("Datastore", HealthcheckConstants.PROBE_SEVERITY_CRITICAL_LABEL);
        put("DBConnectivity", HealthcheckConstants.PROBE_SEVERITY_CRITICAL_LABEL);
        put("ServerLoad", HealthcheckConstants.PROBE_SEVERITY_HIGH_LABEL);
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
            if (severityThresholdParam != null) {
                severityThresholdParam = severityThresholdParam.toUpperCase();
            }
            int severityThreshold = PROBE_SEVERITY_LEVELS.getOrDefault(severityThresholdParam.toUpperCase(), DEFAULT_SEVERITY_THRESHOLD);
            final boolean allowUnauthenticatedAccess = Boolean.parseBoolean(SettingsBean.getInstance().getPropertiesFile().getProperty("modules.healthcheck.allowUnauthenticatedAccess", "false"));
            if (!allowUnauthenticatedAccess && !isUserAllowed(session, token)) {
                result.put("error", "Insufficient privilege");
            } else {

                HealthcheckProbeService healthcheckProbeService = BundleUtils.getOsgiService(HealthcheckProbeService.class, null);

                List<Probe> probes = healthcheckProbeService.getProbes();

                long startTime = System.currentTimeMillis();

                HealthcheckConfigProvider healthcheckConfig = (HealthcheckConfigProvider) SpringContextSingleton.getBean("healthcheckConfig");

                try {
                    String currentStatus = HealthcheckConstants.STATUS_GREEN;
                    for (int i = 0; probes.size() > i; i++) {
                        JSONObject healthcheckerJSON = new JSONObject();
                        String probeSeverity = healthcheckConfig.getProperty(String.format(HealthcheckConstants.PROP_HEALTHCHECK_PROBE_SEVERITY_PARAMETER, probes.get(i).getName()));
                        if (probeSeverity == null) {
                            probeSeverity = DEFAULT_PROBES_SEVERITY.containsKey(probes.get(i).getName()) ? DEFAULT_PROBES_SEVERITY.get(probes.get(i).getName()) : HealthcheckConstants.PROBE_SEVERITY_LOW_LABEL;
                        }
                        probeSeverity = probeSeverity.toUpperCase();
                        int probeSeverityInt = PROBE_SEVERITY_LEVELS.get(probeSeverity);
                        healthcheckerJSON.put("severity", probeSeverity);
                        healthcheckerJSON.put("status", probes.get(i));

                        if (probes.get(i).getStatus().equals(HealthcheckConstants.STATUS_YELLOW) && currentStatus.equals(HealthcheckConstants.STATUS_GREEN)) {
                            currentStatus = HealthcheckConstants.STATUS_YELLOW;
                        }

                        if (probes.get(i).getStatus().equals(HealthcheckConstants.STATUS_RED) && (currentStatus.equals(HealthcheckConstants.STATUS_GREEN) || currentStatus.equals(HealthcheckConstants.STATUS_YELLOW))) {
                            currentStatus = HealthcheckConstants.STATUS_RED;

                            String customCodeOnError = healthcheckConfig.getProperty(DEFAULT_HTTP_CODE_ON_ERROR_PARAMETER);

                            int errorCode = DEFAULT_HTTP_CODE_ON_ERROR;

                            if (customCodeOnError != null) {
                                errorCode = Integer.parseInt(customCodeOnError);
                            }

                            resp.setStatus(errorCode);
                        }

                        healthcheckerJSON.put("data", probes.get(i).getData());
                        JSONObject checkers;
                        if (!result.has("probes")) {
                            checkers = new JSONObject();
                            result.put("probes", checkers);
                        } else {
                            checkers = result.getJSONObject("probes");
                        }
                        checkers.put(probes.get(i).getName(), healthcheckerJSON);

                    }
                    result.put("registeredProbes", probes.size());

                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;

                    result.put("duration", elapsedTime + " ms");
                    result.put("status", currentStatus);

                    if (result.get("status").equals(HealthcheckConstants.STATUS_RED)) {
                        LOGGER.error("The healthcheck returned a RED status: {}", result.toString());
                    }

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
