/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tools.ToolDescriptor;
import org.jkiss.dbeaver.registry.tools.ToolGroupDescriptor;
import org.jkiss.dbeaver.registry.tools.ToolsRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.common.EmptyListAction;
import org.jkiss.dbeaver.ui.actions.common.ExecuteToolHandler;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class DataSourceToolsContributor extends DataSourceMenuContributor
{


    private static final boolean SHOW_GROUPS_AS_SUBMENU = false;

    @Override
    protected void fillContributionItems(List<IContributionItem> menuItems)
    {
        IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart activePart = activePage.getActivePart();
        if (activePart == null) {
            return;
        }
        DBSObject selectedObject = null;
        final ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
        if (selectionProvider != null) {
            ISelection selection = selectionProvider.getSelection();
            if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                selectedObject = RuntimeUtils.getObjectAdapter(((IStructuredSelection) selection).getFirstElement(), DBSObject.class);

                List<ToolDescriptor> tools = ToolsRegistry.getInstance().getTools((IStructuredSelection) selection);
                fillToolsMenu(menuItems, tools, selection);
            }
        } else if (activePart instanceof IEditorPart) {
            IEditorInput editorInput = ((IEditorPart) activePart).getEditorInput();
            if (editorInput instanceof IDatabaseEditorInput) {
                selectedObject = ((IDatabaseEditorInput) editorInput).getDatabaseObject();
            }
        }

        if (selectedObject != null) {

            // Contribute standard tools like session manager
            List<DBXTreeObject> navigatorObjectEditors = new ArrayList<>();
            DBNDatabaseNode dsNode = NavigatorUtils.getNodeByObject(selectedObject.getDataSource());
            if (dsNode != null) {
                Set<DBXTreeNode> processedNodes = new HashSet<>();
                findObjectNodes(dsNode.getMeta(), navigatorObjectEditors, processedNodes);
            }
            if (!navigatorObjectEditors.isEmpty()) {
                DBNDatabaseNode objectNode = dsNode;
//                DBNDatabaseNode objectNode = NavigatorUtils.getNodeByObject(selectedObject);
//                if (objectNode == null) {
//                    objectNode = dsNode;
//                }
                menuItems.add(new Separator());
                for (DBXTreeObject editorMeta : navigatorObjectEditors) {
                    menuItems.add(new ActionContributionItem(new OpenToolsEditorAction(activePage, objectNode, editorMeta)));
                }
            }
        }
    }

    private void findObjectNodes(DBXTreeNode meta, List<DBXTreeObject> editors, Set<DBXTreeNode> processedNodes) {
        if (processedNodes.contains(meta)) {
            return;
        }
        if (meta instanceof DBXTreeObject) {
            editors.add((DBXTreeObject) meta);
        }
        processedNodes.add(meta);
        if (meta.getRecursiveLink() != null) {
            return;
        }
        List<DBXTreeNode> children = meta.getChildren(null);
        if (children != null) {
            for (DBXTreeNode child : children) {
                findObjectNodes(child, editors, processedNodes);
            }
        }
    }

    private static void fillToolsMenu(List<IContributionItem> menuItems, List<ToolDescriptor> tools, ISelection selection)
    {
        boolean hasTools = false;
        if (!CommonUtils.isEmpty(tools)) {
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            if (workbenchWindow.getActivePage() != null) {
                IWorkbenchPart activePart = workbenchWindow.getActivePage().getActivePart();
                if (activePart != null) {
                    Map<ToolGroupDescriptor, IMenuManager> groupsMap = new HashMap<>();
                    Set<ToolGroupDescriptor> groupSet = new HashSet<>();
                    for (ToolDescriptor tool : tools) {
                        hasTools = true;
                        IMenuManager parentMenu = null;
                        if (tool.getGroup() != null) {
                            if (SHOW_GROUPS_AS_SUBMENU) {
                                parentMenu = getGroupMenu(menuItems, groupsMap, tool.getGroup());
                            } else {
                                if (!groupSet.contains(tool.getGroup())) {
                                    groupSet.add(tool.getGroup());
                                    menuItems.add(new Separator(tool.getGroup().getId()));
                                }
                            }
                        }

                        IAction action = ActionUtils.makeAction(
                            new ExecuteToolHandler(workbenchWindow, tool),
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

    private static IMenuManager getGroupMenu(List<IContributionItem> rootItems, Map<ToolGroupDescriptor, IMenuManager> groupsMap, ToolGroupDescriptor group) {
        IMenuManager item = groupsMap.get(group);
        if (item == null) {
            item = new MenuManager(group.getLabel(), null, group.getId());
            if (group.getParent() != null) {
                IMenuManager parentMenu = getGroupMenu(rootItems, groupsMap, group.getParent());
                parentMenu.add(item);
            } else {
                rootItems.add(item);
            }
        }
        groupsMap.put(group, item);
        return item;
    }

    private class OpenToolsEditorAction extends Action {
        private final IWorkbenchPage workbenchPage;
        private final DBNDatabaseNode databaseNode;
        private final DBXTreeObject editorMeta;
        public OpenToolsEditorAction(IWorkbenchPage workbenchPage, DBNDatabaseNode databaseNode, DBXTreeObject editorMeta) {
            super(editorMeta.getLabel(), editorMeta.getIcon(null) == null ? null : DBeaverIcons.getImageDescriptor(editorMeta.getIcon(null)));
            this.workbenchPage = workbenchPage;
            this.databaseNode = databaseNode;
            this.editorMeta = editorMeta;
        }

        @Override
        public void run() {
            DatabaseEditorInput<DBNDatabaseNode> objectInput = new DatabaseEditorInput<DBNDatabaseNode>(databaseNode) {

            };
            try {
                workbenchPage.openEditor(
                    objectInput,
                    editorMeta.getEditorId());
            } catch (PartInitException e) {
                DBWorkbench.getPlatformUI().showError("Editor open", "Error opening tool editor '" + editorMeta.getEditorId() + "'", e.getStatus());
            }
        }
    }
}