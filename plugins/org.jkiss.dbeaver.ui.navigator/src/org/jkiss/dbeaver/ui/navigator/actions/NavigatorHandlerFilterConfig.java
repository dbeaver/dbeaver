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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.EditObjectFilterDialog;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NavigatorHandlerFilterConfig extends NavigatorHandlerObjectCreateBase implements IElementUpdater {
    private static final Log log = Log.getLog(NavigatorHandlerFilterConfig.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDatabaseNode dbNode) {
            configureFilters(HandlerUtil.getActiveShell(event), dbNode);
        }
        return null;
    }

    public static void configureFilters(Shell shell, DBNDatabaseNode dbNode) {
        try {
            DBNDatabaseNode parentNode = !(dbNode instanceof DBNDatabaseFolder) &&
                                         dbNode.getParentNode() instanceof DBNDatabaseNode parent ? parent : dbNode;
            DBXTreeItem itemsMeta = UIUtils.runWithMonitor(monitor -> DBNUtils.getValidItemsMeta(monitor, parentNode));
            if (itemsMeta != null) {
                DBSObjectFilter objectFilter = parentNode.getNodeFilter(itemsMeta, true);
                if (objectFilter == null) {
                    objectFilter = new DBSObjectFilter();
                }
                final DBPDataSourceRegistry dsRegistry = dbNode.getOwnerProject().getDataSourceRegistry();
                final boolean globalFilter = dbNode.getValueObject() instanceof DBPDataSource;
                String parentName = "?";
                if (dbNode.getValueObject() instanceof DBSObject dbsObject) {
                    parentName = dbsObject.getName();
                }
                EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                    shell,
                    dsRegistry,
                    globalFilter ? "All " + dbNode.getNodeTypeLabel() : dbNode.getNodeTypeLabel() + " of " + parentName,
                    objectFilter,
                    globalFilter);
                switch (dialog.open()) {
                    case IDialogConstants.OK_ID:
                        parentNode.setNodeFilter(itemsMeta, dialog.getFilter(), true);
                        NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(parentNode));
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
                        DBPDataSourceContainer dataSourceContainer = dbNode.getDataSourceContainer();
                        objectFilter = dataSourceContainer.getObjectFilter(childrenClass, null, true);
                        dialog = new EditObjectFilterDialog(
                            shell,
                            dsRegistry, "All " + dbNode.getNodeTypeLabel(),
                            objectFilter != null ? objectFilter : new DBSObjectFilter(),
                            true);
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            // Set global filter
                            dataSourceContainer.setObjectFilter(childrenClass, null, dialog.getFilter());
                            dataSourceContainer.persistConfiguration();
                            NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(parentNode));
                        }
                        break;
                    }
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
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