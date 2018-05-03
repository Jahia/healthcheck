package org.jahia.modules.healthcheck.servlet;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.http.HttpService;
import org.jahia.modules.healthcheck.Probe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        PrintWriter writer = resp.getWriter();
        long startTime = System.currentTimeMillis();

        JSONObject result = new JSONObject();
        try {
            String currentStatus = "GREEN";
            for (int i=0; healthcheckers.size() > i; i++) {
                JSONObject healthcheckerJSON = new JSONObject();
                healthcheckerJSON.put("status", healthcheckers.get(i).getStatus());

                if (healthcheckers.get(i).getStatus().equals("YELLOW") && currentStatus.equals("GREEN")) {
                    currentStatus = "YELLOW";
                }
                if (healthcheckers.get(i).getStatus().equals("RED")  &&  (currentStatus.equals("GREEN") || currentStatus.equals("YELLOW"))) {
                    currentStatus = "RED";
                }
                healthcheckerJSON.put("data", healthcheckers.get(i).getData());
                System.out.println("JSONObject: " + healthcheckerJSON.toString());
                JSONObject checkers;
                if (!result.has("probes"))  {
                    logger.info("creating checkers");
                    checkers = new JSONObject();
                    result.put("probes", checkers);
                } else {
                    checkers = result.getJSONObject("probes");
                }
                checkers.put(healthcheckers.get(i).getName(), healthcheckerJSON);
                logger.info("putting checkers " + healthcheckers.get(i).getName());

            }
            result.put("registeredProbesNb", healthcheckers.size());

            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;

            result.put("duration", elapsedTime + " ms");
            result.put("status", currentStatus);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        writer.println(result.toString());
    }

    public List<Probe> getHealthcheckers() {
        return healthcheckers;
    }

    public void setHealthcheckers(List<Probe> healthcheckers) {
        this.healthcheckers = healthcheckers;
    }
}