package org.jahia.modules.healthcheck.actions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.healthcheck.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddHealthcheckToken extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddHealthcheckToken.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        LOGGER.info("Adding new healthcheck token.");
        final int length = 25;
        final boolean useLetters = true;
        final boolean useNumbers = false;
        final String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);

        final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession("default", Locale.ENGLISH, Locale.ENGLISH);

        if (!session.nodeExists("/settings/" + Constants.NODE_HEALTHCHECK_SETTINGS)) {
            session.getNode("/settings").addNode(Constants.NODE_HEALTHCHECK_SETTINGS, "jnt:healthcheckSettings");
            session.save();
        }

        final JCRNodeWrapper healthcheckSettings = session.getNode("/settings/" + Constants.NODE_HEALTHCHECK_SETTINGS);
        if (healthcheckSettings.hasProperty(Constants.PROP_TOKENS)) {
            healthcheckSettings.getProperty(Constants.PROP_TOKENS).addValue(generatedString);
        } else {
            healthcheckSettings.setProperty(Constants.PROP_TOKENS, new String[]{generatedString});
        }

        session.save();
        return null;
    }
}
