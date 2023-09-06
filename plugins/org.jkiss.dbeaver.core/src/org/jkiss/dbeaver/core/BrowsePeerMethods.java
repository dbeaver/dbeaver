/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.core;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrowsePeerMethods {
    public static boolean canBrowseInSWTBrowser() {
        DBPPreferenceStore store = new BundlePreferenceStore(DBeaverActivator.getInstance().getBundle());
        boolean useEmbeddedAuth = store.getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH);
        if (!useEmbeddedAuth) {
            return false;
        }
        AtomicBoolean result = new AtomicBoolean(false);
        UIUtils.syncExec(() -> {
            result.set(PlatformUI.getWorkbench().getBrowserSupport().isInternalWebBrowserAvailable());
        });
        return result.get();
    }

    public static boolean browseInSWTBrowser(URI uri) {
        DBPPreferenceStore store = new BundlePreferenceStore(DBeaverActivator.getInstance().getBundle());
        if (store.getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH)) {
            AtomicBoolean result = new AtomicBoolean();
            UIUtils.syncExec(() -> {
                IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
                boolean internalWebBrowserAvailable = browserSupport.isInternalWebBrowserAvailable();
                if (internalWebBrowserAvailable) {
                    try {
                        // Be careful near URI
                        IWebBrowser browser = browserSupport.createBrowser(
                            IWorkbenchBrowserSupport.AS_EDITOR,
                            uri.getHost(),
                            "Browser",
                            uri.toURL().toString()
                        );
                        browser.openURL(uri.toURL());
                        result.set(true);
                    } catch (PartInitException | MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            });
            return result.get();
        } else {
            return false;
        }
    }

}
