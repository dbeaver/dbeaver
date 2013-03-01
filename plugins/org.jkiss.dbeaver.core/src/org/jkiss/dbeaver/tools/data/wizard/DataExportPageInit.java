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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.registry.DataExportersRegistry;
import org.jkiss.dbeaver.tools.data.IDataTransferProducer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DataExportPageInit extends ActiveWizardPage<DataExportWizard> {

    private TableViewer exporterTable;

    DataExportPageInit() {
        super(CoreMessages.dialog_export_wizard_init_name);
        setTitle(CoreMessages.dialog_export_wizard_init_title);
        setDescription(CoreMessages.dialog_export_wizard_init_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        exporterTable = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        exporterTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        exporterTable.getTable().setLinesVisible(true);
        exporterTable.setContentProvider(new IStructuredContentProvider() {
            @Override
            public void dispose() {
            }
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof Collection) {
                    return ((Collection<?>)inputElement).toArray();
                }
                return new Object[0];
            }
        });
        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DataExporterDescriptor element = (DataExporterDescriptor) cell.getElement();
                if (cell.getColumnIndex() == 0) {
                    cell.setImage(element.getIcon());
                    cell.setText(element.getName());
                } else {
                    cell.setText(element.getDescription());
                }
            }
        };
        {
            TableViewerColumn columnName = new TableViewerColumn(exporterTable, SWT.LEFT);
            columnName.setLabelProvider(labelProvider);
            columnName.getColumn().setText(CoreMessages.dialog_export_wizard_init_column_exported);

            TableViewerColumn columnDesc = new TableViewerColumn(exporterTable, SWT.LEFT);
            columnDesc.setLabelProvider(labelProvider);
            columnDesc.getColumn().setText(CoreMessages.dialog_export_wizard_init_column_description);
        }

        loadExporters();

        exporterTable.getTable().addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final IStructuredSelection selection = (IStructuredSelection)exporterTable.getSelection();
                DataExporterDescriptor exporter;
                if (!selection.isEmpty()) {
                    exporter = (DataExporterDescriptor)selection.getFirstElement();
                } else {
                    exporter = null;
                }
                getWizard().getSettings().setDataExporter(exporter);
                updatePageCompletion();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
                if (isPageComplete()) {
                    getWizard().getContainer().showPage(getWizard().getNextPage(DataExportPageInit.this));
                }
            }
        });
        setControl(composite);

        UIUtils.packColumns(exporterTable.getTable());
        UIUtils.maxTableColumnsWidth(exporterTable.getTable());

        DataExporterDescriptor exporter = getWizard().getSettings().getDataExporter();
        if (exporter != null) {
            exporterTable.setSelection(new StructuredSelection(exporter));
        }
        updatePageCompletion();
    }

    private void loadExporters() {
        List<IDataTransferProducer> dataProducers = getWizard().getSettings().getDataProducers();
        List<Class> rsSources = new ArrayList<Class>();
        for (IDataTransferProducer producer : dataProducers) {
            rsSources.add(producer.getSourceObject().getClass());
        }

        DataExportersRegistry registry = DBeaverCore.getInstance().getDataExportersRegistry();
        List<DataExporterDescriptor> exporters = registry.getDataExporters(rsSources);
        exporterTable.setInput(exporters);
    }

    @Override
    protected boolean determinePageCompletion() {
        return getWizard().getSettings().getDataExporter() != null;
    }

}