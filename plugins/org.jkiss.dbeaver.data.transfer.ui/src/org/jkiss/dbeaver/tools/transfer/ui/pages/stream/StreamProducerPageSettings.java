/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.stream.StreamProducerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StreamProducerPageSettings extends ActiveWizardPage<DataTransferWizard> {

    private PropertyTreeViewer propsEditor;
    private PropertySourceCustom propertySource;
    private Table filesTable;

    public StreamProducerPageSettings() {
        super(DTMessages.data_transfer_wizard_page_input_files_name);
        setTitle(DTMessages.data_transfer_wizard_page_input_files_title);
        setDescription(DTMessages.data_transfer_wizard_page_input_files_description);
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        SashForm settingsDivider = new SashForm(parent, SWT.VERTICAL);

        {
            Composite inputFilesGroup = UIUtils.createControlGroup(settingsDivider, DTMessages.data_transfer_wizard_settings_group_input_files, 1, GridData.FILL_BOTH, 0);

            filesTable = new Table(inputFilesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            filesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            filesTable.setHeaderVisible(true);
            filesTable.setLinesVisible(true);

            UIUtils.createTableColumn(filesTable, SWT.LEFT, DTUIMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(filesTable, SWT.LEFT, DTUIMessages.data_transfer_wizard_final_column_target);

            filesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (filesTable.getSelectionIndex() < 0) {
                        return;
                    }
                    TableItem item = filesTable.getItem(filesTable.getSelectionIndex());
                    DataTransferPipe pipe = (DataTransferPipe) item.getData();
                    chooseSourceFile(pipe);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }
            });
            UIUtils.asyncExec(() -> UIUtils.packColumns(filesTable, true));
        }

        {
            Composite exporterSettings = UIUtils.createControlGroup(settingsDivider, DTMessages.data_transfer_wizard_settings_group_importer, 1, GridData.FILL_BOTH, 0);

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);
        }
        settingsDivider.setWeights(new int[]{400, 600});

        setControl(settingsDivider);

        updatePageCompletion();
    }

    private boolean chooseSourceFile(DataTransferPipe pipe) {
        List<String> extensions = new ArrayList<>();
        String extensionProp = CommonUtils.toString(propertySource.getPropertyValue(null, "extension"));
        for (String ext : extensionProp.split(",")) {
            extensions.add("*." + ext);
        }
        extensions.add("*");

        if (pipe.getConsumer() != null && pipe.getConsumer().getTargetObjectContainer() != null) {
            File[] files = DialogUtils.openFileList(getShell(), extensions.toArray(new String[0]));
            if (files != null && files.length > 0) {
                return updateMultiConsumers(pipe, files);
            }
        } else {
            File file = DialogUtils.openFile(getShell(), extensions.toArray(new String[0]));
            if (file != null) {
                return updateSingleConsumer(pipe, file);
            }
        }
        return false;
    }

    private boolean updateSingleConsumer(DataTransferPipe pipe, File file) {
        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);


        StreamTransferProducer producer = new StreamTransferProducer(new StreamEntityMapping(file));
        pipe.setProducer(producer);

        try {
            getWizard().getRunnableContext().run(true, true, monitor -> {
                producerSettings.updateProducerSettingsFromStream(
                    monitor,
                    producer,
                    getWizard().getSettings());
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Column mappings error", "Error reading column mappings from stream", e.getTargetException());
            return false;
        } catch (InterruptedException e) {
            // ignore
        }
        reloadPipes();
        updatePageCompletion();
        return true;
    }

    private boolean updateMultiConsumers(DataTransferPipe pipe, File[] files) {
        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);

        try {
            getWizard().getRunnableContext().run(true, true, monitor -> {
                IDataTransferConsumer originalConsumer = pipe.getConsumer();

                DataTransferSettings dtSettings = getWizard().getSettings();
                List<DataTransferPipe> newPipes = new ArrayList<>(dtSettings.getDataPipes());
                newPipes.remove(pipe);

                for (File file : files) {
                    StreamTransferProducer producer = new StreamTransferProducer(new StreamEntityMapping(file));
                    IDataTransferConsumer consumer = new DatabaseTransferConsumer();

                    DataTransferPipe singlePipe = new DataTransferPipe(producer, consumer);
                    try {
                        singlePipe.initPipe(dtSettings, newPipes.size(), newPipes.size());
                        newPipes.add(singlePipe);

                        producerSettings.updateProducerSettingsFromStream(
                            monitor,
                            producer,
                            dtSettings);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                    IDataTransferSettings consumerSettings = dtSettings.getNodeSettings(dtSettings.getConsumer());
                    if (consumerSettings instanceof DatabaseConsumerSettings) {
                        DatabaseConsumerSettings dcs = (DatabaseConsumerSettings) consumerSettings;
                        DatabaseMappingContainer mapping = new DatabaseMappingContainer(dcs, producer.getDatabaseObject());
                        mapping.setTargetName(generateTableName(file));
                        dcs.addDataMappings(getWizard().getRunnableContext(), producer.getDatabaseObject(), mapping);

                        if (originalConsumer != null && originalConsumer.getTargetObjectContainer() instanceof DBSObject) {
                            DBNDatabaseNode containerNode = DBNUtils.getNodeByObject(
                                monitor, (DBSObject)originalConsumer.getTargetObjectContainer(), false);
                            if (containerNode != null) {
                                dcs.setContainerNode(containerNode);
                            }
                        }
                    }
                }

                dtSettings.setDataPipes(newPipes, true);
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Column mappings error", "Error reading column mappings from stream", e.getTargetException());
            return false;
        } catch (InterruptedException e) {
            // ignore
        }
        reloadPipes();
        updatePageCompletion();
        return true;
    }

    @NotNull
    private String generateTableName(File file) {
        StringBuilder name = new StringBuilder();
        String fileName = file.getName();
        boolean lastCharSpecial = false;
        char lastChar = (char)0;
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (!Character.isLetter(c) && lastCharSpecial) {
                break;
            }
            lastCharSpecial = !Character.isLetterOrDigit(c);
            if (lastCharSpecial) {
                if (c != '_') c = '_';
                if (lastChar == '_') {
                    continue;
                }
            }
            lastChar = c;
            name.append(c);
        }
        if (name.length() > 0 && name.charAt(name.length() - 1) == '_') {
            name.deleteCharAt(name.length() - 1);
        }
        return name.toString();
    }

    private void updateItemData(TableItem item, DataTransferPipe pipe) {
        if (pipe.getProducer() == null || pipe.getProducer().getObjectName() == null) {
            item.setImage(0, null);
            item.setText(0, DTUIMessages.stream_consumer_page_settings_item_text_none);
        } else {
            item.setImage(0, DBeaverIcons.getImage(getProducerProcessor().getIcon()));
            item.setText(0, pipe.getProducer().getObjectName());
        }
        if (pipe.getConsumer() == null) {
            item.setImage(1, null);
            item.setText(1, DTUIMessages.stream_consumer_page_settings_item_text_none);
        } else {
            item.setImage(1, DBeaverIcons.getImage(getWizard().getSettings().getConsumer().getIcon()));
            item.setText(1, pipe.getConsumer().getObjectName());
        }
    }

    @Override
    public void activatePage() {
        DataTransferProcessorDescriptor processor = getProducerProcessor();
        DBPPropertyDescriptor[] properties = processor == null ? new DBPPropertyDescriptor[0] : processor.getProperties();
        propertySource = new PropertySourceCustom(
            properties,
            getWizard().getSettings().getProcessorProperties());
        propsEditor.loadProperties(propertySource);

        reloadPipes();

        updatePageCompletion();
    }

    private void reloadPipes() {
        boolean firstTime = filesTable.getItemCount() == 0;
        DataTransferSettings settings = getWizard().getSettings();
        filesTable.removeAll();
        List<DataTransferPipe> dataPipes = settings.getDataPipes();
        for (DataTransferPipe pipe : dataPipes) {
            TableItem item = new TableItem(filesTable, SWT.NONE);
            item.setData(pipe);
            updateItemData(item, pipe);
        }
        if (firstTime && !dataPipes.isEmpty()) {
            chooseSourceFile(dataPipes.get(0));
        }
    }

    private DataTransferProcessorDescriptor getProducerProcessor() {
        //getWizard().getSettings().getDataPipes()
        return getWizard().getSettings().getProcessor();
    }

    @Override
    public void deactivatePage() {
        Map<String, Object> processorProperties = propertySource.getPropertiesWithDefaults();
        getWizard().getSettings().setProcessorProperties(processorProperties);

        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        if (producerSettings != null) {
            producerSettings.setProcessorProperties(processorProperties);
        }

        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion() {
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getConsumer() == null || pipe.getConsumer().getObjectName() == null || pipe.getProducer() == null || pipe.getProducer().getObjectName() == null) {
                return false;
            }
        }

        return true;
    }

}