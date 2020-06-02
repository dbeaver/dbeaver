/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.tasks.ui;

import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPEditorContribution;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.view.DatabaseTasksView;
import org.jkiss.dbeaver.tools.registry.ToolDescriptor;
import org.jkiss.dbeaver.tools.registry.ToolGroupDescriptor;
import org.jkiss.dbeaver.tools.registry.ToolsRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.EmptyListAction;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceMenuContributor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
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
        INavigatorModelView navigatorModelView = GeneralUtils.adapt(activePart, INavigatorModelView.class);
        if (navigatorModelView != null) {
            final ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    selectedObject = RuntimeUtils.getObjectAdapter(((IStructuredSelection) selection).getFirstElement(), DBSObject.class);

                    List<ToolDescriptor> tools = getAvailableTools((IStructuredSelection) selection);
                    fillToolsMenu(menuItems, tools, selection);
                }
            }
        } else if (activePart instanceof IEditorPart) {
            IEditorInput editorInput = ((IEditorPart) activePart).getEditorInput();
            if (editorInput instanceof IDatabaseEditorInput) {
                selectedObject = ((IDatabaseEditorInput) editorInput).getDatabaseObject();
            } else if (activePart instanceof IDataSourceContainerProvider) {
                selectedObject = ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
            }
        }

        if (selectedObject != null) {

            // Contribute standard tools like session manager
            DBPDataSource dataSource = selectedObject.getDataSource();
            if (dataSource != null) {
                DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();
                DBPEditorContribution[] contributedEditors = DBWorkbench.getPlatform().getDataSourceProviderRegistry().getContributedEditors(DBPEditorContribution.MB_CONNECTION_EDITOR, dataSourceContainer);
                if (contributedEditors.length > 0) {
                    menuItems.add(new Separator());
                    for (DBPEditorContribution ec : contributedEditors) {
                        menuItems.add(new ActionContributionItem(new OpenToolsEditorAction(activePage, dataSource, ec)));
                    }
                }
            }
        }

        // Tasks management
        {
            menuItems.add(new Separator());
            menuItems.add(ActionUtils.makeCommandContribution(activePart.getSite(), DatabaseTasksView.CREATE_TASK_CMD_ID));
        }
    }

    private List<ToolDescriptor> getAvailableTools(IStructuredSelection selection) {
        List<DBSObject> objects = NavigatorUtils.getSelectedObjects(selection);
        List<ToolDescriptor> result = new ArrayList<>();
        if (!objects.isEmpty()) {
            for (ToolDescriptor descriptor : ToolsRegistry.getInstance().getTools()) {
                if (descriptor.isSingleton() && objects.size() > 1) {
                    continue;
                }
                boolean applies = true;
                for (DBSObject object : objects) {
                    if (!descriptor.appliesTo(object)) {
                        applies = false;
                        break;
                    }
                }
                if (applies) {
                    result.add(descriptor);
                }
            }
        }
        return result;
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
        private final DBPDataSource dataSource;
        private final DBPEditorContribution editor;
        public OpenToolsEditorAction(IWorkbenchPage workbenchPage, DBPDataSource dataSource, DBPEditorContribution editor) {
            super(editor.getLabel(), DBeaverIcons.getImageDescriptor(editor.getIcon()));
            this.workbenchPage = workbenchPage;
            this.dataSource = dataSource;
            this.editor = editor;
        }

        @Override
        public void run() {
            try {
                workbenchPage.openEditor(
                    new DataSourceEditorInput(dataSource, editor),
                    editor.getEditorId());
            } catch (PartInitException e) {
                DBWorkbench.getPlatformUI().showError("Editor open", "Error opening tool editor '" + editor.getEditorId() + "'", e.getStatus());
            }
        }
    }

    public class DataSourceEditorInput implements IEditorInput, IDataSourceContainerProvider, DBPContextProvider {

        private final DBPDataSource dataSource;
        private final DBPEditorContribution editor;

        public DataSourceEditorInput(DBPDataSource dataSource, DBPEditorContribution editor) {
            this.dataSource = dataSource;
            this.editor = editor;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return DBeaverIcons.getImageDescriptor(editor.getIcon());
        }

        @Override
        public String getName() {
            return editor.getLabel();
        }

        @Override
        public IPersistableElement getPersistable() {
            return null;
        }

        @Override
        public String getToolTipText() {
            return editor.getDescription();
        }

        @Override
        public <T> T getAdapter(Class<T> adapter) {
            return null;
        }

        @Override
        public DBPDataSourceContainer getDataSourceContainer() {
            return dataSource.getContainer();
        }

        @Override
        public DBCExecutionContext getExecutionContext() {
            return DBUtils.getDefaultContext(dataSource, false);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this ||
                (obj instanceof DataSourceEditorInput && ((DataSourceEditorInput) obj).editor == editor);
        }

    }

}