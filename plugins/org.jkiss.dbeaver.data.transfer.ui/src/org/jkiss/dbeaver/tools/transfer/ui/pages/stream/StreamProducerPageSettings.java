/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.stream.StreamProducerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StreamProducerPageSettings extends DataTransferPageNodeSettings {
    private static final Log log = Log.getLog(StreamProducerPageSettings.class);

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
            Composite inputFilesGroup = UIUtils.createComposite(settingsDivider, 1);
            inputFilesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createControlLabel(inputFilesGroup, DTMessages.data_transfer_wizard_settings_group_input_files);

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
        }

        {
            Composite exporterSettings = UIUtils.createComposite(settingsDivider, 1);
            UIUtils.createControlLabel(exporterSettings, DTMessages.data_transfer_wizard_settings_group_importer);

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);
        }
        settingsDivider.setWeights(new int[]{400, 600});

        setControl(settingsDivider);

        updatePageCompletion();
    }

    private void chooseSourceFile(DataTransferPipe pipe) {
        List<String> extensions = new ArrayList<>();
        String extensionProp = CommonUtils.toString(propertySource.getPropertyValue(null, "extension"));
        for (String ext : extensionProp.split(",")) {
            extensions.add("*." + ext);
        }
        extensions.add("*");

        DBRRunnableWithProgress initializer = null;
        if (pipe.getConsumer() != null && pipe.getConsumer().getTargetObjectContainer() != null) {
            File[] files = DialogUtils.openFileList(getShell(), "Select input files", extensions.toArray(new String[0]));
            if (files != null && files.length > 0) {
                initializer = monitor -> updateMultiConsumers(monitor, pipe, files);
            }
        } else {
            File file = DialogUtils.openFile(getShell(), extensions.toArray(new String[0]));
            if (file != null) {
                initializer = monitor -> updateSingleConsumer(monitor, pipe, file);
            }
        }
        if (initializer != null) {
            try {
                getWizard().getRunnableContext().run(true, true, initializer);
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError("Column mappings error", "Error reading column mappings from stream", e.getTargetException());
                return;
            } catch (InterruptedException e) {
                // ignore
            }
        }
        reloadPipes();
        updatePageCompletion();
    }

    private void updateSingleConsumer(DBRProgressMonitor monitor, DataTransferPipe pipe, File file) {
        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);

        final StreamTransferProducer oldProducer = pipe.getProducer() instanceof StreamTransferProducer ? (StreamTransferProducer) pipe.getProducer() : null;
        final StreamTransferProducer newProducer = new StreamTransferProducer(new StreamEntityMapping(file));

        pipe.setProducer(newProducer);
        producerSettings.updateProducerSettingsFromStream(monitor, newProducer, getWizard().getSettings());

        IDataTransferSettings consumerSettings = getWizard().getSettings().getNodeSettings(getWizard().getSettings().getConsumer());
        if (consumerSettings instanceof DatabaseConsumerSettings) {
            DatabaseConsumerSettings settings = (DatabaseConsumerSettings) consumerSettings;
            DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, newProducer.getDatabaseObject());
            if (pipe.getConsumer() != null && pipe.getConsumer().getDatabaseObject() instanceof DBSDataManipulator) {
                DBSDataManipulator databaseObject = (DBSDataManipulator) pipe.getConsumer().getDatabaseObject();
                DBNDatabaseNode databaseNode = DBNUtils.getNodeByObject(monitor, databaseObject.getParentObject(), false);
                if (databaseNode != null) {
                    settings.setContainerNode(databaseNode);
                }
                mapping.setTarget(databaseObject);
            } else {
                mapping.setTarget(null);
                mapping.setTargetName(generateTableName(newProducer.getInputFile()));
            }
            if (oldProducer != null) {
                // Remove old mapping because we're just replaced file
                DatabaseMappingContainer oldMappingContainer = settings.getDataMappings().remove(oldProducer.getDatabaseObject());
                if (oldMappingContainer != null && oldMappingContainer.getSource() instanceof StreamEntityMapping) {
                    StreamEntityMapping oldEntityMapping = (StreamEntityMapping) oldMappingContainer.getSource();
                    // Copy mappings from old producer if columns are the same
                    if (oldEntityMapping.isSameColumns(newProducer.getEntityMapping())) {
                        StreamEntityMapping entityMapping = new StreamEntityMapping(file);
                        settings.addDataMappings(getWizard().getRunnableContext(), entityMapping, new DatabaseMappingContainer(oldMappingContainer, entityMapping));

                        StreamTransferProducer producer = new StreamTransferProducer(entityMapping);
                        pipe.setProducer(producer);
                        producerSettings.updateProducerSettingsFromStream(monitor, producer, getWizard().getSettings());

                        return;
                    }
                }
            }
            settings.addDataMappings(getWizard().getRunnableContext(), newProducer.getDatabaseObject(), mapping);
        }
    }

    private void updateMultiConsumers(DBRProgressMonitor monitor, DataTransferPipe pipe, File[] files) {
        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        IDataTransferConsumer<?, ?> originalConsumer = pipe.getConsumer();

        DataTransferSettings dtSettings = getWizard().getSettings();
        List<DataTransferPipe> newPipes = new ArrayList<>(dtSettings.getDataPipes());
        newPipes.remove(pipe);

        for (File file : files) {
            StreamTransferProducer producer = new StreamTransferProducer(new StreamEntityMapping(file));
            IDataTransferConsumer<?, ?> consumer = new DatabaseTransferConsumer();

            DataTransferPipe singlePipe = new DataTransferPipe(producer, consumer);
            try {
                singlePipe.initPipe(dtSettings, newPipes.size(), newPipes.size());
            } catch (DBException e) {
                log.error(e);
                continue;
            }
            newPipes.add(singlePipe);
            producerSettings.updateProducerSettingsFromStream(
                monitor,
                producer,
                dtSettings);

            IDataTransferSettings consumerSettings = dtSettings.getNodeSettings(dtSettings.getConsumer());
            if (consumerSettings instanceof DatabaseConsumerSettings) {
                DatabaseConsumerSettings dcs = (DatabaseConsumerSettings) consumerSettings;
                if (originalConsumer != null && originalConsumer.getTargetObjectContainer() instanceof DBSObject) {
                    DBNDatabaseNode containerNode = DBNUtils.getNodeByObject(
                        monitor, (DBSObject)originalConsumer.getTargetObjectContainer(), false);
                    if (containerNode != null) {
                        dcs.setContainerNode(containerNode);
                    }
                }
                DatabaseMappingContainer mapping = new DatabaseMappingContainer(dcs, producer.getDatabaseObject());
                //mapping.setTarget(null);
                mapping.setTargetName(generateTableName(producer.getInputFile()));

                dcs.addDataMappings(getWizard().getRunnableContext(), producer.getDatabaseObject(), mapping);
            }
        }

        dtSettings.setDataPipes(newPipes, false);
        dtSettings.setPipeChangeRestricted(true);
    }

    private void updateItemData(TableItem item, DataTransferPipe pipe) {
        if (isInvalidDataTransferNode(pipe.getProducer())) {
            item.setImage(0, null);
            item.setText(0, DTUIMessages.stream_consumer_page_settings_item_text_none);
        } else {
            item.setImage(0, DBeaverIcons.getImage(getProducerProcessor().getIcon()));
            item.setText(0, String.valueOf(pipe.getProducer().getObjectName()));
        }
        if (isInvalidDataTransferNode(pipe.getConsumer())) {
            item.setImage(1, null);
            item.setText(1, DTUIMessages.stream_consumer_page_settings_item_text_none);
        } else {
            item.setImage(1, DBeaverIcons.getImage(getWizard().getSettings().getConsumer().getIcon()));
            item.setText(1, String.valueOf(pipe.getConsumer().getObjectName()));
        }
    }

    private boolean isInvalidDataTransferNode(final IDataTransferNode<?> node) {
        return node == null || node.getObjectName() == null;
    }

    @Override
    public void activatePage() {
        getWizard().loadNodeSettings();

        // Initialize property editor
        DataTransferProcessorDescriptor processor = getProducerProcessor();
        DBPPropertyDescriptor[] properties = processor == null ? new DBPPropertyDescriptor[0] : processor.getProperties();
        propertySource = new PropertySourceCustom(
            properties,
            getWizard().getSettings().getProcessorProperties());
        propsEditor.loadProperties(propertySource);

        // Init pipes
        reloadPipes();

        updatePageCompletion();

        UIUtils.asyncExec(() -> UIUtils.packColumns(filesTable, true));
    }

    @Override
    public void deactivatePage() {
        // Save settings.
        // It is a producer so it must prepare data for consumers

        // Save processor properties
        propsEditor.saveEditorValues();
        Map<String, Object> processorProperties = propertySource.getPropertiesWithDefaults();
        DataTransferSettings dtSettings = getWizard().getSettings();
        dtSettings.setProcessorProperties(processorProperties);

        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        if (producerSettings != null) {
            producerSettings.setProcessorProperties(processorProperties);
        }

        // Update column mappings for database consumers
        IDataTransferSettings consumerSettings = getWizard().getSettings().getNodeSettings(getWizard().getSettings().getConsumer());

        try {
            getWizard().getRunnableContext().run(true, true, monitor -> {
                for (DataTransferPipe pipe : dtSettings.getDataPipes()) {
                    if (pipe.getProducer() instanceof StreamTransferProducer) {
                        StreamTransferProducer producer = (StreamTransferProducer) pipe.getProducer();
                        producerSettings.updateProducerSettingsFromStream(
                            monitor,
                            producer,
                            dtSettings);

                        if (consumerSettings instanceof DatabaseConsumerSettings) {
                            DatabaseMappingContainer mapping = ((DatabaseConsumerSettings) consumerSettings).getDataMapping(
                                producer.getDatabaseObject());
                            if (mapping != null) {
                                mapping.getAttributeMappings(monitor);
                            }
                        }
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Error updating stream settings", "Error updating settings", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }

        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion() {
        for (int i = 0; i < filesTable.getItemCount(); i++) {
            final DataTransferPipe pipe = (DataTransferPipe) filesTable.getItem(i).getData();
            if (isInvalidDataTransferNode(pipe.getConsumer()) || isInvalidDataTransferNode(pipe.getProducer())) {
                setMessage(DTUIMessages.stream_consumer_page_warning_not_enough_sources_chosen, IMessageProvider.WARNING);
                return false;
            }
        }
        setMessage(null);
        return true;
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
            DataTransferPipe pipe = dataPipes.get(0);
            if (pipe.getProducer() instanceof StreamTransferProducer && ((StreamTransferProducer) pipe.getProducer()).getInputFile() == null) {
                UIUtils.asyncExec(() -> chooseSourceFile(pipe));
            }
        }
    }

    private DataTransferProcessorDescriptor getProducerProcessor() {
        return getWizard().getSettings().getProcessor();
    }

    @NotNull
    private String generateTableName(File file) {
        StringBuilder name = new StringBuilder();
        String fileName = file.getName();
        // Cut off extension
        int divPos = fileName.lastIndexOf(".");
        if (divPos != -1) {
            fileName = fileName.substring(0, divPos);
        }
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

    @Override
    public boolean isPageApplicable() {
        return isProducerOfType(StreamTransferProducer.class);
    }

}
