/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.help;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.toc.HrefUtil;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.help.AbstractHelpUI;
import org.jkiss.dbeaver.ui.UIUtils;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Lightweight help UI
 */
public class GitHubWikiHelpUI extends AbstractHelpUI {

    private static final Log log = Log.getLog(GitHubWikiHelpUI.class);
    public static final String GITHUB_HELP_ROOT = "https://github.com/serge-rider/dbeaver/wiki/";

    private boolean useHelpView = true;

    @Override
    public void displayHelp()
    {
        showHelpPage(GITHUB_HELP_ROOT);
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
            //Cut plugin ID from href
            while (topicRef.startsWith("/")) {
                topicRef = topicRef.substring(1);
            }
            int divPos = topicRef.indexOf('/');
            if (divPos != -1) {
                topicRef = topicRef.substring(divPos + 1);
            }
            showHelpPage(GITHUB_HELP_ROOT + topicRef);

        } catch (Exception e) {
            log.error(e);
        }
    }

/*
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
*/

    private void showHelpPage(String fileURL)
    {
        try {
            showHelpPage(new URL(fileURL));
        } catch (MalformedURLException e) {
            log.error("Bad help page URL", e);
        }
    }

    private void showHelpPage(URL fileURL)
    {
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
//        useHelpView = support.isInternalWebBrowserAvailable();
//
//        if (useHelpView) {
//            LightweightHelpView helpView = (LightweightHelpView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IActionConstants.HELP_VIEW_ID);
//            helpView.getBrowser().setUrl(fileURL.toString());
//        } else {
        try {
            support.getExternalBrowser().openURL(fileURL);
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(null, "Help system", "Can't open help in external browser", e);
        }
//        }
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
