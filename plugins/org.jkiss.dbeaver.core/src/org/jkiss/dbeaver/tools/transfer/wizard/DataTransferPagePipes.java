/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.transfer.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.registry.transfer.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.registry.transfer.DataTransferRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DataTransferPagePipes extends ActiveWizardPage<DataTransferWizard> {

    private TableViewer consumersTable;

    private static class TransferTarget {
        DataTransferNodeDescriptor consumer;
        DataTransferProcessorDescriptor processor;

        private TransferTarget(DataTransferNodeDescriptor consumer, DataTransferProcessorDescriptor processor)
        {
            this.consumer = consumer;
            this.processor = processor;
        }
    }

    DataTransferPagePipes() {
        super(CoreMessages.data_transfer_wizard_init_name);
        setTitle(CoreMessages.data_transfer_wizard_init_title);
        setDescription(CoreMessages.data_transfer_wizard_init_description);
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
                        cell.setImage(DBeaverIcons.getImage(element.processor.getIcon()));
                        cell.setText(element.processor.getName());
                    } else {
                        cell.setImage(DBeaverIcons.getImage(element.consumer.getIcon()));
                        cell.setText(element.consumer.getName());
                    }
                } else {
                    if (element.processor != null) {
                        cell.setText(element.processor.getDescription());
                    } else {
                        cell.setText(element.consumer.getDescription());
                    }
                }
            }
        };
        {
            TableViewerColumn columnName = new TableViewerColumn(consumersTable, SWT.LEFT);
            columnName.setLabelProvider(labelProvider);
            columnName.getColumn().setText(CoreMessages.data_transfer_wizard_init_column_exported);

            TableViewerColumn columnDesc = new TableViewerColumn(consumersTable, SWT.LEFT);
            columnDesc.setLabelProvider(labelProvider);
            columnDesc.getColumn().setText(CoreMessages.data_transfer_wizard_init_column_description);
        }

        loadConsumers();

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
                if (target == null) {
                    getWizard().getSettings().selectConsumer(null, null);
                } else {
                    getWizard().getSettings().selectConsumer(target.consumer, target.processor);
                }
                updatePageCompletion();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
                if (isPageComplete()) {
                    getWizard().getContainer().showPage(getWizard().getNextPage(DataTransferPagePipes.this));
                }
            }
        });
        consumersTable.getTable().addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e)
            {
                UIUtils.packColumns(consumersTable.getTable());
                UIUtils.maxTableColumnsWidth(consumersTable.getTable());
                consumersTable.getTable().removeControlListener(this);
            }
        });
        setControl(composite);

        DataTransferNodeDescriptor consumer = getWizard().getSettings().getConsumer();
        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
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

    private void loadConsumers()
    {
        DataTransferSettings settings = getWizard().getSettings();
        Collection<Class<?>> objectTypes = settings.getObjectTypes();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor consumer : DataTransferRegistry.getInstance().getAvailableConsumers(objectTypes)) {
            Collection<DataTransferProcessorDescriptor> processors = consumer.getAvailableProcessors(objectTypes);
            if (CommonUtils.isEmpty(processors)) {
                transferTargets.add(new TransferTarget(consumer, null));
            } else {
                for (DataTransferProcessorDescriptor processor : processors) {
                    transferTargets.add(new TransferTarget(consumer, processor));
                }
            }
        }
        consumersTable.setInput(transferTargets);
    }

    @Override
    protected boolean determinePageCompletion() {
        return getWizard().getSettings().getConsumer() != null;
    }

}