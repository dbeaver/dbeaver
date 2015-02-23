/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.editors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ui.UIUtils;

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
        Control container = super.createDialogArea(parent);
        Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Add Label

        // Add combo box with the tablespaces
        UIUtils.createControlLabel(parent, DB2Messages.dialog_explain_choose_tablespace_tablespace);
        final Combo tsCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        tsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (String tablespaceName : listTablespaceNames) {
            tsCombo.add(tablespaceName);
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
