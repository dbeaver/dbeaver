/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageErrorHandle
 */
public class PrefPageErrorHandle extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.errorHandle"; //$NON-NLS-1$

    private Spinner connectionOpenTimeout;
    private Spinner connectionCloseTimeout;
    private Spinner connectionValidateTimeout;

    private Button rollbackOnErrorCheck;
    private Button connectionAutoRecoverEnabled;
    private Spinner connectionAutoRecoverRetryCount;

    private Spinner cancelCheckTimeout;

    public PrefPageErrorHandle()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.CONNECTION_OPEN_TIMEOUT) ||
            store.contains(ModelPreferences.CONNECTION_CLOSE_TIMEOUT) ||
            store.contains(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT) ||

            store.contains(ModelPreferences.QUERY_ROLLBACK_ON_ERROR) ||
            store.contains(ModelPreferences.EXECUTE_RECOVER_ENABLED) ||
            store.contains(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT) ||

            store.contains(ModelPreferences.EXECUTE_CANCEL_CHECK_TIMEOUT)
            ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        // Misc settings
        {
            Group timeoutsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_error_handle_group_timeouts_title, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            connectionOpenTimeout = UIUtils.createLabelSpinner(timeoutsGroup, CoreMessages.pref_page_error_handle_connection_open_timeout_label, CoreMessages.pref_page_error_handle_connection_open_timeout_label_tip, 0, 0, Integer.MAX_VALUE);
            connectionCloseTimeout = UIUtils.createLabelSpinner(timeoutsGroup, CoreMessages.pref_page_error_handle_connection_close_timeout_label, CoreMessages.pref_page_error_handle_connection_close_timeout_label_tip, 0, 0, Integer.MAX_VALUE);
            connectionValidateTimeout = UIUtils.createLabelSpinner(timeoutsGroup, CoreMessages.pref_page_error_handle_connection_validate_timeout_label, CoreMessages.pref_page_error_handle_connection_validate_timeout_label_tip, 0, 0, Integer.MAX_VALUE);
        }

        // Misc settings
        {
            Group errorGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_error_handle_group_execute_title, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            rollbackOnErrorCheck = UIUtils.createCheckbox(errorGroup, CoreMessages.pref_page_database_general_checkbox_rollback_on_error, null, false, 2);
            connectionAutoRecoverEnabled = UIUtils.createCheckbox(errorGroup, CoreMessages.pref_page_error_handle_recover_enabled_label, CoreMessages.pref_page_error_handle_recover_enabled_tip, false, 2);
            connectionAutoRecoverRetryCount = UIUtils.createLabelSpinner(errorGroup, CoreMessages.pref_page_error_handle_recover_retry_count_label, CoreMessages.pref_page_error_handle_recover_retry_count_tip, 0, 0, Integer.MAX_VALUE);
        }

        // Canceling
        {
            Group errorGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_error_handle_group_cancel_title, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            cancelCheckTimeout = UIUtils.createLabelSpinner(errorGroup, CoreMessages.pref_page_error_handle_cancel_check_timeout, CoreMessages.pref_page_error_handle_cancel_check_timeout_tip, 0, 0, Integer.MAX_VALUE);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            connectionOpenTimeout.setSelection(store.getInt(ModelPreferences.CONNECTION_OPEN_TIMEOUT));
            connectionCloseTimeout.setSelection(store.getInt(ModelPreferences.CONNECTION_CLOSE_TIMEOUT));
            connectionValidateTimeout.setSelection(store.getInt(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT));

            rollbackOnErrorCheck.setSelection(store.getBoolean(ModelPreferences.QUERY_ROLLBACK_ON_ERROR));
            connectionAutoRecoverEnabled.setSelection(store.getBoolean(ModelPreferences.EXECUTE_RECOVER_ENABLED));
            connectionAutoRecoverRetryCount.setSelection(store.getInt(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT));

            cancelCheckTimeout.setSelection(store.getInt(ModelPreferences.EXECUTE_CANCEL_CHECK_TIMEOUT));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ModelPreferences.CONNECTION_OPEN_TIMEOUT, connectionOpenTimeout.getSelection());
            store.setValue(ModelPreferences.CONNECTION_CLOSE_TIMEOUT, connectionCloseTimeout.getSelection());
            store.setValue(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT, connectionValidateTimeout.getSelection());

            store.setValue(ModelPreferences.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(ModelPreferences.EXECUTE_RECOVER_ENABLED, connectionAutoRecoverEnabled.getSelection());
            store.setValue(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT, connectionAutoRecoverRetryCount.getSelection());

            store.setValue(ModelPreferences.EXECUTE_CANCEL_CHECK_TIMEOUT, cancelCheckTimeout.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ModelPreferences.CONNECTION_OPEN_TIMEOUT);
        store.setToDefault(ModelPreferences.CONNECTION_CLOSE_TIMEOUT);
        store.setToDefault(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT);

        store.setToDefault(ModelPreferences.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(ModelPreferences.EXECUTE_RECOVER_ENABLED);
        store.setToDefault(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT);

        store.setToDefault(ModelPreferences.EXECUTE_CANCEL_CHECK_TIMEOUT);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}