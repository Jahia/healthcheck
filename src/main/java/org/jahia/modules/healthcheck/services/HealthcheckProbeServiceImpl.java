

package org.jahia.modules.healthcheck.services;

import java.util.ArrayList;
import java.util.List;
import org.jahia.bin.Jahia;
import org.jahia.exceptions.JahiaException;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.healthcheck.interfaces.HealthcheckProbeService;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;


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
        if (success) {
            logger.info(String.format("Unregistered %s in the Healthcheck Probe service", probe));
        } else {
            logger.error(String.format("Failed to unregister %s in the contentIntegrity service", probe));
        }
    }

    @Override
    public List<Probe> getProbes() {
        return probes;
    }
}
