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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.stream.Collectors;

public class DatabaseConsumerPageLoadSettings extends ActiveWizardPage<DataTransferWizard> {

    private Button transferAutoGeneratedColumns;
    private Button truncateTargetTable;

    public DatabaseConsumerPageLoadSettings() {
        super(DTUIMessages.data_transfer_db_page_load_settings_name);
        setTitle(DTUIMessages.data_transfer_db_page_load_settings_title);
        setDescription(DTUIMessages.data_transfer_db_page_load_settings_description);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final DatabaseConsumerSettings settings = getSettings();

        {
            Group loadSettings = UIUtils.createControlGroup(composite, DTUIMessages.data_transfer_db_page_load_settings_name, 1, GridData.FILL_HORIZONTAL, 0);

            transferAutoGeneratedColumns = UIUtils.createCheckbox(
                loadSettings,
                DTUIMessages.data_transfer_db_page_load_settings_checkbox_aoutogenerate_col,
                DTUIMessages.data_transfer_db_page_load_settings_tooltip_aoutogenerate_col,
                settings.isTransferAutoGeneratedColumns(), 1);
            transferAutoGeneratedColumns.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setTransferAutoGeneratedColumns(transferAutoGeneratedColumns .getSelection());
                }
            });

            truncateTargetTable = UIUtils.createCheckbox(loadSettings, DTUIMessages.data_transfer_db_page_load_settings_checkbox_truncate_target_table, settings.isTruncateBeforeLoad());
            truncateTargetTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (truncateTargetTable.getSelection() && !confirmDataTruncate()) {
                        truncateTargetTable.setSelection(false);
                        return;
                    }
                    settings.setTruncateBeforeLoad(truncateTargetTable.getSelection());
                }
            });
        }

        {
            Group performanceSettings = UIUtils.createControlGroup(composite, DTUIMessages.data_transfer_db_page_load_settings_group_perfomance, 4, GridData.FILL_HORIZONTAL, 0);

            final Button newConnectionCheckbox = UIUtils.createCheckbox(
                performanceSettings,
                DTMessages.data_transfer_wizard_output_checkbox_new_connection,
                null,
                settings.isOpenNewConnections(),
                4);
            newConnectionCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenNewConnections(newConnectionCheckbox.getSelection());
                }
            });

            final Button useTransactionsCheck = UIUtils.createCheckbox(performanceSettings, DTUIMessages.data_transfer_db_page_load_settings_checkbox_transactions, null, settings.isUseTransactions(), 4);
            useTransactionsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setUseTransactions(useTransactionsCheck.getSelection());
                }
            });

            final Spinner commitAfterEdit = UIUtils.createLabelSpinner(performanceSettings, DTUIMessages.data_transfer_db_page_load_settings_spinner_commit_after_edit, settings.getCommitAfterRows(), 1, Integer.MAX_VALUE);
            commitAfterEdit.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setCommitAfterRows(commitAfterEdit.getSelection());
                }
            });
            commitAfterEdit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        {
            Group generalSettings = UIUtils.createControlGroup(composite, DTUIMessages.data_transfer_db_page_load_settings_group_general, 4, GridData.FILL_HORIZONTAL, 0);
            final Button showTableCheckbox = UIUtils.createCheckbox(generalSettings, DTUIMessages.data_transfer_db_page_load_settings_checkbox_show_table, null, settings.isOpenTableOnFinish(), 4);
            showTableCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenTableOnFinish(showTableCheckbox.getSelection());
                }
            });
            final Button showFinalMessageCheckbox = UIUtils.createCheckbox(generalSettings, DTUIMessages.data_transfer_db_page_load_settings_checkbox_show_final_message, null, getWizard().getSettings().isShowFinalMessage(), 4);
            showFinalMessageCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setShowFinalMessage(showFinalMessageCheckbox.getSelection());
                }
            });
        }

        setControl(composite);
    }

    private DatabaseConsumerSettings getSettings() {
        return getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
    }

    @Override
    public void activatePage() {

        updatePageCompletion();

        UIUtils.asyncExec(() -> {
            DatabaseConsumerSettings settings = getSettings();
            if (settings.isTruncateBeforeLoad() && !confirmDataTruncate()) {
                truncateTargetTable.setSelection(false);
                settings.setTruncateBeforeLoad(false);
            }
        });
    }

    private boolean confirmDataTruncate() {
        Shell shell = getContainer().getShell();
        if (shell.isVisible() || getSettings().isTruncateBeforeLoad()) {
            String tableNames = getWizard().getSettings().getDataPipes().stream().map(pipe -> pipe.getConsumer() == null ? "" : pipe.getConsumer().getObjectName()).collect(Collectors.joining(","));
            if (!UIUtils.confirmAction(shell, DTUIMessages.data_transfer_db_page_load_settings_dialog_truncate_attention_title,
            		NLS.bind(DTUIMessages.data_transfer_db_page_load_settings_dialog_truncate_attention_text, tableNames )))
//            		"'Truncate target table' option is enabled.\n" +
//                "This will remove ALL data from target table(s) (" + tableNames + ").\n" +
//                "it will not be possible to revert this.\n" +
//                "Are you absolutely sure you want to proceed?"))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void deactivatePage() {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion() {
        return true;
    }

}