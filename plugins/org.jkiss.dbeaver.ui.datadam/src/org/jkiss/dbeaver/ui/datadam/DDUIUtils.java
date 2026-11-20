/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CInfoLabel;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.net.URI;

public class DDUIUtils {
    private DDUIUtils() {
    }

    public static void openCredentialReset() {
        UIUtils.openWebBrowser(URI.create(DDEndpoints.getAccountBaseUrl()).resolve(DDUIConstants.SHARED_PROJECTS_PATH).toString());
    }

    public static void showNotification(
        @NotNull String title, @NotNull String message, @NotNull DBPMessageType type, @Nullable Runnable feedback
    ) {
        DBeaverNotifications.showNotification(DBeaverNotifications.NT_GENERIC, title, message, type, feedback);
    }

    public static void showError(@NotNull String title, @Nullable String message, @NotNull Throwable error) {
        UIUtils.syncExec(() -> new StandardErrorDialog(UIUtils.getActiveWorkbenchShell(), title, message,
            GeneralUtils.makeExceptionStatus(error), IStatus.ERROR).open());
    }

    @NotNull
    public static Label create(
        @NotNull Composite parent, @NotNull String text, @NotNull String linkText, @NotNull String linkUrl
    ) {
        CInfoLabel row = new CInfoLabel(parent, text, DBIcon.SMALL_INFO, 480);

        new Label(row, SWT.NONE);
        Link link = UIUtils.createLink(row, "<a>" + linkText + "</a>",
            SelectionListener.widgetSelectedAdapter(e -> UIUtils.openWebBrowser(linkUrl)));
        link.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
        return row.getTextLabel();
    }
}
