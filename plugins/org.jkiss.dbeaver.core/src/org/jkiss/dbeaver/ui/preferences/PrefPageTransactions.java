/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.Locale;

/**
 * PrefPageTransactions
 */
public class PrefPageTransactions extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.transactions"; //$NON-NLS-1$

    private Button smartCommitCheck;
    private Button smartCommitRecoverCheck;
    private Button autoCloseTransactionsCheck;
    private Text autoCloseTransactionsTtlText;
    private Button showTransactionNotificationsCheck;

    public PrefPageTransactions()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.TRANSACTIONS_SMART_COMMIT) ||
            store.contains(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER) ||
            store.contains(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED) ||
            store.contains(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL) ||
            store.contains(ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS)
            ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        boolean dataSourcePreferencePage = isDataSourcePreferencePage();
        Group txnNameGroup = UIUtils.createControlGroup(
            composite,
            CoreMessages.dialog_connection_edit_wizard_transactions,
            2,
            GridData.FILL_HORIZONTAL,
            0);
        String settingsTipString;
        if (dataSourcePreferencePage) {
            smartCommitCheck = UIUtils.createCheckbox(
                txnNameGroup,
                CoreMessages.action_menu_transaction_smart_auto_commit,
                CoreMessages.action_menu_transaction_smart_auto_commit_tip,
                false,
                2);
            smartCommitCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateCommitRecoverCheckBox();
                }
            });
            smartCommitRecoverCheck = UIUtils.createCheckbox(
                txnNameGroup,
                CoreMessages.action_menu_transaction_smart_auto_commit_recover,
                CoreMessages.action_menu_transaction_smart_auto_commit_recover_tip,
                false,
                2);

            autoCloseTransactionsCheck = UIUtils.createCheckbox(
                txnNameGroup,
                CoreMessages.action_menu_transaction_auto_close_enabled,
                CoreMessages.action_menu_transaction_auto_close_enabled_tip,
                true,
                1);
            autoCloseTransactionsTtlText = new Text(txnNameGroup, SWT.BORDER);
            autoCloseTransactionsTtlText.setToolTipText(CoreMessages.action_menu_transaction_auto_close_ttl_tip);
            autoCloseTransactionsTtlText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
            //autoCloseTransactionsTtlText.setEnabled(false);
            GridData gd = new GridData();
            gd.widthHint = UIUtils.getFontHeight(autoCloseTransactionsTtlText) * 6;
            autoCloseTransactionsTtlText.setLayoutData(gd);

            settingsTipString = CoreMessages.action_menu_transaction_pref_page_link_extended;

/*
            autoCloseTransactionsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    autoCloseTransactionsTtlText.setEnabled(autoCloseTransactionsCheck.getSelection());
                }
            });
*/
        } else {
            settingsTipString = CoreMessages.action_menu_transaction_pref_page_link;
        }

        UIUtils.createPreferenceLink(
            txnNameGroup,
            settingsTipString,
            PrefPageConnectionTypes.PAGE_ID,
            (IWorkbenchPreferenceContainer) getContainer(),
            null
        );

        {
            Group notifyNameGroup = UIUtils.createControlGroup(
                composite, CoreMessages.pref_page_transactions_notify_name_group_label, 2, GridData.FILL_HORIZONTAL, 0
            );
            showTransactionNotificationsCheck = UIUtils.createCheckbox(
                notifyNameGroup,
                CoreMessages.pref_page_transactions_notifications_show_check_label,
                CoreMessages.pref_page_transactions_notifications_show_check_description,
                false,
                2
            );
        }
        return composite;
    }

    private void updateCommitRecoverCheckBox() {
        if (!smartCommitCheck.getSelection()) {
            // Works only with the smart commit mode
            smartCommitRecoverCheck.setEnabled(false);
            smartCommitRecoverCheck.setSelection(false);
        } else if (!smartCommitRecoverCheck.isEnabled()) {
            smartCommitRecoverCheck.setEnabled(true);
        }
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            // First check data source settings, second - connection type, third - global settings.
            DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
            DBPConnectionType connectionType = null;
            if (dataSourceContainer != null) {
                connectionType = dataSourceContainer.getConnectionConfiguration().getConnectionType();
            }
            if (smartCommitCheck != null) {
                smartCommitCheck.setSelection(store.contains(ModelPreferences.TRANSACTIONS_SMART_COMMIT) || connectionType == null ?
                    store.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT) : connectionType.isSmartCommit());
            }
            if (smartCommitRecoverCheck != null) {
                smartCommitRecoverCheck.setSelection(
                    store.contains(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER) || connectionType == null ?
                        store.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER) : connectionType.isSmartCommitRecover());
                if (smartCommitCheck != null) {
                    updateCommitRecoverCheckBox();
                }
            }
            if (autoCloseTransactionsCheck != null) {
                autoCloseTransactionsCheck.setSelection(
                    store.contains(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED) || connectionType == null ?
                        store.getBoolean(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED) : connectionType.isAutoCloseTransactions());
                autoCloseTransactionsTtlText.setText(
                    store.contains(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL) || connectionType == null ?
                        store.getString(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL) :
                        String.valueOf(connectionType.getCloseIdleConnectionPeriod()));
            }
            //autoCloseTransactionsTtlText.setEnabled(autoCloseTransactionsCheck.getSelection());

            showTransactionNotificationsCheck.setSelection(store.getBoolean(ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            if (smartCommitCheck != null) {
                store.setValue(ModelPreferences.TRANSACTIONS_SMART_COMMIT, smartCommitCheck.getSelection());
            }
            if (smartCommitRecoverCheck != null) {
                store.setValue(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER, smartCommitRecoverCheck.getSelection());
            }
            if (autoCloseTransactionsCheck != null) {
                store.setValue(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED, autoCloseTransactionsCheck.getSelection());
                store.setValue(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL, autoCloseTransactionsTtlText.getText());
            }

            store.setValue(ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS, showTransactionNotificationsCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ModelPreferences.TRANSACTIONS_SMART_COMMIT);
        store.setToDefault(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER);
        store.setToDefault(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED);
        store.setToDefault(ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL);

        store.setToDefault(ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS);
    }

    @Override
    protected void performDefaults() {
        showTransactionNotificationsCheck.setSelection(
            DBWorkbench.getPlatform().getPreferenceStore().getDefaultBoolean(ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS));
        super.performDefaults();
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}