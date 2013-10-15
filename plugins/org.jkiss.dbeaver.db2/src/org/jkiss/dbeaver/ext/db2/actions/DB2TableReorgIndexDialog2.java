/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
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
package org.jkiss.dbeaver.ext.db2.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;

/**
 * DB2TableReorgIndexDialog2
 */
public class DB2TableReorgIndexDialog2 extends DB2TableToolDialog {

    private enum Access {
        DEFAULT, ACCESS_NO, ACCESS_READ_ONLY, ACCESS_READ_WRITE
    }

    private enum Cleanup {
        FULL_INDEX, PSEUDO_DELETED_KEYS_PAGES, PSEUDO_DELETED_PAGES_ONLY
    }

    private Button dlgAccessNo;
    private Button dlgAccessReadOnly;
    private Button dlgAccessReadWrite;

    private Button dlgCleanupKeysAndpages;
    private Button dlgCleanupPagesOnly;

    public DB2TableReorgIndexDialog2(IWorkbenchPartSite partSite, DB2DataSource dataSource, Collection<DB2Table> tables)
    {
        super(partSite, "Reorg Index options", dataSource, tables);
    }

    @Override
    protected void createControls(Composite parent)
    {
        SelectionAdapter changeListener = new SQLChangeListener();

        Composite composite = new Composite(parent, 2);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // REORG ACCESS
        UIUtils.createTextLabel(composite, "Table Access:").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupAccess = new Composite(composite, SWT.NULL);
        groupAccess.setLayout(new RowLayout(SWT.VERTICAL));
        Button dlgAccessDefault = new Button(groupAccess, SWT.RADIO);
        dlgAccessDefault.setText(Access.DEFAULT.name());
        dlgAccessDefault.addSelectionListener(changeListener);
        dlgAccessNo = new Button(groupAccess, SWT.RADIO);
        dlgAccessNo.setText(Access.ACCESS_NO.name());
        dlgAccessNo.addSelectionListener(changeListener);
        dlgAccessReadOnly = new Button(groupAccess, SWT.RADIO);
        dlgAccessReadOnly.setText(Access.ACCESS_READ_ONLY.name());
        dlgAccessReadOnly.addSelectionListener(changeListener);
        dlgAccessReadWrite = new Button(groupAccess, SWT.RADIO);
        dlgAccessReadWrite.setText(Access.ACCESS_READ_WRITE.name());
        dlgAccessReadWrite.addSelectionListener(changeListener);

        // PAGE CLEANUP
        UIUtils.createTextLabel(composite, "Stats on Indexes:").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupCleanup = new Composite(composite, SWT.NULL);
        groupCleanup.setLayout(new RowLayout(SWT.VERTICAL));
        Button dlgFullIndex = new Button(groupCleanup, SWT.RADIO);
        dlgFullIndex.setText(Cleanup.FULL_INDEX.name());
        dlgFullIndex.addSelectionListener(changeListener);
        dlgFullIndex.setSelection(true);
        dlgCleanupKeysAndpages = new Button(groupCleanup, SWT.RADIO);
        dlgCleanupKeysAndpages.setText(Cleanup.PSEUDO_DELETED_KEYS_PAGES.name());
        dlgCleanupKeysAndpages.addSelectionListener(changeListener);
        dlgCleanupPagesOnly = new Button(groupCleanup, SWT.RADIO);
        dlgCleanupPagesOnly.setText(Cleanup.PSEUDO_DELETED_PAGES_ONLY.name());
        dlgCleanupPagesOnly.addSelectionListener(changeListener);
    }

    @Override
    protected StringBuilder generateTableCommand(DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(256);
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
        return sb;
    }

}