/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class DataSourceMenuContributor extends CompoundContributionItem
{
    static final Log log = LogFactory.getLog(DataSourceMenuContributor.class);
    private final static IContributionItem[] EMPTY_ITEMS = new IContributionItem[0];;

    @Override
    protected IContributionItem[] getContributionItems()
    {
        DBPDataSource dataSource = null;
        DBSObject selectedObject = null;
        IWorkbenchPart activePart = DBeaverCore.getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart == null) {
            return EMPTY_ITEMS;
        }
        DBSDataSourceContainer container = DataSourceHandler.getDataSourceContainer(activePart);
        if (container != null) {
            dataSource = container.getDataSource();
        }
        if (dataSource == null) {
            return EMPTY_ITEMS;
        }
        ISelection selection = activePart.getSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            selectedObject = RuntimeUtils.getObjectAdapter(element, DBSObject.class);
        }
        List<IContributionItem> menuItems = new ArrayList<IContributionItem>();
        fillContributionItems(menuItems, dataSource, selectedObject);
        return menuItems.isEmpty() ? EMPTY_ITEMS : menuItems.toArray(new IContributionItem[menuItems.size()]);
    }

    protected abstract void fillContributionItems(
        List<IContributionItem> menuItems,
        DBPDataSource dataSource,
        DBSObject selectedObject);

/*
    @Override
    public void fill(Menu menu, int index)
    {
        DBPDataSource dataSource = null;
        DBSObject selectedObject = null;
        IWorkbenchPart activePart = DBeaverCore.getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart == null) {
            return;
        }
        DBSDataSourceContainer container = DataSourceHandler.getDataSourceContainer(activePart);
        if (container != null) {
            dataSource = container.getDataSource();
        }
        if (dataSource == null) {
            return;
        }
        ISelection selection = activePart.getSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            selectedObject = RuntimeUtils.getObjectAdapter(element, DBSObject.class);
        }
        createMenu(menu, dataSource, selectedObject);
    }
    
    protected abstract void createMenu(final Menu menu, final DBPDataSource dataSource, final DBSObject selectedObject);
*/

}