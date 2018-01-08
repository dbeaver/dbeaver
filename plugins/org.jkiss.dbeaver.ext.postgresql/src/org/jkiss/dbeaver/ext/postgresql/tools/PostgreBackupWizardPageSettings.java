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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class PostgreBackupWizardPageSettings extends PostgreWizardPageSettings<PostgreBackupWizard>
{

    private Text outputFolderText;
    private Text outputFileText;
    private Combo formatCombo;
    private Combo compressCombo;
    private Combo encodingCombo;
    private Button useInsertsCheck;

    protected PostgreBackupWizardPageSettings(PostgreBackupWizard wizard)
    {
        super(wizard, PostgreMessages.wizard_backup_page_setting_title_setting);
        setTitle(PostgreMessages.wizard_backup_page_setting_title);
        setDescription(PostgreMessages.wizard_backup_page_setting_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.getOutputFolder() != null;
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
        for (PostgreBackupWizard.ExportFormat format : PostgreBackupWizard.ExportFormat.values()) {
            formatCombo.add(format.getTitle());
        }
        formatCombo.select(wizard.format.ordinal());
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

        Group outputGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_backup_page_setting_group_output, 2, GridData.FILL_HORIZONTAL, 0);
        outputFolderText = DialogUtils.createOutputFolderChooser(outputGroup, PostgreMessages.wizard_backup_page_setting_label_output_folder, new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateState();
            }
        });
        outputFileText = UIUtils.createLabelText(outputGroup, PostgreMessages.wizard_backup_page_setting_label_file_name_pattern, wizard.getOutputFilePattern());
        UIUtils.setContentProposalToolTip(outputFileText, PostgreMessages.wizard_backup_page_setting_label_file_name_pattern_output, "host", "database", "table", "timestamp");
        UIUtils.installContentProposal(
            outputFileText,
            new TextContentAdapter(),
            new SimpleContentProposalProvider(new String[]{"${host}", "${database}", "${table}", "${timestamp}"}));
        outputFileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                wizard.setOutputFilePattern(outputFileText.getText());
            }
        });
        if (wizard.getOutputFolder() != null) {
            outputFolderText.setText(wizard.getOutputFolder().getAbsolutePath());
        }
        createExtraArgsInput(outputGroup);

        createSecurityGroup(composite);

        setControl(composite);
    }

    private void updateState()
    {
        String fileName = outputFolderText.getText();
        wizard.setOutputFolder(CommonUtils.isEmpty(fileName) ? null : new File(fileName));
        wizard.setOutputFilePattern(outputFileText.getText());

        wizard.format = PostgreBackupWizard.ExportFormat.values()[formatCombo.getSelectionIndex()];
        wizard.compression = compressCombo.getText();
        wizard.encoding = encodingCombo.getText();
        wizard.useInserts = useInsertsCheck.getSelection();

        getContainer().updateButtons();
    }

}
