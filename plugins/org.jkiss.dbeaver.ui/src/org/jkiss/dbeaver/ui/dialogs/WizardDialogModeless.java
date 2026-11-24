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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.FilteredPreferenceDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/** Opens the workbench preferences in a modeless dialog with the requested page selected. */
public class WizardDialogModeless extends FilteredPreferenceDialog {
    private static WizardDialogModeless openDialog;

    public WizardDialogModeless(@Nullable Shell parentShell, @Nullable String pageId) throws DBException {
        super(parentShell, PlatformUI.getWorkbench().getPreferenceManager());
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MODELESS);
        setBlockOnOpen(false);
        if (pageId != null) {
            if (findNodeMatching(pageId) == null) {
                throw new DBException("Preference page not found: " + pageId);
            }
            setSelectedNode(pageId);
        }
    }

    public static void showPage(@Nullable Shell parentShell, @Nullable String pageId) throws DBException {
        var dialog = openDialog;
        if (dialog == null) {
            dialog = new WizardDialogModeless(parentShell, pageId);
            dialog.open();
        } else {
            if (pageId != null) {
                if (dialog.findNodeMatching(pageId) == null) {
                    throw new DBException("Preference page not found: " + pageId);
                }
                dialog.setCurrentPageId(pageId);
            }
            dialog.getShell().forceActive();
        }
    }

    @Override
    protected void configureShell(@NotNull Shell shell) {
        super.configureShell(shell);
        openDialog = this;
        shell.addDisposeListener(event -> {
            if (openDialog == this) {
                openDialog = null;
            }
        });
    }
}
