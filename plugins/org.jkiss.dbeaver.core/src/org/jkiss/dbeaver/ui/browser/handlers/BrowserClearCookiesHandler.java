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
package org.jkiss.dbeaver.ui.browser.handlers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.BeanUtils;

public class BrowserClearCookiesHandler implements DBRRunnableWithProgress {
    // BUG: Fragment can't be resolved correctly by our Intellij IDEA workspace generator
    private static final String BROWSER_HELPER_CLASS_NAME = "org.eclipse.swt.browser.DBeaverBrowserHelper";
    private static final String BROWSER_HELPER_CLASS_CLEAR_COOKIES_NAME = "clearCookies";

    private static final Log log = Log.getLog(BrowserClearCookiesHandler.class);

    @Override
    public void run(DBRProgressMonitor monitor) {
        UIUtils.syncExec(BrowserClearCookiesHandler::clearCookies);
    }

    private static void clearCookies() {
        Shell shell = new Shell(SWT.MODELESS);
        Browser browser = null;
        try {
            browser = new Browser(shell, SWT.NONE);
            BeanUtils.invokeStaticMethod(
                Class.forName(BROWSER_HELPER_CLASS_NAME),
                BROWSER_HELPER_CLASS_CLEAR_COOKIES_NAME,
                new Class[]{Browser.class},
                new Object[]{browser}
            );
        } catch (Throwable e) {
            log.error("Error clearing cookies", e);
        } finally {
            if (browser != null) {
                browser.dispose();
            }
            shell.dispose();
        }
    }
}
