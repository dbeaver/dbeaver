/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.prefs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

public class PrefPageDataTransfer extends TargetPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.datatransfer";

    private Button reconnectToLastDatabaseButton;
    private Text fallbackOutputDirectoryText;
    private Combo nameCaseCombo;
    private Combo replaceCombo;
    private Spinner typeLengthSpinner;

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dsContainer) {
        DBPPreferenceStore store = dsContainer.getPreferenceStore();
        return
            store.contains(DTConstants.PREF_NAME_CASE_MAPPING) ||
                store.contains(DTConstants.PREF_REPLACE_MAPPING) ||
                store.contains(DTConstants.PREF_MAX_TYPE_LENGTH);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 1);
        final DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();

        if (!isDataSourcePreferencePage()) {
            final Group group = UIUtils
                .createControlGroup(composite, DTUIMessages.pref_data_transfer_wizard_title, 1, GridData.FILL_HORIZONTAL, 0);

            reconnectToLastDatabaseButton = UIUtils.createCheckbox(
                group,
                DTUIMessages.pref_data_transfer_wizard_reconnect_to_database,
                preferences.getBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE)
            );
        }

        if (!isDataSourcePreferencePage()) {
            final Group group = UIUtils
                .createControlGroup(composite, DTUIMessages.pref_data_transfer_options_title, 2, GridData.FILL_HORIZONTAL, 0);

            fallbackOutputDirectoryText = DialogUtils.createOutputFolderChooser(
                group,
                DTUIMessages.pref_data_transfer_options_fallback_directory,
                DTUIMessages.pref_data_transfer_options_fallback_directory_tip,
                null,
                null,
                false,
                null
            );
            fallbackOutputDirectoryText.setMessage(DTConstants.DEFAULT_FALLBACK_OUTPUT_DIRECTORY);
        }

        {
            final Group mappingGroup = UIUtils.createControlGroup(
                composite,
                DTUIMessages.pref_data_transfer_mapping_group,
                2,
                GridData.FILL_HORIZONTAL,
                0);

            final Label label = UIUtils.createLabel(mappingGroup,
                DTUIMessages.pref_data_transfer_info_label_mapping);
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            label.setLayoutData(gd);

            nameCaseCombo = UIUtils.createLabelCombo(
                mappingGroup,
                DTUIMessages.pref_data_transfer_name_case_label,
                SWT.READ_ONLY | SWT.DROP_DOWN);
            nameCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            nameCaseCombo.add(DTMessages.pref_data_transfer_name_case_default);
            nameCaseCombo.add(DTMessages.pref_data_transfer_name_case_upper);
            nameCaseCombo.add(DTMessages.pref_data_transfer_name_case_lower);

            replaceCombo = UIUtils.createLabelCombo(
                mappingGroup,
                DTUIMessages.pref_data_transfer_replacing_combo_label,
                SWT.READ_ONLY | SWT.DROP_DOWN);
            replaceCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            replaceCombo.add(DTMessages.pref_data_transfer_replacing_combo_do_not);
            replaceCombo.add(DTMessages.pref_data_transfer_replacing_combo_underscores);
            replaceCombo.add(DTMessages.pref_data_transfer_replacing_combo_camel_case);
            replaceCombo.setToolTipText(DTUIMessages.pref_data_transfer_replacing_combo_tip);

            UIUtils.createControlLabel(mappingGroup, DTUIMessages.pref_data_transfer_spanner_max_length);
            typeLengthSpinner = UIUtils.createSpinner(
                mappingGroup,
                DTUIMessages.pref_data_transfer_spanner_max_length_tip,
                DTConstants.DEFAULT_MAX_TYPE_LENGTH,
                1,
                Integer.MAX_VALUE);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();
        if (reconnectToLastDatabaseButton != null) {
            reconnectToLastDatabaseButton.setSelection(preferences.getBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE));
        }
        if (fallbackOutputDirectoryText != null) {
            fallbackOutputDirectoryText.setText(CommonUtils.notEmpty(preferences.getString(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY)));
        }
        if (isDataSourcePreferencePage()) {
            preferences = store;
        }
        nameCaseCombo.select(preferences.getInt(DTConstants.PREF_NAME_CASE_MAPPING));
        replaceCombo.select(preferences.getInt(DTConstants.PREF_REPLACE_MAPPING));
        typeLengthSpinner.setSelection(preferences.contains(DTConstants.PREF_MAX_TYPE_LENGTH) ?
            preferences.getInt(DTConstants.PREF_MAX_TYPE_LENGTH) : DTConstants.DEFAULT_MAX_TYPE_LENGTH);

    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();

        if (reconnectToLastDatabaseButton != null) {
            preferences.setValue(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE, reconnectToLastDatabaseButton.getSelection());
        }
        if (fallbackOutputDirectoryText != null) {
            preferences.setValue(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY, fallbackOutputDirectoryText.getText());
        }

        if (isDataSourcePreferencePage()) {
            preferences = store;
        }
        preferences.setValue(DTConstants.PREF_NAME_CASE_MAPPING, nameCaseCombo.getSelectionIndex());
        preferences.setValue(DTConstants.PREF_REPLACE_MAPPING, replaceCombo.getSelectionIndex());
        preferences.setValue(DTConstants.PREF_MAX_TYPE_LENGTH, typeLengthSpinner.getSelection());

        PrefUtils.savePreferenceStore(preferences);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE);
        store.setToDefault(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY);
        store.setToDefault(DTConstants.PREF_NAME_CASE_MAPPING);
        store.setToDefault(DTConstants.PREF_REPLACE_MAPPING);
        store.setToDefault(DTConstants.PREF_MAX_TYPE_LENGTH);
    }

    @Override
    protected void performDefaults() {
        final DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();
        if (reconnectToLastDatabaseButton != null) {
            reconnectToLastDatabaseButton.setSelection(preferences.getDefaultBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE));
        }
        if (fallbackOutputDirectoryText != null) {
            fallbackOutputDirectoryText.setText("");
            fallbackOutputDirectoryText.setMessage(preferences.getDefaultString(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY));
        }
        nameCaseCombo.select(preferences.getDefaultInt(DTConstants.PREF_NAME_CASE_MAPPING));
        replaceCombo.select(preferences.getDefaultInt(DTConstants.PREF_REPLACE_MAPPING));
        typeLengthSpinner.setSelection(preferences.getDefaultInt(DTConstants.PREF_MAX_TYPE_LENGTH));
    }
}
