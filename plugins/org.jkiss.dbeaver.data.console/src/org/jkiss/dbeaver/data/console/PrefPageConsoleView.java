/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.data.console;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLExecute
 */
public class PrefPageConsoleView extends TargetPrefPage {
    private static final Log log = Log.getLog(PrefPageConsoleView.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqleditor.consoleview"; //$NON-NLS-1$

    private Button showConsoleViewByDefault;

    @Override
    protected boolean hasDataSourceSpecificOptions(@NotNull DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return store.contains(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Composite commonGroup = UIUtils.createControlGroup(
                composite,
                SQLEditorMessages.pref_page_sql_editor_group_common,
                2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );
            showConsoleViewByDefault = UIUtils.createCheckbox(
                commonGroup,
                ConsoleMessages.pref_page_console_view_label_show_output_console_view,
                ConsoleMessages.pref_page_console_view_label_show_output_console_view_tip,
                false,
                2
            );
        }

        return composite;
    }

    @Override
    protected void loadPreferences(@NotNull DBPPreferenceStore store) {
        try {
            showConsoleViewByDefault.setSelection(store.getBoolean(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(@NotNull DBPPreferenceStore store) {
        try {
            store.setValue(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT, showConsoleViewByDefault.getSelection());

        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(@NotNull DBPPreferenceStore store) {
        store.setToDefault(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT);
    }

    @NotNull
    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

}