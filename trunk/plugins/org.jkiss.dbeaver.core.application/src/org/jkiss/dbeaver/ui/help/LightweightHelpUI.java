package org.jkiss.dbeaver.ui.help;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.help.AbstractHelpUI;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.application.ApplicationWorkbenchWindowAdvisor;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
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
            //URL entry = DBeaverActivator.getInstance().getBundle().getEntry(topicRef);
            URL platformURL = FileLocator.find(new URL("platform:/plugin" + topicRef));
            URL fileURL = FileLocator.toFileURL(platformURL);
            getExternalBrowser().openURL(fileURL);

        } catch (PartInitException e) {
            log.error(e);
        } catch (MalformedURLException e) {
            log.error(e);
        } catch (IOException e) {
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
