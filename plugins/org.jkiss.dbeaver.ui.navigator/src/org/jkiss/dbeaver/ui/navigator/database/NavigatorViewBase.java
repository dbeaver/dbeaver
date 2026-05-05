/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class NavigatorViewBase extends ViewPart
    implements INavigatorModelView, DBPDataSourceContainerProvider, DBPPreferenceListener
{
    private DatabaseNavigatorTree tree;
    private transient Object lastSelection;

    protected NavigatorViewBase() {
        super();
    }

    @NotNull
    public static DBNModel getGlobalNavigatorModel() {
        return DBWorkbench.getPlatform().getNavigatorModel();
    }

    public DatabaseNavigatorTree getNavigatorTree() {
        return tree;
    }

    /**
     * Navigator nodes filter.
     * Implementation returns true if element shouldn't be filtered (i.e. always visible).
     *
     * @return filter or null if no filtering is supported.
     */
    protected INavigatorFilter getNavigatorFilter() {
        return null;
    }

    @NotNull
    @Override
    public TreeViewer getNavigatorViewer() {
        return tree.getViewer();
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(@NotNull Composite parent) {
        this.tree = createNavigatorTree(parent);
        this.tree.setItemRenderer(new StatisticsNavigatorNodeRenderer(this));

        getViewSite().setSelectionProvider(tree.getViewer());
        getSite().getService(IContextService.class).activateContext(INavigatorModelView.NAVIGATOR_CONTEXT_ID);
        getSite().getService(IContextService.class).activateContext(INavigatorModelView.NAVIGATOR_VIEW_CONTEXT_ID);

        UIExecutionQueue.queueExec(() -> {
            if (!tree.isDisposed()) {
                tree.setInput(getRootNode());
            }
        });
    }

    private DatabaseNavigatorTree createNavigatorTree(@NotNull Composite parent) {
        // Create tree
        final DatabaseNavigatorTree navigatorTree = new DatabaseNavigatorTree(
            parent,
            null,
            getTreeStyle(),
            false,
            getNavigatorFilter()
        );

        createTreeColumns(navigatorTree);

        navigatorTree.getViewer().addSelectionChangedListener(
            event -> onSelectionChange((IStructuredSelection) event.getSelection())
        );
        navigatorTree.getViewer().getTree().addListener(SWT.MouseDoubleClick, event -> event.doit = false);
        navigatorTree.getViewer().getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                super.mouseDoubleClick(e);
            }

            @Override
            public void mouseDown(MouseEvent e) {
                super.mouseDown(e);
            }

            @Override
            public void mouseUp(MouseEvent e) {
                super.mouseUp(e);
                // Commented because it forced selection reset on connection expand
/*
                Point point = new Point(e.x, e.y);
                TreeItem item = navigatorTree.getViewer().getTree().getItem(point);
                if (item == null) {
                    navigatorTree.getViewer().setSelection(new StructuredSelection());
                } 
*/
            }
        });
        navigatorTree.getViewer().addDoubleClickListener(event -> {
            TreeViewer viewer = tree.getViewer();
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            for (Object node : selection.toArray()) {
                if ((node instanceof DBNResource resNode && resNode.getResource() instanceof IFolder)) {
                    toggleNode(viewer, node);
                } else if (node instanceof DBNDataSource dataSourceNode) {
                    NavigatorPreferences.DoubleClickBehavior dsBehaviorDefault = CommonUtils.valueOf(
                        NavigatorPreferences.DoubleClickBehavior.class,
                        DBWorkbench.getPlatform().getPreferenceStore().getString(NavigatorPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK),
                        NavigatorPreferences.DoubleClickBehavior.EDIT
                    );
                    if (dsBehaviorDefault == NavigatorPreferences.DoubleClickBehavior.EXPAND) {
                        toggleNode(viewer, node);
                    } else {
                        DBPDataSourceContainer dataSource = dataSourceNode.getObject();
                        assert dataSource != null;
                        switch (dsBehaviorDefault) {
                            case EDIT:
                                NavigatorHandlerObjectOpen.openEntityEditor(dataSourceNode, null, UIUtils.getActiveWorkbenchWindow());
                                break;
                            case CONNECT: {
                                UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
                                if (serviceConnections != null) {
                                    if (dataSource.isConnected()) {
                                        serviceConnections.disconnectDataSource(dataSource);
                                    } else {
                                        serviceConnections.connectDataSource(dataSource, null);
                                    }
                                }
                                break;
                            }
                            case SQL_EDITOR: {
                                UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                                if (serviceSQL != null) {
                                    serviceSQL.openRecentScript(dataSource);
                                }
                                break;
                            }
                            case SQL_EDITOR_NEW: {
                                UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                                if (serviceSQL != null) {
                                    serviceSQL.openNewScript(dataSource);
                                }
                                break;
                            }
                        }
                    }
                } else if (node instanceof TreeNodeSpecial) {
                    ((TreeNodeSpecial) node).handleDefaultAction(navigatorTree);
                } else {
                    String defaultEditorPageId = null;
                    NavigatorPreferences.DoubleClickBehavior dcBehaviorDefault = CommonUtils.valueOf(
                        NavigatorPreferences.DoubleClickBehavior.class,
                        DBWorkbench.getPlatform().getPreferenceStore().getString(NavigatorPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK)
                    );

                    if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSDataContainer) {
                        defaultEditorPageId = DBWorkbench.getPlatform().getPreferenceStore()
                            .getString(NavigatorPreferences.NAVIGATOR_DEFAULT_EDITOR_PAGE);
                    }
                    boolean hasChildren = node instanceof DBNNode && ((DBNNode) node).hasChildren(true);
                    if (hasChildren && dcBehaviorDefault == NavigatorPreferences.DoubleClickBehavior.EXPAND) {
                        toggleNode(viewer, node);
                    } else {
                        Map<String, Object> parameters = null;
                        if (!CommonUtils.isEmpty(defaultEditorPageId)) {
                            parameters = Collections.singletonMap(MultiPageDatabaseEditor.PARAMETER_ACTIVE_PAGE, defaultEditorPageId);
                        }
                        NavigatorUtils.executeNodeAction(DBXTreeNodeHandler.Action.open, node, parameters, getSite());
                    }
                }
            }
        });

        // Hook context menu
        NavigatorUtils.addContextMenu(this.getSite(), navigatorTree.getViewer());
        installDragAndDropSupport(navigatorTree);

        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(this);

        return navigatorTree;
    }

    protected void installDragAndDropSupport(@NotNull DatabaseNavigatorTree navigatorTree) {
        // Add drag and drop support
        NavigatorUtils.addDragAndDropSupport(navigatorTree.getViewer());
    }

    protected void createTreeColumns(DatabaseNavigatorTree tree) {

    }

    private void toggleNode(@NotNull TreeViewer viewer, @NotNull Object node) {
        if (viewer.getExpandedState(node)) {
            viewer.collapseToLevel(node, 1);
        } else {
            viewer.expandToLevel(node, 1);
        }
    }

    private void onSelectionChange(@NotNull IStructuredSelection structSel) {
        if (!structSel.isEmpty()) {
            lastSelection = structSel.getFirstElement();
            if (lastSelection instanceof DBNRoot) {
                // Don't display status message for root node - it has no meaningful information
                getViewSite().getActionBars().getStatusLineManager().setMessage(null);
            } else if (lastSelection instanceof DBNNode node) {
                String name = node.getNodeDisplayName();
                String desc = node.getNodeDescription();
                if (node instanceof DBNDatabaseNode && !(node instanceof DBNDatabaseFolder)) {
                    name = node.getNodeTypeLabel() + ": " + name;
                }
                if (CommonUtils.isEmpty(desc)) {
                    getViewSite().getActionBars().getStatusLineManager().setMessage(name);
                } else {
                    getViewSite().getActionBars().getStatusLineManager().setMessage(name + " - " + desc);
                }
            }
        } else {
            lastSelection = null;
        }

        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (lastSelection instanceof DBNDatabaseNode && preferenceStore.getBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE)) {
            IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (activeEditor != null) {
                NavigatorUtils.syncEditorWithNavigator(this, activeEditor);
            }
        }
    }

    protected int getTreeStyle() {
        return SWT.MULTI | SWT.FULL_SELECTION;
    }

    @Override
    public void dispose() {
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(this);

        super.dispose();
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        tree.getViewer().getControl().setFocus();
    }

    public boolean focusFilterControl(boolean selectAll) {
        final Text filterControl = tree.getFilterControl();
        if (filterControl == null || filterControl.isDisposed()) {
            return false;
        }
        filterControl.setFocus();
        if (selectAll) {
            filterControl.selectAll();
        }
        return true;
    }

    @Nullable
    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        if (adapter == IPropertySheetPage.class) {
            return adapter.cast(new PropertyPageStandard());
        }
        return super.getAdapter(adapter);
    }

    public void showNode(@NotNull DBNNode node) {
        tree.showNode(node);
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        if (lastSelection instanceof DBNDatabaseNode databaseNode) {
            if (lastSelection instanceof DBNDataSource dataSourceNode) {
                return dataSourceNode.getDataSourceContainer();
            } else if (databaseNode.getObject() != null) {
                final DBPDataSource dataSource = databaseNode.getObject().getDataSource();
                return dataSource == null ? null : dataSource.getContainer();
            }
        } else if (lastSelection instanceof DBNResource resourceNode) {
            Collection<DBPDataSourceContainer> containers = resourceNode.getAssociatedDataSources();
            if (containers != null && containers.size() == 1) {
                return containers.iterator().next();
            }
        }
        return null;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        String property = event.getProperty();
        if (CommonUtils.equalObjects(event.getOldValue(), event.getNewValue())) {
            return;
        }
        switch (property) {
            case ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS:
            case ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY:
            case ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE:
            case ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST:
            case NavigatorPreferences.NAVIGATOR_COLOR_ALL_NODES:
            case NavigatorPreferences.NAVIGATOR_GROUP_BY_DRIVER:
                tree.getViewer().refresh();
                break;
            case NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO:
            case NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME:
            case NavigatorPreferences.NAVIGATOR_SHOW_OBJECTS_DESCRIPTION:
            case NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS:
                tree.getViewer().getTree().redraw();
                break;
        }
    }
/*

    protected void updateProjectViewTitle() {
        IViewDescriptor viewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(getViewSite().getId());
        if (viewDescriptor == null) {
            return;
        }
        DBPProject project = getViewProject();
        String title = viewDescriptor.getLabel() + (project == null ? "" : " - " + project.getName());
        setPartName(title);
        setTitleToolTip(title + "\n" + viewDescriptor.getDescription());
    }

    @Nullable
    protected static DBPProject getViewProject() {
        return DBWorkbench.getPlatform().getWorkspace().getActiveProject();
    }
*/

}
