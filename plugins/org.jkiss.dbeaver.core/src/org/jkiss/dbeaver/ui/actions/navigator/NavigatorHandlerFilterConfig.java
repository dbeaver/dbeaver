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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.dialogs.connection.EditObjectFilterDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Collections;
import java.util.Map;

public class NavigatorHandlerFilterConfig extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDatabaseItem) {
            node = node.getParentNode();
        }
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
            final DBPDataSourceRegistry dsRegistry = DBeaverCore.getInstance().getProjectManager().getDataSourceRegistry(folder.getOwnerProject());
            final boolean globalFilter = folder.getValueObject() instanceof DBPDataSource;
            String parentName = "?";
            if (folder.getValueObject() instanceof DBSObject) {
                parentName = ((DBSObject) folder.getValueObject()).getName();
            }
            EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                shell,
                dsRegistry,
                globalFilter ? "All " + node.getNodeType() : node.getNodeType() + " of " + parentName,
                objectFilter,
                globalFilter);
            switch (dialog.open()) {
                case IDialogConstants.OK_ID:
                    folder.setNodeFilter(itemsMeta, dialog.getFilter());
                    NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(folder));
                    break;
                case EditObjectFilterDialog.SHOW_GLOBAL_FILTERS_ID:
                    objectFilter = folder.getDataSource().getContainer().getObjectFilter(folder.getChildrenClass(), null, false);
                    dialog = new EditObjectFilterDialog(
                        shell,
                            dsRegistry, "All " + node.getNodeType(),
                        objectFilter != null  ?objectFilter : new DBSObjectFilter(),
                        true);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        // Set global filter
                        folder.getDataSource().getContainer().setObjectFilter(folder.getChildrenClass(), null, dialog.getFilter());
                        folder.getDataSource().getContainer().persistConfiguration();
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
        if (node instanceof DBNDatabaseItem) {
            node = node.getParentNode();
        }
        if (node != null) {
            element.setText("Configure " + node.getNodeType() + " filter ...");
        }
    }

}