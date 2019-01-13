/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractToolWizardPage;
import org.jkiss.utils.CommonUtils;

import java.io.File;


public class OracleScriptExecuteWizardPageSettings extends AbstractToolWizardPage<OracleScriptExecuteWizard>
{
    private Text inputFileText;

    public OracleScriptExecuteWizardPageSettings(OracleScriptExecuteWizard wizard)
    {
        super(wizard, OracleMessages.tools_script_execute_wizard_page_settings_page_name);
        setTitle(OracleMessages.tools_script_execute_wizard_page_settings_page_name);
        setDescription(OracleMessages.tools_script_execute_wizard_page_settings_page_description);
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

        Group outputGroup = UIUtils.createControlGroup(composite, OracleMessages.tools_script_execute_wizard_page_settings_group_input, 3, GridData.FILL_HORIZONTAL, 0);
        inputFileText = UIUtils.createLabelText(outputGroup, OracleMessages.tools_script_execute_wizard_page_settings_label_input_file, null); //$NON-NLS-2$
        Button browseButton = new Button(outputGroup, SWT.PUSH);
        browseButton.setText(OracleMessages.tools_script_execute_wizard_page_settings_button_browse);
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                File file = DialogUtils.openFile(getShell(), new String[]{"*.sql", "*.txt", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                if (file != null) {
                    inputFileText.setText(file.getAbsolutePath());
                }
                updateState();
            }
        });
        if (wizard.getInputFile() != null) {
            inputFileText.setText(wizard.getInputFile().getName());
        }

        setControl(composite);

        //updateState();
    }

    @Override
    protected void updateState()
    {
        String fileName = inputFileText.getText();
        wizard.setInputFile(CommonUtils.isEmpty(fileName) ? null : new File(fileName));

        getContainer().updateButtons();
    }

}
