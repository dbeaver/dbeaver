/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionWizard;

/**
 * UIServiceConnectionsImpl
 */
public class UIServiceConnectionsImpl implements UIServiceConnections {

    private static final Log log = Log.getLog(UIServiceConnectionsImpl.class);

    @Override
    public void openConnectionEditor(DBPDataSourceContainer dataSourceContainer) {
        EditConnectionDialog dialog = new EditConnectionDialog(
            UIUtils.getActiveWorkbenchWindow(),
            new EditConnectionWizard((DataSourceDescriptor) dataSourceContainer));
        dialog.open();
    }

    @Override
    public void conectDataSource(DBPDataSourceContainer dataSourceContainer) {
        DataSourceHandler.connectToDataSource(null, dataSourceContainer, null);
    }

    @Override
    public void disconectDataSource(DBPDataSourceContainer dataSourceContainer) {
        DataSourceHandler.disconnectDataSource(dataSourceContainer, null);
    }
}
