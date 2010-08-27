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
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshableView;
import org.jkiss.dbeaver.model.meta.DBMDataSource;
import org.jkiss.dbeaver.model.meta.DBMEvent;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.IDBMListener;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.DriverManagerAction;
import org.jkiss.dbeaver.ui.actions.LinkEditorAction;
import org.jkiss.dbeaver.ui.actions.NewConnectionAction;
import org.jkiss.dbeaver.ui.actions.RefreshTreeAction;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.utils.ViewUtils;

public class DatabaseNavigatorView extends ViewPart
    implements IDBMListener, IMetaModelView, IRefreshableView, IDoubleClickListener
{
    static final Log log = LogFactory.getLog(DatabaseNavigatorView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseNavigator";

    private TreeViewer viewer;
    private DBMModel model;
    private RefreshTreeAction refreshAction;

    public DatabaseNavigatorView()
    {
        super();
        model = DBeaverCore.getInstance().getMetaModel();
        model.addListener(this);
    }

    /**
     * We will set up a dummy model to initialize tree heararchy. In real
     * code, you will connect to a real model and expose its hierarchy.
     */
    public DBMModel getMetaModel()
    {
        return model;
    }

    public TreeViewer getViewer()
    {
        return viewer;
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public IAction getRefreshAction()
    {
        return refreshAction;
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
        this.viewer.setInput(getMetaModel().getRoot());
        this.viewer.addSelectionChangedListener(
            new ISelectionChangedListener()
            {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
                    if (!structSel.isEmpty()) {
                        Object object = structSel.getFirstElement();
                        if (object instanceof DBSObject) {
                            String desc = ((DBSObject)object).getDescription();
                            if (CommonUtils.isEmpty(desc)) {
                                desc = ((DBSObject)object).getName();
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

        // Add refresh action binding
        refreshAction = new RefreshTreeAction(this);
        refreshAction.setEnabled(true);

        IActionBars actionBars = getViewSite().getActionBars();
        actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

        actionBars.updateActionBars();

        // Make toolbar
        makeToolbar(actionBars);
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
        {
            IAction driverManagerAction = ViewUtils.makeAction(new LinkEditorAction(), this, null, "Link with Editor", DBIcon.ACTION_LINK_TO_EDITOR.getImageDescriptor(), null);
            dropDownMenu.add(driverManagerAction);
            toolBar.add(driverManagerAction);
        }

        dropDownMenu.add(refreshAction);
        toolBar.add(refreshAction);
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

    public void nodeChanged(final DBMEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE:
                if (event.getNode() instanceof DBMDataSource) {
                    asyncExec(new Runnable() { public void run() {
                        if (!viewer.getControl().isDisposed()) {
                            viewer.refresh();
                        }
                    }});
                }
                break;
            case REFRESH:
                asyncExec(new Runnable() { public void run() {
                    if (!viewer.getControl().isDisposed()) {
                        DBSObject nodeObject = event.getNode().getObject();
                        if (nodeObject != null) {
                            switch (event.getNodeChange()) {
                                case LOADED:
                                    viewer.expandToLevel(nodeObject, 1);
                                    viewer.refresh(nodeObject);
                                    break;
                                case UNLOADED:
                                    viewer.collapseToLevel(nodeObject, -1);
                                    viewer.refresh(nodeObject);
                                    break;
                                case CHANGED:
                                    getViewer().update(nodeObject, null);
                                    break;
                                case REFRESH:
                                    viewer.refresh(nodeObject);
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
        DBMNode dbmNode = getSelectedNode();
        if (dbmNode == null) {
            return;
        }
        ViewUtils.runAction(dbmNode.getDefaultAction(), this, this.viewer.getSelection());
    }

    private DBMNode getSelectedNode()
    {
        return ViewUtils.getSelectedNode(this);
    }

    public DBSDataSourceContainer getSelectedDataSourceContainer()
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode == null) {
            return null;
        }

        for (DBMNode curNode = selectedNode; curNode != null; curNode = curNode.getParentNode()) {
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
