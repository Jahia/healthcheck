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
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ===================================================================================
 *
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 * Alternatively, commercial and supported versions of the program - also known as Enterprise Distributions - must be
 * used in accordance with the terms and conditions contained in a separate written agreement between you and Jahia
 * Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.healthcheck.probes;

import org.jahia.modules.healthcheck.HealthcheckConstants;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.jahia.utils.JCRSessionLoadAverage;
import org.jahia.utils.RequestLoadAverage;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Probe.class, immediate = true)
public class RequestLoadProbe implements Probe {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoadProbe.class);

    JSONObject loadAverageJson = new JSONObject();

    @Override
    public String getStatus() {
        loadAverageJson = new JSONObject();
        try {
            loadAverageJson.put("oneMinuteRequestLoadAverage", RequestLoadAverage.getInstance().getOneMinuteLoad());
            loadAverageJson.put("oneMinuteCurrentSessionLoad", JCRSessionLoadAverage.getInstance().getOneMinuteLoad());
        } catch (JSONException ex) {
            LOGGER.error("Impossible to generate the JSON", ex);
        }
        try {
            if (loadAverageJson.getInt("oneMinuteRequestLoadAverage") < 40 && loadAverageJson.getInt("oneMinuteCurrentSessionLoad") < 40) {
                return HealthcheckConstants.STATUS_GREEN;
            }
            if (loadAverageJson.getInt("oneMinuteRequestLoadAverage") < 70 && loadAverageJson.getInt("oneMinuteCurrentSessionLoad") < 70) {
                return HealthcheckConstants.STATUS_YELLOW;
            }

        } catch (JSONException ex) {
            LOGGER.error("Impossible to read the JSON", ex);
        }
        return HealthcheckConstants.STATUS_RED;
    }

    @Override
    public JSONObject getData() {
        return loadAverageJson;
    }

    @Override
    public String getName() {
        return "ServerLoad";
    }
}
