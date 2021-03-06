/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.healthcheck;

import org.apache.jackrabbit.core.fs.FileSystem;

public class HealthcheckConstants {

    public static final String NODE_HEALTHCHECK_SETTINGS = "healthcheckSettings";
    public static final String NODE_SETTINGS = "settings";
    public static final String NODE_TYPE_HEALTHCHECK_SETTINGS = "jnt:healthcheckSettings";
    public static final String PARAM_TOKEN = "token";
    public static final String PARAM_SEVERITY = "severity";
    public static final String PATH_SETTINGS = FileSystem.SEPARATOR + HealthcheckConstants.NODE_SETTINGS;
    public static final String PATH_HEALTHCHECK_SETTINGS = PATH_SETTINGS + FileSystem.SEPARATOR + HealthcheckConstants.NODE_HEALTHCHECK_SETTINGS;
    public static final String PROP_HEALTHCHECK_TOKEN = "healthcheck.token";
    public static final String PROP_HEALTHCHECK_PROBE_SEVERITY_PARAMETER = "probe.%s.severity"; // where %s is the classname of the probe
    public static final String PROP_TOKENS = "tokens";
    public static final String STATUS_GREEN = "GREEN";
    public static final String STATUS_RED = "RED";
    public static final String STATUS_YELLOW = "YELLOW";
    public static final int PROBE_SEVERITY_CRITICAL = 0;
    public static final int PROBE_SEVERITY_HIGH = 1;
    public static final int PROBE_SEVERITY_MEDIUM = 2;
    public static final int PROBE_SEVERITY_LOW = 3;
    public static final String PROBE_SEVERITY_CRITICAL_LABEL = "CRITICAL";
    public static final String PROBE_SEVERITY_HIGH_LABEL = "HIGH";
    public static final String PROBE_SEVERITY_MEDIUM_LABEL = "MEDIUM";
    public static final String PROBE_SEVERITY_LOW_LABEL = "LOW";

    private HealthcheckConstants() {
    }

}
