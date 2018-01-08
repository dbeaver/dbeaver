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

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tools.ToolDescriptor;
import org.jkiss.dbeaver.registry.tools.ToolGroupDescriptor;
import org.jkiss.dbeaver.registry.tools.ToolsRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.actions.common.EmptyListAction;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionExecuteTool;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceToolsContributor extends DataSourceMenuContributor
{


    @Override
    protected void fillContributionItems(List<IContributionItem> menuItems)
    {
        IWorkbenchPart activePart = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart == null) {
            return;
        }
        final ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return;
        }
        ISelection selection = selectionProvider.getSelection();
        DBSObject selectedObject = NavigatorUtils.getSelectedObject(selection);

        if (selectedObject != null) {
            List<ToolDescriptor> tools = ToolsRegistry.getInstance().getTools((IStructuredSelection) selection);
            fillToolsMenu(menuItems, tools, selection);
        }
    }

    private static void fillToolsMenu(List<IContributionItem> menuItems, List<ToolDescriptor> tools, ISelection selection)
    {
        boolean hasTools = false;
        if (!CommonUtils.isEmpty(tools)) {
            IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
            if (workbenchWindow.getActivePage() != null) {
                IWorkbenchPart activePart = workbenchWindow.getActivePage().getActivePart();
                if (activePart != null) {
                    Map<ToolGroupDescriptor, IMenuManager> groupsMap = new HashMap<>();
                    for (ToolDescriptor tool : tools) {
                        hasTools = true;
                        IMenuManager parentMenu = null;
                        if (tool.getGroup() != null) {
                            parentMenu = getGroupMenu(menuItems, groupsMap, tool.getGroup());
                        }
                        IAction action = ActionUtils.makeAction(
                            new NavigatorActionExecuteTool(workbenchWindow, tool),
                            activePart.getSite(),
                            selection,
                            tool.getLabel(),
                            tool.getIcon() == null ? null : DBeaverIcons.getImageDescriptor(tool.getIcon()),
                            tool.getDescription());
                        if (parentMenu == null) {
                            menuItems.add(new ActionContributionItem(action));
                        } else {
                            parentMenu.add(new ActionContributionItem(action));
                        }
                    }
                }
            }
        }
        if (!hasTools) {
            menuItems.add(new ActionContributionItem(new EmptyListAction()));
        }
    }

    private static IMenuManager getGroupMenu(List<IContributionItem> rootItems, Map<ToolGroupDescriptor, IMenuManager> groupsMap, ToolGroupDescriptor group) {
        IMenuManager item = groupsMap.get(group);
        if (item == null) {
            item = new MenuManager(group.getLabel(), null, group.getId());
            if (group.getParent() != null) {
                IMenuManager parentMenu = getGroupMenu(rootItems, groupsMap, group.getParent());
                parentMenu.add(item);
            } else {
                rootItems.add(item);
            }
        }
        groupsMap.put(group, item);
        return item;
    }
}