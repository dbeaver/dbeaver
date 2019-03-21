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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public abstract class NavigatorViewBase extends ViewPart implements INavigatorModelView, IDataSourceContainerProvider, DBPPreferenceListener {

    public enum DoubleClickBehavior {
        EDIT,
        CONNECT,
        SQL_EDITOR,
        EXPAND,
        SQL_EDITOR_NEW,
    }

    private DBNModel model;
    private DatabaseNavigatorTree tree;
    private transient Object lastSelection;

    protected NavigatorViewBase()
    {
        super();
        model = DBWorkbench.getPlatform().getNavigatorModel();
    }

    public DBNModel getModel()
    {
        return model;
    }

    protected DatabaseNavigatorTree getNavigatorTree()
    {
        return tree;
    }

    /**
     * Navigator nodes filter.
     * Implementation returns true if element shouldn't be filtered (i.e. always visible).
     * @return filter or null if no filtering is supported.
     */
    protected INavigatorFilter getNavigatorFilter() {
        return null;
    }

    @NotNull
    @Override
    public TreeViewer getNavigatorViewer()
    {
        return tree.getViewer();
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent)
    {
        this.tree = createNavigatorTree(parent, getRootNode());

        getViewSite().setSelectionProvider(tree.getViewer());
        EditorUtils.trackControlContext(getSite(), this.tree.getViewer().getControl(), INavigatorModelView.NAVIGATOR_CONTEXT_ID);
    }

    private DatabaseNavigatorTree createNavigatorTree(Composite parent, DBNNode rootNode)
    {
        // Create tree
        final DatabaseNavigatorTree navigatorTree = new DatabaseNavigatorTree(parent, rootNode, getTreeStyle(), false, getNavigatorFilter());

        navigatorTree.getViewer().addSelectionChangedListener(
            event -> onSelectionChange((IStructuredSelection)event.getSelection())
        );
        navigatorTree.getViewer().addDoubleClickListener(event -> {
            TreeViewer viewer = tree.getViewer();
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            for (Object node : selection.toArray()) {
                //Object node = selection.getFirstElement();
                if ((node instanceof DBNResource && ((DBNResource) node).getResource() instanceof IFolder)) {
                    toggleNode(viewer, node);
                } else if (node instanceof DBNDataSource) {
                    DoubleClickBehavior dsBehaviorDefault = DoubleClickBehavior.valueOf(DBWorkbench.getPlatform().getPreferenceStore().getString(NavigatorPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK));
                    if (dsBehaviorDefault == DoubleClickBehavior.EXPAND) {
                        toggleNode(viewer, node);
                    } else {
                        DBPDataSourceContainer dataSource = ((DBNDataSource) node).getObject();
                        switch (dsBehaviorDefault) {
                            case EDIT:
                                NavigatorHandlerObjectOpen.openEntityEditor((DBNDataSource) node, null, UIUtils.getActiveWorkbenchWindow());
                                break;
                            case CONNECT: {
                                UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
                                if (dataSource.isConnected()) {
                                    serviceConnections.disconnectDataSource(dataSource);
                                } else {
                                    serviceConnections.connectDataSource(dataSource, null);
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
                    DoubleClickBehavior dcBehaviorDefault = DoubleClickBehavior.valueOf(DBWorkbench.getPlatform().getPreferenceStore().getString(NavigatorPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK));
                    boolean hasChildren = node instanceof DBNNode && ((DBNNode) node).hasChildren(true);
                    if (hasChildren && dcBehaviorDefault == DoubleClickBehavior.EXPAND) {
                        toggleNode(viewer, node);
                    } else {
                        NavigatorUtils.executeNodeAction(DBXTreeNodeHandler.Action.open, node, getSite());
                    }
                }
            }
        });

        // Hook context menu
        NavigatorUtils.addContextMenu(this.getSite(), navigatorTree.getViewer());
        // Add drag and drop support
        NavigatorUtils.addDragAndDropSupport(navigatorTree.getViewer());

        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(this);

        return navigatorTree;
    }

    private void toggleNode(TreeViewer viewer, Object node) {
        if (Boolean.TRUE.equals(viewer.getExpandedState(node))) {
            viewer.collapseToLevel(node, 1);
        } else {
            viewer.expandToLevel(node, 1);
        }
    }

    protected void onSelectionChange(IStructuredSelection structSel) {
        if (!structSel.isEmpty()) {
            lastSelection = structSel.getFirstElement();
            if (lastSelection instanceof DBNNode) {
                String desc = ((DBNNode)lastSelection).getNodeDescription();
                if (CommonUtils.isEmpty(desc)) {
                    desc = ((DBNNode)lastSelection).getNodeName();
                }
                getViewSite().getActionBars().getStatusLineManager().setMessage(desc);
            }
        } else {
            lastSelection = null;
        }

        if (lastSelection instanceof DBNDatabaseNode && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE)) {
            IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (activeEditor != null) {
                NavigatorUtils.syncEditorWithNavigator(this, activeEditor);
            }
        }
    }

    protected int getTreeStyle()
    {
        return SWT.MULTI;
    }

    @Override
    public void dispose()
    {
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(this);

        model = null;
        super.dispose();
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus()
    {
        tree.getViewer().getControl().setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == IPropertySheetPage.class) {
            return adapter.cast(new PropertyPageStandard());
        }
        return super.getAdapter(adapter);
    }

    public void showNode(DBNNode node) {
        tree.showNode(node);
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer()
    {
        if (lastSelection instanceof DBNDatabaseNode) {
            if (lastSelection instanceof DBNDataSource) {
                return ((DBNDataSource)lastSelection).getDataSourceContainer();
            } else if (((DBNDatabaseNode) lastSelection).getObject() != null) {
                final DBPDataSource dataSource = ((DBNDatabaseNode) lastSelection).getObject().getDataSource();
                return dataSource == null ? null : dataSource.getContainer();
            }
        } else if (lastSelection instanceof DBNResource) {
            Collection<DBPDataSourceContainer> containers = ((DBNResource) lastSelection).getAssociatedDataSources();
            if (containers != null && containers.size() == 1) {
                return containers.iterator().next();
            }
        }
        return null;
    }

    public void configureView() {

    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        String property = event.getProperty();
        if (CommonUtils.equalObjects(event.getOldValue(), event.getNewValue())) {
            return;
        }
        switch (property) {
            case ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS:
            case NavigatorPreferences.NAVIGATOR_SORT_ALPHABETICALLY:
            case NavigatorPreferences.NAVIGATOR_SORT_FOLDERS_FIRST:
            case NavigatorPreferences.NAVIGATOR_GROUP_BY_DRIVER:
                tree.getViewer().refresh();
                break;
        }
    }

}
