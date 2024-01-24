/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.browser;

import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrowsePeerMethods {
    private static final Log log = Log.getLog(BrowsePeerMethods.class);

    /**
     * Checks if request can be opened in SWT browser
     *
     * @return true if request is redirectable
     */
    public static boolean canBrowseInSWTBrowser() {
        DBPPreferenceStore store = DBeaverActivator.getInstance().getPreferences();
        boolean useEmbeddedAuth = store.getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH);
        if (!useEmbeddedAuth) {
            return false;
        }
        AtomicBoolean result = new AtomicBoolean(false);
        UIUtils.syncExec(() -> {
            boolean internalWebBrowserAvailable = PlatformUI.getWorkbench().getBrowserSupport()
                .isInternalWebBrowserAvailable();
            result.set(internalWebBrowserAvailable);
            if (!internalWebBrowserAvailable) {
                log.warn("Embedded Browser is disabled or unavailable");
            }
        });
        return result.get();
    }

    /**
     * Open URI via SWT browser API
     *
     * @param uri uri to open
     * @return was it opened successfully
     */
    public static boolean browseInSWTBrowser(URI uri) {
        DBPPreferenceStore store = DBeaverActivator.getInstance().getPreferences();
        if (store.getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH)) {
            AtomicBoolean result = new AtomicBoolean();
            UIUtils.syncExec(() -> {
                try {
                    BrowserPopup.openBrowser("redirect.auth", uri.toURL());
                    result.set(true);
                } catch (MalformedURLException e) {
                    log.warn("Error redirecting request to embedded browser", e);
                }
            });
            return result.get();
        } else {
            return false;
        }
    }
}
