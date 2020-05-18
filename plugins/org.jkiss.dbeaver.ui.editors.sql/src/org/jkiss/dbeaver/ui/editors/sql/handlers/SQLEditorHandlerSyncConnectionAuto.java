/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;

import java.io.IOException;
import java.util.Map;

public class SQLEditorHandlerSyncConnectionAuto extends AbstractHandler implements IElementUpdater {

    public SQLEditorHandlerSyncConnectionAuto() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final DBPPreferenceStore prefs = DBWorkbench.getPlatform().getPreferenceStore();
        prefs.setValue(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE,
            !prefs.getBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));

        try {
            prefs.save();
        } catch (IOException e) {
            throw new ExecutionException("Error saving configuration", e);
        }

        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setChecked(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));
    }
}
