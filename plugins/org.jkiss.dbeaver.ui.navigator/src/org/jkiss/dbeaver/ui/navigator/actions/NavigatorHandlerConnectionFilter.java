/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;

import java.util.Map;

public class NavigatorHandlerConnectionFilter extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof DatabaseNavigatorView) {
            DatabaseNavigatorTree navigatorTree = ((DatabaseNavigatorView) activePart).getNavigatorTree();
            navigatorTree.setFilterShowConnected(!navigatorTree.isFilterShowConnected());
            navigatorTree.getViewer().getControl().setRedraw(false);
            try {
                navigatorTree.getViewer().refresh();
            } finally {
                navigatorTree.getViewer().getControl().setRedraw(true);
            }
        }
        ActionUtils.fireCommandRefresh(NavigatorCommands.CMD_FILTER_CONNECTIONS);
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
        if (partSite != null && partSite.getPart() instanceof DatabaseNavigatorView) {
            DatabaseNavigatorTree navigatorTree = ((DatabaseNavigatorView) partSite.getPart()).getNavigatorTree();
            element.setIcon(DBeaverIcons.getImageDescriptor(
                navigatorTree.isFilterShowConnected() ? UIIcon.FILTER_RESET : UIIcon.FILTER_APPLY
            ));
            String actionName = navigatorTree.isFilterShowConnected() ? "Show all" : "Show connected only";
            element.setText(actionName);
            element.setTooltip(actionName);
        }

    }

}