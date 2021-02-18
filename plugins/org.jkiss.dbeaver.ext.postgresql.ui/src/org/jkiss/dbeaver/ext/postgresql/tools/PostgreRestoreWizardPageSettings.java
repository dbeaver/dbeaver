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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupSettings;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseRestoreSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.utils.CommonUtils;


class PostgreRestoreWizardPageSettings extends PostgreToolWizardPageSettings<PostgreRestoreWizard> {

    private TextWithOpenFile inputFileText;
    private Combo formatCombo;
    private Button cleanFirstButton;
    private Button noOwnerCheck;

    PostgreRestoreWizardPageSettings(PostgreRestoreWizard wizard)
    {
        super(wizard, PostgreMessages.wizard_restore_page_setting_title_setting);
        setTitle(PostgreMessages.wizard_restore_page_setting_title);
        setDescription(PostgreMessages.wizard_restore_page_setting_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && !CommonUtils.isEmpty(wizard.getSettings().getInputFile());
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Listener updateListener = event -> updateState();

        Group formatGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_restore_page_setting_label_setting, 2, GridData.FILL_HORIZONTAL, 0);
        formatCombo = UIUtils.createLabelCombo(formatGroup, PostgreMessages.wizard_restore_page_setting_label_format, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (PostgreDatabaseBackupSettings.ExportFormat format : PostgreDatabaseBackupSettings.ExportFormat.values()) {
            formatCombo.add(format.getTitle());
        }
        formatCombo.select(wizard.getSettings().getFormat().ordinal());
        formatCombo.addListener(SWT.Selection, updateListener);

        cleanFirstButton = UIUtils.createCheckbox(formatGroup,
        	PostgreMessages.wizard_restore_page_setting_btn_clean_first,
            null,
            wizard.getSettings().isCleanFirst(),
            2
        );
        cleanFirstButton.addListener(SWT.Selection, updateListener);

        noOwnerCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_page_setting_checkbox_no_owner,
            null,
            wizard.getSettings().isNoOwner(),
            2
        );
        noOwnerCheck.addListener(SWT.Selection, updateListener);

        Group inputGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_restore_page_setting_label_input, 2, GridData.FILL_HORIZONTAL, 0);
        UIUtils.createControlLabel(inputGroup, PostgreMessages.wizard_restore_page_setting_label_backup_file);
        inputFileText = new TextWithOpenFile(inputGroup, PostgreMessages.wizard_restore_page_setting_label_choose_backup_file, new String[] {"*.backup","*.sql","*"});
        inputFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputFileText.getTextControl().addListener(SWT.Modify, updateListener);
        inputFileText.setText(wizard.getSettings().getInputFile());

        createExtraArgsInput(inputGroup);

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);
        wizard.createTaskSaveGroup(extraGroup);

        setControl(composite);
    }

    @Override
    public void saveState()
    {
        PostgreDatabaseRestoreSettings settings = wizard.getSettings();
        settings.setFormat(PostgreDatabaseBackupSettings.ExportFormat.values()[formatCombo.getSelectionIndex()]);
        settings.setInputFile(inputFileText.getText());
        settings.setCleanFirst(cleanFirstButton.getSelection());
        settings.setNoOwner(noOwnerCheck.getSelection());
    }

    @Override
    protected void updateState() {
        saveState();

        inputFileText.setOpenFolder(wizard.getSettings().getFormat() == PostgreDatabaseBackupSettings.ExportFormat.DIRECTORY);

        getContainer().updateButtons();
    }

}
