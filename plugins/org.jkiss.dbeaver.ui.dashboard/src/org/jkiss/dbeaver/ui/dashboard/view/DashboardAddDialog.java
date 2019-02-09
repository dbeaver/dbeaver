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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.controls.finder.viewer.AdvancedListViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * Dashboard add dialog
 */
public class DashboardAddDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardAddDialog";//$NON-NLS-1$

    private static final int MANAGER_BUTTON_ID = 1000;

    public DashboardAddDialog(Shell parentShell) {
        super(parentShell, "Add Dashboard", null);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        AdvancedListViewer listViewer = new AdvancedListViewer(dialogArea, SWT.NONE);
        listViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridData)parent.getLayoutData()).grabExcessHorizontalSpace = true;

        final Button createButton = createButton(parent, MANAGER_BUTTON_ID, "Manage ...", false);
        ((GridData) createButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;
        ((GridData) createButton.getLayoutData()).grabExcessHorizontalSpace = true;

        createButton(parent, IDialogConstants.OK_ID, "Add", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }
}