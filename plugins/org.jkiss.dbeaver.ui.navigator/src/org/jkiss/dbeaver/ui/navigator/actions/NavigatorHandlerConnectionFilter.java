/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

import java.util.Map;

public class NavigatorHandlerConnectionFilter extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DatabaseNavigatorTree navigatorTree = NavigatorUtils.getNavigatorTree(event);
        if (navigatorTree != null) {
            navigatorTree.setFilterShowConnected(!navigatorTree.isFilterShowConnected());
        }
        // No need to fire global command refresh,
        // because DatabaseNavigatorTree::setFilterShowConnected(..) already updated local tool item state,
        // and we don't need to trigger global command state refresh, because this command doesn't have global state.
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {

        //
        // DO NOT update element's state here, because command's associated flag state is unique for each navigator tree instance,
        //        while IElementUpdater intended for all the command contributions to reflect ony one shared state!
        //        element.getServiceLocator() doesn't help, because a few tree instances might belong to one service context,
        //        like dialog and/or tool windows for example.
        //
        // Associated tool item state should always be updated explicitly only for the containing tree instance!
        //     see DatabaseNavigatorTree::setFilterShowConnected(..)
        //         DatabaseNavigatorTree.CustomFilteredTree::updateFilterConnectedConnectionsToolItem(..)
        //
        // BUT the workbench still wants to update commands' state on its own sometimes (at least after the contribution initialization),
        //     which resets explicit adjustments, so aggregate all these update notifications with job and apply desired state contextfully
        //
        // TODO consider certain infrastructure for such contextful toggle commands because we have others like this
        //     (see multi/single-tabbed resultsets for example)

        DatabaseNavigatorTree.updateFilterCommandsState();
    }

}