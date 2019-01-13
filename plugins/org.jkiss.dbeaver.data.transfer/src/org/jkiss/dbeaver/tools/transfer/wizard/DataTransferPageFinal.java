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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.List;

class DataTransferPageFinal extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(DataTransferPageFinal.class);

    private Table resultTable;
    private boolean activated = false;

    DataTransferPageFinal() {
        super(DTMessages.data_transfer_wizard_final_name);
        setTitle(DTMessages.data_transfer_wizard_final_title);
        setDescription(DTMessages.data_transfer_wizard_final_description);
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
            Group tablesGroup = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_final_group_tables, 3, GridData.FILL_BOTH, 0);

            resultTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            resultTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultTable.setHeaderVisible(true);
            resultTable.setLinesVisible(true);

            UIUtils.createTableColumn(resultTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_source_container);
            UIUtils.createTableColumn(resultTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(resultTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_target_container);
            UIUtils.createTableColumn(resultTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_target);

            UIUtils.packColumns(resultTable);
        }

        setControl(composite);
    }

    @Override
    public void activatePage()
    {
        resultTable.removeAll();
        DataTransferSettings settings = getWizard().getSettings();
        List<DataTransferPipe> dataPipes = settings.getDataPipes();
        for (DataTransferPipe pipe : dataPipes) {
            IDataTransferConsumer consumer = pipe.getConsumer();
            if (consumer == null || pipe.getProducer() == null) {
                continue;
            }
            IDataTransferSettings consumerSettings = settings.getNodeSettings(consumer);
            DataTransferProcessorDescriptor processorDescriptor = settings.getProcessor();
            IDataTransferProcessor processor = null;
            if (processorDescriptor != null) {
                // Processor is optional
                try {
                    processor = processorDescriptor.getInstance();
                } catch (Throwable e) {
                    log.error("Can't create processor", e);
                    continue;
                }
            }
            consumer.initTransfer(
                pipe.getProducer().getDatabaseObject(),
                consumerSettings,
                processorDescriptor != null && processorDescriptor.isBinaryFormat(),
                processor,
                processor == null ?
                    null :
                    settings.getProcessorProperties());


            TableItem item = new TableItem(resultTable, SWT.NONE);
            item.setText(0, pipe.getProducer().getObjectContainerName());
            item.setText(1, pipe.getProducer().getObjectName());
            if (settings.getProducer() != null & settings.getProducer().getIcon() != null) {
                item.setImage(1, DBeaverIcons.getImage(settings.getProducer().getIcon()));
            }
            Color producerColor = pipe.getProducer().getObjectColor();
            if (producerColor != null) {
                item.setBackground(0, producerColor);
                item.setBackground(1, producerColor);
            }

            item.setText(2, consumer.getObjectContainerName());
            item.setText(3, consumer.getObjectName());
            if (processorDescriptor != null && processorDescriptor.getIcon() != null) {
                item.setImage(3, DBeaverIcons.getImage(processorDescriptor.getIcon()));
            } else if (settings.getConsumer() != null && settings.getConsumer().getIcon() != null) {
                item.setImage(3, DBeaverIcons.getImage(settings.getConsumer().getIcon()));
            }
            Color consumerColor = pipe.getConsumer().getObjectColor();
            if (consumerColor != null) {
                item.setBackground(2, consumerColor);
                item.setBackground(3, consumerColor);
            }
        }
        activated = true;
        UIUtils.packColumns(resultTable, true);
        updatePageCompletion();
    }

    public boolean isActivated()
    {
        return activated;
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return activated;
    }

}