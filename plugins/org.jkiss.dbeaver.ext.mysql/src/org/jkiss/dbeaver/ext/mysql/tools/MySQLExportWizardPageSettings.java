/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


class MySQLExportWizardPageSettings extends MySQLWizardPageSettings<MySQLExportWizard>
{

    private Text outputFolderText;
    private Text outputFileText;
    private Combo methodCombo;
    private Button noCreateStatementsCheck;
    private Button addDropStatementsCheck;
    private Button disableKeysCheck;
    private Button extendedInsertsCheck;
    private Button dumpEventsCheck;
    private Button commentsCheck;
    private Button removeDefiner;
    private Button binaryInHex;

    protected MySQLExportWizardPageSettings(MySQLExportWizard wizard)
    {
        super(wizard, MySQLMessages.tools_db_export_wizard_page_settings_page_name);
        setTitle(MySQLMessages.tools_db_export_wizard_page_settings_page_name);
        setDescription((MySQLMessages.tools_db_export_wizard_page_settings_page_description));
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && wizard.getOutputFolder() != null;
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

        Group settingsGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_settings, 3, GridData.FILL_HORIZONTAL, 0);
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
        removeDefiner = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_remove_definer, wizard.removeDefiner);
        removeDefiner.addSelectionListener(changeListener);
        binaryInHex = UIUtils.createCheckbox(settingsGroup, MySQLMessages.tools_db_export_wizard_page_settings_checkbox_binary_hex, wizard.binariesInHex);
        binaryInHex.addSelectionListener(changeListener);

        Group outputGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_output, 2, GridData.FILL_HORIZONTAL, 0);
        outputFolderText = DialogUtils.createOutputFolderChooser(outputGroup, MySQLMessages.tools_db_export_wizard_page_settings_label_out_text, new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateState();
            }
        });
        outputFileText = UIUtils.createLabelText(outputGroup, "File name pattern", wizard.getOutputFilePattern());
        outputFileText.setToolTipText("Output file name pattern. Allowed variables: ${host}, ${database}, ${table}, ${timestamp}.");
        UIUtils.installContentProposal(
            outputFileText,
            new TextContentAdapter(),
            new SimpleContentProposalProvider(new String[]{"${host}", "${database}", "${table}", "${timestamp}"}));
        outputFileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                wizard.outputFilePattern = outputFileText.getText();
            }
        });
        if (wizard.getOutputFolder() != null) {
            outputFolderText.setText(wizard.getOutputFolder().getAbsolutePath());
        }

        createSecurityGroup(composite);

        setControl(composite);
    }

    private void updateState()
    {
        String fileName = outputFolderText.getText();
        wizard.setOutputFolder(CommonUtils.isEmpty(fileName) ? null : new File(fileName));
        wizard.setOutputFilePattern(outputFileText.getText());
        switch (methodCombo.getSelectionIndex()) {
            case 0: wizard.method = MySQLExportWizard.DumpMethod.ONLINE; break;
            case 1: wizard.method = MySQLExportWizard.DumpMethod.LOCK_ALL_TABLES; break;
            default: wizard.method = MySQLExportWizard.DumpMethod.NORMAL; break;
        }
        wizard.noCreateStatements = noCreateStatementsCheck.getSelection();
        wizard.addDropStatements = addDropStatementsCheck.getSelection();
        wizard.disableKeys = disableKeysCheck.getSelection();
        wizard.extendedInserts = extendedInsertsCheck.getSelection();
        wizard.dumpEvents = dumpEventsCheck.getSelection();
        wizard.comments = commentsCheck.getSelection();
        wizard.removeDefiner = removeDefiner.getSelection();
        wizard.binariesInHex = binaryInHex.getSelection();

        getContainer().updateButtons();
    }

}
