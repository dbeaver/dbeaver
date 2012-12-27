/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.actions.common.EmptyListAction;

import java.util.ArrayList;
import java.util.List;

public abstract class DataSourceMenuContributor extends CompoundContributionItem
{
    static final Log log = LogFactory.getLog(DataSourceMenuContributor.class);

    @Override
    protected IContributionItem[] getContributionItems()
    {
        DBSDataSourceContainer container = null;
        DBPDataSource dataSource = null;
        DBSObject selectedObject = null;
        IWorkbenchPart activePart = null;
        IWorkbenchPage activePage = DBeaverUI.getActiveWorkbenchWindow().getActivePage();
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor != null) {
            activePart = activeEditor;
            container = DataSourceHandler.getDataSourceContainer(activeEditor);
        }
        if (container == null) {
            activePart = activePage.getActivePart();
            if (activePart == null) {
                return makeEmptyList();
            }
            container = DataSourceHandler.getDataSourceContainer(activePart);
        }

        if (container != null) {
            dataSource = container.getDataSource();
        }
        if (dataSource == null) {
            return makeEmptyList();
        }
        ISelection selection = activePart.getSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            selectedObject = RuntimeUtils.getObjectAdapter(element, DBSObject.class);
        }
        List<IContributionItem> menuItems = new ArrayList<IContributionItem>();
        fillContributionItems(menuItems, dataSource, selectedObject);
        return menuItems.isEmpty() ? makeEmptyList() : menuItems.toArray(new IContributionItem[menuItems.size()]);
    }

    protected abstract void fillContributionItems(
        List<IContributionItem> menuItems,
        DBPDataSource dataSource,
        DBSObject selectedObject);

    private static IContributionItem[] makeEmptyList(){
        return new IContributionItem[] { new ActionContributionItem(new EmptyListAction())};
    }

}