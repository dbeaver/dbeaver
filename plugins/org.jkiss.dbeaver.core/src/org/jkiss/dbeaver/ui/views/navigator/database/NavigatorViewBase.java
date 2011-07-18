/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.utils.ViewUtils;

public abstract class NavigatorViewBase extends ViewPart implements INavigatorModelView, IDataSourceContainerProvider
{
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

    public TreeViewer getNavigatorViewer()
    {
        return tree.getViewer();
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent)
    {
        this.tree = createNavigatorTree(parent, getRootNode());

        getViewSite().setSelectionProvider(tree.getViewer());
    }

    protected DatabaseNavigatorTree createNavigatorTree(Composite parent, DBNNode rootNode)
    {
        // Create tree
        DatabaseNavigatorTree navigatorTree = new DatabaseNavigatorTree(parent, rootNode, SWT.MULTI);

        navigatorTree.getViewer().addSelectionChangedListener(
            new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
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
                }
            }
        );
        navigatorTree.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection)tree.getViewer().getSelection();
                if (selection.size() == 1) {
                    DBNNode node = (DBNNode)selection.getFirstElement();
                    if (!(node instanceof DBNDatabaseNode) || !node.allowsOpen()) {
                        return;
                    }
                    NavigatorHandlerObjectOpen.openEntityEditor(
                        (DBNDatabaseNode) node,
                        null,
                        getSite().getWorkbenchWindow());
                }
            }

        });

        // Hook context menu
        ViewUtils.addContextMenu(this, navigatorTree.getViewer(), navigatorTree.getViewer().getControl());
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(navigatorTree.getViewer());

        return navigatorTree;
    }

    public void dispose()
    {
        model = null;
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus()
    {
        tree.getViewer().getControl().setFocus();
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IPropertySheetPage.class) {
            //return new PropertyPageTabbed();
        }
        return super.getAdapter(adapter);
    }

    public void showNode(DBNNode node) {
        tree.showNode(node);
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        if (lastSelection instanceof DBNDatabaseNode) {
            if (lastSelection instanceof DBNDataSource) {
                return ((DBNDataSource)lastSelection).getDataSourceContainer();
            } else {
                final DBPDataSource dataSource = ((DBNDatabaseNode) lastSelection).getObject().getDataSource();
                if (dataSource != null) {
                    return dataSource.getContainer();
                }
            }
        }
        return null;
    }
}
