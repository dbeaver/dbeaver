/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class StreamConsumerPageOutput extends ActiveWizardPage<DataTransferWizard> {

    private Combo encodingCombo;
    private Label encodingBOMLabel;
    private Button encodingBOMCheckbox;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Button showFolderCheckbox;
    private Button execProcessCheckbox;
    private Text execProcessText;
    private Button clipboardCheck;

    public StreamConsumerPageOutput() {
        super(CoreMessages.data_transfer_wizard_output_name);
        setTitle(CoreMessages.data_transfer_wizard_output_title);
        setDescription(CoreMessages.data_transfer_wizard_output_description);
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

        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.data_transfer_wizard_output_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            clipboardCheck = UIUtils.createLabelCheckbox(generalSettings, "Copy to clipboard", false);
            clipboardCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 4, 1));
            clipboardCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOutputClipboard(clipboardCheck.getSelection());
                    toggleClipboardOutput();
                    updatePageCompletion();
                }
            });
            directoryText = DialogUtils.createOutputFolderChooser(generalSettings, null, new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    settings.setOutputFolder(directoryText.getText());
                    updatePageCompletion();
                }
            });
            ((GridData)directoryText.getParent().getLayoutData()).horizontalSpan = 4;

            UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_file_name_pattern);
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            UIUtils.setContentProposalToolTip(fileNameText, "Output file name pattern",
                StreamTransferConsumer.VARIABLE_TABLE,
                StreamTransferConsumer.VARIABLE_TIMESTAMP,
                StreamTransferConsumer.VARIABLE_PROJECT);
            fileNameText.setLayoutData(gd);
            fileNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    settings.setOutputFilePattern(fileNameText.getText());
                    updatePageCompletion();
                }
            });
            UIUtils.installContentProposal(
                fileNameText,
                new TextContentAdapter(),
                new SimpleContentProposalProvider(new String[] {
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_PROJECT)
                }));

            {
                UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_encoding);
                encodingCombo = UIUtils.createEncodingCombo(generalSettings, settings.getOutputEncoding());
                //encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingCombo.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        int index = encodingCombo.getSelectionIndex();
                        if (index >= 0) {
                            settings.setOutputEncoding(encodingCombo.getItem(index));
                        }
                        updatePageCompletion();
                    }
                });
                encodingBOMLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_insert_bom);
                encodingBOMLabel.setToolTipText(CoreMessages.data_transfer_wizard_output_label_insert_bom_tooltip);
                encodingBOMCheckbox = new Button(generalSettings, SWT.CHECK);
                encodingBOMCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingBOMCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setOutputEncodingBOM(encodingBOMCheckbox.getSelection());
                    }
                });
                new Label(generalSettings, SWT.NONE);
            }

            compressCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.data_transfer_wizard_output_checkbox_compress, false);
            compressCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 4, 1));
            compressCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setCompressResults(compressCheckbox.getSelection());
                }
            });
        }

        {
            Group resultsSettings = UIUtils.createControlGroup(composite, "Results", 2, GridData.FILL_HORIZONTAL, 0);

            showFolderCheckbox = UIUtils.createCheckbox(resultsSettings, CoreMessages.data_transfer_wizard_output_checkbox_open_folder, true);
            showFolderCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenFolderOnFinish(showFolderCheckbox.getSelection());
                }
            });
            showFolderCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 2, 1));

            execProcessCheckbox = UIUtils.createCheckbox(resultsSettings, "Execute process on finish", true);
            execProcessCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setExecuteProcessOnFinish(execProcessCheckbox.getSelection());
                    toggleExecProcessControls();
                }
            });
            execProcessText = new Text(resultsSettings, SWT.BORDER);
            execProcessText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            execProcessText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    settings.setFinishProcessCommand(execProcessText.getText());
                    updatePageCompletion();
                }
            });
            UIUtils.setContentProposalToolTip(execProcessText, "Process command line",
                StreamTransferConsumer.VARIABLE_FILE,
                StreamTransferConsumer.VARIABLE_TABLE,
                StreamTransferConsumer.VARIABLE_TIMESTAMP,
                StreamTransferConsumer.VARIABLE_PROJECT);
            UIUtils.installContentProposal(
                execProcessText,
                new TextContentAdapter(),
                new SimpleContentProposalProvider(new String[] {
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TABLE),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_TIMESTAMP),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_PROJECT),
                    GeneralUtils.variablePattern(StreamTransferConsumer.VARIABLE_FILE)
                }));
        }

        setControl(composite);

    }

    private void toggleClipboardOutput() {
        boolean clipboard = clipboardCheck.getSelection();
        directoryText.setEnabled(!clipboard);
        fileNameText.setEnabled(!clipboard);
        compressCheckbox.setEnabled(!clipboard);
        encodingCombo.setEnabled(!clipboard);
        encodingBOMLabel.setEnabled(!clipboard);
        encodingBOMCheckbox.setEnabled(!clipboard);
        showFolderCheckbox.setEnabled(!clipboard);
        execProcessCheckbox.setEnabled(!clipboard);
        execProcessText.setEnabled(!clipboard);
    }

    private void toggleExecProcessControls() {
        boolean clipboard = clipboardCheck.getSelection();
        final boolean isExecCommand = execProcessCheckbox.getSelection();
        execProcessText.setEnabled(!clipboard && isExecCommand);
    }

    @Override
    public void activatePage()
    {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        clipboardCheck.setSelection(settings.isOutputClipboard());
        directoryText.setText(CommonUtils.toString(settings.getOutputFolder()));
        fileNameText.setText(CommonUtils.toString(settings.getOutputFilePattern()));
        compressCheckbox.setSelection(settings.isCompressResults());
        encodingCombo.setText(CommonUtils.toString(settings.getOutputEncoding()));
        encodingBOMCheckbox.setSelection(settings.isOutputEncodingBOM());
        showFolderCheckbox.setSelection(settings.isOpenFolderOnFinish());
        execProcessCheckbox.setSelection(settings.isExecuteProcessOnFinish());
        execProcessText.setText(CommonUtils.toString(settings.getFinishProcessCommand()));

        updatePageCompletion();
        toggleClipboardOutput();
        toggleExecProcessControls();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
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