/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.DriverManagerAction;
import org.jkiss.dbeaver.ui.actions.LinkEditorAction;
import org.jkiss.dbeaver.ui.actions.NewConnectionAction;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.utils.ViewUtils;

public class DatabaseNavigatorView extends ViewPart
    implements IDBNListener, INavigatorModelView, IDoubleClickListener
{
    static final Log log = LogFactory.getLog(DatabaseNavigatorView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseNavigator";

    private TreeViewer viewer;
    private DBNModel model;

    public DatabaseNavigatorView()
    {
        super();
        model = DBeaverCore.getInstance().getNavigatorModel();
        model.addListener(this);
    }

    public DBNNode getRootNode() {
        return model.getRoot();
    }

    public TreeViewer getNavigatorViewer()
    {
        return viewer;
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
        this.viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        this.viewer.setLabelProvider(new DatabaseNavigatorLabelProvider(this));
        this.viewer.setContentProvider(new DatabaseNavigatorContentProvider(this));
        this.viewer.setInput(model.getRoot());
        this.viewer.addSelectionChangedListener(
            new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
                    if (!structSel.isEmpty()) {
                        Object object = structSel.getFirstElement();
                        if (object instanceof DBNNode) {
                            String desc = ((DBNNode)object).getObject().getDescription();
                            if (CommonUtils.isEmpty(desc)) {
                                desc = ((DBNNode)object).getNodeName();
                            }
                            getViewSite().getActionBars().getStatusLineManager().setMessage(desc);
                        }
                    }
                }
            }
        );
        this.viewer.addDoubleClickListener(this);
        // Hook context menu
        ViewUtils.addContextMenu(this);
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(this);

        getViewSite().setSelectionProvider(viewer);
    }

    private void makeToolbar(IActionBars actionBars) {
        IMenuManager dropDownMenu = actionBars.getMenuManager();
        IToolBarManager toolBar = actionBars.getToolBarManager();


        {
            IAction driverManagerAction = ViewUtils.makeAction(new DriverManagerAction(), this, null, "Open Driver Manager", DBIcon.ACTION_DRIVER_MANAGER.getImageDescriptor(), null);
            dropDownMenu.add(driverManagerAction);
            toolBar.add(driverManagerAction);
        }
        {
            IAction driverManagerAction = ViewUtils.makeAction(new NewConnectionAction(), this, null, "New Connection", DBIcon.ACTION_NEW_CONNECTION.getImageDescriptor(), null);
            dropDownMenu.add(driverManagerAction);
            toolBar.add(driverManagerAction);
        }
        dropDownMenu.add(new Separator());
        toolBar.add(new Separator());
        {
            IAction driverManagerAction = ViewUtils.makeAction(new LinkEditorAction(), this, null, "Link with Editor", DBIcon.ACTION_LINK_TO_EDITOR.getImageDescriptor(), null);
            dropDownMenu.add(driverManagerAction);
            toolBar.add(driverManagerAction);
        }
    }

    public void dispose()
    {
        if (model != null) {
            model.removeListener(this);
            model = null;
        }
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus()
    {
        viewer.getControl().setFocus();
    }

    public void nodeChanged(final DBNEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE:
                if (event.getNode() instanceof DBNDataSource) {
                    asyncExec(new Runnable() { public void run() {
                        if (!viewer.getControl().isDisposed()) {
                            viewer.refresh();
                        }
                    }});
                }
                break;
            case UPDATE:
                asyncExec(new Runnable() { public void run() {
                    if (!viewer.getControl().isDisposed()) {
                        if (event.getNode() != null) {
                            switch (event.getNodeChange()) {
                                case LOAD:
                                    viewer.expandToLevel(event.getNode(), 1);
                                    viewer.refresh(event.getNode());
                                    break;
                                case UNLOAD:
                                    viewer.collapseToLevel(event.getNode(), -1);
                                    viewer.refresh(event.getNode());
                                    break;
                                case REFRESH:
                                    getNavigatorViewer().update(event.getNode(), null);
                                    break;
                                case LOCK:
                                case UNLOCK:
                                    viewer.refresh(event.getNode());
                                    break;
                            }
                        } else {
                            log.warn("Null node object");
                        }
                    }
                }});
                break;
            default:
                break;
        }
    }

    public void doubleClick(DoubleClickEvent event)
    {
        DBNNode dbmNode = getSelectedNode();
        if (dbmNode == null) {
            return;
        }
        ViewUtils.runAction(dbmNode.getDefaultAction(), this, this.viewer.getSelection());
    }

    private DBNNode getSelectedNode()
    {
        return ViewUtils.getSelectedNode(this);
    }

    public DBSDataSourceContainer getSelectedDataSourceContainer()
    {
        DBNNode selectedNode = getSelectedNode();
        if (selectedNode == null) {
            return null;
        }

        for (DBNNode curNode = selectedNode; curNode != null; curNode = curNode.getParentNode()) {
            if (curNode.getObject() instanceof DBSDataSourceContainer) {
                return (DBSDataSourceContainer)curNode.getObject();
            }
        }
        return null;
    }

    private void asyncExec(Runnable runnable)
    {
        if (!getSite().getShell().isDisposed() && !getSite().getShell().getDisplay().isDisposed()) {
            getSite().getShell().getDisplay().asyncExec(runnable);
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IPropertySheetPage.class) {
            return new PropertyPageTabbed();
        }
        return super.getAdapter(adapter);
    }
}
