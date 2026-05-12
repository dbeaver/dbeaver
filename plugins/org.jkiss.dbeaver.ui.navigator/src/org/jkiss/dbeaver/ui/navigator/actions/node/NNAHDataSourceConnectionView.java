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

import org.eclipse.swt.widgets.Event;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorNodeActionHandlerAbstract;

/**
 * Connection view handler
 */
public class NNAHDataSourceConnectionView extends NavigatorNodeActionHandlerAbstract {

    @Override
    public boolean isEnabledFor(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        if (node instanceof DBNDataSource dbnDataSource) {
            DBNBrowseSettings chosenSettings = dbnDataSource.getDataSourceContainer().getNavigatorSettings();
            return !DataSourceNavigatorSettings.getProductDefaultSettings().equals(chosenSettings);
        }
        return false;
    }

    @Override
    @Nullable
    public DBPImage getNodeActionIcon(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        if (node instanceof DBNDataSource dbnDataSource) {
            DBNBrowseSettings chosenSettings = dbnDataSource.getDataSourceContainer().getNavigatorSettings();
            if (DataSourceNavigatorSettings.PRESET_SIMPLE.getSettings().equals(chosenSettings)) {
                return UIIcon.SIMPLE_MODE;
            } else if (DataSourceNavigatorSettings.PRESET_ADVANCED.getSettings().equals(chosenSettings)) {
                return UIIcon.ADVANCED_MODE;
            } else {
                return UIIcon.CUSTOM_MODE;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public String getNodeActionToolTip(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        if (node instanceof DBNDataSource dbnDataSource) {
            DBNBrowseSettings chosenSettings = dbnDataSource.getDataSourceContainer().getNavigatorSettings();
            if (DataSourceNavigatorSettings.PRESET_SIMPLE.getSettings().equals(chosenSettings)) {
                return UINavigatorMessages.navigator_node_action_connection_view_simple_tooltip;
            } else if (DataSourceNavigatorSettings.PRESET_ADVANCED.getSettings().equals(chosenSettings)) {
                return UINavigatorMessages.navigator_node_action_connection_view_advanced_tooltip;
            } else {
                return UINavigatorMessages.navigator_node_action_connection_view_custom_tooltip;
            }
        }
        return null;
    }

    @Override
    public void handleNodeAction(@NotNull INavigatorModelView view, @NotNull DBNNode node, @NotNull Event event, boolean defaultAction) {
        if (node instanceof DBNDatabaseNode  dbnDatabaseNode) {
            DBPDataSourceContainer dataSourceContainer = dbnDatabaseNode.getDataSourceContainer();
            UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
            if (serviceConnections != null) {
                serviceConnections.openConnectionEditor(dataSourceContainer, "ConnectionPageGeneral");
            }
        }
    }
}
