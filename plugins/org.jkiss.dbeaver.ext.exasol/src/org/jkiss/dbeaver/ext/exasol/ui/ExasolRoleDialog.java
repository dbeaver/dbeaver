/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2017 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;


public class ExasolRoleDialog extends BaseDialog {

    private String name;
    private String comment;

    
    public ExasolRoleDialog(Shell parentShell) {
        super(parentShell,"Create Role",null);
    }
    
    @Override
    protected Composite createDialogArea(Composite parent)
    {
        final Composite composite = super.createDialogArea(parent);
        
        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, "Role Name", "");
        final Text commentText = UIUtils.createLabelText(group, "Description", "");
        ModifyListener mod = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                comment = commentText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
            }
        };
        nameText.addModifyListener(mod);
        commentText.addModifyListener(mod);
        
        return composite;


    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return comment;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
    
}
