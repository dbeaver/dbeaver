/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.mysql.tools.maintenance;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateSQLDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Super class for handling dialogs related to table tools
 * 
 * @author Serge Rieder
 * 
 */
public abstract class MySQLTableToolDialog extends GenerateSQLDialog {

    protected final Collection<MySQLTable> selectedTables;
    private Table tablesTable;

    public MySQLTableToolDialog(
        IWorkbenchPartSite partSite, String title, MySQLDataSource dataSource,
        Collection<MySQLTable> selectedTables)
    {
        super(partSite, dataSource, title, null);
        this.selectedTables = selectedTables;
    }

    protected String[] generateSQLScript()
    {
        List<MySQLTable> checkedObjects = getCheckedObjects();
        String[] lines = new String[checkedObjects.size()];
        int index = 0;
        StringBuilder sb = new StringBuilder(512);
        for (MySQLTable table : checkedObjects) {
//            sb.append("CALL SYSPROC.ADMIN_CMD('");
            generateTableCommand(sb, table);
//            sb.append("')");
            lines[index++] = sb.toString();
            sb.setLength(0);
        }

        return lines;
    }

    private List<MySQLTable> getCheckedObjects() {
        List<MySQLTable> checkedObjects = new ArrayList<MySQLTable>();
        if (tablesTable != null) {
            for (TableItem item : tablesTable.getItems()) {
                if (item.getChecked()) {
                    checkedObjects.add((MySQLTable) item.getData());
                }
            }
        } else {
            checkedObjects.addAll(selectedTables);
        }
        return checkedObjects;
    }

    protected void createObjectsSelector(Composite parent) {
        UIUtils.createControlLabel(parent, "Tables");
        tablesTable = new Table(parent, SWT.BORDER | SWT.CHECK);
        tablesTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (MySQLTable table : selectedTables) {
            TableItem item = new TableItem(tablesTable, SWT.NONE);
            item.setText(table.getFullQualifiedName());
            item.setImage(DBIcon.TREE_TABLE.getImage());
            item.setChecked(true);
            item.setData(table);
        }
        tablesTable.addSelectionListener(SQL_CHANGE_LISTENER);
        tablesTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasChecked = !getCheckedObjects().isEmpty();
                getButton(IDialogConstants.OK_ID).setEnabled(hasChecked);
                getButton(IDialogConstants.DETAILS_ID).setEnabled(hasChecked);
            }
        });
    }

    protected abstract void generateTableCommand(StringBuilder sql, MySQLTable table);

}