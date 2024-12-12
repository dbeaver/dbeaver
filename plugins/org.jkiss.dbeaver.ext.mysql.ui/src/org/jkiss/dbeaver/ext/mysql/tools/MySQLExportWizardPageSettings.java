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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLExportSettings;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;


class MySQLExportWizardPageSettings extends MySQLWizardPageSettings<MySQLExportWizard> {

    private Combo methodCombo;
    private Button noCreateStatementsCheck;
    private Button addDropStatementsCheck;
    private Button disableKeysCheck;
    private Button extendedInsertsCheck;
    private Button dumpEventsCheck;
    private Button commentsCheck;
    private Button removeDefiner;
    private Button binaryInHex;
    private Button noData;
    private Button noRoutines;

    MySQLExportWizardPageSettings(MySQLExportWizard wizard)
    {
        super(wizard, MySQLUIMessages.tools_db_export_wizard_page_settings_page_name);
        setTitle(MySQLUIMessages.tools_db_export_wizard_page_settings_page_name);
        setDescription((MySQLUIMessages.tools_db_export_wizard_page_settings_page_description));
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

        Group methodGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.tools_db_export_wizard_page_settings_group_exe_method, 1, GridData.FILL_HORIZONTAL, 0);
        methodCombo = new Combo(methodGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        methodCombo.add(MySQLUIMessages.tools_db_export_wizard_page_settings_combo_item_online_backup);
        methodCombo.add(MySQLUIMessages.tools_db_export_wizard_page_settings_combo_item_lock_tables);
        methodCombo.add(MySQLUIMessages.tools_db_export_wizard_page_settings_combo_item_normal);
        methodCombo.select(wizard.getSettings().getMethod().ordinal());
        methodCombo.addSelectionListener(changeListener);

        Group settingsGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.tools_db_export_wizard_page_settings_group_settings, 3, GridData.FILL_HORIZONTAL, 0);
        noCreateStatementsCheck = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_no_create, wizard.getSettings().isNoCreateStatements());
        noCreateStatementsCheck.addSelectionListener(changeListener);
        addDropStatementsCheck = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_add_drop, wizard.getSettings().isAddDropStatements());
        addDropStatementsCheck.addSelectionListener(changeListener);
        disableKeysCheck = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_disable_keys, wizard.getSettings().isDisableKeys());
        disableKeysCheck.addSelectionListener(changeListener);
        extendedInsertsCheck = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_ext_inserts, wizard.getSettings().isExtendedInserts());
        extendedInsertsCheck.addSelectionListener(changeListener);
        dumpEventsCheck = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_dump_events, wizard.getSettings().isDumpEvents());
        dumpEventsCheck.addSelectionListener(changeListener);
        commentsCheck = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_addnl_comments, wizard.getSettings().isComments());
        commentsCheck.addSelectionListener(changeListener);
        removeDefiner = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_remove_definer, wizard.getSettings().isRemoveDefiner());
        removeDefiner.addSelectionListener(changeListener);
        binaryInHex = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_binary_hex, wizard.getSettings().isBinariesInHex());
        binaryInHex.addSelectionListener(changeListener);
        noData = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_no_data, wizard.getSettings().isNoData());
        noData.addSelectionListener(changeListener);

        noRoutines = UIUtils.createCheckbox(settingsGroup, MySQLUIMessages.tools_db_export_wizard_page_settings_checkbox_no_routines, wizard.getSettings().isNoRoutines());
        noRoutines.addSelectionListener(changeListener);

        Group outputGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.tools_db_export_wizard_page_settings_group_output, 2, GridData.FILL_HORIZONTAL, 0);
        createOutputFolderInput(outputGroup, wizard.getSettings());
        createExtraArgsInput(outputGroup);
        outputFileText.addModifyListener(e -> {
            wizard.getSettings().setOutputFilePattern(outputFileText.getText());
        });

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);

        setControl(composite);
    }

    @Override
    public void saveState() {
        super.saveState();

        MySQLExportSettings settings = wizard.getSettings();

        String fileName = outputFolderText.getText();
        wizard.getSettings().setOutputFolderPattern(CommonUtils.isEmpty(fileName) ? null : fileName);
        settings.setOutputFilePattern(outputFileText.getText());

        switch (methodCombo.getSelectionIndex()) {
            case 0 -> settings.setMethod(MySQLExportSettings.DumpMethod.ONLINE);
            case 1 -> settings.setMethod(MySQLExportSettings.DumpMethod.LOCK_ALL_TABLES);
            default -> settings.setMethod(MySQLExportSettings.DumpMethod.NORMAL);
        }
        settings.setNoCreateStatements(noCreateStatementsCheck.getSelection());
        settings.setAddDropStatements(addDropStatementsCheck.getSelection());
        settings.setDisableKeys(disableKeysCheck.getSelection());
        settings.setExtendedInserts(extendedInsertsCheck.getSelection());
        settings.setDumpEvents(dumpEventsCheck.getSelection());
        settings.setComments(commentsCheck.getSelection());
        settings.setRemoveDefiner(removeDefiner.getSelection());
        settings.setBinariesInHex(binaryInHex.getSelection());
        settings.setNoData(noData.getSelection());
        settings.setNoRoutines(noRoutines.getSelection());
    }

    @Override
    protected void updateState()
    {
        saveState();
        updatePageCompletion();
        getContainer().updateButtons();
    }

}
