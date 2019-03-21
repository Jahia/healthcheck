/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.healthcheck.servlet;

import org.jahia.modules.healthcheck.interfaces.HealthcheckProbeService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.settings.SettingsBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.http.HttpService;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


/**
 */
public class HealthcheckJSONProducer extends HttpServlet {
    private Logger logger = LoggerFactory.getLogger(HealthcheckJSONProducer.class);

    private HttpService httpService;
    private List<Probe> healthcheckers;

    public HealthcheckJSONProducer() {
    }

    public void postConstruct() {
    }

    public void preDestroy() {
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONObject result = new JSONObject();
        PrintWriter writer = resp.getWriter();

        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            final boolean allowUnauthenticatedAccess = Boolean.parseBoolean(SettingsBean.getInstance().getPropertiesFile().getProperty("modules.healthcheck.allowUnauthenticatedAccess", "false"));
            if (!allowUnauthenticatedAccess && (session.getUser().getUsername().equals("guest") || !session.getNode("/sites/systemsite").hasPermission("healthcheck"))) {
                result.put("error", "Insufficient privilege");
            } else {

                HealthcheckProbeService healthcheckProbeService = BundleUtils.getOsgiService(HealthcheckProbeService.class, null);

                List<Probe> probes = healthcheckProbeService.getProbes();

                long startTime = System.currentTimeMillis();

                try {
                    String currentStatus = "GREEN";
                    for (int i = 0; probes.size() > i; i++) {
                        JSONObject healthcheckerJSON = new JSONObject();
                        healthcheckerJSON.put("status", probes.get(i).getStatus());

                        if (probes.get(i).getStatus().equals("YELLOW") && currentStatus.equals("GREEN")) {
                            currentStatus = "YELLOW";
                        }
                        if (probes.get(i).getStatus().equals("RED") && (currentStatus.equals("GREEN") || currentStatus.equals("YELLOW"))) {
                            currentStatus = "RED";
                        }
                        healthcheckerJSON.put("data", probes.get(i).getData());
                        System.out.println("JSONObject: " + healthcheckerJSON.toString());
                        JSONObject checkers;
                        if (!result.has("probes")) {
                            logger.info("creating checkers");
                            checkers = new JSONObject();
                            result.put("probes", checkers);
                        } else {
                            checkers = result.getJSONObject("probes");
                        }
                        checkers.put(probes.get(i).getName(), healthcheckerJSON);
                        logger.info("putting checkers " + probes.get(i).getName());

                    }
                    result.put("registeredProbes", probes.size());

                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;

                    result.put("duration", elapsedTime + " ms");
                    result.put("status", currentStatus);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        writer.println(result.toString());
    }

}