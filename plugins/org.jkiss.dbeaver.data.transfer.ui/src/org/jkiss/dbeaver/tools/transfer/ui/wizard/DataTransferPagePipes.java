/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.UIWidgets;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DataTransferPagePipes extends ActiveWizardPage<DataTransferWizard> {

    private static final String DATABASE_PRODUCER_ID = "database_producer";
    private static final String DATABASE_CONSUMER_ID = "database_consumer";
    private boolean activated;
    private TableViewer nodesTable;
    private TableViewer inputsTable;

    private static class TransferTarget {
        DataTransferNodeDescriptor node;
        DataTransferProcessorDescriptor processor;

        private TransferTarget(DataTransferNodeDescriptor node, DataTransferProcessorDescriptor processor)
        {
            this.node = node;
            this.processor = processor;
        }
    }

    DataTransferPagePipes(@NotNull DataTransferSettings settings) {
        super(DTMessages.data_transfer_wizard_init_title);

        if (settings.isConsumerOptional()) {
            setTitle(DTMessages.data_transfer_wizard_init_title);
            setDescription(DTMessages.data_transfer_wizard_init_description);
        } else {
            setTitle(DTMessages.data_transfer_wizard_producers_title);
            setDescription(DTMessages.data_transfer_wizard_producers_description);
        }
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);

        SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        createNodesTable(sash);
        createInputsTable(sash);
        sash.setWeights(new int[]{70, 30});

        setControl(composite);
    }

    private void createNodesTable(Composite composite) {
        Composite panel = UIUtils.createComposite(composite, 1);

        //UIUtils.createControlLabel(panel, DTUIMessages.data_transfer_wizard_final_column_target);

        nodesTable = new TableViewer(panel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 400;
        gd.widthHint = 500;
        nodesTable.getTable().setLayoutData(gd);
        nodesTable.getTable().setLinesVisible(true);
        nodesTable.setContentProvider((IStructuredContentProvider) inputElement -> {
            if (inputElement instanceof Collection) {
                return ((Collection<?>) inputElement).toArray();
            }
            return new Object[0];
        });
        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                TransferTarget element = (TransferTarget) cell.getElement();
                String label;
                if (cell.getColumnIndex() == 0) {
                    if (element.processor != null) {
                        cell.setImage(DBeaverIcons.getImage(element.processor.getIcon()));
                        label = element.processor.getName();
                    } else {
                        cell.setImage(DBeaverIcons.getImage(element.node.getIcon()));
                        label = element.node.getName();
                    }
                } else {
                    if (element.processor != null) {
                        label = element.processor.getDescription();
                    } else {
                        label = element.node.getDescription();
                    }
                }
                cell.setText(label);
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

        nodesTable.getTable().addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                setSelectedSettings();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
                if (isPageComplete()) {
                    getWizard().getContainer().nextPressed();
                }
            }
        });
    }

    private void setSelectedSettings() {
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
        getWizard().getContainer().updateNavigationTree();
    }

    private void createInputsTable(Composite composite) {
        Composite panel = UIUtils.createComposite(composite, 1);

        //UIUtils.createControlLabel(panel, DTUIMessages.data_transfer_wizard_final_group_objects);

        inputsTable = new TableViewer(panel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.widthHint = 300;
        gd.heightHint = 300;
        inputsTable.getTable().setLayoutData(gd);
        inputsTable.getTable().setLinesVisible(true);
        inputsTable.setContentProvider(new ListContentProvider());
        UIWidgets.createTableContextMenu(inputsTable.getTable(), null);
        DBNModel nModel = DBWorkbench.getPlatform().getNavigatorModel();
        CellLabelProvider labelProvider = new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DBSObject element = (DBSObject) cell.getElement();
                if (cell.getColumnIndex() == 0) {
                    DBNDatabaseNode objectNode = nModel.getNodeByObject(element);
                    DBPImage icon = objectNode != null ? objectNode.getNodeIconDefault() : DBValueFormatting.getObjectImage(element);
                    cell.setImage(DBeaverIcons.getImage(icon));
                    final SQLQueryContainer queryContainer = DBUtils.getAdapter(SQLQueryContainer.class, element);
                    if (queryContainer != null) {
                        cell.setText(
                            CommonUtils.truncateString(
                                CommonUtils.getSingleLineString(queryContainer.getQuery().getText()), 64));
                    } else {
                        cell.setText(
                            CommonUtils.truncateString(
                                DBUtils.getObjectFullName(element, DBPEvaluationContext.UI), 64));
                    }
                }
            }
        };
        inputsTable.setLabelProvider(labelProvider);
    }

    @Override
    public void activatePage() {
        inputsTable.setInput(getWizard().getSettings().getSourceObjects());
        if (!activated) {
            UIUtils.asyncExec(this::loadNodeSettings);
        }
        if (activated && getWizard().getSettings().isPipeChangeRestricted()) {
            // Second activation - we need to disable any selectors
            nodesTable.getTable().setEnabled(false);
            return;
        }
        activated = true;
    }

    private void loadNodeSettings() {
        if (getWizard().getSettings().isConsumerOptional()) {
            loadConsumers();
        } else {
            loadProducers();
        }

        DataTransferNodeDescriptor consumer = getWizard().getSettings().getConsumer();
        DataTransferNodeDescriptor producer = getWizard().getSettings().getProducer();
        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        List<TransferTarget> targets = (List<TransferTarget>) nodesTable.getInput();
        TransferTarget currentTarget = null;
        if (consumer != null || producer != null) {
            for (TransferTarget target : targets) {
                if ((target.node == consumer || target.node == producer) && target.processor == processor) {
                    currentTarget = target;
                    break;
                }
            }
        }
        if (currentTarget == null && !targets.isEmpty()) {
            currentTarget = targets.get(0);
        }

        if (currentTarget != null) {
            StructuredSelection selection = new StructuredSelection(currentTarget);
            UIUtils.asyncExec(() -> {
                nodesTable.setSelection(selection);
                setSelectedSettings();
            });
        }

        inputsTable.setInput(getWizard().getSettings().getSourceObjects());

        UIUtils.packColumns(nodesTable.getTable());

        updatePageCompletion();
    }

    private void loadConsumers() {
        final DataTransferWizard wizard = getWizard();
        DataTransferSettings settings = wizard.getSettings();
        Collection<DBSObject> objects = settings.getSourceObjects();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor consumer : DataTransferRegistry.getInstance().getAvailableConsumers(objects)) {
            if (consumer.isAdvancedNode() && !DBWorkbench.hasFeature(DTConstants.PRODUCT_FEATURE_ADVANCED_DATA_TRANSFER)) {
                continue;
            }
            if (DATABASE_CONSUMER_ID.equals(consumer.getId())
                && !DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
                continue;
            }
            if (wizard.isTaskEditor() && settings.getConsumer() != null && !settings.getConsumer().getId().equals(consumer.getId())) {
                continue;
            }
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

    private void loadProducers() {
        final DataTransferWizard wizard = getWizard();
        DataTransferSettings settings = wizard.getSettings();
        Collection<DBSObject> objects = settings.getSourceObjects();

        List<TransferTarget> transferTargets = new ArrayList<>();
        for (DataTransferNodeDescriptor producer : DataTransferRegistry.getInstance().getAvailableProducers(objects)) {
            if (producer.isAdvancedNode() && !DBWorkbench.hasFeature(DTConstants.PRODUCT_FEATURE_ADVANCED_DATA_TRANSFER)) {
                continue;
            }
            if (DATABASE_PRODUCER_ID.equals(producer.getId())
                && !DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER)) {
                continue;
            }
            if (wizard.isTaskEditor() && settings.getProducer() != null && !settings.getProducer().getId().equals(producer.getId())) {
                continue;
            }

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
        DataTransferSettings settings = getWizard().getSettings();
        if (settings.getDataPipes().isEmpty()) {
            setErrorMessage("No objects selected");
            return false;
        }
        if (settings.getConsumer() == null || settings.getProducer() == null) {
            return false;
        }
//        if (settings.isProducerOptional()) {
//            settings.setProcessorProperties();
//        }

        return true;
    }

}