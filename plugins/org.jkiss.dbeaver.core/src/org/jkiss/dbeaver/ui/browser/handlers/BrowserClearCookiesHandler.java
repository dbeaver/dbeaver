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
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;

public class BrowserClearCookiesHandler implements DBRRunnableWithProgress {
    private static final Log log = Log.getLog(BrowserClearCookiesHandler.class);

    @Override
    public void run(DBRProgressMonitor monitor) {
        UIUtils.syncExec(() -> {
            Shell shell = new Shell(SWT.MODELESS);
            Browser browser = null;

            try {
                browser = new Browser(UIUtils.getActiveWorkbenchShell(), SWT.NONE);
                Browser.clearSessions();
            } catch (SWTException e) {
                log.error("Error clearing browser cookies", e);
            } finally {
                if (browser != null) {
                    browser.dispose();
                }
                shell.dispose();
            }
        });
    }
}
