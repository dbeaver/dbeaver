/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPTool;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceToolDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionExecuteTool;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DataSourceToolsMenuContributor extends ContributionItem
{
    static final Log log = LogFactory.getLog(DataSourceToolsMenuContributor.class);

    @Override
    public void fill(Menu menu, int index)
    {
        createMenu(menu);
    }
    
    private void createMenu(final Menu menu)
    {
        final DBSObject object = getCurrentObject();
        if (object.getDataSource() != null) {
            final IWorkbenchPart activePart = DBeaverCore.getActiveWorkbenchWindow().getActivePage().getActivePart();
            DriverDescriptor driver = (DriverDescriptor) object.getDataSource().getContainer().getDriver();
            List<DataSourceToolDescriptor> tools = driver.getProviderDescriptor().getTools(object);
            if (!CommonUtils.isEmpty(tools)) {
                for (final DataSourceToolDescriptor tool : tools) {
                    IAction action = ActionUtils.makeAction(
                        new NavigatorActionExecuteTool(activePart.getSite().getWorkbenchWindow(), tool),
                        activePart,
                        activePart.getSite().getSelectionProvider().getSelection(),
                        tool.getLabel(),
                        tool.getIcon() == null ? null : ImageDescriptor.createFromImage(tool.getIcon()),
                        tool.getDescription());
                    MenuItem item = new MenuItem(menu, SWT.NONE);
                    item.setText(tool.getLabel());
                    item.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e)
                        {
                            try {
                                DBPTool toolInstance = tool.createTool();
                                toolInstance.execute(activePart.getSite().getWorkbenchWindow(), object);
                            } catch (DBException e1) {
                                UIUtils.showErrorDialog(activePart.getSite().getShell(), "Tool error", "Can't execute tool '" + tool.getLabel() + "'", e1);
                            }
                        }
                    });
                }
            }
        }

    }

    private DBSObject getCurrentObject()
    {
        IWorkbenchPart activePart = DBeaverCore.getActiveWorkbenchWindow().getActivePage().getActivePart();
        return null;
    }

}