package org.jahia.modules.healthcheck;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

public interface Probe {
    String getStatus ();
    JSONObject getData ();
    String getName();
}
