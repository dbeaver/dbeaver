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
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Super class for handling dialogs related to
 * 
 * @author Serge Rieder
 * 
 */
public abstract class GenerateMultiSQLDialog<T extends DBSObject> extends GenerateSQLDialog {

    protected final Collection<T> selectedObjects;
    private Table objectsTable;

    public GenerateMultiSQLDialog(
        IWorkbenchPartSite partSite,
        String title,
        Collection<T> objects)
    {
        super(
            partSite,
            (SQLDataSource) objects.iterator().next().getDataSource(),
            title,
            null);
        this.selectedObjects = objects;
    }

    protected String[] generateSQLScript()
    {
        List<T> checkedObjects = getCheckedObjects();
        List<String> lines = new ArrayList<String>();
        for (T object : checkedObjects) {
            generateObjectCommand(lines, object);
        }

        return lines.toArray(new String[lines.size()]);
    }

    private List<T> getCheckedObjects() {
        List<T> checkedObjects = new ArrayList<T>();
        if (objectsTable != null) {
            for (TableItem item : objectsTable.getItems()) {
                if (item.getChecked()) {
                    checkedObjects.add((T) item.getData());
                }
            }
        } else {
            checkedObjects.addAll(selectedObjects);
        }
        return checkedObjects;
    }

    protected void createObjectsSelector(Composite parent) {
        if (selectedObjects.size() < 2) {
            // Don't need it for a single object
            return;
        }
        UIUtils.createControlLabel(parent, "Tables");
        objectsTable = new Table(parent, SWT.BORDER | SWT.CHECK);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 100;
        objectsTable.setLayoutData(gd);
        for (T table : selectedObjects) {
            TableItem item = new TableItem(objectsTable, SWT.NONE);
            item.setText(DBUtils.getObjectFullName(table));
            item.setImage(DBIcon.TREE_TABLE.getImage());
            item.setChecked(true);
            item.setData(table);
        }
        objectsTable.addSelectionListener(SQL_CHANGE_LISTENER);
        objectsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasChecked = !getCheckedObjects().isEmpty();
                getButton(IDialogConstants.OK_ID).setEnabled(hasChecked);
                getButton(IDialogConstants.DETAILS_ID).setEnabled(hasChecked);
            }
        });
    }

    protected abstract void generateObjectCommand(List<String> sql, T object);

}