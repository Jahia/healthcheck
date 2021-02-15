package org.jahia.modules.healthcheck.config;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HealthcheckConfigProvider implements ManagedServiceFactory, InitializingBean {

    private Map<String, String> trialConfig = new HashMap<>();
    private static transient Logger logger = org.slf4j.LoggerFactory.getLogger(HealthcheckConfigProvider.class);

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public String getProperty (String name) {
        return trialConfig.get(name);
    }

    public String getName() {
        return null;
    }

    @Override
    public void updated(String s, Dictionary<String, ?> dictionary) throws ConfigurationException {
        trialConfig.clear();
        if (dictionary != null) {
            Enumeration<String> keys = dictionary.keys();
            logger.info("keys:" + keys.toString());
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (StringUtils.startsWith(key, "healthcheck.")) {
                    String subKey = StringUtils.substringAfter(key, "healthcheck.");
                    String name = StringUtils.substringBefore(subKey, ".");
                    if (!trialConfig.containsKey(name)) {
                        trialConfig.put(subKey, (String) dictionary.get(key));
                    }
                }
            }
            logger.info("Healthcheck config configuration reloaded");
        }
    }

    @Override
    public void deleted(String s) {

    }
}