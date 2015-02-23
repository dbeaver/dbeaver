/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.help;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.toc.HrefUtil;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.help.AbstractHelpUI;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Lightweight help UI
 */
public class LightweightHelpUI extends AbstractHelpUI {

    static final Log log = Log.getLog(LightweightHelpUI.class);

    private boolean useHelpView = true;

    @Override
    public void displayHelp()
    {
        try {
            showHelpPage("org.jkiss.dbeaver.core", "/org.jkiss.dbeaver.core/docs/help/html/toc.html");
        } catch (IOException e) {
            log.error(e);
        } catch (PartInitException e) {
            log.error(e);
        }
    }

    @Override
    public void displayContext(IContext context, int x, int y)
    {
        try {
            IHelpResource[] relatedTopics = context.getRelatedTopics();
            if (relatedTopics == null || relatedTopics.length == 0) {
                return;
            }
            IHelpResource relatedTopic = relatedTopics[0];
            String topicRef = relatedTopic.getHref();
            String pluginID = HrefUtil.getPluginIDFromHref(topicRef);
            showHelpPage(pluginID, topicRef);

        } catch (Exception e) {
            log.error(e);
        }
    }

    private void showHelpPage(String pluginID, String topicRef) throws IOException, PartInitException
    {
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
            showHelpPage(fileURL);
        }
    }

    private void showHelpPage(URL fileURL) throws PartInitException
    {
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
        useHelpView = support.isInternalWebBrowserAvailable();

        if (useHelpView) {
            LightweightHelpView helpView = (LightweightHelpView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IActionConstants.HELP_VIEW_ID);
            helpView.getBrowser().setUrl(fileURL.toString());
        } else {
            support.getExternalBrowser().openURL(fileURL);
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

}
