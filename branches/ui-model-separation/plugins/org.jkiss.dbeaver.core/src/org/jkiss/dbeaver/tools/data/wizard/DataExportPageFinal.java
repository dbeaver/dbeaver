/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.tools.data.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.io.File;
import java.util.List;

class DataExportPageFinal extends ActiveWizardPage<DataExportWizard> {
    private Table resultTable;

    DataExportPageFinal() {
        super(CoreMessages.dialog_export_wizard_final_name);
        setTitle(CoreMessages.dialog_export_wizard_final_title);
        setDescription(CoreMessages.dialog_export_wizard_final_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group tablesGroup = UIUtils.createControlGroup(composite, CoreMessages.dialog_export_wizard_final_group_tables, 3, GridData.FILL_BOTH, 0);

            resultTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            resultTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultTable.setHeaderVisible(true);
            resultTable.setLinesVisible(true);
            resultTable.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e) {
                    UIUtils.packColumns(resultTable);
                }
            });

            UIUtils.createTableColumn(resultTable, SWT.LEFT, CoreMessages.dialog_export_wizard_final_column_table);
            UIUtils.createTableColumn(resultTable, SWT.LEFT, CoreMessages.dialog_export_wizard_final_column_output);

            UIUtils.packColumns(resultTable);
        }

        setControl(composite);
    }

    @Override
    public void activatePage()
    {
        resultTable.removeAll();
        List<DataExportProvider> dataProviders = getWizard().getSettings().getDataProviders();
        for (DataExportProvider provider : dataProviders) {
            TableItem item = new TableItem(resultTable, SWT.NONE);
            item.setText(0, provider.getDataContainer().getName());
            File outputFile = getWizard().getSettings().makeOutputFile(provider.getDataContainer());
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