/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
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
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        DBSObject selectedObject = NavigatorUtils.getSelectedObject((IStructuredSelection) selection);

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
                    Map<ToolGroupDescriptor, MenuManager> groupsMap = new HashMap<>();
                    for (ToolDescriptor tool : tools) {
                        hasTools = true;
                        MenuManager parentMenu = null;
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

    private static MenuManager getGroupMenu(List<IContributionItem> rootItems, Map<ToolGroupDescriptor, MenuManager> groupsMap, ToolGroupDescriptor group) {
        MenuManager item = groupsMap.get(group);
        if (item == null) {
            item = new MenuManager(group.getLabel(), null, group.getId());
            if (group.getParent() != null) {
                MenuManager parentMenu = getGroupMenu(rootItems, groupsMap, group.getParent());
                parentMenu.add(item);
            } else {
                rootItems.add(item);
            }
        }
        groupsMap.put(group, item);
        return item;
    }
}