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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablespaceDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manage the Dialog to enter Reorg option
 * 
 * @author Denis Forveille
 * @author Serge Rieder
 * 
 */
public class DB2TableReorgDialog extends DB2TableToolDialog {

    private String indexName; // From a list of table indexes
    private String tempTablespace; // From a list of temp tablespaces
    private String lobsTablespace; // From a list of temp tablespaces

    // Dialog artefacts
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

    private final List<String> listTempTsNames = new ArrayList<String>();
    private final List<String> listIndexNames = new ArrayList<String>();

    public DB2TableReorgDialog(IWorkbenchPartSite partSite, final DB2DataSource dataSource, final DB2Table selectedDB2Table)
    {
        super(partSite, DB2Messages.dialog_table_tools_reorg_title, dataSource, Collections.singleton(selectedDB2Table));

        // Read TS and indexes
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        monitor.beginTask("Read system info", 2);

                        for (DB2Tablespace db2Tablespace : dataSource.getTablespaces(monitor)) {
                            if (db2Tablespace.getDataType().equals(DB2TablespaceDataType.T)) {
                                listTempTsNames.add(db2Tablespace.getName());
                            }
                        }

                        monitor.worked(1);

                        for (DB2Index db2Index : selectedDB2Table.getIndexes(monitor)) {
                            listIndexNames.add(db2Index.getFullQualifiedName());
                        }

                        monitor.worked(1);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(partSite.getShell(), "Error", "Can't read system info", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    protected void createControls(Composite parent)
    {
        SelectionAdapter changeListener = new SQLChangeListener();

        Composite composite = new Composite(parent, 2);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // INPLACE
        dlgInplace = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_inplace, false);
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
                    dlgAccesNo.setEnabled(false);

                    dlgAccesNo.setSelection(false);
                    dlgAccesReadWrite.setSelection(false);
                    dlgAccesReadOnly.setSelection(true);

                } else {
                    dlgTruncate.setEnabled(false);
                    dlgTruncate.setSelection(false);
                    dlgUseTempTS.setEnabled(true);
                    dlgIndexScan.setEnabled(true);
                    dlgReorgLobsTS.setEnabled(true);
                    dlgUseLobsTemp.setEnabled(true);
                    dlgResetDictionary.setEnabled(true);
                    dlgAccesNo.setEnabled(true);
                    dlgAccesReadWrite.setEnabled(false);

                    dlgAccesReadWrite.setSelection(false);
                    dlgAccesReadOnly.setSelection(false);
                    dlgAccesNo.setSelection(true);
                }
                updateSQL();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        // USE INDEX
        dlgUseIndex = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_useindex, false);
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
                updateSQL();
            }
        });
        indexesCombo = createIndexesCombo(composite);

        // INDEXSCAN
        dlgIndexScan = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_useindexscan, false);
        dlgIndexScan.addSelectionListener(changeListener);
        UIUtils.createPlaceholder(composite, 1);

        // TRUNCATE
        dlgTruncate = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_truncate, false);
        dlgTruncate.addSelectionListener(changeListener);
        UIUtils.createPlaceholder(composite, 1);

        // USE TEMP TS
        dlgUseTempTS = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_usetempts, false);
        dlgUseTempTS.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseTempTS.getSelection()) {
                    tempTSCombo.setEnabled(true);
                } else {
                    tempTSCombo.setEnabled(false);
                }
                updateSQL();
            }
        });
        tempTSCombo = createTempTablespaceCombo(composite);

        // REORG LONG AND LOBS
        dlgReorgLobsTS = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_reorglobs, false);
        dlgReorgLobsTS.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgReorgLobsTS.getSelection()) {
                    dlgUseLobsTemp.setEnabled(true);
                    tempLobsTSCombo.setEnabled(false);
                } else {
                    dlgUseLobsTemp.setEnabled(false);
                    tempLobsTSCombo.setEnabled(false);
                }
                updateSQL();
            }
        });
        UIUtils.createPlaceholder(composite, 1);

        // REORG LONG AND LOBS TEMP TS
        dlgUseLobsTemp = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_reorglobsts, false);
        dlgUseLobsTemp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (dlgUseLobsTemp.getSelection()) {
                    tempLobsTSCombo.setEnabled(true);
                } else {
                    tempLobsTSCombo.setEnabled(false);
                }
                updateSQL();
            }
        });
        tempLobsTSCombo = createLobsTempTablespaceCombo(composite);

        // RESET DICTIONARY
        dlgResetDictionary = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_reorg_resetdict, false);
        dlgResetDictionary.addSelectionListener(changeListener);
        UIUtils.createPlaceholder(composite, 1);

        // TABLE ACCESS
        UIUtils.createTextLabel(composite, DB2Messages.dialog_table_tools_reorg_access_title);
        Composite groupRB = new Composite(composite, SWT.NULL);
        groupRB.setLayout(new RowLayout());
        dlgAccesNo = new Button(groupRB, SWT.RADIO);
        dlgAccesNo.setText(DB2Messages.dialog_table_tools_reorg_access_no);
        dlgAccesNo.addSelectionListener(changeListener);
        dlgAccesReadOnly = new Button(groupRB, SWT.RADIO);
        dlgAccesReadOnly.setText(DB2Messages.dialog_table_tools_reorg_access_read);
        dlgAccesReadOnly.addSelectionListener(changeListener);
        dlgAccesReadWrite = new Button(groupRB, SWT.RADIO);
        dlgAccesReadWrite.setText(DB2Messages.dialog_table_tools_reorg_access_readwrite);
        dlgAccesReadWrite.addSelectionListener(changeListener);

        // Initial setup
        dlgTruncate.setEnabled(false);
        dlgIndexScan.setEnabled(false);
        dlgUseLobsTemp.setEnabled(false);
        tempLobsTSCombo.setEnabled(false);
        dlgAccesReadWrite.setEnabled(false);
        dlgAccesReadOnly.setSelection(true);
        indexName = listIndexNames.isEmpty() ? null : listIndexNames.get(0);
        tempTablespace = listTempTsNames.isEmpty() ? null : listTempTsNames.get(0);
        lobsTablespace = listTempTsNames.isEmpty() ? null : listTempTsNames.get(0);
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
                updateSQL();
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
                updateSQL();
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
        if (!listTempTsNames.isEmpty()) {
            combo.select(0);
        }
        combo.setEnabled(false);
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                lobsTablespace = listTempTsNames.get(combo.getSelectionIndex());
                updateSQL();
            }
        });
        return combo;
    }

    @Override
    protected StringBuilder generateTableCommand(DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("REORG TABLE ").append(db2Table.getFullQualifiedName());

        if (dlgUseIndex.getSelection() && indexName != null) {
            sb.append(" INDEX ").append(indexName);
        }

        if (dlgInplace.getSelection()) {
            sb.append(" INPLACE");
            if (dlgAccesReadOnly.getSelection()) {
                sb.append(" ALLOW READ ACCESS");
            }
            sb.append(" START");
            if (!(dlgTruncate.getSelection())) {
                sb.append(" NO TRUNCATE TABLE");
            }
        } else {
            if (dlgAccesNo.getSelection()) {
                sb.append(" ALLOW NO ACCESS");
            }
            if (dlgUseTempTS.getSelection() && tempTablespace != null) {
                sb.append(" USE ").append(tempTablespace);
            }
            if (dlgIndexScan.getSelection()) {
                sb.append(" INDEXSCAN");
            }
            if (dlgReorgLobsTS.getSelection()) {
                sb.append(" LONGLOBDATA");
            }
            if (dlgUseLobsTemp.getSelection() && lobsTablespace != null) {
                sb.append(" USE ").append(lobsTablespace);
            }
            if (dlgResetDictionary.getSelection()) {
                sb.append(" RESETDICTIONARY");
            }
        }
        return sb;
    }

}
