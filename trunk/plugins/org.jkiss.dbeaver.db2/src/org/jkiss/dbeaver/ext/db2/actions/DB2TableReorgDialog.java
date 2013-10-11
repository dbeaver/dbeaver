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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
 * Manage the Dialog to enter Reorg option
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableReorgDialog extends Dialog {

    private static final String REORG = "REORG TABLE %s";
    private static final String ACCESS_READ = " ALLOW READ ACCESS";
    private static final String ACCESS_NO = " ALLOW NO ACCESS";
    private static final String INDEXSCAN = " INDEXSCAN";
    private static final String INDEX = " INDEX %s";
    private static final String NO_TRUNCATE = " NO TRUNCATE TABLE";
    private static final String RESET_DICTIONARY = " RESETDICTIONARY";
    private static final String TEMP_TS = " USE %s";
    private static final String INPLACE = " INPLACE";
    private static final String LONGLOB = " LONGLOBDATA";
    private static final String START = " START";

    private String indexName; // From a list of table indexes
    private String tempTablespace; // From a list of temp tablespaces
    private String lobsTablespace; // From a list of temp tablespaces

    // Dialog managment
    private Button dlgInplace;
    private Button dlgUseIndex;
    private Combo indexesCombo;
    private Button dlgTruncate;
    private Button dlgUseTempTS;
    private Combo tempTSCombo;
    private Button dlgIndexScan;
    private Button dlgReorgLobsTS;
    private Button dlgUseLobsTemp;
    private Combo tempLobsTSCombo;
    private Button dlgResetDictionary;
    private Button dlgAccesNo;
    private Button dlgAccesReadOnly;
    private Button dlgAccesReadWrite;
    private Text dlgCmdText;

    private List<String> listTempTsNames;
    private List<String> listIndexNames;
    private DB2Table db2Table;

    private String cmdText;

    public DB2TableReorgDialog(Shell parentShell, DB2Table db2Table, List<String> listTempTsNames, List<String> listIndexNames)
    {
        super(parentShell);
        this.db2Table = db2Table;
        this.listTempTsNames = listTempTsNames;
        this.listIndexNames = listIndexNames;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Reorg Table options");
        Control container = super.createDialogArea(parent);
        Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        dlgInplace = UIUtils.createCheckbox(composite, "Inplace Reorg? ", false);
        dlgInplace.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgInplace.getSelection()) {
                    dlgTruncate.setEnabled(true);
                    dlgTruncate.setSelection(true);
                    dlgUseTempTS.setEnabled(false);
                    dlgIndexScan.setEnabled(false);
                    dlgReorgLobsTS.setEnabled(false);
                    dlgUseLobsTemp.setEnabled(false);
                    dlgResetDictionary.setEnabled(false);
                    dlgAccesReadWrite.setEnabled(true);
                    dlgAccesReadOnly.setSelection(true);
                    dlgAccesNo.setEnabled(false);
                } else {
                    dlgTruncate.setEnabled(false);
                    dlgTruncate.setSelection(false);
                    dlgUseTempTS.setEnabled(true);
                    dlgIndexScan.setEnabled(true);
                    dlgReorgLobsTS.setEnabled(true);
                    dlgUseLobsTemp.setEnabled(true);
                    dlgResetDictionary.setEnabled(true);
                    dlgAccesNo.setEnabled(true);
                    dlgAccesNo.setSelection(true);
                    dlgAccesReadWrite.setEnabled(false);
                }
                computeCmd();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        dlgUseIndex = UIUtils.createCheckbox(composite, "Reorg Using Index", false);
        dlgUseIndex.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseIndex.getSelection()) {
                    indexesCombo.setEnabled(true);
                    dlgIndexScan.setEnabled(true);
                } else {
                    indexesCombo.setEnabled(false);
                    dlgIndexScan.setEnabled(false);
                }
                computeCmd();
            }
        });
        indexesCombo = createIndexesCombo(composite);

        dlgIndexScan = UIUtils.createCheckbox(composite, "Use Index Scan?", false);
        dlgIndexScan.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        dlgTruncate = UIUtils.createCheckbox(composite, "Truncate after Reorg?", false);
        dlgTruncate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        dlgUseTempTS = UIUtils.createCheckbox(composite, "Use Temporary Tablespace", false);
        dlgUseTempTS.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseTempTS.getSelection()) {
                    tempTSCombo.setEnabled(true);
                } else {
                    tempTSCombo.setEnabled(false);
                }
                computeCmd();
            }
        });
        tempTSCombo = createTempTablespaceCombo(composite);

        dlgReorgLobsTS = UIUtils.createCheckbox(composite, "Reorg LOBs using Temp TS ", false);
        dlgReorgLobsTS.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgReorgLobsTS.getSelection()) {
                    dlgUseLobsTemp.setEnabled(true);
                    tempLobsTSCombo.setEnabled(true);
                } else {
                    dlgUseLobsTemp.setEnabled(false);
                    tempLobsTSCombo.setEnabled(false);
                }
                computeCmd();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        dlgUseLobsTemp = UIUtils.createCheckbox(composite, "LOBs Reorg Temporary Tablespace ", false);
        dlgUseLobsTemp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseLobsTemp.getSelection()) {
                    tempLobsTSCombo.setEnabled(true);
                } else {
                    tempLobsTSCombo.setEnabled(false);
                }
                computeCmd();
            }
        });
        tempLobsTSCombo = createLobsTempTablespaceCombo(composite);

        dlgResetDictionary = UIUtils.createCheckbox(composite, "Reset Dictionary?", false);
        dlgResetDictionary.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                computeCmd();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        // Table Access
        UIUtils.createTextLabel(composite, "Table Access");
        Composite groupRB = new Composite(composite, SWT.NULL);
        groupRB.setLayout(new RowLayout());
        dlgAccesNo = new Button(groupRB, SWT.RADIO);
        dlgAccesNo.setText(TableAccess.NO_ACCESS.name());
        dlgAccesReadOnly = new Button(groupRB, SWT.RADIO);
        dlgAccesReadOnly.setText(TableAccess.READ_ONLY.name());
        dlgAccesReadWrite = new Button(groupRB, SWT.RADIO);
        dlgAccesReadWrite.setText(TableAccess.READ_WRITE.name());

        // Initial setup
        dlgTruncate.setEnabled(false);
        dlgIndexScan.setEnabled(false);
        dlgUseLobsTemp.setEnabled(false);
        tempLobsTSCombo.setEnabled(false);
        dlgAccesReadWrite.setEnabled(false);
        dlgAccesReadOnly.setSelection(true);
        indexName = listIndexNames.get(0);
        tempTablespace = listTempTsNames.get(0);
        lobsTablespace = listTempTsNames.get(0);

        // Read only Resulting REORG Command
        dlgCmdText = new Text(composite, SWT.BORDER);
        dlgCmdText.setEditable(false);

        computeCmd();

        // Behavior

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

    private enum TableAccess {
        NO_ACCESS, READ_ONLY, READ_WRITE;
    }

    private void computeCmd()
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append(String.format(REORG, db2Table.getFullQualifiedName()));

        if (dlgUseIndex.getSelection()) {
            sb.append(String.format(INDEX, indexName));
        }

        if (dlgInplace.getSelection()) {
            sb.append(INPLACE);
            if (dlgAccesReadOnly.getSelection()) {
                sb.append(ACCESS_READ);
            }
            sb.append(START);
            if (!(dlgTruncate.getSelection())) {
                sb.append(NO_TRUNCATE);
            }
        } else {
            if (dlgAccesNo.getSelection()) {
                sb.append(ACCESS_NO);
            }
            if (dlgUseTempTS.getSelection()) {
                sb.append(String.format(TEMP_TS, tempTablespace));
            }
            if (dlgIndexScan.getSelection()) {
                sb.append(INDEXSCAN);
            }
            if (dlgReorgLobsTS.getSelection()) {
                sb.append(LONGLOB);
            }
            if (dlgReorgLobsTS.getSelection()) {
                sb.append(LONGLOB);
            }
            if (dlgUseLobsTemp.getSelection()) {
                sb.append(String.format(TEMP_TS, lobsTablespace));
            }
            if (dlgResetDictionary.getSelection()) {
                sb.append(RESET_DICTIONARY);
            }
        }

        cmdText = sb.toString();
        dlgCmdText.setText(cmdText);
    }

    private Combo createIndexesCombo(Composite parent)
    {
        final Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (String indexName : listIndexNames) {
            combo.add(indexName);
        }
        combo.select(0);
        combo.setEnabled(false);
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                indexName = listIndexNames.get(combo.getSelectionIndex());
                computeCmd();
            }
        });
        return combo;
    }

    private Combo createTempTablespaceCombo(Composite parent)
    {
        final Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (String tablespaceName : listTempTsNames) {
            combo.add(tablespaceName);
        }
        combo.select(0);
        combo.setEnabled(false);
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                tempTablespace = listTempTsNames.get(combo.getSelectionIndex());
                computeCmd();
            }
        });

        return combo;
    }

    private Combo createLobsTempTablespaceCombo(Composite parent)
    {
        final Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (String tablespaceName : listTempTsNames) {
            combo.add(tablespaceName);
        }
        combo.select(0);
        combo.setEnabled(false);
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                lobsTablespace = listTempTsNames.get(combo.getSelectionIndex());
                computeCmd();
            }
        });
        return combo;
    }

}