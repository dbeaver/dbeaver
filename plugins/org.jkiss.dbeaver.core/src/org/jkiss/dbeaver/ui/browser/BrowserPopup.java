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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BrowserPopup extends AbstractPopupPanel {
    private static final Log log = Log.getLog(BrowserPopup.class);
    private static final Map<String, BrowserPopup> browserRegistry = new HashMap<>();

    private final String id;
    private final URL url;
    private Browser browser;

    public static BrowserPopup openBrowser(@NotNull String id, @NotNull URL url) {
        BrowserPopup browser = browserRegistry.get(id);
        if (browser != null && !browser.getContents().isDisposed()) {
            browser.browser.setUrl(url.toString());
            return browser;
        } else {
            BrowserPopup browserPopup = new BrowserPopup(UIUtils.getActiveShell(), id, url);
            browserPopup.open();
            return browserPopup;
        }
    }
    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 500;
        gd.widthHint = 800;
        composite.setLayoutData(gd);
        try {
            browser = new Browser(composite, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 600;
            gd.heightHint = 500;
            browser.setLayoutData(gd);
            browserRegistry.put(id, this);
            browser.setUrl(url.toString());
        } catch (SWTError e) {
            log.error("Could not instantiate Browser", e);
        }
        return composite;
    }

    private BrowserPopup(@NotNull Shell parentShell, @NotNull String id, @NotNull URL url) {
        super(parentShell, "Browser");
        this.id = id;
        this.url = url;
        setModeless(false);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE);
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent, int alignment) {
        if (alignment == SWT.LEFT) {
            createButton(parent, IDialogConstants.DETAILS_ID, CoreMessages.popup_open_browser_open_external_browser, false);
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            ShellUtils.launchProgram(url.toString());
            close();
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public boolean close() {
        browserRegistry.remove(id);
        return super.close();
    }
}
