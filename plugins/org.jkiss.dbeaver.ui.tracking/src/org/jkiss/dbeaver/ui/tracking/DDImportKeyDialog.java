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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.tracking.DDAccessKey;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class DDImportKeyDialog extends BaseDialog {

    private Text keyText;
    private String key;

    public DDImportKeyDialog(@NotNull Shell parentShell) {
        super(parentShell, "Import Access Key", null);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);
        UIUtils.createLabel(composite, "Copy/paste the access key");
        keyText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        gd.heightHint = UIUtils.getFontHeight(keyText) * 6;
        keyText.setLayoutData(gd);
        UIUtils.createPushButton(composite, "Paste", null, SelectionListener.widgetSelectedAdapter(e -> {
            keyText.selectAll();
            keyText.paste();
        }));
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Import", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        String value = keyText.getText().trim();
        try {
            DDAccessKey.parse(value);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Import Access Key", e.getMessage());
            return;
        }
        key = value;
        super.okPressed();
    }

    @NotNull
    public String getKey() {
        return key;
    }
}
