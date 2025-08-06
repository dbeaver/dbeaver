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

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class ToggleSelectionStatAction extends ToggleConnectionPreferenceAction {
    public ToggleSelectionStatAction(ResultSetViewer resultSetViewer, String prefId, String title) {
        super(resultSetViewer, prefId, title);
    }

    @Override
    public void run() {
        super.run();
        getResultSetViewer().fireResultSetSelectionChange(
            new SelectionChangedEvent(getResultSetViewer(), getResultSetViewer().getSelection()));
    }

    @Override
    DBPPreferenceStore getActionPreferenceStore() {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }
}
