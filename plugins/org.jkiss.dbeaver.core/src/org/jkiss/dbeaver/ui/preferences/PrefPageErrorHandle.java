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

    private Button rollbackOnErrorCheck;
    private Button connectionAutoRecoverEnabled;
    private Spinner connectionAutoRecoverRetryCount;

    public PrefPageErrorHandle()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.QUERY_ROLLBACK_ON_ERROR) ||
            store.contains(ModelPreferences.EXECUTE_RECOVER_ENABLED) ||
            store.contains(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT)
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
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        // Misc settings
        {
            Group errorGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_error_handle_group_execute_title, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            rollbackOnErrorCheck = UIUtils.createCheckbox(errorGroup, CoreMessages.pref_page_database_general_checkbox_rollback_on_error, null, false, 2);
            connectionAutoRecoverEnabled = UIUtils.createCheckbox(errorGroup, CoreMessages.pref_page_error_handle_recover_enabled_label, CoreMessages.pref_page_error_handle_recover_enabled_tip, false, 2);
            connectionAutoRecoverRetryCount = UIUtils.createLabelSpinner(errorGroup, CoreMessages.pref_page_error_handle_recover_retry_count_label, CoreMessages.pref_page_error_handle_recover_retry_count_tip, 0, 0, Integer.MAX_VALUE);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            rollbackOnErrorCheck.setSelection(store.getBoolean(ModelPreferences.QUERY_ROLLBACK_ON_ERROR));
            connectionAutoRecoverEnabled.setSelection(store.getBoolean(ModelPreferences.EXECUTE_RECOVER_ENABLED));
            connectionAutoRecoverRetryCount.setSelection(store.getInt(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ModelPreferences.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(ModelPreferences.EXECUTE_RECOVER_ENABLED, connectionAutoRecoverEnabled.getSelection());
            store.setValue(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT, connectionAutoRecoverRetryCount.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ModelPreferences.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(ModelPreferences.EXECUTE_RECOVER_ENABLED);
        store.setToDefault(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}