package org.jahia.modules.healthcheck.servlet;

import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;


public class HealthcheckServlet implements BundleContextAware {

    public static final Logger logger = LoggerFactory.getLogger(HealthcheckServlet.class);

    HealthcheckJSONProducer simpleServlet;
    BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setSimpleServlet(HealthcheckJSONProducer simpleServlet) {
        this.simpleServlet = simpleServlet;
    }

    public void onBind(ServiceReference serviceReference) {
        ServiceReference realServiceReference = bundleContext.getServiceReference(HttpService.class.getName());
        HttpService httpService = (HttpService) bundleContext.getService(realServiceReference);
        try {
            httpService.registerServlet("/healthcheck", simpleServlet, null, null);
            logger.info("Successfully registered custom servlet at /modules/healthcheck");
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (NamespaceException e) {
            e.printStackTrace();
        }

    }

    public void onUnbind(ServiceReference serviceReference) {

        if (serviceReference == null) {
            return;
        }
        ServiceReference realServiceReference = bundleContext.getServiceReference(HttpService.class.getName());
        if (realServiceReference == null) {
            return;
        }
        HttpService httpService = (HttpService) bundleContext.getService(realServiceReference);
        if (httpService == null) {
            return;
        }
        httpService.unregister("/healthcheck");
        logger.info("Successfully unregistered custom servlet from /modules/healthcheck");
    }
}