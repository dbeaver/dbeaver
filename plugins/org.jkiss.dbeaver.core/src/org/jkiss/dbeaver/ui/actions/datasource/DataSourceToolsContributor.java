/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceToolDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.actions.common.EmptyListAction;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionExecuteTool;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceToolsContributor extends DataSourceMenuContributor
{


    @Override
    protected void fillContributionItems(List<IContributionItem> menuItems, DBPDataSource dataSource, DBSObject selectedObject)
    {
        if (selectedObject == null) {
            selectedObject = dataSource.getContainer();
        }
        if (selectedObject.getDataSource() != null) {
            DriverDescriptor driver = (DriverDescriptor) selectedObject.getDataSource().getContainer().getDriver();
            List<DataSourceToolDescriptor> tools = driver.getProviderDescriptor().getTools(selectedObject);
            fillToolsMenu(menuItems, tools, new StructuredSelection(selectedObject));
        }
    }

    public static void fillToolsMenu(List<IContributionItem> menuItems, List<DataSourceToolDescriptor> tools, ISelection selection)
    {
        boolean hasTools = false;
        if (!CommonUtils.isEmpty(tools)) {
            IWorkbenchWindow workbenchWindow = DBeaverCore.getActiveWorkbenchWindow();
            if (workbenchWindow != null && workbenchWindow.getActivePage() != null) {
                IWorkbenchPart activePart = workbenchWindow.getActivePage().getActivePart();
                if (activePart != null) {
                    hasTools = true;
                    for (DataSourceToolDescriptor tool : tools) {
                        IAction action = ActionUtils.makeAction(
                            new NavigatorActionExecuteTool(workbenchWindow, tool),
                            activePart,
                            selection,
                            tool.getLabel(),
                            tool.getIcon() == null ? null : ImageDescriptor.createFromImage(tool.getIcon()),
                            tool.getDescription());
                        menuItems.add(new ActionContributionItem(action));
                    }
                }
            }
        }
        if (!hasTools) {
            menuItems.add(new ActionContributionItem(new EmptyListAction()));
        }
    }
}