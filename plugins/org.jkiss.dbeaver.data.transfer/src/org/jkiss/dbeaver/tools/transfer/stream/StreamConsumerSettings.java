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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.processor.ExecuteCommandEventProcessor;
import org.jkiss.dbeaver.tools.transfer.processor.FailedExportFileCleanerProcessor;
import org.jkiss.dbeaver.tools.transfer.processor.ShowInExplorerEventProcessor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream transfer settings
 */
public class StreamConsumerSettings implements IDataTransferConsumerSettings {

    private static final Log log = Log.getLog(StreamConsumerSettings.class);

    public class ConsumerRuntimeParameters {
        public DataFileConflictBehavior dataFileConflictBehavior;
        public Integer dataFileConflictPreviousChoice = null;
        public BlobFileConflictBehavior blobFileConflictBehavior;
        public Integer blobFileConflictPreviousChoice = null;
        public boolean dontDropBlobFileConflictBehavior = false;
        public String outputFileNameToReuse = null;
        
        public ConsumerRuntimeParameters() {
            this.dataFileConflictBehavior = StreamConsumerSettings.this.dataFileConflictBehavior;
            this.blobFileConflictBehavior = StreamConsumerSettings.this.blobFileConflictBehavior;
        }

        /**
         * Initialize non-persistent parameters for data transfer execution which is shared between consumers of the task
         */
        public void initForConsumer() {
            if (!this.dontDropBlobFileConflictBehavior) {
                this.blobFileConflictBehavior = StreamConsumerSettings.this.blobFileConflictBehavior;
            }
        }
    }

    public enum LobExtractType {
        SKIP,
        FILES,
        INLINE
    }

    public enum LobEncoding {
        BASE64,
        HEX,
        BINARY,
        NATIVE
    }
    
    public enum DataFileConflictBehavior {
        ASK(DTMessages.data_transfer_file_conflict_ask),
        APPEND(DTMessages.data_transfer_file_conflict_append),
        PATCHNAME(DTMessages.data_transfer_file_conflict_fix_name),
        OVERWRITE(DTMessages.data_transfer_file_conflict_override);
        
        public final String title; 
        
        DataFileConflictBehavior(String title) {
            this.title = title;
        }
    }
    
    public enum BlobFileConflictBehavior {
        ASK(DTMessages.data_transfer_file_conflict_ask),
        PATCHNAME(DTMessages.data_transfer_file_conflict_fix_name),
        OVERWRITE(DTMessages.data_transfer_file_conflict_override);
        
        public final String title; 
        
        BlobFileConflictBehavior(String title) {
            this.title = title;
        }
    }

    public static final String PROP_EXTRACT_IMAGES = "extractImages";
    public static final String PROP_FILE_EXTENSION = "extension";

    private static final String SETTING_VALUE_FORMAT = "valueFormat"; //$NON-NLS-1$
    private static final String DATA_FILE_CONFLICT_BEHAVIOR = "dataFileConflictBehavior"; //$NON-NLS-1$
    private static final String BLOB_FILE_CONFLICT_BEHAVIOR = "blobFileConflictBehavior"; //$NON-NLS-1$

    private LobExtractType lobExtractType = LobExtractType.INLINE;
    private LobEncoding lobEncoding = LobEncoding.BINARY;

