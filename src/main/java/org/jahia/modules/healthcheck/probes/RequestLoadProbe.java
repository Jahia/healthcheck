package org.jahia.modules.healthcheck.probes;

import org.jahia.modules.healthcheck.interfaces.Probe;
import org.jahia.utils.RequestLoadAverage;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;

@Component(service = Probe.class, immediate = true)
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
