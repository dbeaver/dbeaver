/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017 Adolfo Suarez  (agustavo@gmail.com)
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
package org.jkiss.dbeaver.data.office.export;


import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * @author Andrey.Hitrin
 *
 */
public class StreamPOIConsumerPageOutput extends ActiveWizardPage<DataTransferWizard> {

    private Text directoryText;
    private Text fileNameText;
    private Button showFolderCheckbox;
    private Button execProcessCheckbox;
    private Text execProcessText;

    public StreamPOIConsumerPageOutput() {
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

        final StreamPOIConsumerSettings settings = getWizard().getPageSettings(this, StreamPOIConsumerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.data_transfer_wizard_output_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            
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
                StreamPOITransferConsumer.VARIABLE_TABLE,
                StreamPOITransferConsumer.VARIABLE_TIMESTAMP,
                StreamPOITransferConsumer.VARIABLE_PROJECT);
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
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_TABLE),
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_TIMESTAMP),
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_PROJECT)
                }));

            {
                new Label(generalSettings, SWT.NONE);
            }

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
                StreamPOITransferConsumer.VARIABLE_FILE,
                StreamPOITransferConsumer.VARIABLE_TABLE,
                StreamPOITransferConsumer.VARIABLE_TIMESTAMP,
                StreamPOITransferConsumer.VARIABLE_PROJECT);
            UIUtils.installContentProposal(
                execProcessText,
                new TextContentAdapter(),
                new SimpleContentProposalProvider(new String[] {
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_TABLE),
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_TIMESTAMP),
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_PROJECT),
                    GeneralUtils.variablePattern(StreamPOITransferConsumer.VARIABLE_FILE)
                }));
        }

        setControl(composite);

    }


    private void toggleExecProcessControls() {
        final boolean isExecCommand = execProcessCheckbox.getSelection();
        execProcessText.setEnabled(isExecCommand);
    }

    @Override
    public void activatePage()
    {
        final StreamPOIConsumerSettings settings = getWizard().getPageSettings(this, StreamPOIConsumerSettings.class);

        directoryText.setText(CommonUtils.toString(settings.getOutputFolder()));
        fileNameText.setText(CommonUtils.toString(settings.getOutputFilePattern()));
        showFolderCheckbox.setSelection(settings.isOpenFolderOnFinish());
        execProcessCheckbox.setSelection(settings.isExecuteProcessOnFinish());
        execProcessText.setText(CommonUtils.toString(settings.getFinishProcessCommand()));
        updatePageCompletion();
        toggleExecProcessControls();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final StreamPOIConsumerSettings settings = getWizard().getPageSettings(this, StreamPOIConsumerSettings.class);

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
        
        //To avoid problem with BOM 
        //Do not TOUCH !!!
        settings.setOutputEncodingBOM(false);
        
        return valid;
    }

}
