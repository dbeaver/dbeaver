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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Manage the Dialog to enter Reorg Table Index option
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableReorgIndexDialog extends Dialog {

    private static final String REORG = "REORG INDEXES ALL FOR TABLE %s";
    private static final String ACCESS_NO = " ALLOW NO ACCESS";
    private static final String ACCESS_READ = " ALLOW READ ACCESS";
    private static final String ACCESS_WRITE = " ALLOW WRITE ACCESS";
    private static final String CLEANUP_ALL = "  CLEANUP ALL";
    private static final String CLEANUP_PAGES = " CLEANUP PAGES";

    // Dialog artefacts
    private Button dlgAccessDefault;
    private Button dlgAccessNo;
    private Button dlgAccessReadOnly;
    private Button dlgAccessReadWrite;

    private Button dlgFullIndex;
    private Button dlgCleanupKeysAndpages;
    private Button dlgCleanupPagesOnly;

    private Text dlgCmdText;

    private DB2Table db2Table;

    private String cmdText;

    public DB2TableReorgIndexDialog(Shell parentShell, DB2Table db2Table)
    {
        super(parentShell);
        this.db2Table = db2Table;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Reorg Index options");
        Control container = super.createDialogArea(parent);
        Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // REORG ACCESS
        UIUtils.createTextLabel(composite, "Table Access:");
        Composite groupAccess = new Composite(composite, SWT.NULL);
        groupAccess.setLayout(new RowLayout());
        dlgAccessDefault = new Button(groupAccess, SWT.RADIO);
        dlgAccessDefault.setText(Access.DEFAULT.name());
        dlgAccessDefault.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        dlgAccessNo = new Button(groupAccess, SWT.RADIO);
        dlgAccessNo.setText(Access.ACCESS_NO.name());
        dlgAccessNo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        dlgAccessReadOnly = new Button(groupAccess, SWT.RADIO);
        dlgAccessReadOnly.setText(Access.ACCESS_READ_ONLY.name());
        dlgAccessReadOnly.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        dlgAccessReadWrite = new Button(groupAccess, SWT.RADIO);
        dlgAccessReadWrite.setText(Access.ACCESS_READ_WRITE.name());
        dlgAccessReadWrite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });

        // PAGE CLEANUP
        UIUtils.createTextLabel(composite, "Stats on Indexes:");
        Composite groupCleanup = new Composite(composite, SWT.NULL);
        groupCleanup.setLayout(new RowLayout());
        dlgFullIndex = new Button(groupCleanup, SWT.RADIO);
        dlgFullIndex.setText(Cleanup.FULL_INDEX.name());
        dlgFullIndex.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        dlgCleanupKeysAndpages = new Button(groupCleanup, SWT.RADIO);
        dlgCleanupKeysAndpages.setText(Cleanup.PSEUDO_DELETED_KEYS_PAGES.name());
        dlgCleanupKeysAndpages.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        dlgCleanupPagesOnly = new Button(groupCleanup, SWT.RADIO);
        dlgCleanupPagesOnly.setText(Cleanup.PSEUDO_DELETED_PAGES_ONLY.name());
        dlgCleanupPagesOnly.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });

        // Read only Resulting REORG Command
        GridData gd = new GridData();
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalAlignment = GridData.FILL;
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;

        UIUtils.createTextLabel(composite, "Command:");

        dlgCmdText = new Text(composite, SWT.BORDER | SWT.WRAP);
        dlgCmdText.setEditable(false);
        dlgCmdText.setLayoutData(gd);

        // Initial setup
        dlgAccessDefault.setSelection(true);
        dlgFullIndex.setSelection(true);

        computeCmd();

        return parent;
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    // ----------------
    // Getters
    // ----------------
    public String getCmdText()
    {
        return cmdText;
    }

    // -------
    // Helpers
    // -------

    private void computeCmd()
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append(String.format(REORG, db2Table.getFullQualifiedName()));

        if (dlgAccessNo.getSelection()) {
            sb.append(ACCESS_NO);
        }
        if (dlgAccessReadOnly.getSelection()) {
            sb.append(ACCESS_READ);
        }
        if (dlgAccessReadWrite.getSelection()) {
            sb.append(ACCESS_WRITE);
        }
        if (dlgCleanupKeysAndpages.getSelection()) {
            sb.append(CLEANUP_ALL);
        }
        if (dlgCleanupPagesOnly.getSelection()) {
            sb.append(CLEANUP_PAGES);
        }

        cmdText = sb.toString();
        dlgCmdText.setText(cmdText);
    }

    private enum Access {
        DEFAULT, ACCESS_NO, ACCESS_READ_ONLY, ACCESS_READ_WRITE;
    }

    private enum Cleanup {
        FULL_INDEX, PSEUDO_DELETED_KEYS_PAGES, PSEUDO_DELETED_PAGES_ONLY;
    }

}