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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.List;

/**
 * PrefPageMetaData
 */
public class PrefPageMetaData extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.meta"; //$NON-NLS-1$

    private Button readExpensiveCheck;
    private Combo separateMetaConnectionCombo;
    private Button disableExtraMetadataRead;
    private Button caseSensitiveNamesCheck;
    private Button serverSideFiltersCheck;
    private Button addExtraDDLInfo;

    public PrefPageMetaData()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.READ_EXPENSIVE_PROPERTIES) ||
            store.contains(ModelPreferences.META_SEPARATE_CONNECTION) ||
            store.contains(ModelPreferences.META_CASE_SENSITIVE) ||
            store.contains(ModelPreferences.META_DISABLE_EXTRA_READ) ||
            store.contains(ModelPreferences.META_USE_SERVER_SIDE_FILTERS)
            ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }
    
    private static final List<SeparateConnectionBehavior> metaUseSeparateConnectionValues = List.of(
        SeparateConnectionBehavior.ALWAYS,
        SeparateConnectionBehavior.DEFAULT,
        SeparateConnectionBehavior.NEVER
    );


    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        {
            Group metadataGroup = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_database_general_group_metadata,
                1,
                GridData.HORIZONTAL_ALIGN_BEGINNING,
                0);

            separateMetaConnectionCombo = UIUtils.createLabelCombo(
                UIUtils.createComposite(metadataGroup, 3),
                CoreMessages.pref_page_database_general_separate_meta_connection,
                NLS.bind(CoreMessages.pref_page_database_general_separate_meta_connection_tip, PrefUtils.collectSingleConnectionDrivers()),
                SWT.READ_ONLY | SWT.DROP_DOWN
            );
            if (this.getDataSourceContainer() != null && this.getDataSourceContainer().getDriver().isEmbedded()) {
                separateMetaConnectionCombo.setEnabled(false);
            } else {
                separateMetaConnectionCombo.setItems(metaUseSeparateConnectionValues.stream()
                    .map(SeparateConnectionBehavior::getTitle).toArray(String[]::new));
            }
            separateMetaConnectionCombo.setToolTipText(
                NLS.bind(CoreMessages.pref_page_database_general_separate_meta_connection_tip, PrefUtils.collectSingleConnectionDrivers())
            );
            ((GridData) separateMetaConnectionCombo.getLayoutData()).grabExcessHorizontalSpace = false;
            caseSensitiveNamesCheck = UIUtils.createCheckbox(
                metadataGroup,
                CoreMessages.pref_page_database_general_checkbox_case_sensitive_names,
                CoreMessages.pref_page_database_general_checkbox_case_sensitive_names_tip,
                false,
                1);
            addExtraDDLInfo = UIUtils.createCheckbox(
                metadataGroup,
                CoreMessages.pref_page_database_general_checkbox_add_special_DDL_info,
                CoreMessages.pref_page_database_general_checkbox_add_special_DDL_info_tip,
                true,
                1);
            serverSideFiltersCheck = UIUtils.createCheckbox(
                metadataGroup,
                CoreMessages.pref_page_database_general_server_side_object_filters,
                CoreMessages.pref_page_database_general_server_side_object_filters_tip,
                false,
                1);
        }

        {
            Group performanceGroup = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_database_general_group_performance,
                1,
                GridData.HORIZONTAL_ALIGN_BEGINNING,
                0);

            disableExtraMetadataRead = UIUtils.createCheckbox(
                performanceGroup,
                CoreMessages.pref_page_database_general_checkbox_disable_extra_metadata,
                CoreMessages.pref_page_database_general_checkbox_disable_extra_metadata_tip,
                false,
                1);
            readExpensiveCheck = UIUtils.createCheckbox(
                performanceGroup,
                CoreMessages.pref_page_database_general_checkbox_show_row_count,
                CoreMessages.pref_page_database_general_checkbox_show_row_count_tip,
                false,
                1);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            readExpensiveCheck.setSelection(store.getBoolean(ModelPreferences.READ_EXPENSIVE_PROPERTIES));
            separateMetaConnectionCombo.select(metaUseSeparateConnectionValues.indexOf(
                SeparateConnectionBehavior.parse(store.getString(ModelPreferences.META_SEPARATE_CONNECTION))
            ));
            caseSensitiveNamesCheck.setSelection(store.getBoolean(ModelPreferences.META_CASE_SENSITIVE));
            disableExtraMetadataRead.setSelection(store.getBoolean(ModelPreferences.META_DISABLE_EXTRA_READ));
            addExtraDDLInfo.setSelection(store.getBoolean(ModelPreferences.META_EXTRA_DDL_INFO));
            serverSideFiltersCheck.setSelection(store.getBoolean(ModelPreferences.META_USE_SERVER_SIDE_FILTERS));

        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ModelPreferences.READ_EXPENSIVE_PROPERTIES, readExpensiveCheck.getSelection());
            store.setValue(
                ModelPreferences.META_SEPARATE_CONNECTION,
                metaUseSeparateConnectionValues.get(separateMetaConnectionCombo.getSelectionIndex()).name()
            );
            store.setValue(ModelPreferences.META_CASE_SENSITIVE, caseSensitiveNamesCheck.getSelection());
            store.setValue(ModelPreferences.META_DISABLE_EXTRA_READ, disableExtraMetadataRead.getSelection());
            store.setValue(ModelPreferences.META_EXTRA_DDL_INFO, addExtraDDLInfo.getSelection());
            store.setValue(ModelPreferences.META_USE_SERVER_SIDE_FILTERS, serverSideFiltersCheck.getSelection());

        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ModelPreferences.READ_EXPENSIVE_PROPERTIES);
        store.setToDefault(ModelPreferences.META_SEPARATE_CONNECTION);
        store.setToDefault(ModelPreferences.META_CASE_SENSITIVE);
        store.setToDefault(ModelPreferences.META_DISABLE_EXTRA_READ);
        store.setToDefault(ModelPreferences.META_USE_SERVER_SIDE_FILTERS);
        store.setToDefault(ModelPreferences.META_EXTRA_DDL_INFO);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        readExpensiveCheck.setSelection(store.getDefaultBoolean(ModelPreferences.READ_EXPENSIVE_PROPERTIES));
        separateMetaConnectionCombo.select(metaUseSeparateConnectionValues.indexOf(
            SeparateConnectionBehavior.parse(store.getDefaultString(ModelPreferences.META_SEPARATE_CONNECTION))
        ));
        caseSensitiveNamesCheck.setSelection(store.getDefaultBoolean(ModelPreferences.META_CASE_SENSITIVE));
        disableExtraMetadataRead.setSelection(store.getDefaultBoolean(ModelPreferences.META_DISABLE_EXTRA_READ));
        addExtraDDLInfo.setSelection(store.getDefaultBoolean(ModelPreferences.META_EXTRA_DDL_INFO));
        serverSideFiltersCheck.setSelection(store.getDefaultBoolean(ModelPreferences.META_USE_SERVER_SIDE_FILTERS));
        super.performDefaults();
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}