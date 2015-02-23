/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.ToolDescriptor;
import org.jkiss.dbeaver.registry.ToolGroupDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.actions.common.EmptyListAction;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionExecuteTool;
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

        ISelection selection = activePart.getSite().getSelectionProvider().getSelection();
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        DBSObject selectedObject = NavigatorUtils.getSelectedObject((IStructuredSelection) selection);

        if (selectedObject != null && selectedObject.getDataSource() != null) {
            DriverDescriptor driver = (DriverDescriptor) selectedObject.getDataSource().getContainer().getDriver();
            List<ToolDescriptor> tools = driver.getProviderDescriptor().getTools((IStructuredSelection) selection);
            fillToolsMenu(menuItems, tools, selection);
        }
    }

    private static void fillToolsMenu(List<IContributionItem> menuItems, List<ToolDescriptor> tools, ISelection selection)
    {
        boolean hasTools = false;
        if (!CommonUtils.isEmpty(tools)) {
            IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
            if (workbenchWindow != null && workbenchWindow.getActivePage() != null) {
                IWorkbenchPart activePart = workbenchWindow.getActivePage().getActivePart();
                if (activePart != null) {
                    Map<ToolGroupDescriptor, MenuManager> groupsMap = new HashMap<ToolGroupDescriptor, MenuManager>();
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
                            tool.getIcon() == null ? null : ImageDescriptor.createFromImage(tool.getIcon()),
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