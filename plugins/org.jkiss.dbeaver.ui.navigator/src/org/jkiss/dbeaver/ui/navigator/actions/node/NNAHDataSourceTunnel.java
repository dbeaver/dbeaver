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

package org.jkiss.dbeaver.ui.navigator.actions.node;

import org.eclipse.swt.widgets.Event;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorNodeActionHandlerAbstract;
import org.jkiss.utils.CommonUtils;

/**
 * Tunnel action handler
 */
public class NNAHDataSourceTunnel extends NavigatorNodeActionHandlerAbstract {

    @Override
    public boolean isEnabledFor(INavigatorModelView view, DBNNode node) {
        if (node instanceof DBNDataSource) {
            return ((DBNDataSource) node).hasNetworkHandlers();
        }
        return false;
    }

    @Override
    public DBPImage getNodeActionIcon(INavigatorModelView view, DBNNode node) {
        return UIIcon.BUTTON_TUNNEL;
    }

    @Override
    public String getNodeActionToolTip(INavigatorModelView view, DBNNode node) {
        StringBuilder tip = new StringBuilder("Network handlers enabled:");
        for (DBWHandlerConfiguration handler : ((DBNDataSource)node).getDataSourceContainer().getConnectionConfiguration().getHandlers()) {
            if (handler.isEnabled()) {
                tip.append("\n  -").append(handler.getHandlerDescriptor().getLabel());
                String hostName = handler.getStringProperty(DBWHandlerConfiguration.PROP_HOST);
                if (!CommonUtils.isEmpty(hostName)) {
                    tip.append(": ").append(hostName);
                }
            }
        }
        return tip.toString();
    }

    @Override
    public void handleNodeAction(INavigatorModelView view, DBNNode node, Event event, boolean defaultAction) {
        if (node instanceof DBNDatabaseNode) {
            DBPDataSourceContainer dataSourceContainer = ((DBNDatabaseNode) node).getDataSourceContainer();
            UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
            if (serviceConnections != null) {
                serviceConnections.openConnectionEditor(dataSourceContainer, "ConnectionPageNetworkHandler.ssh_tunnel");
            }
        }
    }

}
