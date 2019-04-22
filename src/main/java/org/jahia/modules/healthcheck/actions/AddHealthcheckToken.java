package org.jahia.modules.healthcheck.actions;

import org.apache.commons.lang.RandomStringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.healthcheck.probes.DatastoreProbe;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddHealthcheckToken extends Action {
    private static final Logger logger = LoggerFactory.getLogger(AddHealthcheckToken.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        logger.info("Adding new healthcheck token.");
        int length = 25;
        boolean useLetters = true;
        boolean useNumbers = false;
        String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);

        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession("default", new Locale("en"),new Locale("en"));

        if (!session.nodeExists("/settings/healthcheckSettings")) {
            session.getNode("/settings").addNode("healthcheckSettings", "jnt:healthcheckSettings");
            session.save();
        }

        session.getNode("/settings/healthcheckSettings").getProperty("tokens").addValue(generatedString);
        session.save();

        return null;
    }
}
