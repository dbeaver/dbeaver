/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.io.IOException;
import java.util.Map;

public class SQLEditorHandlerSeparateConnectionOption extends AbstractDataSourceHandler implements IElementUpdater {

    public SQLEditorHandlerSeparateConnectionOption()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final DBPPreferenceStore prefs = getPreferenceStore(event);
        prefs.setValue(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION,
            !prefs.getBoolean(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION));
        try {
            prefs.save();
        } catch (IOException e) {
            throw new ExecutionException("Error saving configuration", e);
        }

        return null;
    }

    @NotNull
    private DBPPreferenceStore getPreferenceStore(ExecutionEvent event) {
        DBPDataSourceContainer dsContainer = getActiveDataSourceContainer(event, false);
        return dsContainer == null ? DBWorkbench.getPlatform().getPreferenceStore() : dsContainer.getPreferenceStore();
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setText(SQLEditorMessages.pref_page_sql_editor_label_separate_connection_each_editor);
        element.setTooltip(SQLEditorMessages.pref_page_sql_editor_label_separate_connection_each_editor);

        IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        DBPDataSourceContainer dsContainer = activeEditor == null ? null : getDataSourceContainerFromPart(activeEditor);
        DBPPreferenceStore prefStore = dsContainer == null ? DBWorkbench.getPlatform().getPreferenceStore() : dsContainer.getPreferenceStore();

        element.setChecked(prefStore.getBoolean(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION));
    }

}
