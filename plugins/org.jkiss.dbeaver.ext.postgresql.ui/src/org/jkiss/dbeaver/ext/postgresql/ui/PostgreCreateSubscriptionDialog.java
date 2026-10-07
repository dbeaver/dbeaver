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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseTitleDialog;
import org.jkiss.dbeaver.ui.dialogs.IDialogPageContainer;

/**
 * Modeless subscription configuration. The caller starts persistence only after confirmation.
 */
public class PostgreCreateSubscriptionDialog extends BaseTitleDialog implements IDialogPageContainer {
    private final PostgreCreateSubscriptionPage page;
    private final Runnable onConfirm;

    public PostgreCreateSubscriptionDialog(
        @NotNull Shell parentShell,
        @NotNull PostgreSubscription subscription,
        @NotNull Runnable onConfirm
    ) {
        super(parentShell, null);
        setShellStyle(SWT.SHELL_TRIM | SWT.RESIZE | SWT.MODELESS);
        setBlockOnOpen(false);
        page = new PostgreCreateSubscriptionPage(subscription);
        page.setContainer(this);
        this.onConfirm = onConfirm;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        getShell().setText(page.getTitle());
        setTitle(page.getTitle());
        Composite area = super.createDialogArea(parent);
        Composite content = UIUtils.createComposite(area, 1);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));
        page.createControl(content);
        return area;
    }

    @NotNull
    @Override
    protected Control createContents(@NotNull Composite parent) {
        Control contents = super.createContents(parent);
        page.validateProperties();
        return contents;
    }

    @Override
    protected void okPressed() {
        try {
            page.performFinish();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(PostgreMessages.dialog_create_subscription_title, null, e);
            return;
        }
        onConfirm.run();
        super.okPressed();
    }

    @Override
    public void updateMessage() {
        setErrorMessage(page.getErrorMessage());
        updateButtons();
    }

    @Override
    public void updateButtons() {
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(page.isPageComplete());
        }
    }
}
