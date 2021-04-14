/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream transfer settings
 */
public class StreamConsumerSettings implements IDataTransferSettings {

    private static final Log log = Log.getLog(StreamConsumerSettings.class);

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

    public static final String PROP_EXTRACT_IMAGES = "extractImages";
    public static final String PROP_FILE_EXTENSION = "extension";

    private static final String SETTING_VALUE_FORMAT = "valueFormat"; //$NON-NLS-1$

    private LobExtractType lobExtractType = LobExtractType.SKIP;
    private LobEncoding lobEncoding = LobEncoding.HEX;

    private String outputFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
    private String outputFilePattern = GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE) + "_" + GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP);
    private String outputEncoding = GeneralUtils.getDefaultFileEncoding();
    private boolean outputEncodingBOM = false;
    private String outputTimestampPattern = GeneralUtils.DEFAULT_TIMESTAMP_PATTERN;

    private DBDDataFormatterProfile formatterProfile;
    @NotNull
    private DBDDisplayFormat valueFormat = DBDDisplayFormat.UI;

    private boolean outputClipboard = false;
    private boolean useSingleFile = false;
    private boolean compressResults = false;
    private boolean splitOutFiles = false;
    private long maxOutFileSize = 10 * 1000 * 1000;
    private boolean openFolderOnFinish = true;
    private boolean executeProcessOnFinish = false;
    private String finishProcessCommand = null;
    private final Map<DBSDataContainer, StreamMappingContainer> dataMappings = new LinkedHashMap<>();

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

    public boolean isOpenFolderOnFinish() {
        return openFolderOnFinish;
    }

    public void setOpenFolderOnFinish(boolean openFolderOnFinish) {
        this.openFolderOnFinish = openFolderOnFinish;
    }

    public boolean isExecuteProcessOnFinish() {
        return executeProcessOnFinish;
    }

    public void setExecuteProcessOnFinish(boolean executeProcessOnFinish) {
        this.executeProcessOnFinish = executeProcessOnFinish;
    }

    public String getFinishProcessCommand() {
        return finishProcessCommand;
    }

    public void setFinishProcessCommand(String finishProcessCommand) {
        this.finishProcessCommand = finishProcessCommand;
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

    public DBDDataFormatterProfile getFormatterProfile() {
        return formatterProfile;
    }

    public void setFormatterProfile(DBDDataFormatterProfile formatterProfile) {
        this.formatterProfile = formatterProfile;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DataTransferSettings dataTransferSettings, Map<String, Object> settings) {
        lobExtractType = CommonUtils.valueOf(LobExtractType.class, (String) settings.get("lobExtractType"), LobExtractType.SKIP);
        lobEncoding = CommonUtils.valueOf(LobEncoding.class, (String) settings.get("lobEncoding"), LobEncoding.HEX);

        outputFolder = CommonUtils.toString(settings.get("outputFolder"), outputFolder);
        outputFilePattern = CommonUtils.toString(settings.get("outputFilePattern"), outputFilePattern);
        outputEncoding = CommonUtils.toString(settings.get("outputEncoding"), outputEncoding);
        outputTimestampPattern = CommonUtils.toString(settings.get("outputTimestampPattern"), outputTimestampPattern);
        outputEncodingBOM = CommonUtils.getBoolean(settings.get("outputEncodingBOM"), outputEncodingBOM);
        outputClipboard = CommonUtils.getBoolean(settings.get("outputClipboard"), outputClipboard);
        if (dataTransferSettings.getDataPipes().size() > 1) {
            useSingleFile = CommonUtils.getBoolean(settings.get("useSingleFile"), useSingleFile);
        } else {
            useSingleFile = false;
        }

        compressResults = CommonUtils.getBoolean(settings.get("compressResults"), compressResults);
        splitOutFiles = CommonUtils.getBoolean(settings.get("splitOutFiles"), splitOutFiles);
        maxOutFileSize = CommonUtils.toLong(settings.get("maxOutFileSize"), maxOutFileSize);
        openFolderOnFinish = CommonUtils.getBoolean(settings.get("openFolderOnFinish"), openFolderOnFinish);
        executeProcessOnFinish = CommonUtils.getBoolean(settings.get("executeProcessOnFinish"), executeProcessOnFinish);
        finishProcessCommand = CommonUtils.toString(settings.get("finishProcessCommand"), finishProcessCommand);

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
    }

    @Override
    public void saveSettings(Map<String, Object> settings) {
        settings.put("lobExtractType", lobExtractType.name());
        settings.put("lobEncoding", lobEncoding.name());

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

        settings.put("openFolderOnFinish", openFolderOnFinish);
        settings.put("executeProcessOnFinish", executeProcessOnFinish);
        settings.put("finishProcessCommand", finishProcessCommand);

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
    }

    @Override
    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();

        if (!outputClipboard) {
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_use_single_file, useSingleFile);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_directory, outputFolder);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_file_name_pattern, outputFilePattern);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_encoding, outputEncoding);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_timestamp_pattern, outputTimestampPattern);
            DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_label_insert_bom, outputEncodingBOM);
        } else {
            DTUtils.addSummary(summary, "Copy to clipboard", outputClipboard);
        }

        DTUtils.addSummary(summary, DTMessages.data_transfer_wizard_output_checkbox_compress, compressResults);
        if (executeProcessOnFinish) {
            DTUtils.addSummary(summary, "Execute process on finish", finishProcessCommand);
        }

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
