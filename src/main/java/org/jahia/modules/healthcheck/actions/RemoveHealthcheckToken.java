package org.jahia.modules.healthcheck.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RemoveHealthcheckToken extends Action {
    private static final Logger logger = LoggerFactory.getLogger(RemoveHealthcheckToken.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        logger.info("Removing healthcheck token.");
        int length = 25;
        boolean useLetters = true;
        boolean useNumbers = false;
        String token = httpServletRequest.getParameter("token");

        if (token == null) {
            return null;
        }

        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentSystemSession("default", new Locale("en"),new Locale("en"));

        if (!session.nodeExists("/settings/healthcheckSettings")) {
            session.getNode("/settings").addNode("healthcheckSettings", "jnt:healthcheckSettings");
            session.save();
        }

        JCRValueWrapper[] values = session.getNode("/settings/healthcheckSettings").getProperty("tokens").getValues();
        for (int i = 0 ; i < values.length; i++) {
            if (values[i].getString().equals(token)) {
                session.getNode("/settings/healthcheckSettings").getProperty("tokens").removeValue(values[i]);
            }
        }
        session.save();

        return null;
    }
}
