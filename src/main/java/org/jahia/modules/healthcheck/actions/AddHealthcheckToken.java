package org.jahia.modules.healthcheck.actions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.healthcheck.HealthcheckConstants;
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

        final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH);

        if (!session.nodeExists(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS)) {
            session.getNode(HealthcheckConstants.PATH_SETTINGS).addNode(HealthcheckConstants.NODE_HEALTHCHECK_SETTINGS, HealthcheckConstants.NODE_TYPE_HEALTHCHECK_SETTINGS);
            session.save();
        }

        final JCRNodeWrapper healthcheckSettings = session.getNode(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS);
        if (healthcheckSettings.hasProperty(HealthcheckConstants.PROP_TOKENS)) {
            healthcheckSettings.getProperty(HealthcheckConstants.PROP_TOKENS).addValue(generatedString);
        } else {
            healthcheckSettings.setProperty(HealthcheckConstants.PROP_TOKENS, new String[]{generatedString});
        }

        session.save();
        return null;
    }
}
