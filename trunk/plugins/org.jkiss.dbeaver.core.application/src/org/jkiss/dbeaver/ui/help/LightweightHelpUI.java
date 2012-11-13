package org.jkiss.dbeaver.ui.help;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

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

            // Cache all html content
            {
                int divPos = topicPath.indexOf("/html/");
                if (divPos != -1) {
                    String rootPath = topicPath.substring(0, divPos + 5);
                    cacheContent(plugin, rootPath);
                }
            }

            URL bundleURL = plugin.getEntry(topicPath);
            if (bundleURL != null) {
                URL fileURL = FileLocator.toFileURL(bundleURL);
                getExternalBrowser().openURL(fileURL);
            }

        } catch (Exception e) {
            log.error(e);
        }
    }

    private void cacheContent(Bundle plugin, String filePath) throws IOException
    {
        Enumeration<String> entryPaths = plugin.getEntryPaths(filePath);
        if (entryPaths == null) {
            // It is a file
            URL bundleURL = plugin.getEntry(filePath);
            if (bundleURL != null) {
                FileLocator.toFileURL(bundleURL);
            }
            return;
        }
        while (entryPaths.hasMoreElements()) {
            cacheContent(plugin, entryPaths.nextElement());
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
