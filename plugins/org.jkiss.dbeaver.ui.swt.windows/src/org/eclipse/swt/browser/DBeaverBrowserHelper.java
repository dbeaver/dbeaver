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

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.ICoreWebView2Cookie;
import org.eclipse.swt.internal.ole.win32.ICoreWebView2CookieList;
import org.eclipse.swt.internal.ole.win32.ICoreWebView2CookieManager;
import org.eclipse.swt.internal.win32.OS;

import java.util.Objects;

public class DBeaverBrowserHelper {
    private DBeaverBrowserHelper() {
    }

    public static void clearCookies(Browser browser) {
        Objects.requireNonNull(browser, "browser cannot be null");
        if (browser.webBrowser instanceof Edge) {
            clearCookiesEdgeImpl();
        } else {
            clearCookiesIEImpl();
        }
    }

    private static void clearCookiesEdgeImpl() {
        ICoreWebView2CookieManager manager = Edge.getCookieManager();
        if (manager == null) {
            return;
        }

        long[] ppv = new long[1];
        int hr = Edge.callAndWait(ppv, completion -> manager.GetCookies(null, completion));
        if (hr != COM.S_OK) {
            Edge.error(SWT.ERROR_NO_HANDLES, hr);
        }

        ICoreWebView2CookieList cookieList = new ICoreWebView2CookieList(ppv[0]);

        try {
            int[] count = new int[1];
            cookieList.get_Count(count);

            for (int i = 0; i < count[0]; i++) {
                hr = cookieList.GetValueAtIndex(i, ppv);
                if (hr != COM.S_OK) {
                    Edge.error(SWT.ERROR_NO_HANDLES, hr);
                }

                ICoreWebView2Cookie cookie = new ICoreWebView2Cookie(ppv[0]);
                manager.DeleteCookie(cookie);
                cookie.Release();
            }
        } finally {
            cookieList.Release();
            manager.Release();
        }

        // Bug in WebView2. DeleteCookie is asynchronous. Wait a short while for it to take effect.
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void clearCookiesIEImpl() {
        OS.InternetSetOption(0, OS.INTERNET_OPTION_END_BROWSER_SESSION, 0, 0);
    }
}
