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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.postgresql.PostgresMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


public class PostgreScriptExecuteWizardPageSettings extends PostgreWizardPageSettings<PostgreScriptExecuteWizard>
{
    private Text inputFileText;
    //private Combo logLevelCombo;

    public PostgreScriptExecuteWizardPageSettings(PostgreScriptExecuteWizard wizard)
    {
        super(wizard, wizard.isImport() ?
        		PostgresMessages.tool_script_title_import :
        		PostgresMessages.tool_script_title_execute);
        setTitle(wizard.isImport() ?
        	PostgresMessages.tool_script_title_import :
        	PostgresMessages.tool_script_title_execute);
        setDescription(wizard.isImport() ?
        	PostgresMessages.tool_script_description_import :
        	PostgresMessages.tool_script_description_execute);
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

        Group inputGroup = UIUtils.createControlGroup(
                composite, PostgresMessages.tool_script_label_input, 3, GridData.FILL_HORIZONTAL, 0);
        inputFileText = UIUtils.createLabelText(
                inputGroup, PostgresMessages.tool_script_label_input_file, "", SWT.BORDER | SWT.READ_ONLY); //$NON-NLS-2$
        inputFileText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e)
            {
                chooseInputFile();
            }
        });
        Button browseButton = new Button(inputGroup, SWT.PUSH);
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

/*
        Group settingsGroup = UIUtils.createControlGroup(
                composite, PostgreMessages.tools_script_execute_wizard_page_settings_group_settings, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
        logLevelCombo = UIUtils.createLabelCombo(
                settingsGroup, PostgreMessages.tools_script_execute_wizard_page_settings_label_log_level, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (PostgreScriptExecuteWizard.LogLevel logLevel : PostgreScriptExecuteWizard.LogLevel.values()) {
            logLevelCombo.add(logLevel.name());
        }
        logLevelCombo.select(wizard.getLogLevel().ordinal());
        logLevelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                wizard.setLogLevel(PostgreScriptExecuteWizard.LogLevel.valueOf(logLevelCombo.getText()));
            }
        });
*/

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
