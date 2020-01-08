package org.jahia.modules.healthcheck.probes;

import java.io.File;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.apache.jackrabbit.core.version.InternalXAVersionManager;
import org.jahia.modules.healthcheck.HealthcheckConstants;
import org.jahia.modules.healthcheck.interfaces.Probe;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Probe.class, immediate = true)
public class DatastoreProbe implements Probe {

    private static final Logger logger = LoggerFactory.getLogger(DatastoreProbe.class);

    @Override
    public String getStatus() {
        if (isDbPersistenceManager()) {
            return HealthcheckConstants.STATUS_GREEN;
        }
        final String datastoreHome = System.getProperty("jahia.jackrabbit.datastore.path");
        return (new File(datastoreHome)).canWrite() ? HealthcheckConstants.STATUS_GREEN : HealthcheckConstants.STATUS_RED;
    }

    @Override
    public JSONObject getData() {
        return null;
    }

    @Override
    public String getName() {
        return "Datastore";
    }

    private boolean isDbPersistenceManager() {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, null, null, new JCRCallback<Boolean>() {
                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    final SessionImpl providerSession = (SessionImpl) session.getProviderSession(session.getNode("/").getProvider());
                    final InternalVersionManager vm = providerSession.getInternalVersionManager();
                    PersistenceManager pm = null;
                    if (vm instanceof InternalVersionManagerImpl) {
                        pm = ((InternalVersionManagerImpl) vm).getPersistenceManager();
                    } else if (vm instanceof InternalXAVersionManager) {
                        pm = ((InternalXAVersionManager) vm).getPersistenceManager();
                    } else {
                        logger.warn("Unknown implemmentation of the InternalVersionManager: {}.", vm.getClass().getName());
                        return false;
                    }
                    return pm instanceof BundleDbPersistenceManager;
                }
            });
        } catch (RepositoryException e) {
            logger.error("", e);
            return false;
        }
    }
}
