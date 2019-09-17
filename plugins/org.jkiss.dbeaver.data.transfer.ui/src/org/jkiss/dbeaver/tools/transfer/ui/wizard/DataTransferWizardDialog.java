/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * Data transfer wizard dialog
 */
public class DataTransferWizardDialog extends ActiveWizardDialog {

    private static final int PROFILE_CONFIG_BTN_ID = 1000;
    private Combo profileCombo;

    private DataTransferWizardDialog(IWorkbenchWindow window, DataTransferWizard wizard) {
        this(window, wizard, null);
    }

    private DataTransferWizardDialog(IWorkbenchWindow window, DataTransferWizard wizard, IStructuredSelection selection) {
        super(window, wizard, selection);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation());

        setHelpAvailable(false);
    }

    @Override
    protected DataTransferWizard getWizard() {
        return (DataTransferWizard) super.getWizard();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        {
            boolean nativeClientRequired = getWizard().isProfileSelectorVisible();
            if (nativeClientRequired) {
                parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                {
                    Composite configPanel = UIUtils.createComposite(parent, 3);
                    configPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                    profileCombo = UIUtils.createLabelCombo(configPanel, "Preset", SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
                    ((GridData)profileCombo.getLayoutData()).widthHint = UIUtils.getFontHeight(profileCombo) * 16;

                    ToolBar editToolbar = new ToolBar(configPanel, SWT.HORIZONTAL);
                    ToolItem editItem = new ToolItem(editToolbar, SWT.PUSH);
                    editItem.setImage(DBeaverIcons.getImage(UIIcon.EDIT));
                    editItem.setToolTipText("Edit profiles");
                }

                Label spacer = new Label(parent, SWT.NONE);
                spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                ((GridLayout) parent.getLayout()).numColumns += 2;
                ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;
            }
        }

        super.createButtonsForButtonBar(parent);
        Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
        cancelButton.setText(IDialogConstants.CLOSE_LABEL);
        Button finishButton = getButton(IDialogConstants.FINISH_ID);
        finishButton.setText(UIMessages.button_start);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == PROFILE_CONFIG_BTN_ID) {
            openProfilesConfiguration();
        }
        super.buttonPressed(buttonId);
    }

    private void openProfilesConfiguration() {
    }

    public static int openWizard(
        @NotNull IWorkbenchWindow workbenchWindow,
        @Nullable IDataTransferProducer[] producers,
        @Nullable IDataTransferConsumer[] consumers) {
        DataTransferWizard wizard = new DataTransferWizard(producers, consumers);
        DataTransferWizardDialog dialog = new DataTransferWizardDialog(workbenchWindow, wizard);
        return dialog.open();
    }

    public static int openWizard(
        @NotNull IWorkbenchWindow workbenchWindow,
        @Nullable IDataTransferProducer[] producers,
        @Nullable IDataTransferConsumer[] consumers,
        @Nullable IStructuredSelection selection) {
        DataTransferWizard wizard = new DataTransferWizard(producers, consumers);
        DataTransferWizardDialog dialog = new DataTransferWizardDialog(workbenchWindow, wizard, selection);
        return dialog.open();
    }

}
