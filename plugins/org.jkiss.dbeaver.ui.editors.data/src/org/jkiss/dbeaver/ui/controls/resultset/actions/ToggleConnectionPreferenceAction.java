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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public abstract class ToggleConnectionPreferenceAction extends AbstractResultSetViewerAction {
    private final String prefId;

    ToggleConnectionPreferenceAction(ResultSetViewer resultSetViewer, String prefId, String title) {
        super(resultSetViewer, title);
        this.prefId = prefId;
    }

    @Override
    public int getStyle() {
        return AS_CHECK_BOX;
    }

    @Override
    public boolean isChecked() {
        return getActionPreferenceStore().getBoolean(prefId);
    }

    @Override
    public void run() {
        DBPPreferenceStore preferenceStore = getActionPreferenceStore();
        preferenceStore.setValue(
            prefId,
            !preferenceStore.getBoolean(prefId));
    }

    DBPPreferenceStore getActionPreferenceStore() {
        return getResultSetViewer().getPreferenceStore();
    }
}
