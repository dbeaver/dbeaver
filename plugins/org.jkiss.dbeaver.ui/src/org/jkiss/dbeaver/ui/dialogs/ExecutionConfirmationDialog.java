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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Confirmation of code execution requested by connection settings. */
public class ExecutionConfirmationDialog extends BaseDialog {
    private static final int MESSAGE_WIDTH = 60;
    private static final int CONTENT_WIDTH = 72;
    private static final int CONTENT_LINES = 6;

    private final String message;
    private final String allowLabel;
    private final Map<String, String> context;
    private final Map<String, String> execution;
    private Map<String, String> additionalDetails = Map.of();
    private Composite details;

    public ExecutionConfirmationDialog(
        @Nullable Shell parentShell,
        @NotNull String title,
        @NotNull String message,
        @NotNull String allowLabel,
        @NotNull Map<String, String> context,
        @NotNull Map<String, String> execution
    ) {
        super(parentShell, title, null);
        this.message = message;
        this.allowLabel = allowLabel;
        this.context = new LinkedHashMap<>(context);
        this.execution = new LinkedHashMap<>(execution);
    }

    public void setAdditionalDetails(@NotNull Map<String, String> additionalDetails) {
        this.additionalDetails = new LinkedHashMap<>(additionalDetails);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite content = UIUtils.createComposite(area, 2);
        content.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite warning = UIUtils.createComposite(content, 2);
        warning.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());
        Label icon = new Label(warning, SWT.NONE);
        icon.setImage(DBeaverIcons.getImage(DBIcon.STATUS_WARNING));
        icon.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
        Label description = new Label(warning, SWT.WRAP);
        description.setText(message);
        description.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
            .hint(convertWidthInCharsToPixels(MESSAGE_WIDTH), SWT.DEFAULT).create());

        for (Map.Entry<String, String> entry : context.entrySet()) {
            UIUtils.createLabelText(content, entry.getKey(), entry.getValue(), SWT.READ_ONLY,
                GridDataFactory.fillDefaults().grab(true, false)
                    .hint(convertWidthInCharsToPixels(MESSAGE_WIDTH), SWT.DEFAULT).create());
        }
        createTextSections(content, execution);
        if (!additionalDetails.isEmpty()) {
            details = UIUtils.createComposite(content, 2);
            details.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());
            createTextSections(details, additionalDetails);
            UIUtils.setControlVisible(details, false);
        }
        return area;
    }

    private void createTextSections(@NotNull Composite parent, @NotNull Map<String, String> sections) {
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String[] lines = entry.getValue().split("\\R", -1);
            int width = 1;
            for (String line : lines) {
                width = Math.max(width, line.length());
            }
            int style = SWT.BORDER | SWT.MULTI | SWT.READ_ONLY;
            if (width > CONTENT_WIDTH) {
                style |= SWT.H_SCROLL;
            }
            if (lines.length > CONTENT_LINES) {
                style |= SWT.V_SCROLL;
            }
            UIUtils.createLabelText(parent, entry.getKey(), entry.getValue(), style,
                GridDataFactory.fillDefaults().grab(true, false)
                    .hint(convertWidthInCharsToPixels(Math.min(width, CONTENT_WIDTH)),
                        convertHeightInCharsToPixels(Math.min(lines.length, CONTENT_LINES))).create());
        }
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        if (!additionalDetails.isEmpty()) {
            createButton(parent, IDialogConstants.DETAILS_ID, IDialogConstants.SHOW_DETAILS_LABEL, false);
        }
        createButton(parent, IDialogConstants.OK_ID, allowLabel, false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            boolean visible = !details.isVisible();
            UIUtils.setControlVisible(details, visible);
            Button button = getButton(IDialogConstants.DETAILS_ID);
            button.setText(visible ? IDialogConstants.HIDE_DETAILS_LABEL : IDialogConstants.SHOW_DETAILS_LABEL);
            getShell().pack();
        } else {
            super.buttonPressed(buttonId);
        }
    }
}
