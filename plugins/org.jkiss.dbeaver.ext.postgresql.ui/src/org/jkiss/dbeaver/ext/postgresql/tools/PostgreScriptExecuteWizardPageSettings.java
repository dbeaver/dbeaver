/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.io.File;


public class PostgreScriptExecuteWizardPageSettings extends PostgreToolWizardPageSettings<PostgreScriptExecuteWizard> {
    private Text inputFileText;

    PostgreScriptExecuteWizardPageSettings(PostgreScriptExecuteWizard wizard) {
        super(wizard, PostgreMessages.tool_script_title_execute);
        setTitle(PostgreMessages.tool_script_title_execute);
        setDescription(PostgreMessages.tool_script_description_execute);
    }

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete() && wizard.getSettings().getInputFile() != null;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group inputGroup = UIUtils.createControlGroup(
            composite, PostgreMessages.tool_script_label_input, 3, GridData.FILL_HORIZONTAL, 0);
        inputFileText = UIUtils.createLabelText(
            inputGroup, PostgreMessages.tool_script_label_input_file, "", SWT.BORDER | SWT.READ_ONLY); //$NON-NLS-2$
        inputFileText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                chooseInputFile();
            }
        });
        Button browseButton = new Button(inputGroup, SWT.PUSH);
        browseButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                chooseInputFile();
            }
        });

        if (wizard.getSettings().getInputFile() != null) {
            inputFileText.setText(wizard.getSettings().getInputFile());
        }

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);
        wizard.createTaskSaveGroup(extraGroup);

        setControl(composite);
    }

    private void chooseInputFile() {
        File file = DialogUtils.openFile(getShell(), new String[]{"*.sql", "*.txt", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (file != null) {
            inputFileText.setText(file.getAbsolutePath());
        }
        updateState();
    }

    @Override
    public void saveState() {
        super.saveState();
        wizard.getSettings().setInputFile(inputFileText.getText());
    }


    @Override
    protected void updateState() {
        saveState();
        getContainer().updateButtons();
    }

}
