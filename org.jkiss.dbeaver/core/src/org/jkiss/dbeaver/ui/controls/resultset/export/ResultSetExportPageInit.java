/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset.export;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.dialogs.WizardDataTransferPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.registry.DataExportersRegistry;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.List;

class ResultSetExportPageInit extends WizardDataTransferPage {

    private TableViewer exporterTable;

    ResultSetExportPageInit() {
        super("Choose export type");
        setTitle("Export type");
        setDescription("Choose export type");
        setPageComplete(false);
    }

    @Override
    protected boolean allowNewContainerName() {
        return false;
    }

    public void handleEvent(Event event) {

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
                    return ((Collection)inputElement).toArray();
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

        setControl(composite);

        UIUtils.packColumns(exporterTable.getTable());
        UIUtils.maxTableColumnsWidth(exporterTable.getTable());
    }

    private void loadExporters() {
        ResultSetExportWizard wizard = (ResultSetExportWizard)getWizard();
        IResultSetProvider resultSetProvider = wizard.getResultSetProvider();
        DBPObject rsSource = resultSetProvider.getResultSetSource();

        DataExportersRegistry registry = DBeaverCore.getInstance().getDataExportersRegistry();
        List<DataExporterDescriptor> exporters = registry.getDataExporters(rsSource == null ? Object.class : rsSource.getClass());
        exporterTable.setInput(exporters);
    }

}