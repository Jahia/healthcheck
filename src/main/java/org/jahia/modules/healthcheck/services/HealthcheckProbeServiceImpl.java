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

package org.jahia.modules.healthcheck.services;

import org.jahia.exceptions.JahiaException;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.healthcheck.interfaces.HealthcheckProbeService;


import org.jahia.modules.healthcheck.interfaces.Probe;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.framework.Constants;
import org.jahia.bin.Jahia;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Component(name = "org.jahia.modules.healthcheck.service", service = HealthcheckProbeService.class, property = {
        Constants.SERVICE_PID + "=org.jahia.modules.healthcheck.service",
        Constants.SERVICE_DESCRIPTION + "=Healthcheck Probe Service",
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME}, immediate = true)
public class HealthcheckProbeServiceImpl implements HealthcheckProbeService {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(HealthcheckProbeServiceImpl.class);
    private List<Probe> probes = new ArrayList<>();

    @Activate
    public void start() throws JahiaInitializationException {
        logger.info("Healthcheck service started");
    }

    @Deactivate
    public void stop() throws JahiaException {
        logger.info("Healthcheck service stopped");
    }

    @Reference(service = Probe.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "unregisterProbe")
    public void registerProbe(Probe probe) {
        probes.add(probe);
        logger.info(String.format("Registered %s in the Healthcheck Probe service, service: %s, CL: %s", probe, this, this.getClass().getClassLoader()));
    }

    public void unregisterProbe(Probe probe) {
        final boolean success = probes.remove(probe);
        if (success)
            logger.info(String.format("Unregistered %s in the Healthcheck Probe service", probe));
        else
            logger.error(String.format("Failed to unregister %s in the contentIntegrity service", probe));
    }

    @Override
    public List<Probe> getProbes() {
        return probes;
    }
}
