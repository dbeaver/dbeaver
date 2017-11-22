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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.postgresql.PostgresMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.utils.CommonUtils;


class PostgreRestoreWizardPageSettings extends PostgreWizardPageSettings<PostgreRestoreWizard>
{

    private TextWithOpenFile inputFileText;
    private Combo formatCombo;
    private Button cleanFirstButton;

    protected PostgreRestoreWizardPageSettings(PostgreRestoreWizard wizard)
    {
        super(wizard, PostgresMessages.wizard_restore_page_setting_title_setting);
        setTitle(PostgresMessages.wizard_restore_page_setting_title);
        setDescription(PostgresMessages.wizard_restore_page_setting_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && !CommonUtils.isEmpty(wizard.inputFile);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Listener updateListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                updateState();
            }
        };

        Group formatGroup = UIUtils.createControlGroup(composite, PostgresMessages.wizard_restore_page_setting_label_setting, 2, GridData.FILL_HORIZONTAL, 0);
        formatCombo = UIUtils.createLabelCombo(formatGroup, PostgresMessages.wizard_restore_page_setting_label_format, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (PostgreBackupWizard.ExportFormat format : PostgreBackupWizard.ExportFormat.values()) {
            formatCombo.add(format.getTitle());
        }
        formatCombo.select(wizard.format.ordinal());
        formatCombo.addListener(SWT.Selection, updateListener);

        cleanFirstButton = UIUtils.createCheckbox(formatGroup,
        	PostgresMessages.wizard_restore_page_setting_btn_clean_first,
            false
        );
        cleanFirstButton.addListener(SWT.Selection, updateListener);

        Group inputGroup = UIUtils.createControlGroup(composite, PostgresMessages.wizard_restore_page_setting_label_input, 2, GridData.FILL_HORIZONTAL, 0);
        UIUtils.createControlLabel(inputGroup, PostgresMessages.wizard_restore_page_setting_label_backup_file);
        inputFileText = new TextWithOpenFile(inputGroup, PostgresMessages.wizard_restore_page_setting_label_choose_backup_file, new String[] {"*.backup","*"});
        inputFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputFileText.getTextControl().addListener(SWT.Modify, updateListener);

        createSecurityGroup(composite);

        setControl(composite);
    }

    private void updateState()
    {
        wizard.format = PostgreBackupWizard.ExportFormat.values()[formatCombo.getSelectionIndex()];
        wizard.inputFile = inputFileText.getText();
        wizard.cleanFirst = cleanFirstButton.getSelection();

        getContainer().updateButtons();
    }

}
