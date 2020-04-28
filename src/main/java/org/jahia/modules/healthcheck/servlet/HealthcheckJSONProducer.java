package org.jahia.modules.healthcheck.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jahia.api.Constants;
import org.jahia.modules.healthcheck.HealthcheckConstants;
import org.jahia.modules.healthcheck.interfaces.HealthcheckProbeService;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.jahia.osgi.BundleUtils;
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

    private Logger LOGGER = LoggerFactory.getLogger(HealthcheckJSONProducer.class);
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
            final boolean allowUnauthenticatedAccess = Boolean.parseBoolean(SettingsBean.getInstance().getPropertiesFile().getProperty("modules.healthcheck.allowUnauthenticatedAccess", "false"));
            if (!allowUnauthenticatedAccess && !isUserAllowed(session, token)) {
                result.put("error", "Insufficient privilege");
            } else {

                HealthcheckProbeService healthcheckProbeService = BundleUtils.getOsgiService(HealthcheckProbeService.class, null);

                List<Probe> probes = healthcheckProbeService.getProbes();

                long startTime = System.currentTimeMillis();

                try {
                    String currentStatus = HealthcheckConstants.STATUS_GREEN;
                    for (int i = 0; probes.size() > i; i++) {
                        JSONObject healthcheckerJSON = new JSONObject();
                        healthcheckerJSON.put("status", probes.get(i).getStatus());

                        if (probes.get(i).getStatus().equals(HealthcheckConstants.STATUS_YELLOW) && currentStatus.equals(HealthcheckConstants.STATUS_GREEN)) {
                            currentStatus = HealthcheckConstants.STATUS_YELLOW;
                        }
                        if (probes.get(i).getStatus().equals(HealthcheckConstants.STATUS_RED) && (currentStatus.equals(HealthcheckConstants.STATUS_GREEN) || currentStatus.equals(HealthcheckConstants.STATUS_YELLOW))) {
                            currentStatus = HealthcheckConstants.STATUS_RED;
                        }
                        healthcheckerJSON.put("data", probes.get(i).getData());
                        System.out.println("JSONObject: " + healthcheckerJSON.toString());
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

        if (token != null) {
            // checking if the token passed in jahia.property matches this one
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
