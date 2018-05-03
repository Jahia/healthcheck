package org.jahia.modules.healthcheck.probes;

import org.jahia.modules.healthcheck.Probe;
import org.jahia.modules.healthcheck.servlet.HealthcheckJSONProducer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteHTMLProbe implements Probe {

    private Logger logger = LoggerFactory.getLogger(SiteHTMLProbe.class);
    // URL is the key, then "protocol, patterns."
    private Map<String, Map<String, Object>> urlList = new HashMap<String, Map<String, Object>>();
    private Map<String, Boolean> data = new HashMap<String, Boolean>();


    private JSONObject siteHTMLJson;
    private boolean status;

    public SiteHTMLProbe () {
        doCheck();
    }


    @Override
    public String getStatus() {
        loadURLs();
        doCheck();

        if (status) {
            return "GREEN";
        } else {
            return "RED";
        }
    }

    @Override
    public JSONObject getData() {
        siteHTMLJson = new JSONObject();
        try {
            siteHTMLJson.put("digitall-home-page-navmenu",  true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return siteHTMLJson;
    }

    @Override
    public String getName() {
        return "SiteHTMLProbe";
    }

    private void loadURLs() {
        HashMap <String, Object> parameters = new  HashMap <String, Object>();
        parameters.put("protocol", "http");
        List<String> patterns = new ArrayList<String>();
        patterns.add("navbar mega-menu");
        parameters.put("patterns", "navbar mega-menu");
        urlList.put("127.0.0.1:8080/sites/digitall/home.html", parameters);
    }

    private boolean doCheck () {
        URL url = null;
            for(Map.Entry<String, Map<String, Object>> entry : urlList.entrySet()) {
                String urlString = entry.getKey();
                Map<String, Object> parameters = entry.getValue();
                String protocol = (String) parameters.get("protocol");
                String patterns = (String) parameters.get("patterns");

                try {
                    url = new URL(protocol+"://"+urlString);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    if (content.toString().contains(patterns)) {
                        status = true;
                    } else {
                        status = false;
                    }
                    in.close();


                    data.put(protocol+"://"+urlString, status);

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        return status;
    }
}
