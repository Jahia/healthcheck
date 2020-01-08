package org.jahia.modules.healthcheck.actions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.healthcheck.HealthcheckConstants;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveHealthcheckToken extends Action {

    private static final Logger logger = LoggerFactory.getLogger(RemoveHealthcheckToken.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        logger.info("Removing healthcheck token.");
        String token = httpServletRequest.getParameter(HealthcheckConstants.PARAM_TOKEN);

        if (token == null) {
            return null;
        }

        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH);

        if (!session.nodeExists(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS)) {
            session.getNode(HealthcheckConstants.PATH_SETTINGS).addNode(HealthcheckConstants.NODE_HEALTHCHECK_SETTINGS, HealthcheckConstants.NODE_TYPE_HEALTHCHECK_SETTINGS);
            session.save();
        }

        JCRValueWrapper[] values = session.getNode(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS).getProperty(HealthcheckConstants.PROP_TOKENS).getValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getString().equals(token)) {
                session.getNode(HealthcheckConstants.PATH_HEALTHCHECK_SETTINGS).getProperty(HealthcheckConstants.PROP_TOKENS).removeValue(values[i]);
            }
        }
        session.save();

        return null;
    }
}
