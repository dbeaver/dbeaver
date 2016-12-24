/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public abstract class NavigatorViewBase extends ViewPart implements INavigatorModelView, IDataSourceContainerProvider
{
    public enum DoubleClickBehavior {
        EDIT,
        CONNECT,
        SQL_EDITOR,
        EXPAND
    }

    private DBNModel model;
    private DatabaseNavigatorTree tree;
    private transient Object lastSelection;

    protected NavigatorViewBase()
    {
        super();
        model = DBeaverCore.getInstance().getNavigatorModel();
    }

    public DBNModel getModel()
    {
        return model;
    }

    protected DatabaseNavigatorTree getNavigatorTree()
    {
        return tree;
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
    }

    protected DatabaseNavigatorTree createNavigatorTree(Composite parent, DBNNode rootNode)
    {
        // Create tree
        final DatabaseNavigatorTree navigatorTree = new DatabaseNavigatorTree(parent, rootNode, getTreeStyle());

        navigatorTree.getViewer().addSelectionChangedListener(
            new ISelectionChangedListener()
            {
                @Override
                public void selectionChanged(SelectionChangedEvent event)
                {
                    onSelectionChange((IStructuredSelection)event.getSelection());
                }
            }
        );
        navigatorTree.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                TreeViewer viewer = tree.getViewer();
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                if (selection.size() == 1) {
                    Object node = selection.getFirstElement();
                    if ((node instanceof DBNResource && ((DBNResource) node).getResource() instanceof IFolder) ||
                        (node instanceof DBNDataSource &&
                            DoubleClickBehavior.valueOf(DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK)) == DoubleClickBehavior.EXPAND))
                    {
                        if (Boolean.TRUE.equals(viewer.getExpandedState(node))) {
                            viewer.collapseToLevel(node, 1);
                        } else {
                            viewer.expandToLevel(node, 1);
                        }
                    } else if (node instanceof DBNDataSource) {
                        DBPDataSourceContainer dataSource = ((DBNDataSource) node).getObject();
                        NavigatorViewBase.DoubleClickBehavior doubleClickBehavior =
                            NavigatorViewBase.DoubleClickBehavior.valueOf(DBeaverCore.getGlobalPreferenceStore().getString(DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK));
                        switch (doubleClickBehavior) {
                            case EDIT:
                                NavigatorHandlerObjectOpen.openEntityEditor((DBNDataSource) node, null, DBeaverUI.getActiveWorkbenchWindow());
                                break;
                            case CONNECT:
                                if (dataSource.isConnected()) {
                                    DataSourceHandler.disconnectDataSource(dataSource, null);
                                } else {
                                    DataSourceHandler.connectToDataSource(null, dataSource, null);
                                }
                                break;
                            case SQL_EDITOR:
                                try {
                                    OpenHandler.openRecentScript(getSite().getWorkbenchWindow(), dataSource, null);
                                } catch (CoreException e) {
                                    UIUtils.showErrorDialog(getSite().getShell(), "Open SQL editor", "Can't open SQL editor", e);
                                }
                                break;
                        }
                    } else {
                        NavigatorUtils.openNavigatorNode(node, getSite().getWorkbenchWindow());
                    }
                }
            }

        });

        // Hook context menu
        NavigatorUtils.addContextMenu(this.getSite(), navigatorTree.getViewer());
        // Add drag and drop support
        NavigatorUtils.addDragAndDropSupport(navigatorTree.getViewer());

        return navigatorTree;
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

        if (lastSelection instanceof DBNDatabaseNode && DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE)) {
            IEditorPart activeEditor = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
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

}
