/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.io.File;
import java.util.List;

class DataExportPageFinal extends ActiveWizardPage<DataExportWizard> {
    private Table resultTable;

    DataExportPageFinal() {
        super("Confirm");
        setTitle("Confirm");
        setDescription("Check results");
        setPageComplete(false);
    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group tablesGroup = UIUtils.createControlGroup(composite, "Tables", 3, GridData.FILL_BOTH, 0);

            resultTable = new Table(tablesGroup, SWT.BORDER | SWT.FULL_SELECTION);
            resultTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultTable.setHeaderVisible(true);
            resultTable.setLinesVisible(true);

            TableColumn columnTable = new TableColumn(resultTable, SWT.LEFT);
            columnTable.setText("Table");

            TableColumn columnOutput = new TableColumn(resultTable, SWT.RIGHT);
            columnOutput.setText("Output");

            UIUtils.packColumns(resultTable);
        }

        setControl(composite);
    }

    @Override
    public void activatePart()
    {
        resultTable.removeAll();
        List<IResultSetProvider> dataProviders = getWizard().getSettings().getDataProviders();
        for (IResultSetProvider provider : dataProviders) {
            DBPNamedObject source = provider.getResultSetSource();
            TableItem item = new TableItem(resultTable, SWT.NONE);
            item.setText(0, source.getName());
            File outputFile = getWizard().getSettings().makeOutputFile(source);
            item.setText(1, outputFile.getAbsolutePath());
            if (outputFile.exists()) {
                item.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
            }
        }
        UIUtils.packColumns(resultTable);
        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }

}