/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.EditObjectFilterDialog;

import java.util.Collections;
import java.util.List;
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
        final DBNDatabaseNode dbNode = (DBNDatabaseNode) node;
        DBXTreeItem itemsMeta = dbNode.getItemsMeta();
        if (itemsMeta != null) {
            DBSObjectFilter objectFilter = dbNode.getNodeFilter(itemsMeta, true);
            if (objectFilter == null) {
                objectFilter = new DBSObjectFilter();
            }
            final DBPDataSourceRegistry dsRegistry = dbNode.getOwnerProject().getDataSourceRegistry();
            final boolean globalFilter = dbNode.getValueObject() instanceof DBPDataSource;
            String parentName = "?";
            if (dbNode.getValueObject() instanceof DBSObject) {
                parentName = ((DBSObject) dbNode.getValueObject()).getName();
            }
            EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                shell,
                dsRegistry,
                globalFilter ? "All " + node.getNodeTypeLabel() : node.getNodeTypeLabel() + " of " + parentName,
                objectFilter,
                globalFilter);
            switch (dialog.open()) {
                case IDialogConstants.OK_ID:
                    dbNode.setNodeFilter(itemsMeta, dialog.getFilter(), true);
                    NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(dbNode));
                    break;
                case EditObjectFilterDialog.SHOW_GLOBAL_FILTERS_ID: {
                    Class<?> childrenClass = null;
                    if (dbNode instanceof DBNDatabaseFolder folder) {
                        childrenClass = folder.getChildrenClass();
                    } else {
                        List<DBXTreeNode> childMeta = dbNode.getMeta().getChildren(dbNode);
                        if (!childMeta.isEmpty() && childMeta.get(0) instanceof DBXTreeItem item) {
                            childrenClass = dbNode.getChildrenClass(item);
                        }
                    }
                    if (childrenClass == null) {
                        DBWorkbench.getPlatformUI().showMessageBox(
                            "Bad node", "Cannot use node '" + dbNode.getNodeUri() + "' for filters", true);
                        return;
                    }
                    objectFilter = dbNode.getDataSource().getContainer().getObjectFilter(childrenClass, null, true);
                    dialog = new EditObjectFilterDialog(
                        shell,
                        dsRegistry, "All " + node.getNodeTypeLabel(),
                        objectFilter != null ? objectFilter : new DBSObjectFilter(),
                        true);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        // Set global filter
                        dbNode.getDataSource().getContainer().setObjectFilter(childrenClass, null, dialog.getFilter());
                        dbNode.getDataSource().getContainer().persistConfiguration();
                        NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(dbNode));
                    }
                    break;
                }
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
            element.setText(NLS.bind(UINavigatorMessages.actions_navigator_filter_objects, node.getNodeTypeLabel()));
        }
    }

}