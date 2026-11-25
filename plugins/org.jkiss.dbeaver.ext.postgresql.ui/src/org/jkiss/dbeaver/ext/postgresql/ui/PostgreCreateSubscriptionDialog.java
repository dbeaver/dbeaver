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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithReturn;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseTitleDialog;
import org.jkiss.dbeaver.ui.dialogs.IDialogPageContainer;

public class PostgreCreateSubscriptionDialog extends BaseTitleDialog implements IDialogPageContainer {
    private final PostgreCreateSubscriptionPage page;
    private final DBRRunnableWithReturn<Boolean> onConfirm;
    private boolean saving;

    public PostgreCreateSubscriptionDialog(
        @NotNull Shell parentShell,
        @NotNull PostgreSubscription subscription,
        @NotNull DBRRunnableWithReturn<Boolean> onConfirm
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
        setTitle(page.getStepTitle());
        setMessage(page.getStepDescription());
        Composite area = super.createDialogArea(parent);
        GridLayout areaLayout = (GridLayout) area.getLayout();
        areaLayout.marginWidth = 0;
        areaLayout.marginHeight = 0;
        Composite content = UIUtils.createComposite(area, 1);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout layout = (GridLayout) content.getLayout();
        layout.marginWidth = PostgreSubscriptionDialogLayout.ROW_SPACING;
        layout.marginHeight = PostgreSubscriptionDialogLayout.ROW_SPACING;
        page.createControl(content);
        return area;
    }

    @NotNull
    @Override
    protected Control createContents(@NotNull Composite parent) {
        Control contents = super.createContents(parent);
        page.validateProperties();
        UIUtils.asyncExec(() -> {
            Shell shell = getShell();
            if (shell != null && !shell.isDisposed()) {
                shell.forceActive();
            }
        });
        return contents;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.BACK_ID, IDialogConstants.BACK_LABEL, false);
        createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.FINISH_ID, IDialogConstants.FINISH_LABEL, false);
        updateButtons();
    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        getButton(IDialogConstants.NEXT_ID).moveBelow(getButton(IDialogConstants.BACK_ID));
        getButton(IDialogConstants.CANCEL_ID).moveBelow(getButton(IDialogConstants.NEXT_ID));
        getButton(IDialogConstants.FINISH_ID).moveBelow(getButton(IDialogConstants.CANCEL_ID));
        getButton(IDialogConstants.FINISH_ID).getParent().layout();
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (saving) {
            return;
        }
        if (buttonId == IDialogConstants.BACK_ID || buttonId == IDialogConstants.NEXT_ID) {
            page.changeStep(buttonId == IDialogConstants.NEXT_ID);
        } else if (buttonId == IDialogConstants.FINISH_ID) {
            okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void okPressed() {
        if (saving || !page.isPageComplete()) {
            return;
        }
        try {
            page.performFinish();
            saving = true;
            page.getControl().setEnabled(false);
            updateButtons();
            if (Boolean.TRUE.equals(UIUtils.runWithDialog(onConfirm))) {
                saving = false;
                super.okPressed();
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(PostgreMessages.dialog_create_subscription_title, null, e);
        } finally {
            saving = false;
            Shell shell = getShell();
            if (shell != null && !shell.isDisposed()) {
                page.getControl().setEnabled(true);
                updateButtons();
                shell.forceActive();
            }
        }
    }

    @Override
    public boolean close() {
        return !saving && super.close();
    }

    @Override
    public void updateMessage() {
        setTitle(page.getStepTitle());
        setMessage(page.getStepDescription());
        setErrorMessage(page.getStepError());
        updateButtons();
    }

    @Override
    public void updateButtons() {
        Button finish = getButton(IDialogConstants.FINISH_ID);
        if (finish != null) {
            finish.setEnabled(!saving && page.isPageComplete());
            Button next = getButton(IDialogConstants.NEXT_ID);
            next.setEnabled(!saving && page.hasNextStep() && page.canAdvance());
            getButton(IDialogConstants.BACK_ID).setEnabled(!saving && page.hasPreviousStep());
            getButton(IDialogConstants.CANCEL_ID).setEnabled(!saving);
            getShell().setDefaultButton(page.hasNextStep() ? next : finish);
        }
    }
}
