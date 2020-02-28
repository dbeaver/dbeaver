/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.List;

/**
 * DB2 table truncate dialog
 */
public class DB2TruncateDialog extends DB2BaseTableToolDialog {

    private Button dlgStorageDrop;
    private Button dlgStorageReuse;

    private Button dlgTriggersDelete;
    private Button dlgTriggersRestrict;

    DB2TruncateDialog(IWorkbenchPartSite partSite, Collection<DB2Table> selectedTables)
    {
        super(partSite, DB2Messages.dialog_table_tools_truncate_title, selectedTables);
    }

    @Override
    protected void createControls(Composite parent)
    {
        Group optionsGroup = UIUtils.createControlGroup(parent, DB2Messages.dialog_table_tools_options, 1, 0, 0);
        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite composite = new Composite(optionsGroup, 2);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Drop/Reuse Storage
        UIUtils.createLabel(composite, DB2Messages.dialog_table_tools_truncate_storage_title).setLayoutData(
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
        UIUtils.createLabel(composite, DB2Messages.dialog_table_tools_truncate_triggers_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupIx = new Composite(composite, SWT.NULL);
        groupIx.setLayout(new RowLayout(SWT.VERTICAL));
        dlgTriggersDelete = new Button(groupIx, SWT.RADIO);
        dlgTriggersDelete.setText(DB2Messages.dialog_table_tools_truncate_triggers_ignore);
        dlgTriggersDelete.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgTriggersRestrict = new Button(groupIx, SWT.RADIO);
        dlgTriggersRestrict.setText(DB2Messages.dialog_table_tools_truncate_triggers_restrict);
        dlgTriggersRestrict.addSelectionListener(SQL_CHANGE_LISTENER);

        // Initial setup
        dlgStorageDrop.setSelection(true);
        dlgTriggersDelete.setSelection(true);

        // Object Selector
        createObjectsSelector(parent);
    }

    @Override
    protected void generateObjectCommand(List<String> lines, DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("TRUNCATE TABLE ").append(db2Table.getFullyQualifiedName(DBPEvaluationContext.DML));

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

        lines.add(sb.toString());
    }

    @Override
    protected boolean needsRefreshOnFinish() {
        return true;
    }
}
