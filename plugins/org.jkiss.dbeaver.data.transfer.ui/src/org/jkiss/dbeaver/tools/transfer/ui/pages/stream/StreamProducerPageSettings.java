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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
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
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class StreamProducerPageSettings extends DataTransferPageNodeSettings {
    private static final Log log = Log.getLog(StreamProducerPageSettings.class);
    private static final String HELP_DATA_TRANSFER_LINK = "Data-transfer#import-parameters";

    private PropertyTreeViewer propsEditor;
    private PropertySourceCustom propertySource;
    private Table filesTable;
    private ToolItem tiOpenLocal;
    private ToolItem tiOpenRemote;

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

            final Composite inputFilesTableGroup = new Composite(inputFilesGroup, SWT.BORDER);
            inputFilesTableGroup.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
            inputFilesTableGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            DBPProject project = getWizard().getProject();
            boolean showLocalFS = true;//!DBWorkbench.isDistributed();
            boolean showRemoteFS = project != null && DBFUtils.supportsMultiFileSystems(project);

            if (showLocalFS || showRemoteFS) {
                final ToolBar toolbar = new ToolBar(inputFilesTableGroup, SWT.HORIZONTAL | SWT.FLAT | SWT.RIGHT);
                if (showLocalFS) {
                    tiOpenLocal = UIUtils.createToolItem(
                        toolbar,
                        UIMessages.text_with_open_dialog_browse,
                        UIMessages.text_with_open_dialog_browse,
                        DBIcon.TREE_FOLDER,
                        SelectionListener.widgetSelectedAdapter(e -> new SelectInputFileAction(false).run())
                    );
                }
                if (showRemoteFS) {
                    tiOpenRemote = UIUtils.createToolItem(
                        toolbar,
                        UIMessages.text_with_open_dialog_browse_remote,
                        UIMessages.text_with_open_dialog_browse_remote,
                        UIIcon.OPEN_EXTERNAL,
                        SelectionListener.widgetSelectedAdapter(e -> new SelectInputFileAction(true).run())
                    );
                }

                UIUtils.createLabelSeparator(inputFilesTableGroup, SWT.HORIZONTAL);
            }

            filesTable = new Table(inputFilesTableGroup, SWT.SINGLE | SWT.FULL_SELECTION);
            filesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            filesTable.setHeaderVisible(true);
            filesTable.setLinesVisible(true);

            if (showLocalFS || showRemoteFS) {
                UIWidgets.setControlContextMenu(filesTable, manager -> {
                    if (showLocalFS) {
                        manager.add(new SelectInputFileAction(false));
                    }
                    if (showRemoteFS) {
                        manager.add(new SelectInputFileAction(true));
                    }
                });
            }

            if (DBWorkbench.getPlatform().getApplication().isDistributed()) {
                UIUtils.createInfoLink(
                    inputFilesGroup,
                    "You cannot use files stored on this PC in scheduled tasks." +
                        "\nIf you want to use the files for export/import, please transfer them to <a href=\"#\">Cloud Storage</a>.",
                    () -> ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("Cloud-Storage"))
                ).setToolTipText("Scheduled tasks don't have access to files stored on this PC because they're executed on a remote server.");
            }

            UIUtils.createTableColumn(filesTable, SWT.LEFT, DTUIMessages.data_transfer_wizard_final_column_source);
            List<DBSObject> sourceObjects = getWizard().getSettings().getSourceObjects();
            boolean skipTargetColumn;
            if (CommonUtils.isEmpty(sourceObjects)) {
                skipTargetColumn = true;
            } else {
                boolean allSourceObjectsNotTables = true;
                for (DBSObject sourceObject : sourceObjects) {
                    if (sourceObject instanceof DBSDataManipulator) {
                        allSourceObjectsNotTables = false;
                        break;
                    }
                }
                skipTargetColumn = allSourceObjectsNotTables;
            }
            if (!skipTargetColumn) {
                UIUtils.createTableColumn(filesTable, SWT.LEFT, DTUIMessages.data_transfer_wizard_final_column_target);
            }

            {
                filesTable.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updateBrowseButtons();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        new SelectInputFileAction(!showLocalFS).run();
                    }
                });
            }
        }

        {
            Composite exporterSettings = UIUtils.createComposite(settingsDivider, 1);
            UIUtils.createControlLabel(exporterSettings, DTMessages.data_transfer_wizard_settings_group_importer);

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);

            UIUtils.createInfoLink(
                exporterSettings,
                DTUIMessages.stream_producer_page_input_files_hint,
                () -> ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(HELP_DATA_TRANSFER_LINK))
            );
        }
        settingsDivider.setWeights(400, 600);

        setControl(settingsDivider);

        updatePageCompletion();
    }

    private void chooseSourceFile(DataTransferPipe pipe, boolean remoteFS) {
        final String[] extensions = new String[]{
            "*." + CommonUtils.toString(propertySource.getPropertyValue(null, "extension")).replace(",", ";*."),
            "*.*"
        };

        DBRRunnableWithProgress initializer = null;

        DBPProject project = pipe.getConsumer().getProject();
        if (remoteFS && project != null) {
            DBNPathBase selected = DBWorkbench.getPlatformUI().openFileSystemSelector(
                DTUIMessages.stream_producer_select_input_file,
                false,
                SWT.OPEN,
                false,
                extensions,
                pipe.getConsumer().getObjectName());
            if (selected != null) {
                initializer = monitor -> {
                    updateSingleConsumer(monitor, pipe, selected.getPath());
                };
            }
        } else if (pipe.getConsumer() != null && pipe.getConsumer().getTargetObjectContainer() != null) {
            File[] files = DialogUtils.openFileList(
                getShell(),
                DTUIMessages.stream_producer_select_input_file,
                extensions);
            if (files != null && files.length > 0) {
                initializer = monitor -> updateMultiConsumers(
                    monitor,
                    pipe,
                    Arrays.stream(files).map(File::toPath).toArray(Path[]::new));
            }
        } else {
            File file = DialogUtils.openFile(getShell(), extensions);
            if (file != null) {
                initializer = monitor -> updateSingleConsumer(monitor, pipe, file.toPath());
            }
        }
        if (initializer != null) {
            try {
                getWizard().getRunnableContext().run(true, true, initializer);
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError(
                    DTUIMessages.stream_producer_column_mapping_error_title,
                    DTUIMessages.stream_producer_column_mapping_error_message,
                    e.getTargetException());
                return;
            } catch (InterruptedException e) {
                // ignore
            }
        }
        reloadPipes();
        updatePageCompletion();
    }

    private void updateSingleConsumer(DBRProgressMonitor monitor, DataTransferPipe pipe, Path path) {
        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);

        final StreamTransferProducer oldProducer = pipe.getProducer() instanceof StreamTransferProducer stp ? stp : null;
        final StreamTransferProducer newProducer = new StreamTransferProducer(new StreamEntityMapping(path));

        pipe.setProducer(newProducer);
        producerSettings.updateProducerSettingsFromStream(monitor, newProducer, getWizard().getSettings());

        IDataTransferSettings consumerSettings = getWizard().getSettings().getNodeSettings(getWizard().getSettings().getConsumer());
        if (consumerSettings instanceof DatabaseConsumerSettings settings) {
            DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, newProducer.getDatabaseObject());
            if (pipe.getConsumer() != null && pipe.getConsumer().getDatabaseObject() instanceof DBSDataManipulator databaseObject) {
                DBSObject container = databaseObject.getParentObject();
                if (container instanceof DBSObjectContainer) {
                    settings.setContainer((DBSObjectContainer) container);
                }
                mapping.setTarget(databaseObject);
            } else {
                mapping.setTarget(null);
                mapping.setTargetName(generateTableName(newProducer.getObjectName()));
            }
            if (oldProducer != null) {
                // Remove old mapping because we're just replaced file
                DatabaseMappingContainer oldMappingContainer = settings.getDataMappings().remove(oldProducer.getDatabaseObject());
                if (oldMappingContainer != null && oldMappingContainer.getSource() instanceof StreamEntityMapping oldEntityMapping) {
                    // Copy mappings from old producer if columns are the same
                    if (oldEntityMapping.isSameColumns(newProducer.getEntityMapping())) {
                        StreamEntityMapping entityMapping = new StreamEntityMapping(path);
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

    private void updateMultiConsumers(DBRProgressMonitor monitor, DataTransferPipe pipe, Path[] files) {
        final StreamProducerSettings producerSettings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        IDataTransferConsumer<?, ?> originalConsumer = pipe.getConsumer();

        DataTransferSettings dtSettings = getWizard().getSettings();
        List<DataTransferPipe> newPipes = new ArrayList<>(dtSettings.getDataPipes());
        newPipes.remove(pipe);

        final Deque<StreamEntityMapping> pendingEntityMappings = Arrays.stream(files)
            .map(StreamEntityMapping::new)
            .collect(Collectors.toCollection(ArrayDeque::new));

        while (!pendingEntityMappings.isEmpty()) {
            final StreamEntityMapping entityMapping = pendingEntityMappings.remove();

            if (producerSettings.extractExtraEntities(monitor, entityMapping, dtSettings, pendingEntityMappings)) {
                continue;
            }

            StreamTransferProducer producer = new StreamTransferProducer(entityMapping);
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
            if (consumerSettings instanceof DatabaseConsumerSettings dcs) {
                if (originalConsumer != null && originalConsumer.getTargetObjectContainer() instanceof DBSObjectContainer oc) {
                    dcs.setContainer(oc);
                }
                DatabaseMappingContainer mapping = new DatabaseMappingContainer(dcs, producer.getDatabaseObject());
                //mapping.setTarget(null);
                mapping.setTargetName(generateTableName(producer.getObjectName()));

                dcs.addDataMappings(getWizard().getRunnableContext(), producer.getDatabaseObject(), mapping);
            }
        }

        dtSettings.setDataPipes(newPipes, false);
        dtSettings.setPipeChangeRestricted(true);
    }

    private void updateItemData(TableItem item, DataTransferPipe pipe) {
        IDataTransferProducer<?> producer = pipe.getProducer();
        if (isInvalidDataTransferNode(producer)) {
            item.setImage(0, null);
            item.setText(0, DTUIMessages.stream_consumer_page_settings_item_text_none);
        } else {
            item.setImage(0, DBeaverIcons.getImage(getProducerProcessor().getIcon()));
            item.setText(0, producer instanceof StreamTransferProducer stp ?
                stp.getInputFile().toString() : String.valueOf(producer.getObjectName()));
        }

        IDataTransferConsumer<?, ?> consumer = pipe.getConsumer();
        if (isInvalidDataTransferNode(consumer)) {
            item.setImage(1, null);
            item.setText(1, DTUIMessages.stream_consumer_page_settings_item_text_none);
        } else {
            item.setImage(1, DBeaverIcons.getImage(getWizard().getSettings().getConsumer().getIcon()));
            item.setText(1, String.valueOf(consumer.getObjectName()));
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
                    if (pipe.getProducer() instanceof StreamTransferProducer producer) {
                        if (producerSettings != null) {
                            producerSettings.updateProducerSettingsFromStream(
                                monitor,
                                producer,
                                dtSettings);
                        }

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
        int selectionIndex = filesTable.getSelectionIndex();
        filesTable.removeAll();
        List<DataTransferPipe> dataPipes = settings.getDataPipes();
        for (DataTransferPipe pipe : dataPipes) {
            TableItem item = new TableItem(filesTable, SWT.NONE);
            item.setData(pipe);
            updateItemData(item, pipe);
        }
        if (!dataPipes.isEmpty()) {
            if (selectionIndex < 0) {
                selectionIndex = 0;
            } else if (selectionIndex >= dataPipes.size()) {
                selectionIndex = dataPipes.size() - 1;
            }
            DataTransferPipe pipe = dataPipes.get(selectionIndex);
            filesTable.select(selectionIndex);
            if (firstTime) {
                if (pipe.getProducer() instanceof StreamTransferProducer stp && stp.getInputFile() == null) {
                    UIUtils.asyncExec(() -> chooseSourceFile(pipe, DBWorkbench.isDistributed() && getWizard().getCurrentTask() != null));
                }
            }
        }
        updateBrowseButtons();
    }

    private void updateBrowseButtons() {
        boolean hasSelection = filesTable.getSelection().length > 0;
        if (tiOpenLocal != null) tiOpenLocal.setEnabled(hasSelection);
        if (tiOpenRemote != null) tiOpenRemote.setEnabled(hasSelection);
    }

    private DataTransferProcessorDescriptor getProducerProcessor() {
        return getWizard().getSettings().getProcessor();
    }

    @NotNull
    private String generateTableName(String fileName) {
        StringBuilder name = new StringBuilder();
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

    private class SelectInputFileAction extends Action {
        private boolean remote;
        public SelectInputFileAction(boolean remote) {
            super(remote ? UIMessages.text_with_open_dialog_browse_remote : UIMessages.text_with_open_dialog_browse);
            this.remote = remote;
        }

        @Override
        public void run() {
            if (filesTable.getSelectionIndex() < 0) {
                return;
            }
            TableItem item = filesTable.getItem(filesTable.getSelectionIndex());
            DataTransferPipe pipe = (DataTransferPipe) item.getData();
            chooseSourceFile(pipe, remote);
        }
    }

}
