/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data.wizard;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.registry.DataExportersRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DataExportPageInit extends ActiveWizardPage<DataExportWizard> {

    private TableViewer exporterTable;

    DataExportPageInit() {
        super("Export type");
        setTitle("Export type");
        setDescription("Choose export type");
        setPageComplete(false);
    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        exporterTable = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        exporterTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        exporterTable.getTable().setLinesVisible(true);
        exporterTable.setContentProvider(new IStructuredContentProvider() {
            public void dispose() {
            }
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
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
            columnName.getColumn().setText("Exporter");

            TableViewerColumn columnDesc = new TableViewerColumn(exporterTable, SWT.LEFT);
            columnDesc.setLabelProvider(labelProvider);
            columnDesc.getColumn().setText("Description");
        }

        loadExporters();

        exporterTable.getTable().addSelectionListener(new SelectionListener() {
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
        List<DBSDataContainer> dataProviders = getWizard().getSettings().getDataProviders();
        List<Class> rsSources = new ArrayList<Class>();
        for (DBSDataContainer provider : dataProviders) {
            rsSources.add(provider.getClass());
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