/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2019 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriorityGroup;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Locale;


public class ExasolPriorityGroupDialog extends BaseDialog {

    private String name = "";
    private int weight = -1;
    private String comment = "";

    public ExasolPriorityGroupDialog(Shell parentShell, ExasolPriorityGroup group) {
        super(parentShell, ExasolMessages.dialog_create_priority_group, null);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 150;
        gd.verticalIndent = 0;
        gd.horizontalIndent = 0;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(2, true));
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, ExasolMessages.dialog_priority_group_name, "");
        final Text weightText = UIUtils.createLabelText(group, ExasolMessages.dialog_priority_group_weight, "");
        weightText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

        final Text commentText = UIUtils.createLabelText(group, ExasolMessages.dialog_priority_group_description, "");

        ModifyListener mod = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                try {
                	if (weightText.getText().length()>0)
                		weight = Integer.parseInt(weightText.getText());
				} catch (NumberFormatException ex) {
				}
                comment = commentText.getText();
                //enable/disable OK button   
                if (name.isEmpty() | weight == -1 | weight > 1000 | weight < 1) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                }
            }
        };

        nameText.addModifyListener(mod);
        commentText.addModifyListener(mod);
        weightText.addModifyListener(mod);
        return composite;
    }

    public String getName() {
        return name;
    }


    public int getWeight() {
        return weight;
    }

    public String getComment() {
        return comment;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

}
