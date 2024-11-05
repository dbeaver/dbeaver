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

import org.eclipse.swt.internal.cocoa.NSArray;
import org.eclipse.swt.internal.cocoa.NSHTTPCookie;
import org.eclipse.swt.internal.cocoa.NSHTTPCookieStorage;

import java.util.Objects;

public class DBeaverBrowserHelper {
    private DBeaverBrowserHelper() {
    }

    public static void clearCookies(Browser browser) {
        Objects.requireNonNull(browser, "browser cannot be null");
        NSHTTPCookieStorage storage = NSHTTPCookieStorage.sharedHTTPCookieStorage();
        NSArray cookies = storage.cookies();
        int count = (int) cookies.count();
        for (int i = 0; i < count; i++) {
            NSHTTPCookie cookie = new NSHTTPCookie(cookies.objectAtIndex(i));
            storage.deleteCookie(cookie);
        }
    }
}
