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
package org.jkiss.dbeaver.ui.services;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Map;

final class ConfirmScriptDialog extends BaseDialog {
    private static final int MAX_SCRIPT_WIDTH = 80;
    private static final int MAX_SCRIPT_LINES = 8;

    private final Map<String, String> approvalContext;
    private final String script;
    private final String description;
    private final String scriptLabel;
    private final String approveLabel;

    ConfirmScriptDialog(
        @Nullable Shell parentShell,
        @NotNull String title,
        @NotNull String description,
        @NotNull Map<String, String> approvalContext,
        @NotNull String script,
        @NotNull String scriptLabel,
        @NotNull String approveLabel
    ) {
        super(parentShell, title, null);
        this.approvalContext = approvalContext;
        this.script = script;
        this.description = description;
        this.scriptLabel = scriptLabel;
        this.approveLabel = approveLabel;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        String[] scriptLines = script.split("\\R", -1);
        int scriptWidth = 0;
        for (String line : scriptLines) {
            scriptWidth = Math.max(scriptWidth, line.length());
        }
        int scriptStyle = SWT.BORDER | SWT.MULTI | SWT.READ_ONLY;
        if (scriptWidth > MAX_SCRIPT_WIDTH) {
            scriptStyle |= SWT.H_SCROLL;
        }
        if (scriptLines.length > MAX_SCRIPT_LINES) {
            scriptStyle |= SWT.V_SCROLL;
        }

        Composite area = super.createDialogArea(parent);
        Composite content = UIUtils.createComposite(area, 2);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createWarningLabel(
            content,
            description,
            GridData.FILL_HORIZONTAL,
            2
        );
        for (Map.Entry<String, String> entry : approvalContext.entrySet()) {
            UIUtils.createLabelText(content, entry.getKey(), entry.getValue(), SWT.READ_ONLY);
        }

        UIUtils.createLabelText(
            content,
            scriptLabel,
            script,
            scriptStyle,
            GridDataFactory.fillDefaults()
                .grab(true, true)
                .hint(
                    convertWidthInCharsToPixels(Math.min(scriptWidth, MAX_SCRIPT_WIDTH)),
                    convertHeightInCharsToPixels(Math.min(scriptLines.length, MAX_SCRIPT_LINES))
                )
                .create()
        );

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(
            parent,
            IDialogConstants.OK_ID,
            approveLabel,
            false
        );
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }
}
