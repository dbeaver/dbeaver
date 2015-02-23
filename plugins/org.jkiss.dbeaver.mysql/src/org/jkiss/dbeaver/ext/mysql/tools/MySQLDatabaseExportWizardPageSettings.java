/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class MySQLDatabaseExportWizardPageSettings extends MySQLDatabaseWizardPageSettings<MySQLDatabaseExportWizard>
{

    private Text outputFileText;
    private Combo methodCombo;
    private Button noCreateStatementsCheck;
    private Button addDropStatementsCheck;
    private Button disableKeysCheck;
    private Button extendedInsertsCheck;
    private Button dumpEventsCheck;
    private Button commentsCheck;

    protected MySQLDatabaseExportWizardPageSettings(MySQLDatabaseExportWizard wizard)
    {
        super(wizard, MySQLMessages.tools_db_export_wizard_page_settings_page_name);
        setTitle(MySQLMessages.tools_db_export_wizard_page_settings_page_name);
        setDescription((MySQLMessages.tools_db_export_wizard_page_settings_page_description));
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.getOutputFile() != null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group methodGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_exe_method, 1, GridData.FILL_HORIZONTAL, 0);
        methodCombo = new Combo(methodGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        methodCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        methodCombo.add(MySQLMessages.tools_db_export_wizard_page_settings_combo_item_online_backup);
        methodCombo.add(MySQLMessages.tools_db_export_wizard_page_settings_combo_item_lock_tables);
        methodCombo.add(MySQLMessages.tools_db_export_wizard_page_settings_combo_item_normal);
        methodCombo.select(wizard.method.ordinal());

        SelectionListener changeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }
        };

        Group settingsGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_settings, 2, GridData.FILL_HORIZONTAL, 0);
        noCreateStatementsCheck = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_no_create, wizard.noCreateStatements);
        noCreateStatementsCheck.addSelectionListener(changeListener);
        addDropStatementsCheck = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_add_drop, wizard.addDropStatements);
        addDropStatementsCheck.addSelectionListener(changeListener);
        disableKeysCheck = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_disable_keys, wizard.disableKeys);
        disableKeysCheck.addSelectionListener(changeListener);
        extendedInsertsCheck = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_ext_inserts, wizard.extendedInserts);
        extendedInsertsCheck.addSelectionListener(changeListener);
        dumpEventsCheck = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_dump_events, wizard.dumpEvents);
        dumpEventsCheck.addSelectionListener(changeListener);
        commentsCheck = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_addnl_comments, wizard.comments);
        commentsCheck.addSelectionListener(changeListener);

        Group outputGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_output, 3, GridData.FILL_HORIZONTAL, 0);
        outputFileText = UIUtils.createLabelText(outputGroup, MySQLMessages.tools_db_export_wizard_page_settings_label_out_text, null);
        if (wizard.getOutputFile() != null) {
            outputFileText.setText(wizard.getOutputFile().getAbsolutePath());
        }
        outputFileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                updateState();
            }
        });
        Button browseButton = new Button(outputGroup, SWT.PUSH);
        browseButton.setImage(DBIcon.TREE_FOLDER.getImage());
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                File file = ContentUtils.selectFileForSave(getShell(), MySQLMessages.tools_db_export_wizard_page_settings_file_selector_title, new String[]{"*.sql", "*.txt", "*.*"}, outputFileText.getText()); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                if (file != null) {
                    outputFileText.setText(file.getAbsolutePath());
                    updateState();
                }
            }
        });

        createSecurityGroup(composite);

        setControl(composite);
    }

    private void updateState()
    {
        String fileName = outputFileText.getText();
        wizard.setOutputFile(CommonUtils.isEmpty(fileName) ? null : new File(fileName));
        switch (methodCombo.getSelectionIndex()) {
            case 0: wizard.method = MySQLDatabaseExportWizard.DumpMethod.ONLINE; break;
            case 1: wizard.method = MySQLDatabaseExportWizard.DumpMethod.LOCK_ALL_TABLES; break;
            default: wizard.method = MySQLDatabaseExportWizard.DumpMethod.NORMAL; break;
        }
        wizard.noCreateStatements = noCreateStatementsCheck.getSelection();
        wizard.addDropStatements = addDropStatementsCheck.getSelection();
        wizard.disableKeys = disableKeysCheck.getSelection();
        wizard.extendedInserts = extendedInsertsCheck.getSelection();
        wizard.dumpEvents = dumpEventsCheck.getSelection();
        wizard.comments = commentsCheck.getSelection();

        getContainer().updateButtons();
    }

}
