/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


public class MySQLScriptExecuteWizardPageSettings extends MySQLWizardPageSettings<MySQLScriptExecuteWizard>
{
    private Text inputFileText;
    private Combo logLevelCombo;

    public MySQLScriptExecuteWizardPageSettings(MySQLScriptExecuteWizard wizard)
    {
        super(wizard, wizard.isImport() ?
                MySQLMessages.tools_script_execute_wizard_page_settings_import_configuration :
                MySQLMessages.tools_script_execute_wizard_page_settings_script_configuration);
        setTitle(wizard.isImport() ?
                MySQLMessages.tools_script_execute_wizard_page_settings_import_configuration :
                MySQLMessages.tools_script_execute_wizard_page_settings_script_configuration);
        setDescription(wizard.isImport() ?
                MySQLMessages.tools_script_execute_wizard_page_settings_set_db_import_settings :
                MySQLMessages.tools_script_execute_wizard_page_settings_set_script_execution_settings);
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.getInputFile() != null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group outputGroup = UIUtils.createControlGroup(
                composite, MySQLMessages.tools_script_execute_wizard_page_settings_group_input, 3, GridData.FILL_HORIZONTAL, 0);
        inputFileText = UIUtils.createLabelText(
                outputGroup, MySQLMessages.tools_script_execute_wizard_page_settings_label_input_file, "", SWT.BORDER | SWT.READ_ONLY); //$NON-NLS-2$
        inputFileText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e)
            {
                chooseInputFile();
            }
        });
        Button browseButton = new Button(outputGroup, SWT.PUSH);
        browseButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                chooseInputFile();
            }
        });

        if (wizard.getInputFile() != null) {
            inputFileText.setText(wizard.getInputFile().getName());
        }

        Group settingsGroup = UIUtils.createControlGroup(
                composite, MySQLMessages.tools_script_execute_wizard_page_settings_group_settings, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
        logLevelCombo = UIUtils.createLabelCombo(
                settingsGroup, MySQLMessages.tools_script_execute_wizard_page_settings_label_log_level, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MySQLScriptExecuteWizard.LogLevel logLevel : MySQLScriptExecuteWizard.LogLevel.values()) {
            logLevelCombo.add(logLevel.name());
        }
        logLevelCombo.select(wizard.getLogLevel().ordinal());
        logLevelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                wizard.setLogLevel(MySQLScriptExecuteWizard.LogLevel.valueOf(logLevelCombo.getText()));
            }
        });
        createExtraArgsInput(settingsGroup);

        createSecurityGroup(composite);

        setControl(composite);

        //updateState();
    }

    private void chooseInputFile()
    {
        File file = DialogUtils.openFile(getShell(), new String[]{"*.sql", "*.txt", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (file != null) {
            inputFileText.setText(file.getAbsolutePath());
        }
        updateState();
    }

    private void updateState()
    {
        String fileName = inputFileText.getText();
        wizard.setInputFile(CommonUtils.isEmpty(fileName) ? null : new File(fileName));

        getContainer().updateButtons();
    }

}
