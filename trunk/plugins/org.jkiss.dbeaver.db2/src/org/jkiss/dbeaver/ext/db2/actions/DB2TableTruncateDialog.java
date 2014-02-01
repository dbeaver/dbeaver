/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2014 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;

/**
 * Dialog that manages Options for Truncate
 * 
 * @author Denis Forveille
 */
public class DB2TableTruncateDialog extends DB2TableToolDialog {

    private Button dlgStorageDrop;
    private Button dlgStorageReuse;

    private Button dlgTriggersDelete;
    private Button dlgTriggersRestrict;

    public DB2TableTruncateDialog(IWorkbenchPartSite partSite, DB2DataSource dataSource, Collection<DB2Table> selectedDB2Tables)
    {
        super(partSite, DB2Messages.dialog_table_tools_truncate_title, dataSource, selectedDB2Tables);
    }

    @Override
    protected void createControls(Composite parent)
    {
        Composite composite = new Composite(parent, 2);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Drop/Reuse Storage
        UIUtils.createTextLabel(composite, DB2Messages.dialog_table_tools_truncate_storage_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupCols = new Composite(composite, SWT.NONE);
        groupCols.setLayout(new RowLayout(SWT.VERTICAL));
        dlgStorageDrop = new Button(groupCols, SWT.RADIO);
        dlgStorageDrop.setText(DB2Messages.dialog_table_tools_truncate_storage_drop);
        dlgStorageDrop.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgStorageReuse = new Button(groupCols, SWT.RADIO);
        dlgStorageReuse.setText(DB2Messages.dialog_table_tools_truncate_storage_reuse);
        dlgStorageReuse.addSelectionListener(SQL_CHANGE_LISTENER);

        // Triggers Clauses
        UIUtils.createTextLabel(composite, DB2Messages.dialog_table_tools_truncate_triggers_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupIx = new Composite(composite, SWT.NULL);
        groupIx.setLayout(new RowLayout(SWT.VERTICAL));
        dlgTriggersDelete = new Button(groupIx, SWT.RADIO);
        dlgTriggersDelete.setText(DB2Messages.dialog_table_tools_truncate_triggers_ignore);
        dlgTriggersDelete.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgTriggersRestrict = new Button(groupIx, SWT.RADIO);
        dlgTriggersRestrict.setText(DB2Messages.dialog_table_tools_truncate_triggers_restrict);
        dlgTriggersRestrict.addSelectionListener(SQL_CHANGE_LISTENER);

        // Read only Resulting RUNSTATS Command
        GridData gd = new GridData();
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalAlignment = GridData.FILL;
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;

        // Initial setup
        dlgStorageDrop.setSelection(true);
        dlgTriggersDelete.setSelection(true);
    }

    @Override
    protected String[] generateSQLScript()
    {
        String[] lines = new String[selectedDB2Tables.size()];
        int index = 0;
        StringBuilder sb = new StringBuilder(512);
        for (DB2Table db2Table : selectedDB2Tables) {
            sb.append(generateTableCommand(db2Table));
            lines[index++] = sb.toString();
            sb.setLength(0);
        }

        return lines;
    }

    @Override
    protected StringBuilder generateTableCommand(DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("TRUNCATE TABLE ").append(db2Table.getFullQualifiedName());

        if (dlgStorageDrop.getSelection()) {
            sb.append(" DROP STORAGE");
        }
        if (dlgStorageReuse.getSelection()) {
            sb.append(" REUSE STORAGE");
        }
        if (dlgTriggersDelete.getSelection()) {
            sb.append(" IGNORE DELETE TRIGGERS");
        }
        if (dlgTriggersRestrict.getSelection()) {
            sb.append(" RESTRICT WHEN DELETE TRIGGERS");
        }
        sb.append(" CONTINUE IDENTITY IMMEDIATE");

        return sb;
    }
}