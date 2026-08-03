/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.navigator.actions.node;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorNodeActionHandlerAbstract;

/**
 * Read-only action handler
 */
public class NNAHDataSourceReadOnly extends NavigatorNodeActionHandlerAbstract {

    @Override
    public boolean isEnabledFor(@NotNull DBNNode node) {
        if (node instanceof DBNDataSource dbnDataSource) {
            return dbnDataSource.getDataSourceContainer().isConnectionReadOnly();
        }
        return false;
    }

    @Override
    @Nullable
    public DBPImage getNodeActionIcon(@NotNull DBNNode node) {
        return UIIcon.BUTTON_READ_ONLY;
    }

    @Override
    @Nullable
    public String getNodeActionToolTip(@NotNull DBNNode node) {
        return "Connection is read-only.\nYou cannot change data or database structure.";
    }

    @Override
    public void handleNodeAction(@NotNull DBNNode node, boolean defaultAction) {
        if (node instanceof DBNDatabaseNode dbNode) {
            DBPDataSourceContainer dataSourceContainer = dbNode.getDataSourceContainer();
            UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
            if (serviceConnections != null) {
                serviceConnections.openConnectionEditor(dataSourceContainer, "ConnectionPageGeneral");
            }
        }
    }

}
