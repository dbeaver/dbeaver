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

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.core.CoreMessages;

/**
 * PrefPageConnections
 */
public class PrefPageConnections extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.connections"; //$NON-NLS-1$

    private Button disableClientApplicationNameCheck;
    private Button overrideClientApplicationNameCheck;
    private Text clientApplicationNameText;

    private Button connUseEnvVariables;

    public PrefPageConnections()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.META_CLIENT_NAME_DISABLE) ||
            store.contains(ModelPreferences.META_CLIENT_NAME_OVERRIDE) ||
            store.contains(ModelPreferences.META_CLIENT_NAME_VALUE) ||

            store.contains(ModelPreferences.CONNECT_USE_ENV_VARS)
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
        {
            Group clientNameGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_client_name_group, 2, GridData.FILL_HORIZONTAL, 0);

            disableClientApplicationNameCheck = UIUtils.createCheckbox(clientNameGroup, CoreMessages.pref_page_database_label_disable_client_application_name, null, false, 2);

            final Label label = UIUtils.createLabel(clientNameGroup,
                CoreMessages.pref_page_database_client_name_group_description);
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            label.setLayoutData(gd);
            overrideClientApplicationNameCheck = UIUtils.createCheckbox(clientNameGroup, CoreMessages.pref_page_database_label_override_client_application_name, null, false, 2);
            overrideClientApplicationNameCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateClientAppEnablement();
                }
            });
            clientApplicationNameText = UIUtils.createLabelText(clientNameGroup, CoreMessages.pref_page_database_label_client_application_name, "");
        }

        {
            Group connGroup = UIUtils.createControlGroup(composite, "General", 2, GridData.FILL_HORIZONTAL, 0);

            connUseEnvVariables = UIUtils.createCheckbox(connGroup, "Use environment variables in connection parameters", null, false, 2);
        }
        return composite;
    }

    private void updateClientAppEnablement() {
        clientApplicationNameText.setEnabled(overrideClientApplicationNameCheck.getSelection());
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            disableClientApplicationNameCheck.setSelection(store.getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE));
            overrideClientApplicationNameCheck.setSelection(store.getBoolean(ModelPreferences.META_CLIENT_NAME_OVERRIDE));
            clientApplicationNameText.setText(store.getString(ModelPreferences.META_CLIENT_NAME_VALUE));

            connUseEnvVariables.setSelection(store.getBoolean(ModelPreferences.CONNECT_USE_ENV_VARS));

            updateClientAppEnablement();
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ModelPreferences.META_CLIENT_NAME_DISABLE, disableClientApplicationNameCheck.getSelection());
            store.setValue(ModelPreferences.META_CLIENT_NAME_OVERRIDE, overrideClientApplicationNameCheck.getSelection());
            store.setValue(ModelPreferences.META_CLIENT_NAME_VALUE, clientApplicationNameText.getText());

            store.setValue(ModelPreferences.CONNECT_USE_ENV_VARS, connUseEnvVariables.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ModelPreferences.META_CLIENT_NAME_DISABLE);
        store.setToDefault(ModelPreferences.META_CLIENT_NAME_OVERRIDE);
        store.setToDefault(ModelPreferences.META_CLIENT_NAME_VALUE);

        store.setToDefault(ModelPreferences.CONNECT_USE_ENV_VARS);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}