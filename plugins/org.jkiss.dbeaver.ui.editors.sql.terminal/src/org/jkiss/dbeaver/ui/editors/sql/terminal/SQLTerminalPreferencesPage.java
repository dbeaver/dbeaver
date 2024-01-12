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
package org.jkiss.dbeaver.ui.editors.sql.terminal;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.terminal.internal.SQLTerminalMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

public class SQLTerminalPreferencesPage extends TargetPrefPage {
    private static final Log log = Log.getLog(SQLTerminalPreferencesPage.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor.terminalview"; //$NON-NLS-1$

    private Button showTerminalViewByDefault;
    private Button showQueryText;
    private Button showServerOutput;

    @Override
    protected boolean hasDataSourceSpecificOptions(@NotNull DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return store.contains(SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT)
            || store.contains(SQLTerminalPreferencesConstants.SHOW_QUERY_TEXT)
            || store.contains(SQLTerminalPreferencesConstants.SHOW_SERVER_OUTPUT);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);
        
        Composite commonGroup = UIUtils.createControlGroup(
            composite,
            SQLEditorMessages.pref_page_sql_editor_group_common,
            2,
            GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
            0
        );
        showTerminalViewByDefault = UIUtils.createCheckbox(
            commonGroup,
            SQLTerminalMessages.pref_page_sql_temrinal_show_output_console_view_label,
            SQLTerminalMessages.pref_page_sql_temrinal_show_output_console_view_tip,
            false,
            2
        );
        showQueryText = UIUtils.createCheckbox(
            commonGroup,
            SQLTerminalMessages.pref_page_sql_temrinal_show_query_text_label,
            null,
            false,
            2
        );
        showServerOutput = UIUtils.createCheckbox(
            commonGroup,
            SQLTerminalMessages.pref_page_sql_temrinal_show_server_output_label,
            null,
            false,
            2
        );

        return composite;
    }

    @Override
    protected void loadPreferences(@NotNull DBPPreferenceStore store) {
        try {
            showTerminalViewByDefault.setSelection(store.getBoolean(SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT));
            showQueryText.setSelection(store.getBoolean(SQLTerminalPreferencesConstants.SHOW_QUERY_TEXT));
            showServerOutput.setSelection(store.getBoolean(SQLTerminalPreferencesConstants.SHOW_SERVER_OUTPUT));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(@NotNull DBPPreferenceStore store) {
        try {
            store.setValue(SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT, showTerminalViewByDefault.getSelection());
            store.setValue(SQLTerminalPreferencesConstants.SHOW_QUERY_TEXT, showQueryText.getSelection());
            store.setValue(SQLTerminalPreferencesConstants.SHOW_SERVER_OUTPUT, showServerOutput.getSelection());

        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(@NotNull DBPPreferenceStore store) {
        store.setToDefault(SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT);
        store.setToDefault(SQLTerminalPreferencesConstants.SHOW_QUERY_TEXT);
        store.setToDefault(SQLTerminalPreferencesConstants.SHOW_SERVER_OUTPUT);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        showTerminalViewByDefault.setSelection(store.getDefaultBoolean(SQLTerminalPreferencesConstants.SHOW_TERMINAL_VIEW_BY_DEFAULT));
        showQueryText.setSelection(store.getDefaultBoolean(SQLTerminalPreferencesConstants.SHOW_QUERY_TEXT));
        showServerOutput.setSelection(store.getDefaultBoolean(SQLTerminalPreferencesConstants.SHOW_SERVER_OUTPUT));
        super.performDefaults();
    }

    @NotNull
    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

}