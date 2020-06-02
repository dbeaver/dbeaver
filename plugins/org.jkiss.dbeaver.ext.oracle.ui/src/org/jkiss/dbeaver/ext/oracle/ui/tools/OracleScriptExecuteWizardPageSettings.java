/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.tools;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.oracle.tasks.OracleScriptExecuteSettings;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeToolWizardPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;

import java.util.List;


class OracleScriptExecuteWizardPageSettings extends AbstractNativeToolWizardPage<OracleScriptExecuteWizard> {
    private TextWithOpenFile inputFileText;

    OracleScriptExecuteWizardPageSettings(OracleScriptExecuteWizard wizard) {
        super(wizard, OracleUIMessages.tools_script_execute_wizard_page_settings_page_name);
        setTitle(OracleUIMessages.tools_script_execute_wizard_page_settings_page_name);
        setDescription(OracleUIMessages.tools_script_execute_wizard_page_settings_page_description);
    }

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete() && wizard.getSettings().getInputFile() != null;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group outputGroup = UIUtils.createControlGroup(composite, OracleUIMessages.tools_script_execute_wizard_page_settings_group_input, 3, GridData.FILL_HORIZONTAL, 0);
        outputGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputFileText = new TextWithOpenFile(outputGroup, OracleUIMessages.tools_script_execute_wizard_page_settings_label_input_file, new String[] { "*.sql", "*.txt", "*" } );
        inputFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        wizard.createTaskSaveGroup(composite);

        setControl(composite);

        //updateState();
    }

    @Override
    public void activatePage() {
        if (wizard.getSettings().getInputFile() != null) {
            inputFileText.setText(wizard.getSettings().getInputFile());
        }

        updateState();
    }

    @Override
    public void deactivatePage() {
        super.deactivatePage();
        saveState();
    }

    @Override
    public void saveState() {
        super.saveState();

        OracleScriptExecuteSettings settings = wizard.getSettings();
        List<DBSObject> selectedConnections = settings.getDatabaseObjects();
        settings.setDataSourceContainer(selectedConnections.isEmpty() ? null : selectedConnections.get(0).getDataSource().getContainer());
        settings.setInputFile(inputFileText.getText());
    }

    @Override
    protected void updateState() {
        saveState();

        getContainer().updateButtons();
    }

}
