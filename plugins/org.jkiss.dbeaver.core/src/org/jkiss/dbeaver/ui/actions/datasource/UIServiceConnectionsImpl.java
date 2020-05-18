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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBServiceConnections;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;

/**
 * UIServiceConnectionsImpl
 */
public class UIServiceConnectionsImpl implements DBServiceConnections, UIServiceConnections {

    private static final Log log = Log.getLog(UIServiceConnectionsImpl.class);

    @Override
    public void openConnectionEditor(DBPDataSourceContainer dataSourceContainer) {
        EditConnectionDialog.openEditConnectionDialog(UIUtils.getActiveWorkbenchWindow(), dataSourceContainer);
    }

    @Override
    public void connectDataSource(DBPDataSourceContainer dataSourceContainer, DBRProgressListener onFinish) {
        DataSourceHandler.connectToDataSource(null, dataSourceContainer, onFinish);
    }

    @Override
    public void disconnectDataSource(DBPDataSourceContainer dataSourceContainer) {
        DataSourceHandler.disconnectDataSource(dataSourceContainer, null);
    }

    @Override
    public void closeActiveTransaction(DBRProgressMonitor monitor, DBCExecutionContext context, boolean commitTxn) {
        DataSourceHandler.closeActiveTransaction(monitor, context, commitTxn);
    }

    @Override
    public boolean checkAndCloseActiveTransaction(DBCExecutionContext[] contexts) {
        return DataSourceHandler.checkAndCloseActiveTransaction(contexts);
    }

    @Override
    public void initConnection(DBRProgressMonitor monitor, DBPDataSourceContainer dataSourceContainer, DBRProgressListener onFinish) {
        DataSourceHandler.connectToDataSource(monitor, dataSourceContainer, onFinish);
    }
}
