/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DataTransferPagePipes extends ActiveWizardPage<DataTransferWizard> {

    private TableViewer nodesTable;

    private static class TransferTarget {
        DataTransferNodeDescriptor node;
        DataTransferProcessorDescriptor processor;

        private TransferTarget(DataTransferNodeDescriptor node, DataTransferProcessorDescriptor processor)
        {
            this.node = node;
            this.processor = processor;
        }
    }

    DataTransferPagePipes() {
        super(DTMessages.data_transfer_wizard_init_name);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        nodesTable = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        nodesTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        nodesTable.getTable().setLinesVisible(true);
        nodesTable.setContentProvider(new IStructuredContentProvider() {
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
                        cell.setImage(DBeaverIcons.getImage(element.node.getIcon()));
                        cell.setText(element.node.getName());
                    }
                } else {
                    if (element.processor != null) {
                        cell.setText(element.processor.getDescription());
                    } else {
                        cell.setText(element.node.getDescription());
                    }
                }
            }
        };
        {
            TableViewerColumn columnName = new TableViewerColumn(nodesTable, SWT.LEFT);
            columnName.setLabelProvider(labelProvider);
            columnName.getColumn().setText(DTMessages.data_transfer_wizard_init_column_exported);

            TableViewerColumn columnDesc = new TableViewerColumn(nodesTable, SWT.LEFT);
            columnDesc.setLabelProvider(labelProvider);
            columnDesc.getColumn().setText(DTMessages.data_transfer_wizard_init_column_description);
        }

        if (getWizard().getSettings().isConsumerOptional()) {
            setTitle(DTMessages.data_transfer_wizard_init_title);
            setDescription(DTMessages.data_transfer_wizard_init_description);

            loadConsumers();
        } else {
            setTitle(DTMessages.data_transfer_wizard_producers_title);
            setDescription(DTMessages.data_transfer_wizard_producers_description);

            loadProducers();
        }

        nodesTable.getTable().addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                final IStructuredSelection selection = (IStructuredSelection) nodesTable.getSelection();
                TransferTarget target;
                if (!selection.isEmpty()) {
                    target = (TransferTarget) selection.getFirstElement();
                } else {
                    target = null;
                }
                DataTransferSettings settings = getWizard().getSettings();
                if (target == null) {
                    settings.selectConsumer(null, null, true);
                } else {
                    if (settings.isConsumerOptional()) {
                        settings.selectConsumer(target.node, target.processor, true);
                    } else if (settings.isProducerOptional()) {
                        settings.selectProducer(target.node, target.processor, true);
                    } else {
                        // no optional nodes
                    }
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
        nodesTable.getTable().addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e)
            {
                UIUtils.packColumns(nodesTable.getTable());
                UIUtils.maxTableColumnsWidth(nodesTable.getTable());
                nodesTable.getTable().removeControlListener(this);
            }
        });
        setControl(composite);

        DataTransferNodeDescriptor consumer = getWizard().getSettings().getConsumer();
        DataTransferNodeDescriptor producer = getWizard().getSettings().getProducer();
        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        if (consumer != null || producer != null) {
            Collection<TransferTarget> targets = (Collection<TransferTarget>) nodesTable.getInput();
            for (TransferTarget target : targets) {
                if ((target.node == consumer || target.node == producer) && target.processor == processor) {
                    nodesTable.setSelection(new StructuredSelection(target));
                    break;
                }
            }
        }
        updatePageCompletion();
    }

    private void loadConsumers()
    {
        DataTransferSettings settings = getWizard().getSettings();
        Collection<DBSObject> objects = settings.getSourceObjects();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor consumer : DataTransferRegistry.getInstance().getAvailableConsumers(objects)) {
            Collection<DataTransferProcessorDescriptor> processors = consumer.getAvailableProcessors(objects);
            if (CommonUtils.isEmpty(processors)) {
                transferTargets.add(new TransferTarget(consumer, null));
            } else {
                for (DataTransferProcessorDescriptor processor : processors) {
                    transferTargets.add(new TransferTarget(consumer, processor));
                }
            }
        }
        nodesTable.setInput(transferTargets);
    }

    private void loadProducers()
    {
        DataTransferSettings settings = getWizard().getSettings();
        Collection<DBSObject> objects = settings.getSourceObjects();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor producer : DataTransferRegistry.getInstance().getAvailableProducers(objects)) {
            Collection<DataTransferProcessorDescriptor> processors = producer.getAvailableProcessors(objects);
            if (CommonUtils.isEmpty(processors)) {
                transferTargets.add(new TransferTarget(producer, null));
            } else {
                for (DataTransferProcessorDescriptor processor : processors) {
                    transferTargets.add(new TransferTarget(producer, processor));
                }
            }
        }
        nodesTable.setInput(transferTargets);
    }

    @Override
    protected boolean determinePageCompletion() {
        return getWizard().getSettings().getConsumer() != null;
    }

}