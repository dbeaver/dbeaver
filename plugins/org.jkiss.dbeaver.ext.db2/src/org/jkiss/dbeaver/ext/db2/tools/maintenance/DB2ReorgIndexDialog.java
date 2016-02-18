/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.List;

/**
 * DB2 Table reorg index dialog
 */
public class DB2ReorgIndexDialog extends DB2BaseTableToolDialog {

    private Button dlgAccessNo;
    private Button dlgAccessReadOnly;
    private Button dlgAccessReadWrite;

    private Button dlgCleanupKeysAndpages;
    private Button dlgCleanupPagesOnly;

    public DB2ReorgIndexDialog(IWorkbenchPartSite partSite, Collection<DB2Table> selectedTables)
    {
        super(partSite, DB2Messages.dialog_table_tools_runstats_title, selectedTables);
    }

    @Override
    protected void createControls(Composite parent)
    {
        Group optionsGroup = UIUtils.createControlGroup(parent, DB2Messages.dialog_table_tools_options, 1, 0, 0);
        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite composite = new Composite(optionsGroup, 2);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // REORG ACCESS
        UIUtils.createTextLabel(composite, DB2Messages.dialog_table_tools_reorgix_access_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupAccess = new Composite(composite, SWT.NULL);
        groupAccess.setLayout(new RowLayout(SWT.VERTICAL));
        Button dlgAccessDefault = new Button(groupAccess, SWT.RADIO);
        dlgAccessDefault.setText(DB2Messages.dialog_table_tools_reorgix_access_default);
        dlgAccessDefault.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgAccessNo = new Button(groupAccess, SWT.RADIO);
        dlgAccessNo.setText(DB2Messages.dialog_table_tools_reorgix_access_no);
        dlgAccessNo.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgAccessReadOnly = new Button(groupAccess, SWT.RADIO);
        dlgAccessReadOnly.setText(DB2Messages.dialog_table_tools_reorgix_access_read);
        dlgAccessReadOnly.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgAccessReadWrite = new Button(groupAccess, SWT.RADIO);
        dlgAccessReadWrite.setText(DB2Messages.dialog_table_tools_reorgix_access_readwrite);
        dlgAccessReadWrite.addSelectionListener(SQL_CHANGE_LISTENER);

        // PAGE CLEANUP
        UIUtils.createTextLabel(composite, DB2Messages.dialog_table_tools_reorgix_options_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupCleanup = new Composite(composite, SWT.NULL);
        groupCleanup.setLayout(new RowLayout(SWT.VERTICAL));
        Button dlgFullIndex = new Button(groupCleanup, SWT.RADIO);
        dlgFullIndex.setText(DB2Messages.dialog_table_tools_reorgix_options_full);
        dlgFullIndex.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgFullIndex.setSelection(true);
        dlgCleanupKeysAndpages = new Button(groupCleanup, SWT.RADIO);
        dlgCleanupKeysAndpages.setText(DB2Messages.dialog_table_tools_reorgix_options_cleanup_keys);
        dlgCleanupKeysAndpages.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgCleanupPagesOnly = new Button(groupCleanup, SWT.RADIO);
        dlgCleanupPagesOnly.setText(DB2Messages.dialog_table_tools_reorgix_options_cleanup_pages);
        dlgCleanupPagesOnly.addSelectionListener(SQL_CHANGE_LISTENER);

        // Object Selector
        createObjectsSelector(parent);
    }

    @Override
    protected void generateObjectCommand(List<String> lines, DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("CALL SYSPROC.ADMIN_CMD('");

        sb.append("REORG INDEXES ALL FOR TABLE ").append(db2Table.getFullQualifiedName());

        if (dlgAccessNo.getSelection()) {
            sb.append(" ALLOW NO ACCESS");
        }
        if (dlgAccessReadOnly.getSelection()) {
            sb.append(" ALLOW READ ACCESS");
        }
        if (dlgAccessReadWrite.getSelection()) {
            sb.append(" ALLOW WRITE ACCESS");
        }
        if (dlgCleanupKeysAndpages.getSelection()) {
            sb.append("  CLEANUP ALL");
        }
        if (dlgCleanupPagesOnly.getSelection()) {
            sb.append(" CLEANUP PAGES");
        }

        sb.append("')");

        lines.add(sb.toString());
    }
}
