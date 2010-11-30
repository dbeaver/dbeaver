/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
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
    private Button grantOptionCheck;
    private boolean showGrantOption;

    public PrivilegeTableControl(Composite parent, String title, boolean showGrantOption)
    {
        super(parent, SWT.NONE);
        this.showGrantOption = showGrantOption;
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        setLayout(gl);

        Composite privsGroup = UIUtils.createControlGroup(this, title, 1, GridData.FILL_VERTICAL, 0);
        GridData gd = (GridData)privsGroup.getLayoutData();
        gd.horizontalSpan = 2;

        privTable = new Table(privsGroup, SWT.BORDER | SWT.CHECK | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        privTable.setHeaderVisible(true);
        gd = new GridData(GridData.FILL_BOTH);
        gd.minimumWidth = 400;
        privTable.setLayoutData(gd);
        UIUtils.createTableColumn(privTable, SWT.LEFT, "Privilege");
        UIUtils.createTableColumn(privTable, SWT.LEFT, "Description");
        UIUtils.packColumns(privTable);

        Composite buttonsPanel = UIUtils.createPlaceholder(privsGroup, 3);
        buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (showGrantOption) {
            grantOptionCheck = UIUtils.createCheckbox(buttonsPanel, "With Grant Option", false);
            grantOptionCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        Button checkButton = UIUtils.createPushButton(buttonsPanel, "Check All", null);
        checkButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                for (TableItem item : privTable.getItems()) {
                    item.setChecked(true);
                }
            }
        });
        Button clearButton = UIUtils.createPushButton(buttonsPanel, "Clear All", null);
        clearButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                for (TableItem item : privTable.getItems()) {
                    item.setChecked(false);
                }
            }
        });
    }

    public void fillPrivileges(Collection<MySQLPrivilege> privs)
    {
        if (privTable.isDisposed()) {
            return;
        }
        privTable.removeAll();
        for (MySQLPrivilege priv : privs) {
            if (showGrantOption && priv.getName().equalsIgnoreCase(MySQLPrivilege.GRANT_PRIVILEGE)) {
                continue;
            }
            TableItem item = new TableItem(privTable, SWT.NONE);
            item.setText(0, priv.getName());
            item.setText(1, priv.getDescription());
            item.setData(priv);
        }
        UIUtils.packColumns(privTable);
    }

    public void fillGrants(List<MySQLGrant> grants)
    {
        for (TableItem item : privTable.getItems()) {
            MySQLPrivilege privilege = (MySQLPrivilege) item.getData();
            boolean checked = false;
            for (MySQLGrant grant : grants) {
                if (grant.isAllPrivileges() || grant.getPrivileges().contains(privilege)) {
                    checked = true;
                    break;
                }
            }
            item.setChecked(checked);
        }
        if (showGrantOption) {
            boolean hasGrantOption = false;
            for (MySQLGrant grant : grants) {
                if (grant.isGrantOption()) {
                    hasGrantOption = true;
                    break;
                }
            }
            grantOptionCheck.setSelection(hasGrantOption);
        }
    }
}
