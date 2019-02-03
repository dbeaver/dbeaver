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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

/**
 * Stream transfer settings
 */
public class StreamConsumerSettings implements IDataTransferSettings {

    public enum LobExtractType {
        SKIP,
        FILES,
        INLINE
    }

    public enum LobEncoding {
        BASE64,
        HEX,
        BINARY
    }

    public static final String PROP_EXTRACT_IMAGES = "extractImages";
    public static final String PROP_FILE_EXTENSION = "extension";
    public static final String PROP_FORMAT = "format";

    private LobExtractType lobExtractType = LobExtractType.SKIP;
    private LobEncoding lobEncoding = LobEncoding.HEX;

    private String outputFolder = System.getProperty(StandardConstants.ENV_USER_HOME);
    private String outputFilePattern = GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE) + "_" + GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP);
    private String outputEncoding = GeneralUtils.getDefaultFileEncoding();
    private boolean outputEncodingBOM = true;

    private DBDDataFormatterProfile formatterProfile;

    private boolean outputClipboard = false;
    private boolean useSingleFile = false;
    private boolean compressResults = false;
    private boolean openFolderOnFinish = true;
    private boolean executeProcessOnFinish = false;
    private String finishProcessCommand = null;

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

    public DBDDataFormatterProfile getFormatterProfile() {
        return formatterProfile;
    }

    public void setFormatterProfile(DBDDataFormatterProfile formatterProfile) {
        this.formatterProfile = formatterProfile;
    }

    @Override
    public void loadSettings(IRunnableContext runnableContext, DataTransferSettings dataTransferSettings, IDialogSettings dialogSettings) {
        if (!CommonUtils.isEmpty(dialogSettings.get("lobExtractType"))) {
            try {
                lobExtractType = LobExtractType.valueOf(dialogSettings.get("lobExtractType"));
            } catch (IllegalArgumentException e) {
                lobExtractType = LobExtractType.SKIP;
            }
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("lobEncoding"))) {
            try {
                lobEncoding = LobEncoding.valueOf(dialogSettings.get("lobEncoding"));
            } catch (IllegalArgumentException e) {
                lobEncoding = LobEncoding.HEX;
            }
        }

        if (!CommonUtils.isEmpty(dialogSettings.get("outputFolder"))) {
            outputFolder = dialogSettings.get("outputFolder");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputFilePattern"))) {
            outputFilePattern = dialogSettings.get("outputFilePattern");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputEncoding"))) {
            outputEncoding = dialogSettings.get("outputEncoding");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputEncodingBOM"))) {
            outputEncodingBOM = dialogSettings.getBoolean("outputEncodingBOM");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputClipboard"))) {
            outputClipboard = dialogSettings.getBoolean("outputClipboard");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("useSingleFile"))) {
            useSingleFile = dialogSettings.getBoolean("useSingleFile");
        }

        if (!CommonUtils.isEmpty(dialogSettings.get("compressResults"))) {
            compressResults = dialogSettings.getBoolean("compressResults");
        }
        if (dialogSettings.get("openFolderOnFinish") != null) {
            openFolderOnFinish = dialogSettings.getBoolean("openFolderOnFinish");
        }
        if (dialogSettings.get("executeProcessOnFinish") != null) {
            executeProcessOnFinish = dialogSettings.getBoolean("executeProcessOnFinish");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("finishProcessCommand"))) {
            finishProcessCommand = dialogSettings.get("finishProcessCommand");
        }

        if (!CommonUtils.isEmpty(dialogSettings.get("formatterProfile"))) {
            formatterProfile = DBWorkbench.getPlatform().getDataFormatterRegistry().getCustomProfile(dialogSettings.get("formatterProfile"));
        }
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings) {
        dialogSettings.put("lobExtractType", lobExtractType.name());
        dialogSettings.put("lobEncoding", lobEncoding.name());

        dialogSettings.put("outputFolder", outputFolder);
        dialogSettings.put("outputFilePattern", outputFilePattern);
        dialogSettings.put("outputEncoding", outputEncoding);
        dialogSettings.put("outputEncodingBOM", outputEncodingBOM);
        dialogSettings.put("outputClipboard", outputClipboard);
        dialogSettings.put("useSingleFile", useSingleFile);

        dialogSettings.put("compressResults", compressResults);

        dialogSettings.put("openFolderOnFinish", openFolderOnFinish);
        dialogSettings.put("executeProcessOnFinish", executeProcessOnFinish);
        dialogSettings.put("finishProcessCommand", finishProcessCommand);

        if (formatterProfile != null) {
            dialogSettings.put("formatterProfile", formatterProfile.getProfileName());
        } else {
            dialogSettings.put("formatterProfile", "");
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

}
