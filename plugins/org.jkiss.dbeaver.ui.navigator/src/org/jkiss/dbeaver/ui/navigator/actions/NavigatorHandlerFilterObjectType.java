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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilterObjectType;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NavigatorHandlerFilterObjectType extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(NavigatorHandlerFilterObjectType.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DatabaseNavigatorTree navigatorTree = NavigatorUtils.getNavigatorTree(event);
        if (navigatorTree != null) {
            DatabaseNavigatorTreeFilterObjectType objectType = CommonUtils.valueOf(
                DatabaseNavigatorTreeFilterObjectType.class,
                event.getParameter("type")
            );

            if (objectType == null && navigatorTree.getNavigatorFilter() instanceof DatabaseNavigatorTreeFilter navigatorFilter) {
                // Cycle through all object types starting from the active one
                var typesList = navigatorFilter.getSupportedObjectTypes().stream()
                    .sorted()
                    .toList();
                if (typesList.isEmpty()) {
                    return null;
                }
                var selection = navigatorTree.getFilterObjectType();
                objectType = typesList.get((typesList.indexOf(selection) + 1) % typesList.size());
            } else if (objectType == null) {
                var types = List.of(DatabaseNavigatorTreeFilterObjectType.values());
                var selection = navigatorTree.getFilterObjectType();
                objectType = types.get((types.indexOf(selection) + 1) % types.size());
            }

            if (objectType == navigatorTree.getFilterObjectType()) {
                return null;
            }

            navigatorTree.setFilterObjectType(objectType);
            navigatorTree.getViewer().getControl().setRedraw(false);
            try {
                navigatorTree.getViewer().refresh();
            } finally {
                navigatorTree.getViewer().getControl().setRedraw(true);
            }
            ActionUtils.fireCommandRefresh(NavigatorCommands.CMD_FILTER_OBJECT_TYPE);
        } else {
            log.debug("Can't find active navigator tree");
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        DatabaseNavigatorTree navigatorTree = NavigatorUtils.getNavigatorTree(element.getServiceLocator());
        DatabaseNavigatorTreeFilterObjectType curObjectType = DatabaseNavigatorTreeFilterObjectType.connection;
        if (navigatorTree != null) {
            curObjectType = navigatorTree.getFilterObjectType();
        }
        DatabaseNavigatorTreeFilterObjectType objectType = CommonUtils.valueOf(
            DatabaseNavigatorTreeFilterObjectType.class,
            CommonUtils.toString(parameters.get("type"))
        );
        if (objectType == null) {
            element.setTooltip(NLS.bind("{0} (click to cycle through)", curObjectType.getDescription()));
            element.setIcon(DBeaverIcons.getImageDescriptor(curObjectType.getIcon()));
        } else {
            element.setText(objectType.getName());
            element.setTooltip(objectType.getDescription());
            element.setChecked(objectType == curObjectType);
            element.setIcon(null);
        }
    }

    public static class MenuContributor extends CompoundContributionItem {
        @Override
        protected IContributionItem[] getContributionItems() {
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            DatabaseNavigatorTree navigatorTree = NavigatorUtils.getNavigatorTree(workbenchWindow);
            if (navigatorTree == null || !(navigatorTree.getNavigatorFilter() instanceof DatabaseNavigatorTreeFilter navigatorFilter)) {
                return new IContributionItem[0];
            }

            List<IContributionItem> menuItems = new ArrayList<>();
            var typesList = navigatorFilter.getSupportedObjectTypes().stream()
                .sorted()
                .toList();

            for (DatabaseNavigatorTreeFilterObjectType objectType : typesList) {
                menuItems.add(ActionUtils.makeCommandContribution(
                    workbenchWindow,
                    NavigatorCommands.CMD_FILTER_OBJECT_TYPE,
                    CommandContributionItem.STYLE_RADIO,
                    objectType.getName(),
                    objectType.getIcon(),
                    objectType.getDescription(),
                    false,
                    Collections.singletonMap("type", objectType.name())
                ));
            }
            return menuItems.toArray(new IContributionItem[0]);
        }
    }
}
