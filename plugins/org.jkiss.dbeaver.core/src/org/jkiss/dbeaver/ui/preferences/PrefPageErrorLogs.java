/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

/**
 * PrefPageErrorHandle
 */
public class PrefPageErrorLogs extends AbstractPrefPage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.errorLogs"; //$NON-NLS-1$

    private Button logsDebugEnabled;
    private TextWithOpenFile logsDebugLocation;

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            // Logs
            Group groupLogs = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_debug_logs, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);

            logsDebugEnabled = UIUtils.createCheckbox(groupLogs,
                CoreMessages.pref_page_ui_general_label_enable_debug_logs,
                CoreMessages.pref_page_ui_general_label_enable_debug_logs_tip, false, 2);
            UIUtils.createControlLabel(groupLogs, CoreMessages.pref_page_ui_general_label_log_file_location);
            logsDebugLocation = new TextWithOpenFile(groupLogs, CoreMessages.pref_page_ui_general_label_open_file_text, new String[] { "*.log", "*.txt" } );
            ContentAssistUtils.installContentProposal(
                logsDebugLocation.getTextControl(),
                new SmartTextContentAdapter(),
                new StringContentProposalProvider(
                    GeneralUtils.variablePattern(SystemVariablesResolver.VAR_WORKSPACE),
                    GeneralUtils.variablePattern(SystemVariablesResolver.VAR_HOME)));
            logsDebugLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label tipLabel = UIUtils.createLabel(groupLogs, CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart);
            tipLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false , 2, 1));
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        logsDebugEnabled.setSelection(store.getBoolean(DBeaverPreferences.LOGS_DEBUG_ENABLED));
        logsDebugLocation.setText(store.getString(DBeaverPreferences.LOGS_DEBUG_LOCATION));

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(DBeaverPreferences.LOGS_DEBUG_ENABLED, logsDebugEnabled.getSelection());
        store.setValue(DBeaverPreferences.LOGS_DEBUG_LOCATION, logsDebugLocation.getText());

        PrefUtils.savePreferenceStore(store);

        return super.performOk();
    }

}