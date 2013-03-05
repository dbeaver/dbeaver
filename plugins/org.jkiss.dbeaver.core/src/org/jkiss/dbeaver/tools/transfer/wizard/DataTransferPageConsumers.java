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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.*;

class DataTransferPageConsumers extends ActiveWizardPage<DataTransferWizard> {

    private TableViewer consumersTable;

    private static class TransferTarget {
        IDataTransferConsumer consumer;
        IDataTransferProcessor processor;

        private TransferTarget(IDataTransferConsumer consumer, IDataTransferProcessor processor)
        {
            this.consumer = consumer;
            this.processor = processor;
        }
    }

    DataTransferPageConsumers() {
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

        consumersTable = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        consumersTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        consumersTable.getTable().setLinesVisible(true);
        consumersTable.setContentProvider(new IStructuredContentProvider() {
            @Override
            public void dispose()
            {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
            {
            }

            @Override
            public Object[] getElements(Object inputElement)
            {
                if (inputElement instanceof Collection) {
                    return ((Collection<?>) inputElement).toArray();
                }
                return new Object[0];
            }
        });
        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                TransferTarget element = (TransferTarget) cell.getElement();
                if (cell.getColumnIndex() == 0) {
                    if (element.processor != null) {
                        cell.setImage(element.processor.getIcon());
                        cell.setText(element.processor.getName());
                    } else {

                    }
                } else {
                    if (element.processor != null) {
                        cell.setText(element.processor.getDescription());
                    }
                }
            }
        };
        {
            TableViewerColumn columnName = new TableViewerColumn(consumersTable, SWT.LEFT);
            columnName.setLabelProvider(labelProvider);
            columnName.getColumn().setText(CoreMessages.dialog_export_wizard_init_column_exported);

            TableViewerColumn columnDesc = new TableViewerColumn(consumersTable, SWT.LEFT);
            columnDesc.setLabelProvider(labelProvider);
            columnDesc.getColumn().setText(CoreMessages.dialog_export_wizard_init_column_description);
        }

        loadPipes();

        consumersTable.getTable().addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                final IStructuredSelection selection = (IStructuredSelection) consumersTable.getSelection();
                TransferTarget target;
                if (!selection.isEmpty()) {
                    target = (TransferTarget) selection.getFirstElement();
                } else {
                    target = null;
                }
                getWizard().getSettings().setConsumer(target == null ? null : target.consumer);
                getWizard().getSettings().setProcessor(target == null ? null : target.processor);
                updatePageCompletion();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
                if (isPageComplete()) {
                    getWizard().getContainer().showPage(getWizard().getNextPage(DataTransferPageConsumers.this));
                }
            }
        });
        setControl(composite);

        UIUtils.packColumns(consumersTable.getTable());
        UIUtils.maxTableColumnsWidth(consumersTable.getTable());

        IDataTransferConsumer consumer = getWizard().getSettings().getConsumer();
        IDataTransferProcessor processor = getWizard().getSettings().getProcessor();
        if (consumer != null) {
            Collection<TransferTarget> targets = (Collection<TransferTarget>) consumersTable.getInput();
            for (TransferTarget target : targets) {
                if (target.consumer == consumer && target.processor == processor) {
                    consumersTable.setSelection(new StructuredSelection(target));
                    break;
                }
            }
        }
        updatePageCompletion();
    }

    private void loadPipes()
    {
        DataTransferSettings settings = getWizard().getSettings();
        List<DataTransferPipe> dataPipes = settings.getDataPipes();
        Set<Class<?>> objectTypes = new HashSet<Class<?>>();
        for (DataTransferPipe transferPipe : dataPipes) {
            objectTypes.add(transferPipe.getProducer().getSourceObject().getClass());
        }

        List<TransferTarget> transferTargets = new ArrayList<TransferTarget>();
        for (IDataTransferConsumer consumer : settings.getAvailableConsumers()) {
            Collection<IDataTransferProcessor> processors = consumer.getAvailableProcessors(objectTypes);
            for (IDataTransferProcessor processor : processors) {
                transferTargets.add(new TransferTarget(consumer, processor));
            }
        }
        consumersTable.setInput(transferTargets);
    }

    @Override
    protected boolean determinePageCompletion() {
        return getWizard().getSettings().getConsumer() != null && getWizard().getSettings().getProcessor() != null;
    }

}