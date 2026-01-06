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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.PostgreUIUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFileRemote;


public class PostgreScriptExecuteWizardPageSettings extends PostgreToolWizardPageSettings<PostgreScriptExecuteWizard> {
    private Text inputFileText;

    PostgreScriptExecuteWizardPageSettings(PostgreScriptExecuteWizard wizard) {
        super(wizard, PostgreMessages.tool_script_title_execute);
        setTitle(PostgreMessages.tool_script_title_execute);
        setDescription(PostgreMessages.tool_script_description_execute);
    }

    @Override
    protected boolean determinePageCompletion() {
        if (wizard.getSettings().getInputFile() == null) {
            setErrorMessage("Input file not specified");
            return false;
        }
        return super.determinePageCompletion();
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group inputGroup = UIUtils.createControlGroup(
            composite, PostgreMessages.tool_script_label_input, 3, GridData.FILL_HORIZONTAL, 0);
        TextWithOpenFile inputText = new TextWithOpenFileRemote(
            inputGroup,
            PostgreMessages.tool_script_label_input_file,
            new String[]{"*.sql", "*.txt", "*"},
            SWT.OPEN | SWT.SINGLE,
            true,
            getWizard().getProject());
        inputText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.inputFileText = inputText.getTextControl();

        if (wizard.getSettings().getInputFile() != null) {
            inputFileText.setText(wizard.getSettings().getInputFile());
        }

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);

        PostgreUIUtils.addCompatibilityInfoLabelForForks(composite, wizard, null);

        setControl(composite);
    }

    @Override
    public void saveState() {
        super.saveState();
        wizard.getSettings().setInputFile(inputFileText.getText());
    }


    @Override
    protected void updateState() {
        saveState();
        updatePageCompletion();
        getContainer().updateButtons();
    }

}
