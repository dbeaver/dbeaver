/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

public class PrefPageDataTransfer extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.datatransfer";

    private Button reconnectToLastDatabaseButton;
    private Text fallbackOutputDirectoryText;

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 1);
        final DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();

        {
            final Group group = UIUtils
                .createControlGroup(composite, DTUIMessages.pref_data_transfer_wizard_title, 1, GridData.FILL_HORIZONTAL, 0);

            reconnectToLastDatabaseButton = UIUtils.createCheckbox(
                group,
                DTUIMessages.pref_data_transfer_wizard_reconnect_to_database,
                preferences.getBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE)
            );
        }

        {
            final Group group = UIUtils
                .createControlGroup(composite, DTUIMessages.pref_data_transfer_options_title, 2, GridData.FILL_HORIZONTAL, 0);

            fallbackOutputDirectoryText = DialogUtils.createOutputFolderChooser(
                group,
                DTUIMessages.pref_data_transfer_options_fallback_directory,
                DTUIMessages.pref_data_transfer_options_fallback_directory_tip,
                null,
                null
            );

            fallbackOutputDirectoryText.setText(CommonUtils.notEmpty(preferences.getString(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY)));
            fallbackOutputDirectoryText.setMessage(DTConstants.DEFAULT_FALLBACK_OUTPUT_DIRECTORY);
        }

        return composite;
    }

    @Override
    protected void performDefaults() {
        final DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();

        reconnectToLastDatabaseButton.setSelection(preferences.getDefaultBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE));
        fallbackOutputDirectoryText.setText("");
        fallbackOutputDirectoryText.setMessage(preferences.getDefaultString(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY));
    }

    @Override
    public boolean performOk() {
        final DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();

        preferences.setValue(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE, reconnectToLastDatabaseButton.getSelection());
        preferences.setValue(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY, fallbackOutputDirectoryText.getText());

        PrefUtils.savePreferenceStore(preferences);

        return true;
    }
}
