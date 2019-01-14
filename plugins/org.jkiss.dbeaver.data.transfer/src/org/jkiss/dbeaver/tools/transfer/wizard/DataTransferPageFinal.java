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
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.util.List;

class DataTransferPageFinal extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(DataTransferPageFinal.class);

    private Table resultTable;
    private boolean activated = false;
    private Text sourceSettingsText;
    private Text targetSettingsText;

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
        GridLayout gl = new GridLayout(2, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group tablesGroup = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_final_group_objects, 3, GridData.FILL_BOTH, 0);
            ((GridData)tablesGroup.getLayoutData()).horizontalSpan = 2;

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

        {
            Group sourceSettingsGroup = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_final_group_settings_source, 1, GridData.FILL_BOTH, 0);
            sourceSettingsText = new Text(sourceSettingsGroup, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
            sourceSettingsText.setLayoutData(new GridData(GridData.FILL_BOTH));

            Group targetSettingsGroup = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_final_group_settings_target, 1, GridData.FILL_BOTH, 0);
            targetSettingsText = new Text(targetSettingsGroup, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
            targetSettingsText.setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        setControl(composite);
    }

    @Override
    public void activatePage()
    {
        resultTable.removeAll();
        DataTransferSettings settings = getWizard().getSettings();
        List<DataTransferPipe> dataPipes = settings.getDataPipes();

        IDataTransferSettings producerSettings = null, consumerSettings = null;
        for (DataTransferPipe pipe : dataPipes) {
            IDataTransferConsumer consumer = pipe.getConsumer();
            if (consumer == null || pipe.getProducer() == null) {
                continue;
            }
            if (consumerSettings == null) {
                consumerSettings = settings.getNodeSettings(consumer);
            }
            if (producerSettings == null) {
                producerSettings = settings.getNodeSettings(pipe.getProducer());
            }
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
            {
                item.setText(0, pipe.getProducer().getObjectContainerName());
                if (pipe.getProducer().getObjectContainerIcon() != null) {
                    item.setImage(0, DBeaverIcons.getImage(pipe.getProducer().getObjectContainerIcon()));
                }
                item.setText(1, pipe.getProducer().getObjectName());
                DBPImage producerObjectIcon = pipe.getProducer().getObjectIcon();
                if (producerObjectIcon == null) {
                    producerObjectIcon = settings.getProducer().getIcon();
                }
                if (producerObjectIcon != null) {
                    item.setImage(1, DBeaverIcons.getImage(producerObjectIcon));
                }
                Color producerColor = pipe.getProducer().getObjectColor();
                if (producerColor != null) {
                    item.setBackground(0, producerColor);
                    item.setBackground(1, producerColor);
                }
            }
            {
                item.setText(2, consumer.getObjectContainerName());
                if (pipe.getConsumer().getObjectContainerIcon() != null) {
                    item.setImage(2, DBeaverIcons.getImage(pipe.getConsumer().getObjectContainerIcon()));
                }
                item.setText(3, consumer.getObjectName());
                DBPImage consumerObjectIcon = consumer.getObjectIcon();
                if (consumerObjectIcon == null && processorDescriptor != null) {
                    consumerObjectIcon = processorDescriptor.getIcon();
                }
                if (consumerObjectIcon == null && settings.getConsumer() != null) {
                    consumerObjectIcon = settings.getConsumer().getIcon();
                }
                if (consumerObjectIcon != null) {
                    item.setImage(3, DBeaverIcons.getImage(consumerObjectIcon));
                }
                Color consumerColor = pipe.getConsumer().getObjectColor();
                if (consumerColor != null) {
                    item.setBackground(2, consumerColor);
                    item.setBackground(3, consumerColor);
                }
            }
        }

        printSummary(sourceSettingsText,
            settings.getProducer(),
            producerSettings,
            settings.isProducerProcessor() ? settings.getProcessor() : null);
        printSummary(targetSettingsText,
            settings.getConsumer(),
            consumerSettings,
            settings.isProducerProcessor() ? null : settings.getProcessor());

        activated = true;
        UIUtils.packColumns(resultTable, true);
        updatePageCompletion();
    }

    private void printSummary(Text text, DataTransferNodeDescriptor node, IDataTransferSettings settings, DataTransferProcessorDescriptor processor) {
        StringBuilder summary = new StringBuilder();
        if (settings != null) {
            if (node != null) {
                summary.append(node.getName()).append(" settings:\n");
            }
            summary.append(CommonUtils.notEmpty(settings.getSettingsSummary()));
        }
        if (processor != null) {
            DTUtils.addSummary(summary, processor, getWizard().getSettings().getProcessorProperties());
        }
        text.setText(summary.toString());
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