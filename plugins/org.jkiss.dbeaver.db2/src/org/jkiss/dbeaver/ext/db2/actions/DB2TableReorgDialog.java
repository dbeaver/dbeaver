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
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
 * Manage the Dialog to enter Reorg option
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableReorgDialog extends Dialog {

    private Boolean inplace;
    private TableAccess tableAcces;
    private Boolean useIndex;
    private String indexName; // From a list of table indexes
    private Boolean truncate;
    private Boolean useTempTS;
    private String tempTablespace; // From a list of temp tablespaces
    private Boolean indexScan;
    private Boolean reorgLobs;
    private Boolean useLobsTempTS;
    private String lobsTablespace; // From a list of temp tablespaces
    private Boolean resetDictionary;

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

    List<String> listTempTsNames;
    List<String> listIndexNames;

    public DB2TableReorgDialog(Shell parentShell, List<String> listTempTsNames, List<String> listIndexNames)
    {
        super(parentShell);
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

        dlgInplace = UIUtils.createCheckbox(composite, "Inplace Reorg? ", false);
        UIUtils.createPlaceholder(composite, 1);
        dlgUseIndex = UIUtils.createCheckbox(composite, "Reorg Using Index", false);
        indexesCombo = createIndexesCombo(composite);
        dlgTruncate = UIUtils.createCheckbox(composite, "Truncate after Reorg?", false);
        UIUtils.createPlaceholder(composite, 1);
        dlgUseTempTS = UIUtils.createCheckbox(composite, "Use Temporary Tablespace", false);
        tempTSCombo = createTempTablespaceCombo(composite);
        dlgIndexScan = UIUtils.createCheckbox(composite, "Use Index Scan?", false);
        UIUtils.createPlaceholder(composite, 1);
        dlgReorgLobsTS = UIUtils.createCheckbox(composite, "Reorg LOBs using Temporary Tablespace ", false);
        UIUtils.createPlaceholder(composite, 1);
        dlgUseLobsTemp = UIUtils.createCheckbox(composite, "LOBs Reorg Temporary Tablespace ", false);
        tempLobsTSCombo = createLobsTempTablespaceCombo(composite);
        dlgResetDictionary = UIUtils.createCheckbox(composite, "Reset Dictionary?", false);
        UIUtils.createPlaceholder(composite, 1);

        // Initial setup
        dlgTruncate.setEnabled(false);
        dlgIndexScan.setEnabled(false);
        dlgUseLobsTemp.setEnabled(false);
        tempLobsTSCombo.setEnabled(false);
        dlgAccesReadWrite.setEnabled(false);

        // Behavior
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
                    dlgAccesNo.setEnabled(false);
                    dlgAccesReadOnly.setSelection(true);
                    dlgAccesReadWrite.setEnabled(true);
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
            }
        });
        dlgUseIndex.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseIndex.getSelection()) {
                    indexesCombo.setEnabled(true);
                } else {
                    indexesCombo.setEnabled(false);
                }
            }
        });

        dlgUseTempTS.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseTempTS.getSelection()) {
                    tempTSCombo.setEnabled(true);
                } else {
                    tempTSCombo.setEnabled(false);
                }
            }
        });

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
            }
        });

        dlgUseLobsTemp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseLobsTemp.getSelection()) {
                    tempLobsTSCombo.setEnabled(true);
                } else {
                    tempLobsTSCombo.setEnabled(false);
                }
            }
        });

        return parent;
    }

    @Override
    protected void okPressed()
    {
        this.inplace = dlgInplace.getSelection();
        this.truncate = dlgTruncate.getSelection();
        this.indexScan = dlgIndexScan.getSelection();
        this.resetDictionary = dlgResetDictionary.getSelection();
        super.okPressed();
    }

    // ----------------
    // Getters
    // ----------------

    public Boolean getInplace()
    {
        return inplace;
    }

    public TableAccess getTableAcces()
    {
        return tableAcces;
    }

    public Boolean getTruncate()
    {
        return truncate;
    }

    public Boolean getResetDictionary()
    {
        return resetDictionary;
    }

    public String getIndexName()
    {
        return indexName;
    }

    public Boolean getIndexScan()
    {
        return indexScan;
    }

    public Boolean getReorgLobs()
    {
        return reorgLobs;
    }

    public String getLobsTablespace()
    {
        return lobsTablespace;
    }

    // -------
    // Helpers
    // -------

    private enum TableAccess {
        NO_ACCESS, READ_ONLY, READ_WRITE;
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
            }
        });
        return combo;
    }

}