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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

public class ColorizeDataTypesToggleAction extends AbstractResultSetViewerAction {
    public ColorizeDataTypesToggleAction(ResultSetViewer resultSetViewer) {
        super(resultSetViewer, ResultSetMessages.actions_name_colorize_data_types, AS_CHECK_BOX);
        setToolTipText("Set different foreground color for data types");
    }

    @Override
    public boolean isChecked() {
        DBPDataSource dataSource = getResultSetViewer().getDataContainer().getDataSource();
        return dataSource != null &&
               dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
    }

    @Override
    public void run() {
        DBPDataSource dataSource = getResultSetViewer().getDataContainer().getDataSource();
        if (dataSource == null) {
            return;
        }
        DBPPreferenceStore dsStore = dataSource.getContainer().getPreferenceStore();
        boolean curValue = dsStore.getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
        // Set local setting to default
        dsStore.setValue(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES, !curValue);
        dataSource.getContainer().persistConfiguration();
        getResultSetViewer().refreshData(null);
    }

}
