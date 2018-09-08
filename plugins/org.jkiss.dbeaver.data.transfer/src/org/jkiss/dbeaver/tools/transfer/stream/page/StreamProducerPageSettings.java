/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream.page;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StreamProducerPageSettings extends ActiveWizardPage<DataTransferWizard> {

    private PropertyTreeViewer propsEditor;
    private PropertySourceCustom propertySource;
    private Table filesTable;

    public StreamProducerPageSettings() {
        super(DTMessages.data_transfer_wizard_page_input_files_name);
        setTitle(DTMessages.data_transfer_wizard_page_input_files_title);
        setDescription(DTMessages.data_transfer_wizard_page_input_files_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        SashForm settingsDivider = new SashForm(parent, SWT.VERTICAL);
        settingsDivider.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group inputFilesGroup = new Group(settingsDivider, SWT.NONE);
            inputFilesGroup.setText(DTMessages.data_transfer_wizard_settings_group_input_files);
            inputFilesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            inputFilesGroup.setLayout(new GridLayout(1, false));

            filesTable = new Table(inputFilesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            filesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            filesTable.setHeaderVisible(true);
            filesTable.setLinesVisible(true);

            UIUtils.createTableColumn(filesTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(filesTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_target);

            filesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (filesTable.getSelectionIndex() < 0) {
                        return;
                    }
                    TableItem item = filesTable.getItem(filesTable.getSelectionIndex());
                    DataTransferPipe pipe = (DataTransferPipe) item.getData();
                    if (chooseSourceFile(pipe)) {
                        updateItemData(item, pipe);
                        updatePageCompletion();
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }
            });
            UIUtils.asyncExec(() -> UIUtils.packColumns(filesTable, true));
        }

        {
            Group exporterSettings = new Group(settingsDivider, SWT.NONE);
            exporterSettings.setText(DTMessages.data_transfer_wizard_settings_group_importer);
            exporterSettings.setLayoutData(new GridData(GridData.FILL_BOTH));
            exporterSettings.setLayout(new GridLayout(1, false));

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);
        }
        settingsDivider.setWeights(new int[]{ 300, 700 });

        setControl(settingsDivider);
    }

    private boolean chooseSourceFile(DataTransferPipe pipe) {
        List<String> extensions = new ArrayList<>();
        String extensionProp = CommonUtils.toString(propertySource.getPropertyValue(null, "extension"));
        for (String ext : extensionProp.split(",")) {
            extensions.add("*." + ext);
        }
        extensions.add("*");

        File file = DialogUtils.openFile(getShell(), extensions.toArray(new String[extensions.size()]));
        if (file != null) {
            pipe.setProducer(new StreamTransferProducer(file));
            return true;
        }
        return false;
    }

    private void updateItemData(TableItem item, DataTransferPipe pipe) {
        if (pipe.getProducer() == null || pipe.getProducer().getObjectName() == null) {
            item.setImage(0, null);
            item.setText(0, "<none>");
        } else {
            item.setImage(0, DBeaverIcons.getImage(getProducerProcessor().getIcon()));
            item.setText(0, pipe.getProducer().getObjectName());
        }
        if (pipe.getConsumer() == null || pipe.getConsumer().getDatabaseObject() == null) {
            item.setImage(1, null);
            item.setText(1, "<none>");
        } else {
            item.setImage(1, DBeaverIcons.getImage(getWizard().getSettings().getConsumer().getIcon()));
            item.setText(1, DBUtils.getObjectFullName(pipe.getConsumer().getDatabaseObject(), DBPEvaluationContext.DML));
        }
    }

    @Override
    public void activatePage() {
        DataTransferProcessorDescriptor processor = getProducerProcessor();
        propertySource = new PropertySourceCustom(
            processor.getProperties(),
            getWizard().getSettings().getProcessorProperties());
        propsEditor.loadProperties(propertySource);

        {
            DataTransferSettings settings = getWizard().getSettings();
            filesTable.removeAll();
            for (DataTransferPipe pipe : settings.getDataPipes()) {
                TableItem item = new TableItem(filesTable, SWT.NONE);
                item.setData(pipe);
                updateItemData(item, pipe);
            }
        }

        updatePageCompletion();
    }

    private DataTransferProcessorDescriptor getProducerProcessor() {
        //getWizard().getSettings().getDataPipes()
        return getWizard().getSettings().getProcessor();
    }

    @Override
    public void deactivatePage()
    {
        //final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);

        getWizard().getSettings().setProcessorProperties(propertySource.getPropertiesWithDefaults());

        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getConsumer() == null || pipe.getConsumer().getObjectName() == null || pipe.getProducer() == null || pipe.getProducer().getObjectName() == null) {
                return false;
            }
        }

        return true;
    }

}