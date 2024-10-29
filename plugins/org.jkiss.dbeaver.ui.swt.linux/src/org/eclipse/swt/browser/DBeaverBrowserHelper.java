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
package org.eclipse.swt.browser;

import org.eclipse.swt.internal.webkit.WebKitGTK;
import org.jkiss.dbeaver.Log;

import java.util.Objects;

public class DBeaverBrowserHelper {
    private static final Log log = Log.getLog(DBeaverBrowserHelper.class);

    private DBeaverBrowserHelper() {
    }

    public static void clearCookies(Browser browser) {
        Objects.requireNonNull(browser, "browser cannot be null");
        if (!WebKitGTK.LibraryLoaded) {
            return;
        }
        if (WebKitGTK.webkit_get_minor_version() < 16) {
            log.warn("SWT WebKit: clear sessions only supported on WebKitGtk version 2.16 and above.");
            return;
        }
        long context = WebKitGTK.webkit_web_context_get_default();
        long manager = WebKitGTK.webkit_web_context_get_website_data_manager(context);
        WebKitGTK.webkit_website_data_manager_clear(manager, WebKitGTK.WEBKIT_WEBSITE_DATA_COOKIES, 0, 0, 0, 0);
    }
}
