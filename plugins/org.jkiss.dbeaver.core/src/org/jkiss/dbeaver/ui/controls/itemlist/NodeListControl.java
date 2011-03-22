/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * NodeListControl
 */
public abstract class NodeListControl extends ObjectListControl<DBNNode> implements INavigatorModelView
{
    //static final Log log = LogFactory.getLog(NodeListControl.class);

    private DBNNode node;
    private IWorkbenchPart workbenchPart;

    public NodeListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node)
    {
        super(parent, style);
        this.workbenchPart = workbenchPart;
        this.node = node;

        ViewUtils.addContextMenu(workbenchPart, getItemsViewer());

        setDoubleClickHandler(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event)
            {
                // Run default node action
                DBNNode dbmNode = ViewUtils.getSelectedNode(getItemsViewer());
                if (dbmNode == null) {
                    return;
                }
                ViewUtils.runCommand(dbmNode.getDefaultCommandId(), workbenchPart);
            }
        });
    }

    @Override
    protected IContentProvider createContentProvider()
    {
        return new ListContentProvider();
    }

    public Viewer getNavigatorViewer()
    {
        return getItemsViewer();
    }

    public DBNNode getRootNode() {
        return node;
    }

    @Override
    protected Object getObjectValue(DBNNode item)
    {
        return item instanceof DBSWrapper ? ((DBSWrapper)item).getObject() : item;
    }

    @Override
    protected Image getObjectImage(DBNNode item)
    {
        return item.getNodeIconDefault();
    }

    @Override
    protected boolean isHyperlink(Object cellValue)
    {
        Object ownerObject = null;
        if (node instanceof DBNDatabaseNode) {
            ownerObject = ((DBNDatabaseNode)node).getValueObject();
        }
        return cellValue instanceof DBSObject && cellValue != ownerObject;
    }

    protected void navigateHyperlink(Object cellValue)
    {
        if (cellValue instanceof DBSObject) {
            DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject((DBSObject) cellValue);
            if (node != null) {
                NavigatorHandlerObjectOpen.openEntityEditor(node, null, workbenchPart.getSite().getWorkbenchWindow());
            }
        }
    }

}