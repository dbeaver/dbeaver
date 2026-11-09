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
package org.jkiss.dbeaver.ui.datadam.project;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/** Collects project metadata; the caller decides whether to create or update the remote project. */
public class DDProjectEditDialog<T> extends BaseDialog {
    @NotNull
    private final T project;
    @NotNull
    private final String initialName;
    @Nullable
    private final String initialDescription;
    private Text nameText;
    private Text descriptionText;
    private ProjectDetails<T> details;

    public DDProjectEditDialog(
        @NotNull Shell parentShell, @NotNull String title, @NotNull T project,
        @NotNull String name, @Nullable String description
    ) {
        super(parentShell, title, null);
        this.project = project;
        this.initialName = name;
        this.initialDescription = description;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite content = new Composite(area, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        UIUtils.createLabel(content, DDTrackingUIMessages.projects_name);
        nameText = new Text(content, SWT.BORDER);
        GridData nameData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        nameData.widthHint = 400;
        nameText.setLayoutData(nameData);
        nameText.setText(initialName);
        nameText.addModifyListener(e -> getButton(IDialogConstants.OK_ID).setEnabled(!nameText.getText().trim().isEmpty()));

        UIUtils.createLabel(content, DDTrackingUIMessages.projects_description);
        descriptionText = new Text(content, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData descriptionData = new GridData(SWT.FILL, SWT.FILL, true, true);
        descriptionData.heightHint = UIUtils.getFontHeight(descriptionText) * 5;
        descriptionText.setLayoutData(descriptionData);
        descriptionText.setText(initialDescription == null ? "" : initialDescription);
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true)
            .setEnabled(!nameText.getText().trim().isEmpty());
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        String description = descriptionText.getText().trim();
        details = new ProjectDetails<>(project, nameText.getText().trim(), description.isEmpty() ? null : description);
        super.okPressed();
    }

    @NotNull
    public ProjectDetails<T> getDetails() {
        return details;
    }

    public record ProjectDetails<T>(@NotNull T project, @NotNull String name, @Nullable String description) {
    }
}
