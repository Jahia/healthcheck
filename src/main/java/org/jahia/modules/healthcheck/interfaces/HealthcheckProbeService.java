package org.jahia.modules.healthcheck.interfaces;

import java.util.List;

public interface HealthcheckProbeService {
    public List<Probe> getProbes();
}
