/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.utils.ViewUtils;

public abstract class NavigatorViewBase extends ViewPart implements INavigatorModelView
{
    private DBNModel model;
    private DatabaseNavigatorTree tree;

    protected NavigatorViewBase()
    {
        super();
        model = DBeaverCore.getInstance().getNavigatorModel();
    }

    public DBNModel getModel()
    {
        return model;
    }

    public TreeViewer getNavigatorViewer()
    {
        return tree.getViewer();
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent)
    {
        // Create tree
        tree = new DatabaseNavigatorTree(parent, getRootNode(), SWT.MULTI);

        tree.getViewer().addSelectionChangedListener(
            new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
                    if (!structSel.isEmpty()) {
                        Object object = structSel.getFirstElement();
                        if (object instanceof DBNNode) {
                            String desc = ((DBNNode)object).getNodeDescription();
                            if (CommonUtils.isEmpty(desc)) {
                                desc = ((DBNNode)object).getNodeName();
                            }
                            getViewSite().getActionBars().getStatusLineManager().setMessage(desc);
                        }
                    }
                }
            }
        );
        tree.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event)
            {
                DBNNode dbmNode = getSelectedNode();
                if (dbmNode == null) {
                    return;
                }
                ViewUtils.runCommand(dbmNode.getDefaultCommandId(), NavigatorViewBase.this);
            }

        });

        // Hook context menu
        ViewUtils.addContextMenu(this);
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(this);

        getViewSite().setSelectionProvider(tree.getViewer());
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

    private DBNNode getSelectedNode()
    {
        return ViewUtils.getSelectedNode(this);
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IPropertySheetPage.class) {
            return new PropertyPageTabbed();
        }
        return super.getAdapter(adapter);
    }

    public void showNode(DBNNode node) {
        tree.showNode(node);
    }

}
