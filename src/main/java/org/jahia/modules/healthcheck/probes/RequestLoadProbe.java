package org.jahia.modules.healthcheck.probes;

import org.jahia.modules.healthcheck.Probe;
import org.jahia.utils.RequestLoadAverage;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestLoadProbe implements Probe {
    @Override
    public String getStatus() {
        return "GREEN";
    }

    @Override
    public JSONObject getData() {
        JSONObject loadAverageJson = new JSONObject();
        try {
            loadAverageJson.put("currentRequestLoad",  RequestLoadAverage.getInstance().getCount());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return loadAverageJson;
    }

    @Override
    public String getName() {
        return "RequestLoad";
    }
}
