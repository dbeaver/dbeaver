/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.eula;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public abstract class EULABaseDialog extends BaseDialog {
    private final String eula;

    public EULABaseDialog(@NotNull Shell parentShell, @Nullable String eula) {
        super(parentShell, CoreMessages.core_eula_dialog_title, DBIcon.TREE_INFO);
        this.eula = eula;
    }

    @Nullable
    public String getEula() {
        return eula;
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite eulaArea = new Composite(super.createDialogArea(parent), SWT.BORDER);
        eulaArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout(1, false);

        gl.marginWidth = 0;
        gl.marginHeight = 0;
        eulaArea.setLayout(gl);

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = UIUtils.getFontHeight(eulaArea.getFont()) * 40;
        gd.widthHint = UIUtils.getFontHeight(eulaArea.getFont()) * 50;

        Text eulaText = new Text(eulaArea, SWT.V_SCROLL | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.NO_FOCUS);
        eulaText.setLayoutData(gd);
        eulaText.setText(eula == null ? "End-User Agreement not found" : eula);
        return eulaArea;
    }
}
