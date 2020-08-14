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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class StreamConsumerPageOutput extends ActiveWizardPage<DataTransferWizard> {

    private Combo encodingCombo;
    private Button encodingBOMCheckbox;
    private Text timestampPattern;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Button showFolderCheckbox;
    private Button execProcessCheckbox;
    private Text execProcessText;
    private Button clipboardCheck;
    private Button singleFileCheck;
    private Button showFinalMessageCheckbox;
    private Button splitFilesCheckbox;
    private Label maximumFileSizeLabel;
    private Text maximumFileSizeText;

    public StreamConsumerPageOutput() {
        super(DTMessages.data_transfer_wizard_output_name);
        setTitle(DTMessages.data_transfer_wizard_output_title);
        setDescription(DTMessages.data_transfer_wizard_output_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);

        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_output_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            clipboardCheck = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_copy_to_clipboard, null, false, 5);
            clipboardCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOutputClipboard(clipboardCheck.getSelection());
                    updateControlsEnablement();
                    updatePageCompletion();
                }
            });

            // Output path/pattern

            directoryText = DialogUtils.createOutputFolderChooser(generalSettings, null, e -> {
                settings.setOutputFolder(directoryText.getText());
                updatePageCompletion();
            });
            ((GridData)directoryText.getParent().getLayoutData()).horizontalSpan = 4;

            UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_file_name_pattern);
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            UIUtils.setContentProposalToolTip(fileNameText, DTUIMessages.stream_consumer_page_output_tooltip_output_file_name_pattern,
                StreamTransferConsumer.VARIABLE_DATASOURCE,
                StreamTransferConsumer.VARIABLE_CATALOG,
                StreamTransferConsumer.VARIABLE_SCHEMA,
                StreamTransferConsumer.VARIABLE_TABLE,
                StreamTransferConsumer.VARIABLE_TIMESTAMP,
                StreamTransferConsumer.VARIABLE_DATE,
                StreamTransferConsumer.VARIABLE_INDEX,
                StreamTransferConsumer.VARIABLE_PROJECT);
            fileNameText.setLayoutData(gd);
            fileNameText.addModifyListener(e -> {
                settings.setOutputFilePattern(fileNameText.getText());
                updatePageCompletion();
            });
            ContentAssistUtils.installContentProposal(
                fileNameText,
                new SmartTextContentAdapter(),
                new StringContentProposalProvider(
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_DATASOURCE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_CATALOG),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_SCHEMA),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_DATE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_INDEX),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_PROJECT)));

            {
                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_encoding);
                encodingCombo = UIUtils.createEncodingCombo(generalSettings, settings.getOutputEncoding());
                //encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingCombo.addModifyListener(e -> {
                    int index = encodingCombo.getSelectionIndex();
                    if (index >= 0) {
                        settings.setOutputEncoding(encodingCombo.getItem(index));
                    }
                    updatePageCompletion();
                });
                timestampPattern = UIUtils.createLabelText(generalSettings, DTMessages.data_transfer_wizard_output_label_timestamp_pattern, GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, SWT.BORDER);
                timestampPattern.addModifyListener(e -> {
                    settings.setOutputTimestampPattern(timestampPattern.getText());
                });
                encodingBOMCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_insert_bom, DTMessages.data_transfer_wizard_output_label_insert_bom_tooltip, false, 1);
                encodingBOMCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setOutputEncodingBOM(encodingBOMCheckbox.getSelection());
                    }
                });
            }

            singleFileCheck = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_use_single_file, DTMessages.data_transfer_wizard_output_label_use_single_file_tip, false, 5);
            singleFileCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setUseSingleFile(singleFileCheck.getSelection());
                    updatePageCompletion();
                }
            });

            compressCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_checkbox_compress, null, false, 1);
            compressCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setCompressResults(compressCheckbox.getSelection());
                    updateControlsEnablement();
                }
            });

            {
                Composite outFilesSettings = UIUtils.createComposite(generalSettings, 3);
                outFilesSettings.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 5, 1));

                splitFilesCheckbox = UIUtils.createCheckbox(outFilesSettings, DTMessages.data_transfer_wizard_output_checkbox_split_files, DTMessages.data_transfer_wizard_output_checkbox_split_files_tip, false, 1);
                splitFilesCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setSplitOutFiles(splitFilesCheckbox.getSelection());
                        updateControlsEnablement();
                    }
                });
                maximumFileSizeLabel = UIUtils.createControlLabel(outFilesSettings, DTUIMessages.stream_consumer_page_output_label_maximum_file_size);
                maximumFileSizeText = new Text(outFilesSettings, SWT.BORDER);
                maximumFileSizeText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
                maximumFileSizeText.addModifyListener(e ->
                    settings.setMaxOutFileSize(CommonUtils.toLong(maximumFileSizeText.getText())));
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = UIUtils.getFontHeight(maximumFileSizeText) * 10;
                maximumFileSizeText.setLayoutData(gd);
            }
        }

        {
            Group resultsSettings = UIUtils.createControlGroup(composite, DTUIMessages.stream_consumer_page_output_label_results, 2, GridData.FILL_HORIZONTAL, 0);

            showFolderCheckbox = UIUtils.createCheckbox(resultsSettings, DTMessages.data_transfer_wizard_output_checkbox_open_folder, true);
            showFolderCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenFolderOnFinish(showFolderCheckbox.getSelection());
                }
            });
            showFolderCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 2, 1));

            execProcessCheckbox = UIUtils.createCheckbox(resultsSettings, DTUIMessages.stream_consumer_page_output_checkbox_execute_process, true);
            execProcessCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setExecuteProcessOnFinish(execProcessCheckbox.getSelection());
                    updateControlsEnablement();
                    updatePageCompletion();
                }
            });
            execProcessText = new Text(resultsSettings, SWT.BORDER);
            execProcessText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            execProcessText.addModifyListener(e -> {
                settings.setFinishProcessCommand(execProcessText.getText());
                updatePageCompletion();
            });
            UIUtils.setContentProposalToolTip(execProcessText, DTUIMessages.stream_consumer_page_output_tooltip_process_command_line,
                StreamTransferConsumer.VARIABLE_FILE,
                StreamTransferConsumer.VARIABLE_TABLE,
                StreamTransferConsumer.VARIABLE_TIMESTAMP,
                StreamTransferConsumer.VARIABLE_DATE,
                StreamTransferConsumer.VARIABLE_PROJECT);
            ContentAssistUtils.installContentProposal(
                execProcessText,
                new SmartTextContentAdapter(),
                new StringContentProposalProvider(
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_DATE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_PROJECT),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_FILE)));

            showFinalMessageCheckbox = UIUtils.createCheckbox(resultsSettings, DTUIMessages.stream_consumer_page_output_label_show_finish_message, null, getWizard().getSettings().isShowFinalMessage(), 4);
            showFinalMessageCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setShowFinalMessage(showFinalMessageCheckbox.getSelection());
                }
            });
        }

        setControl(composite);

    }

    private void updateControlsEnablement() {
        boolean isBinary = getWizard().getSettings().getProcessor().isBinaryFormat();
        boolean clipboard = !isBinary && clipboardCheck.getSelection();
        boolean isMulti = getWizard().getSettings().getDataPipes().size() > 1;
        boolean singleFile = singleFileCheck.getSelection();

        clipboardCheck.setEnabled(!isBinary);
        singleFileCheck.setEnabled(isMulti && !clipboard && getWizard().getSettings().getMaxJobCount() <= 1);
        directoryText.setEnabled(!clipboard);
        fileNameText.setEnabled(!clipboard);
        compressCheckbox.setEnabled(!clipboard);
        splitFilesCheckbox.setEnabled(!clipboard);
        maximumFileSizeLabel.setEnabled(!clipboard && splitFilesCheckbox.getSelection());
        maximumFileSizeText.setEnabled(!clipboard && splitFilesCheckbox.getSelection());
        encodingCombo.setEnabled(!isBinary && !clipboard);
        encodingBOMCheckbox.setEnabled(!isBinary && !clipboard);
        timestampPattern.setEnabled(!clipboard);
        showFolderCheckbox.setEnabled(!clipboard);
        execProcessCheckbox.setEnabled(!clipboard);
        execProcessText.setEnabled(!clipboard);
    }

    @Override
    public void activatePage()
    {
        boolean isBinary = getWizard().getSettings().getProcessor().isBinaryFormat();

        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        clipboardCheck.setSelection(settings.isOutputClipboard());
        singleFileCheck.setSelection(settings.isUseSingleFile());
        directoryText.setText(CommonUtils.toString(settings.getOutputFolder()));
        fileNameText.setText(CommonUtils.toString(settings.getOutputFilePattern()));
        compressCheckbox.setSelection(settings.isCompressResults());
        splitFilesCheckbox.setSelection(settings.isSplitOutFiles());
        maximumFileSizeText.setText(String.valueOf(settings.getMaxOutFileSize()));
        encodingCombo.setText(CommonUtils.toString(settings.getOutputEncoding()));
        timestampPattern.setText(settings.getOutputTimestampPattern());
        encodingBOMCheckbox.setSelection(settings.isOutputEncodingBOM());
        showFolderCheckbox.setSelection(settings.isOpenFolderOnFinish());
        execProcessCheckbox.setSelection(settings.isExecuteProcessOnFinish());
        execProcessText.setText(CommonUtils.toString(settings.getFinishProcessCommand()));

        if (isBinary) {
            clipboardCheck.setSelection(false);
            encodingBOMCheckbox.setSelection(false);
            settings.setOutputClipboard(false);
        }
        showFinalMessageCheckbox.setSelection(getWizard().getSettings().isShowFinalMessage());

        updatePageCompletion();
        updateControlsEnablement();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        if (settings == null) {
            return false;
        }
        /*
        int selectionIndex = encodingCombo.getSelectionIndex();

        String encoding = null;
        if (selectionIndex >= 0) {
            encoding = encodingCombo.getItem(selectionIndex);
        }

        if (settings.isOutputClipboard() || encoding == null || GeneralUtils.getCharsetBOM(encoding) == null) {
            encodingBOMLabel.setEnabled(false);
            encodingBOMCheckbox.setEnabled(false);
        } else {
            encodingBOMLabel.setEnabled(true);
            encodingBOMCheckbox.setEnabled(true);
        }
*/

        if (settings.isOutputClipboard()) {
            return true;
        }

        boolean valid = true;
        if (CommonUtils.isEmpty(settings.getOutputFolder())) {
            valid = false;
        }
        if (CommonUtils.isEmpty(settings.getOutputFilePattern())) {
            valid = false;
        }
        if (settings.isExecuteProcessOnFinish() && CommonUtils.isEmpty(settings.getFinishProcessCommand())) {
            return false;
        }
        return valid;
    }

}