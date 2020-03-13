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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.List;

/**
 * DB2 Table runstats dialog
 */
public class DB2RunstatsDialog extends DB2BaseTableToolDialog {

    private Button dlgColsAllAndDistribution;
    private Button dlgColsAll;

    private Button dlgSample;
    private Spinner dlgSampleValue;

    private Button dlgIndexesDetailed;
    private Button dlgIndexesAll;

    public DB2RunstatsDialog(IWorkbenchPartSite partSite, Collection<DB2Table> selectedTables)
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

        // RUNSTATS ON COLUMNS
        UIUtils.createLabel(composite, DB2Messages.dialog_table_tools_runstats_cols_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupCols = new Composite(composite, SWT.NONE);
        groupCols.setLayout(new RowLayout(SWT.VERTICAL));
        dlgColsAllAndDistribution = new Button(groupCols, SWT.RADIO);
        dlgColsAllAndDistribution.setText(DB2Messages.dialog_table_tools_runstats_cols_all_and_distribution);
        dlgColsAllAndDistribution.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgColsAll = new Button(groupCols, SWT.RADIO);
        dlgColsAll.setText(DB2Messages.dialog_table_tools_runstats_cols_all);
        dlgColsAll.addSelectionListener(SQL_CHANGE_LISTENER);
        Button dlgColsNo = new Button(groupCols, SWT.RADIO);
        dlgColsNo.setText(DB2Messages.dialog_table_tools_runstats_cols_no);
        dlgColsNo.addSelectionListener(SQL_CHANGE_LISTENER);

        // RUNSTATS ON INDEXES
        UIUtils.createLabel(composite, DB2Messages.dialog_table_tools_runstats_indexes_title).setLayoutData(
            new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite groupIx = new Composite(composite, SWT.NULL);
        groupIx.setLayout(new RowLayout(SWT.VERTICAL));
        dlgIndexesDetailed = new Button(groupIx, SWT.RADIO);
        dlgIndexesDetailed.setText(DB2Messages.dialog_table_tools_runstats_indexes_detailed);
        dlgIndexesDetailed.addSelectionListener(SQL_CHANGE_LISTENER);
        dlgIndexesAll = new Button(groupIx, SWT.RADIO);
        dlgIndexesAll.setText(DB2Messages.dialog_table_tools_runstats_indexes_all);
        dlgIndexesAll.addSelectionListener(SQL_CHANGE_LISTENER);
        Button dlgIndexesNo = new Button(groupIx, SWT.RADIO);
        dlgIndexesNo.setText(DB2Messages.dialog_table_tools_runstats_indexes_no);
        dlgIndexesNo.addSelectionListener(SQL_CHANGE_LISTENER);

        // SAMPLING
        dlgSample = UIUtils.createCheckbox(composite, DB2Messages.dialog_table_tools_runstats_stats_title, false);
        dlgSample.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                dlgSampleValue.setEnabled(dlgSample.getSelection());
                updateSQL();
            }
        });

        dlgSampleValue = new Spinner(composite, SWT.BORDER);
        dlgSampleValue.setMinimum(0);
        dlgSampleValue.setMaximum(100);
        dlgSampleValue.setIncrement(1);
        Rectangle clientArea = getShell().getClientArea();
        dlgSampleValue.setLocation(clientArea.x, clientArea.y);
        dlgSampleValue.pack();
        dlgSampleValue.addSelectionListener(SQL_CHANGE_LISTENER);

        // Initial setup
        dlgColsAllAndDistribution.setSelection(true);
        dlgIndexesDetailed.setSelection(true);
        dlgSampleValue.setSelection(0);
        dlgSampleValue.setEnabled(false);

        // Object Selector
        createObjectsSelector(parent);
    }

    @Override
    protected void generateObjectCommand(List<String> lines, DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("CALL SYSPROC.ADMIN_CMD('");

        sb.append("RUNSTATS ON TABLE ").append(db2Table.getFullyQualifiedName(DBPEvaluationContext.DDL));

        if (dlgColsAllAndDistribution.getSelection()) {
            sb.append(" ON ALL COLUMNS WITH DISTRIBUTION ON ALL COLUMNS");
        }
        if (dlgColsAll.getSelection()) {
            sb.append(" ON ALL COLUMNS");
        }
        if (dlgIndexesDetailed.getSelection()) {
            sb.append(" AND SAMPLED DETAILED INDEXES ALL");
        }
        if (dlgIndexesAll.getSelection()) {
            sb.append(" AND INDEXES ALL");
        }
        if (dlgSample.getSelection()) {
            sb.append(" TABLESAMPLE SYSTEM(").append(dlgSampleValue.getSelection()).append(")");
        }

        sb.append("')");

        lines.add(sb.toString());
    }

}
