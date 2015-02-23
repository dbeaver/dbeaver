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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditObjectFilterDialog;

import java.util.Collections;
import java.util.Map;

public class NavigatorHandlerConfigureFilter extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDatabaseFolder) {
            configureFilters(HandlerUtil.getActiveShell(event), node);
        }
        return null;
    }

    public static void configureFilters(Shell shell, DBNNode node)
    {
        final DBNDatabaseFolder folder = (DBNDatabaseFolder) node;
        DBXTreeItem itemsMeta = folder.getItemsMeta();
        if (itemsMeta != null) {
            DBSObjectFilter objectFilter = folder.getNodeFilter(itemsMeta, true);
            if (objectFilter == null) {
                objectFilter = new DBSObjectFilter();
            }
            boolean globalFilter = folder.getValueObject() instanceof DBPDataSource;
            String parentName = "?";
            if (folder.getValueObject() instanceof DBSObject) {
                parentName = ((DBSObject) folder.getValueObject()).getName();
            }
            EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                shell,
                globalFilter ? "All " + node.getNodeType() : node.getNodeType() + " of " + parentName,
                objectFilter,
                globalFilter);
            switch (dialog.open()) {
                case IDialogConstants.OK_ID:
                    folder.setNodeFilter(itemsMeta, dialog.getFilter());
                    NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(folder));
                    break;
                case EditObjectFilterDialog.SHOW_GLOBAL_FILTERS_ID:
                    objectFilter = folder.getDataSource().getContainer().getObjectFilter(folder.getChildrenClass(), null);
                    dialog = new EditObjectFilterDialog(
                        shell,
                        "All " + node.getNodeType(),
                        objectFilter != null  ?objectFilter : new DBSObjectFilter(),
                        true);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        folder.setNodeFilter(itemsMeta, dialog.getFilter());
                        NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(folder));
                    }
                    break;
            }
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        DBNNode node = NavigatorUtils.getSelectedNode(element);
        if (node != null) {
            element.setText("Filter " + node.getNodeType() + " ...");
        }
    }

}