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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupSettings;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class PostgreBackupWizardPageSettings extends PostgreToolWizardPageSettings<PostgreBackupWizard> {

    private Text outputFolderText;
    private Text outputFileText;
    private Combo formatCombo;
    private Combo compressCombo;
    private Combo encodingCombo;
    private Button useInsertsCheck;
    private Button noPrivilegesCheck;
    private Button noOwnerCheck;

    PostgreBackupWizardPageSettings(PostgreBackupWizard wizard)
    {
        super(wizard, PostgreMessages.wizard_backup_page_setting_title_setting);
        setTitle(PostgreMessages.wizard_backup_page_setting_title);
        setDescription(PostgreMessages.wizard_backup_page_setting_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.getSettings().getOutputFolder() != null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SelectionListener changeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }
        };

        Group formatGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_backup_page_setting_group_setting, 2, GridData.FILL_HORIZONTAL, 0);
        formatCombo = UIUtils.createLabelCombo(formatGroup, PostgreMessages.wizard_backup_page_setting_label_format, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (PostgreDatabaseBackupSettings.ExportFormat format : PostgreDatabaseBackupSettings.ExportFormat.values()) {
            formatCombo.add(format.getTitle());
        }
        formatCombo.select(wizard.getSettings().getFormat().ordinal());
        formatCombo.addSelectionListener(changeListener);

        compressCombo = UIUtils.createLabelCombo(formatGroup, PostgreMessages.wizard_backup_page_setting_label_compression, SWT.DROP_DOWN | SWT.READ_ONLY);
        compressCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        compressCombo.add("");
        for (int i = 0; i <= 9; i++) {
            compressCombo.add(String.valueOf(i));
        }
        compressCombo.select(0);
        compressCombo.addSelectionListener(changeListener);

        UIUtils.createControlLabel(formatGroup, PostgreMessages.wizard_backup_page_setting_label_encoding);
        encodingCombo = UIUtils.createEncodingCombo(formatGroup, null);
        encodingCombo.addSelectionListener(changeListener);

        useInsertsCheck = UIUtils.createCheckbox(formatGroup,
        	PostgreMessages.wizard_backup_page_setting_checkbox_use_insert,
            null,
            false,
            2
        );
        useInsertsCheck.addSelectionListener(changeListener);

        noPrivilegesCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_page_setting_checkbox_no_privileges,
            null,
            false,
            2
        );
        noPrivilegesCheck.addSelectionListener(changeListener);

        noOwnerCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_page_setting_checkbox_no_owner,
            null,
            false,
            2
        );
        noOwnerCheck.addSelectionListener(changeListener);

        Group outputGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_backup_page_setting_group_output, 2, GridData.FILL_HORIZONTAL, 0);
        outputFolderText = DialogUtils.createOutputFolderChooser(
            outputGroup,
            PostgreMessages.wizard_backup_page_setting_label_output_folder,
            wizard.getSettings().getOutputFolder() != null ? wizard.getSettings().getOutputFolder().getAbsolutePath() : null,
            e -> updateState());
        outputFileText = UIUtils.createLabelText(
            outputGroup,
            PostgreMessages.wizard_backup_page_setting_label_file_name_pattern,
            wizard.getSettings().getOutputFilePattern());
        UIUtils.setContentProposalToolTip(outputFileText, PostgreMessages.wizard_backup_page_setting_label_file_name_pattern_output,
            NativeToolUtils.VARIABLE_HOST,
            NativeToolUtils.VARIABLE_DATABASE,
            NativeToolUtils.VARIABLE_TABLE,
            NativeToolUtils.VARIABLE_DATE,
            NativeToolUtils.VARIABLE_TIMESTAMP);
        ContentAssistUtils.installContentProposal(
            outputFileText,
            new SmartTextContentAdapter(),
            new StringContentProposalProvider(
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_HOST),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATABASE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TABLE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TIMESTAMP)));
        outputFileText.addModifyListener(e -> wizard.getSettings().setOutputFilePattern(outputFileText.getText()));

        createExtraArgsInput(outputGroup);

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);
        wizard.createTaskSaveGroup(extraGroup);

        setControl(composite);
    }

    @Override
    protected void updateState()
    {
        saveState();

        getContainer().updateButtons();
    }

    @Override
    public void saveState() {
        super.saveState();

        String fileName = outputFolderText.getText();
        wizard.getSettings().setOutputFolder(CommonUtils.isEmpty(fileName) ? null : new File(fileName));
        wizard.getSettings().setOutputFilePattern(outputFileText.getText());

        wizard.getSettings().setFormat(PostgreDatabaseBackupSettings.ExportFormat.values()[formatCombo.getSelectionIndex()]);
        wizard.getSettings().setCompression(compressCombo.getText());
        wizard.getSettings().setEncoding(encodingCombo.getText());
        wizard.getSettings().setUseInserts(useInsertsCheck.getSelection());
        wizard.getSettings().setNoPrivileges(noPrivilegesCheck.getSelection());
        wizard.getSettings().setNoOwner(noOwnerCheck.getSelection());
    }

}
