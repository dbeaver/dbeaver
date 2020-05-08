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

package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolGrantee;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;


public class ExasolCreateSchemaDialog extends BaseDialog {

    private List<ExasolGrantee> grantees;
    private String name;
    private ExasolDataSource datasource;
    private ExasolGrantee owner;

    
    public ExasolCreateSchemaDialog(Shell parentShell, ExasolDataSource datasource) {
        super(parentShell,"Create schema",null);
        this.datasource = datasource;
    }
    
    @Override
    protected Composite createDialogArea(Composite parent)
    {
        final Composite composite = super.createDialogArea(parent);
        
        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, "Schema Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updateButtons();
        });

        final Combo userCombo = UIUtils.createLabelCombo(group, "Owner", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);

        userCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                owner = grantees.get(userCombo.getSelectionIndex());
            }
        });
        
        new AbstractJob("Load users") {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    grantees = new ArrayList<>(datasource.getAllGrantees(monitor));
                    
                    UIUtils.syncExec(() -> {
                        for (ExasolGrantee grantee: grantees)
                        {
                            String name = grantee.getName();
                            userCombo.add(name);
                        }
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
        
        return composite;


    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
    }

    public String getName() {
        return name;
    }
    
    public ExasolGrantee getOwner() {
        return owner;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
    
}
