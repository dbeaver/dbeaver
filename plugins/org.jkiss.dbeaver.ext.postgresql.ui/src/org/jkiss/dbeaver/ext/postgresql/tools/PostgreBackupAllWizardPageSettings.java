/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreBackupAllSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class PostgreBackupAllWizardPageSettings extends PostgreToolWizardPageSettings<PostgreBackupAllWizard> {

    private Combo encodingCombo;
    private Button metadataOnly;
    private Button globalsOnly;
    private Button rolesOnly;
    private Button tablespacesOnly;
    private Button noPrivilegesCheck;
    private Button noOwnerCheck;
    private Button addRolesPasswords;

    public PostgreBackupAllWizardPageSettings(PostgreBackupAllWizard wizard) {
        super(wizard, PostgreMessages.wizard_backup_all_page_setting_title_setting);
        setTitle(PostgreMessages.wizard_backup_all_page_setting_title);
        setDescription(PostgreMessages.wizard_backup_all_page_setting_title);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);
        SelectionListener changeListener = SelectionListener.widgetSelectedAdapter(e -> updateState());
        Group formatGroup = UIUtils.createControlGroup(
            composite,
            PostgreMessages.wizard_backup_all_page_setting_title_setting,
            2,
            GridData.FILL_HORIZONTAL,
            0);
        UIUtils.createControlLabel(formatGroup, PostgreMessages.wizard_backup_all_page_setting_label_encoding);
        encodingCombo = UIUtils.createEncodingCombo(formatGroup, null);
        encodingCombo.addSelectionListener(changeListener);
        PostgreBackupAllSettings settings = wizard.getSettings();
        encodingCombo.setText(settings.getEncoding());

        metadataOnly = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_metadata,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_metadata_tip,
            settings.isExportOnlyMetadata(),
            2
        );
        metadataOnly.addSelectionListener(changeListener);

        globalsOnly = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_global,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_global_tip,
            settings.isExportOnlyGlobals(),
            2
        );
        globalsOnly.addSelectionListener(changeListener);

        rolesOnly = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_roles,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_roles_tip,
            settings.isExportOnlyRoles(),
            2
        );
        rolesOnly.addSelectionListener(changeListener);

        tablespacesOnly = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_tablespaces,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_only_tablespaces_tip,
            settings.isExportOnlyTablespaces(),
            2
        );
        tablespacesOnly.addSelectionListener(changeListener);

        noPrivilegesCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_no_privileges,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_no_privileges_tip,
            settings.isNoPrivileges(),
            2
        );
        noPrivilegesCheck.addSelectionListener(changeListener);

        noOwnerCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_no_owner,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_no_owner_tip,
            settings.isNoOwner(),
            2
        );
        noOwnerCheck.addSelectionListener(changeListener);

        addRolesPasswords = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_add_passwords,
            PostgreMessages.wizard_backup_all_page_setting_checkbox_add_passwords_tip,
            settings.isAddRolesPasswords(),
            2
        );
        addRolesPasswords.addSelectionListener(changeListener);

        Group outputGroup = UIUtils.createControlGroup(
            composite,
            PostgreMessages.wizard_backup_page_setting_group_output,
            2,
            GridData.FILL_HORIZONTAL,
            0);
        createOutputFolderInput(outputGroup, settings);
        createExtraArgsInput(outputGroup);

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);

        setControl(composite);
    }

    @Override
    protected void updateState() {
        saveState();
        updatePageCompletion();
        getContainer().updateButtons();
    }

    @Override
    public void saveState() {
        super.saveState();

        PostgreBackupAllSettings settings = wizard.getSettings();

        String fileName = outputFolderText.getText();
        settings.setOutputFolderPattern(CommonUtils.isEmpty(fileName) ? null : fileName);
        settings.setOutputFilePattern(outputFileText.getText());

        settings.setEncoding(encodingCombo.getText());
        settings.setExportOnlyMetadata(metadataOnly.getSelection());
        settings.setExportOnlyGlobals(globalsOnly.getSelection());
        settings.setExportOnlyRoles(rolesOnly.getSelection());
        settings.setExportOnlyTablespaces(tablespacesOnly.getSelection());
        settings.setNoPrivileges(noPrivilegesCheck.getSelection());
        settings.setNoOwner(noOwnerCheck.getSelection());
        settings.setAddRolesPasswords(addRolesPasswords.getSelection());
    }
}
