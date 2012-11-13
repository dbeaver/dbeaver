package org.jkiss.dbeaver.ui.help;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.toc.HrefUtil;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.help.AbstractHelpUI;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.net.URL;

/**
 * Lightweight help UI
 */
public class LightweightHelpUI extends AbstractHelpUI {

    static final Log log = LogFactory.getLog(LightweightHelpUI.class);

    @Override
    public void displayHelp()
    {
    }

    @Override
    public void displayContext(IContext context, int x, int y)
    {
        try {
            IHelpResource[] relatedTopics = context.getRelatedTopics();
            if (CommonUtils.isEmpty(relatedTopics)) {
                return;
            }
            IHelpResource relatedTopic = relatedTopics[0];
            String topicRef = relatedTopic.getHref();
            String pluginID = HrefUtil.getPluginIDFromHref(topicRef);
            String topicPath = HrefUtil.getResourcePathFromHref(topicRef);
            Bundle plugin = Platform.getBundle(pluginID);
            URL bundleURL = FileLocator.find(plugin, new Path(topicPath), null);
            //URL platformURL = FileLocator.toFileURL(new URL("platform", "plugin", topicRef));
            URL fileURL = FileLocator.toFileURL(bundleURL);

//            URL platformURL = FileLocator.find(new URL("platform:/plugin" + topicRef));
//            URL fileURL = FileLocator.toFileURL(platformURL);
            getExternalBrowser().openURL(fileURL);

        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void displayHelpResource(String href)
    {
    }

    @Override
    public boolean isContextHelpDisplayed()
    {
        return false;
    }

    private IWebBrowser getExternalBrowser() throws PartInitException
    {
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
        return support.getExternalBrowser();
    }

}
