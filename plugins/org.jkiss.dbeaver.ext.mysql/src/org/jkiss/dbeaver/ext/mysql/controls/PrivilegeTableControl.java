/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.List;

/**
 * Privilege table control
 */
public class PrivilegeTableControl extends Composite {

    private Table privTable;

    public PrivilegeTableControl(Composite parent, String title)
    {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        setLayout(gl);

        Composite privsGroup = UIUtils.createControlGroup(this, title, 1, GridData.FILL_BOTH, 0);
        GridData gd = (GridData)privsGroup.getLayoutData();
        gd.horizontalSpan = 2;

        privTable = new Table(privsGroup, SWT.BORDER | SWT.CHECK | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        privTable.setHeaderVisible(true);
        gd = new GridData(GridData.FILL_BOTH);
        gd.minimumWidth = 300;
        privTable.setLayoutData(gd);
        privTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (e.detail == SWT.CHECK) {
                    TableItem item = (TableItem) e.item;
                    notifyPrivilegeCheck((MySQLPrivilege)item.getData(), item.getChecked());
                }
            }
        });
        UIUtils.createTableColumn(privTable, SWT.LEFT, "Privilege");
        //UIUtils.createTableColumn(privTable, SWT.LEFT, "Grant Option");
        UIUtils.createTableColumn(privTable, SWT.LEFT, "Description");
        UIUtils.packColumns(privTable);

        Composite buttonsPanel = UIUtils.createPlaceholder(privsGroup, 3);
        buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createPushButton(buttonsPanel, "Check All", null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                for (TableItem item : privTable.getItems()) {
                    if (!item.getChecked()) {
                        item.setChecked(true);
                        notifyPrivilegeCheck((MySQLPrivilege)item.getData(), true);
                    }
                }
            }
        });
        UIUtils.createPushButton(buttonsPanel, "Clear All", null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                for (TableItem item : privTable.getItems()) {
                    if (item.getChecked()) {
                        item.setChecked(false);
                        notifyPrivilegeCheck((MySQLPrivilege)item.getData(), false);
                    }
                }
            }
        });
    }

    private void notifyPrivilegeCheck(MySQLPrivilege privilege, boolean checked)
    {
        Event event = new Event();
        event.detail = checked ? 1 : 0;
        event.widget = this;
        event.data = privilege;
        super.notifyListeners(SWT.Modify, event);
    }

    public void fillPrivileges(Collection<MySQLPrivilege> privs)
    {
        if (privTable.isDisposed()) {
            return;
        }
        privTable.removeAll();
        for (MySQLPrivilege priv : privs) {
            TableItem item = new TableItem(privTable, SWT.NONE);
            item.setText(0, priv.getName());
            item.setText(1, priv.getDescription());
            item.setData(priv);

/*
            Button checkbox = new Button(privTable, SWT.CHECK);
            checkbox.pack();
            TableEditor editor = new TableEditor(privTable);
            editor.setEditor(checkbox, item, 1);
            Point size = checkbox.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            editor.minimumWidth = size.x;
            editor.minimumHeight = size.y;
            editor.horizontalAlignment = SWT.CENTER;
            editor.verticalAlignment = SWT.CENTER;

            item.setData("grant", checkbox);
*/
        }
        UIUtils.packColumns(privTable);
    }

    public void fillGrants(List<MySQLGrant> grants)
    {
        if (grants == null) {
            return;
        }
        for (TableItem item : privTable.getItems()) {
            MySQLPrivilege privilege = (MySQLPrivilege) item.getData();
            //Button grantCheck = (Button)item.getData("grant");
            boolean checked = false;//, grantOption = false;
            for (MySQLGrant grant : grants) {
                if (grant.isAllPrivileges() || grant.getPrivileges().contains(privilege) ||
                    (grant.isGrantOption() && privilege.isGrantOption()))
                {
                    checked = true;
                    //grantOption = grant.isGrantOption();
                    break;
                }
            }
            item.setChecked(checked);
            //grantCheck.setSelection(grantOption);
        }
    }

    public void checkPrivilege(MySQLPrivilege privilege, boolean grant)
    {
        for (TableItem item : privTable.getItems()) {
            if (item.getData() == privilege) {
                item.setChecked(grant);
                break;
            }
        }
    }

}
