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
package org.jkiss.dbeaver.ui.tracking;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.tracking.sync.DDPartSelection;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.tracking.internal.DDTrackingUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DDCreateConfigurationDialog extends BaseDialog {

    private final List<DDPartSelection> availableParts;
    private final Map<DDPartSelection, Button> partButtons = new LinkedHashMap<>();

    private Text nameText;
    private String name;
    private List<String> selectedKeys = List.of();

    public DDCreateConfigurationDialog(@NotNull Shell parentShell, @NotNull List<DDPartSelection> availableParts) {
        super(parentShell, DDTrackingUIMessages.create_configuration_dialog_title, null);
        this.availableParts = availableParts;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);

        UIUtils.createControlLabel(composite, DDTrackingUIMessages.create_configuration_dialog_name_label);
        nameText = new Text(composite, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(composite, DDTrackingUIMessages.create_configuration_dialog_include_label);
        for (DDPartSelection part : availableParts) {
            Button button = new Button(composite, SWT.CHECK);
            button.setText(part.displayName());
            button.setSelection(true);
            partButtons.put(part, button);
        }

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, DDTrackingUIMessages.create_configuration_dialog_create_button, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        String value = nameText.getText().trim();
        if (CommonUtils.isEmpty(value)) {
            return;
        }
        List<String> keys = new ArrayList<>();
        partButtons.forEach((part, button) -> {
            if (button.getSelection()) {
                keys.add(part.key());
            }
        });
        if (keys.isEmpty()) {
            return;
        }
        name = value;
        selectedKeys = keys;
        super.okPressed();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public List<String> getSelectedKeys() {
        return selectedKeys;
    }
}
