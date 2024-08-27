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

package org.jkiss.dbeaver.ui.navigator.actions.node;

import org.eclipse.swt.widgets.Event;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorNodeActionHandlerAbstract;

/**
 * Configuration action handler
 */
public class NNAHDataSourceConfiguration extends NavigatorNodeActionHandlerAbstract {

    @Override
    public boolean isEnabledFor(INavigatorModelView view, DBNNode node) {
        return false;
    }

    @Override
    public boolean isSticky(INavigatorModelView view, DBNNode node) {
        return false;
    }

    @Override
    public DBPImage getNodeActionIcon(INavigatorModelView view, DBNNode node) {
        return DBIcon.OVER_LAMP;
    }

    @Override
    public String getNodeActionToolTip(INavigatorModelView view, DBNNode node) {
        return node.getName() + " configuration";
    }

    @Override
    public void handleNodeAction(INavigatorModelView view, DBNNode node, Event event, boolean defaultAction) {

    }
}
