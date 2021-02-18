/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.ui.editors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * 
 * Dialog with the list of tablespaces usable for the user to create theExplain tables
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TablespaceChooser extends Dialog {

    private String selectedTablespace;
    private List<String> listTablespaceNames;

    public DB2TablespaceChooser(Shell parentShell, List<String> listTablespaceNames)
    {
        super(parentShell);
        this.listTablespaceNames = listTablespaceNames;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(DB2Messages.dialog_explain_choose_tablespace);
        Composite composite = (Composite) super.createDialogArea(parent);
        Group infoGroup = UIUtils.createControlGroup(composite, DB2Messages.dialog_explain_choose_tablespace_tablespace, 1, GridData.FILL_BOTH, 0);

        // Add combo box with the tablespaces
        final Combo tsCombo = new Combo(infoGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        tsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (String tablespaceName : listTablespaceNames) {
            tsCombo.add(tablespaceName);
        }
        if(!CommonUtils.isEmpty(listTablespaceNames)){
            selectedTablespace = listTablespaceNames.get(0);
        }
        tsCombo.select(0);
        tsCombo.setEnabled(true);
        tsCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                selectedTablespace = listTablespaceNames.get(tsCombo.getSelectionIndex());
            }
        });

        return parent;
    }

    // ----------------
    // Standard Getters
    // ----------------

    public String getSelectedTablespace()
    {
        return selectedTablespace;
    }
}