    private String outputFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
    private String outputFilePattern = GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE) + "_" + GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP);
    private String outputEncoding = GeneralUtils.getDefaultFileEncoding();
    private boolean outputEncodingBOM = false;
    private String outputTimestampPattern = GeneralUtils.DEFAULT_TIMESTAMP_PATTERN;

    private DBDDataFormatterProfile formatterProfile;
    @NotNull
    private DBDDisplayFormat valueFormat = DBDDisplayFormat.UI;
    private DataFileConflictBehavior dataFileConflictBehavior = DataFileConflictBehavior.ASK;
    private BlobFileConflictBehavior blobFileConflictBehavior = BlobFileConflictBehavior.ASK;
    private boolean outputClipboard = false;
    private boolean useSingleFile = false;
    private boolean compressResults = false;
    private boolean splitOutFiles = false;
    private long maxOutFileSize = 10 * 1000 * 1000;
    private final Map<DBSDataContainer, StreamMappingContainer> dataMappings = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> eventProcessors = new HashMap<>();


    public void setDataFileConflictBehavior(@NotNull DataFileConflictBehavior dataFileConflictBehavior) {
        this.dataFileConflictBehavior = dataFileConflictBehavior;
    }
    
    @NotNull
    public DataFileConflictBehavior getDataFileConflictBehavior() {
        return dataFileConflictBehavior;
    }

    public void setBlobFileConflictBehavior(@NotNull BlobFileConflictBehavior blobFileConflictBehavior) {
        this.blobFileConflictBehavior = blobFileConflictBehavior;
    }
    
    @NotNull
    public BlobFileConflictBehavior getBlobFileConflictBehavior() {
        return blobFileConflictBehavior;
    }
    
    @Override
    public ConsumerRuntimeParameters prepareRuntimeParameters() {
        return new ConsumerRuntimeParameters();
    }

    public LobExtractType getLobExtractType() {
        return lobExtractType;
    }

    public void setLobExtractType(LobExtractType lobExtractType) {
        this.lobExtractType = lobExtractType;
    }

    public LobEncoding getLobEncoding() {
        return lobEncoding;
    }

    public void setLobEncoding(LobEncoding lobEncoding) {
        this.lobEncoding = lobEncoding;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public String getOutputFilePattern() {
        return outputFilePattern;
    }

    public void setOutputFilePattern(String outputFilePattern) {
        this.outputFilePattern = outputFilePattern;
    }

    public String getOutputEncoding() {
        return outputEncoding;
    }

    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public boolean isOutputEncodingBOM() {
        return outputEncodingBOM;
    }

    public void setOutputEncodingBOM(boolean outputEncodingBOM) {
        this.outputEncodingBOM = outputEncodingBOM;
    }

    public String getOutputTimestampPattern() {
        return outputTimestampPattern;
    }

    public void setOutputTimestampPattern(String outputTimestampPattern) {
        this.outputTimestampPattern = outputTimestampPattern;
    }

    public boolean isOutputClipboard() {
        return outputClipboard;
    }

    public void setOutputClipboard(boolean outputClipboard) {
        this.outputClipboard = outputClipboard;
    }

    public boolean isUseSingleFile() {
        return useSingleFile;
    }

    public void setUseSingleFile(boolean useSingleFile) {
        this.useSingleFile = useSingleFile;
    }

    public boolean isCompressResults() {
        return compressResults;
    }

    public void setCompressResults(boolean compressResults) {
        this.compressResults = compressResults;
    }

    public boolean isSplitOutFiles() {
        return splitOutFiles;
    }

    public void setSplitOutFiles(boolean splitOutFiles) {
        this.splitOutFiles = splitOutFiles;
    }

    public long getMaxOutFileSize() {
        return maxOutFileSize;
    }

    public void setMaxOutFileSize(long maxOutFileSize) {
        this.maxOutFileSize = maxOutFileSize;
    }

    @NotNull
    public Map<DBSDataContainer, StreamMappingContainer> getDataMappings() {
        return dataMappings;
    }

    @Nullable
    public StreamMappingContainer getDataMapping(@NotNull DBSDataContainer container) {
        return dataMappings.get(container);
    }

    public void addDataMapping(@NotNull StreamMappingContainer container) {
        dataMappings.put(container.getSource(), container);
    }

    @NotNull
    public Map<String, Object> getEventProcessorSettings(@NotNull String id) {
        return eventProcessors.computeIfAbsent(id, x -> new HashMap<>());
    }

    @Override
    public void addEventProcessor(@NotNull DataTransferEventProcessorDescriptor descriptor) {
        eventProcessors.putIfAbsent(descriptor.getId(), new HashMap<>());
    }

    @Override
    public void removeEventProcessor(@NotNull DataTransferEventProcessorDescriptor descriptor) {
        eventProcessors.remove(descriptor.getId());
    }

    public boolean hasEventProcessor(@NotNull String id) {
        return eventProcessors.containsKey(id);
    }

    @NotNull
    public Map<String, Map<String, Object>> getEventProcessors() {
        return eventProcessors;
    }

    public DBDDataFormatterProfile getFormatterProfile() {
        return formatterProfile;
    }

    public void setFormatterProfile(DBDDataFormatterProfile formatterProfile) {
        this.formatterProfile = formatterProfile;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DataTransferSettings dataTransferSettings, Map<String, Object> settings) {
        lobExtractType = CommonUtils.valueOf(LobExtractType.class, CommonUtils.toString(settings.get("lobExtractType")), LobExtractType.INLINE);
        lobEncoding = CommonUtils.valueOf(LobEncoding.class, CommonUtils.toString(settings.get("lobEncoding")), LobEncoding.BINARY);

        outputFolder = CommonUtils.toString(settings.get("outputFolder"), outputFolder);
        outputFilePattern = CommonUtils.toString(settings.get("outputFilePattern"), outputFilePattern);
        outputEncoding = CommonUtils.toString(settings.get("outputEncoding"), outputEncoding);
        outputTimestampPattern = CommonUtils.toString(settings.get("outputTimestampPattern"), outputTimestampPattern);
        outputEncodingBOM = CommonUtils.getBoolean(settings.get("outputEncodingBOM"), outputEncodingBOM);
        outputClipboard = CommonUtils.getBoolean(settings.get("outputClipboard"), outputClipboard);
        dataFileConflictBehavior = CommonUtils.valueOf(
            DataFileConflictBehavior.class,
            CommonUtils.toString(settings.get(DATA_FILE_CONFLICT_BEHAVIOR)),
            DataFileConflictBehavior.PATCHNAME
        );
        blobFileConflictBehavior = CommonUtils.valueOf(
            BlobFileConflictBehavior.class,
            CommonUtils.toString(settings.get(BLOB_FILE_CONFLICT_BEHAVIOR)),
            BlobFileConflictBehavior.PATCHNAME
        );

        compressResults = CommonUtils.getBoolean(settings.get("compressResults"), compressResults);
        splitOutFiles = CommonUtils.getBoolean(settings.get("splitOutFiles"), splitOutFiles);
        maxOutFileSize = CommonUtils.toLong(settings.get("maxOutFileSize"), maxOutFileSize);

        final boolean openFolderOnFinish = CommonUtils.getBoolean(settings.get("openFolderOnFinish"), false);
        final boolean deleteFileInCaseOfFail = CommonUtils.getBoolean(settings.get("deleteFileInCaseOfFail"), true);
        final boolean executeProcessOnFinish = CommonUtils.getBoolean(settings.get("executeProcessOnFinish"), false);
        final String finishProcessCommand = CommonUtils.toString(settings.get("finishProcessCommand"));

        String formatterProfile = CommonUtils.toString(settings.get("formatterProfile"));
        if (!CommonUtils.isEmpty(formatterProfile)) {
            this.formatterProfile = DBWorkbench.getPlatform().getDataFormatterRegistry().getCustomProfile(formatterProfile);
        }
        valueFormat = DBDDisplayFormat.safeValueOf(CommonUtils.toString(settings.get(SETTING_VALUE_FORMAT)));

        final Map<String, Object> mappings = JSONUtils.getObjectOrNull(settings, "mappings");
        if (mappings != null && !mappings.isEmpty()) {
            try {
                runnableContext.run(true, true, monitor -> {
                    final List<DataTransferPipe> pipes = dataTransferSettings.getDataPipes();
                    for (DataTransferPipe pipe : pipes) {
                        final IDataTransferProducer<?> producer = pipe.getProducer();
                        if (producer != null) {
                            final DBSObject object = producer.getDatabaseObject();
                            if (object instanceof DBSDataContainer) {
                                final DBSDataContainer container = (DBSDataContainer) object;
                                final Map<String, Object> containerSettings = JSONUtils.getObjectOrNull(mappings, DBUtils.getObjectFullId(container));
                                if (containerSettings != null) {
                                    final StreamMappingContainer mappingContainer = new StreamMappingContainer(container);
                                    mappingContainer.loadSettings(monitor, containerSettings);
                                    addDataMapping(mappingContainer);
                                }
                            }
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError(
                    DTMessages.stream_transfer_consumer_title_configuration_load_failed,
                    DTMessages.stream_transfer_consumer_message_cannot_load_configuration,
                    e
                );
            } catch (InterruptedException e) {
                log.debug("Canceled by user", e);
            }
        }

        final Map<String, Object> processors = JSONUtils.getObject(settings, "eventProcessors");
        for (String processor : processors.keySet()) {
            eventProcessors.put(processor, JSONUtils.getObject(processors, processor));
        }

        if (openFolderOnFinish && !eventProcessors.containsKey(ShowInExplorerEventProcessor.ID)) {
            eventProcessors.put(ShowInExplorerEventProcessor.ID, new HashMap<>());
        }

        if (deleteFileInCaseOfFail && !eventProcessors.containsKey(FailedExportFileCleanerProcessor.ID)) {
            eventProcessors.put(FailedExportFileCleanerProcessor.ID, new HashMap<>());
        }

        if (executeProcessOnFinish && !eventProcessors.containsKey(ExecuteCommandEventProcessor.ID)) {
            final Map<String, Object> config = new HashMap<>();
            config.put(ExecuteCommandEventProcessor.PROP_COMMAND, finishProcessCommand);
            config.put(ExecuteCommandEventProcessor.PROP_WORKING_DIRECTORY, null);
            eventProcessors.put(ExecuteCommandEventProcessor.ID, config);
        }

        useSingleFile = CommonUtils.getBoolean(settings.get("useSingleFile"), useSingleFile)
            && dataTransferSettings.getDataPipes().size() > 1
            && dataTransferSettings.getProcessor().isAppendable();
    }

    @Override
    public void saveSettings(Map<String, Object> settings) {
        settings.put("lobExtractType", lobExtractType.name());
        settings.put("lobEncoding", lobEncoding.name());
        // settings.put("appendToFile", appendToFileEnd);
        settings.put(DATA_FILE_CONFLICT_BEHAVIOR, dataFileConflictBehavior.name());
        settings.put(BLOB_FILE_CONFLICT_BEHAVIOR, blobFileConflictBehavior.name());
        settings.put("outputFolder", outputFolder);
        settings.put("outputFilePattern", outputFilePattern);
        settings.put("outputEncoding", outputEncoding);
        settings.put("outputTimestampPattern", outputTimestampPattern);
        settings.put("outputEncodingBOM", outputEncodingBOM);
        settings.put("outputClipboard", outputClipboard);
        settings.put("useSingleFile", useSingleFile);

        settings.put("compressResults", compressResults);
        settings.put("splitOutFiles", splitOutFiles);
        settings.put("maxOutFileSize", maxOutFileSize);

        if (formatterProfile != null) {
            settings.put("formatterProfile", formatterProfile.getProfileName());
        } else {
            settings.put("formatterProfile", "");
        }
        settings.put(SETTING_VALUE_FORMAT, valueFormat.name());

        if (!dataMappings.isEmpty()) {
            final Map<String, Object> mappings = new LinkedHashMap<>();
            for (StreamMappingContainer container : dataMappings.values()) {
                final Map<String, Object> containerSettings = new LinkedHashMap<>();
                container.saveSettings(containerSettings);
                mappings.put(DBUtils.getObjectFullId(container.getSource()), containerSettings);
            }
            settings.put("mappings", mappings);
        }

        if (!eventProcessors.isEmpty()) {
            settings.put("eventProcessors", eventProcessors);
        }
    }
    
    @Override
    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();

        if (!outputClipboard) {
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_use_single_file, useSingleFile);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_directory, outputFolder);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_file_name_pattern, outputFilePattern);
            DTUtils.addSummary(summary, DTMessages.data_transfer_file_conflict_behavior_setting, dataFileConflictBehavior.title);
            DTUtils.addSummary(summary, DTMessages.data_transfer_blob_file_conflict_behavior_setting, blobFileConflictBehavior.title);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_encoding, outputEncoding);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_timestamp_pattern, outputTimestampPattern);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_insert_bom, outputEncodingBOM);
        } else {
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_copy_to_clipboard, outputClipboard);
        }

        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_compress, compressResults);

        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_settings_label_binaries, lobExtractType);
        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_settings_label_encoding, lobEncoding);
        if (formatterProfile != null) {
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_settings_label_formatting, formatterProfile.getProfileName());
        }

        return summary.toString();
    }

    @NotNull
    public DBDDisplayFormat getValueFormat() {
        return valueFormat;
    }

    public void setValueFormat(@NotNull DBDDisplayFormat valueFormat) {
        this.valueFormat = valueFormat;
    }
}
