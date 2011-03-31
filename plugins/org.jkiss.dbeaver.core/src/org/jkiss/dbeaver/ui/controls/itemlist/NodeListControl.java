/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * NodeListControl
 */
public abstract class NodeListControl extends ObjectListControl<DBNNode> implements INavigatorModelView,IDBNListener
{
    //static final Log log = LogFactory.getLog(NodeListControl.class);

    private IWorkbenchPart workbenchPart;
    private DBNNode node;
    private DBXTreeNode nodeMeta;

    public NodeListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node,
        DBXTreeNode nodeMeta)
    {
        super(parent, style, createContentProvider(node, nodeMeta));
        this.workbenchPart = workbenchPart;
        this.node = node;
        this.nodeMeta = nodeMeta;

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

        DBeaverCore.getInstance().getNavigatorModel().addListener(this);
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return workbenchPart;
    }

    @Override
    public void dispose()
    {
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);
        super.dispose();
    }

    private static IContentProvider createContentProvider(DBNNode node, DBXTreeNode metaNode)
    {
        if (node instanceof DBNDatabaseNode) {
            final DBNDatabaseNode dbNode = (DBNDatabaseNode) node;
            if (metaNode == null) {
                metaNode = dbNode.getMeta();
            }
            final List<DBXTreeNode> inlineMetas = collectInlineMetas(dbNode, metaNode);

            if (!inlineMetas.isEmpty()) {
                return new TreeContentProvider() {
                    @Override
                    public boolean hasChildren(Object parentElement)
                    {
                        if (parentElement instanceof DBNDatabaseNode) {
                            return ((DBNDatabaseNode) parentElement).hasChildren();
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public Object[] getChildren(Object parentElement)
                    {
                        if (parentElement instanceof DBNDatabaseNode) {
                            try {
                                // Read children with void progress monitor because inline children SHOULD be already cached
                                List<DBNDatabaseNode> children = ((DBNDatabaseNode) parentElement).getChildren(VoidProgressMonitor.INSTANCE);
                                if (CommonUtils.isEmpty(children)) {
                                    return null;
                                } else {
                                    return children.toArray();
                                }
                            } catch (DBException e) {
                                log.error(e);
                            }
                        }
                        return null;
                    }
                };
            }
        }
        return new ListContentProvider();
    }

    private static List<DBXTreeNode> collectInlineMetas(DBNDatabaseNode node, DBXTreeNode meta)
    {
        final List<DBXTreeNode> inlineMetas = new ArrayList<DBXTreeNode>();

        if (meta instanceof DBXTreeFolder) {
            // If this is a folder - iterate through all its children
            for (DBXTreeNode metaChild : meta.getChildren(node)) {
                collectInlineChildren(metaChild, inlineMetas);
            }

        } else {
            // Just check child metas
            collectInlineChildren(meta, inlineMetas);
        }
        return inlineMetas;
    }

    private static void collectInlineChildren(DBXTreeNode meta, List<DBXTreeNode> inlineMetas)
    {
        final List<DBXTreeNode> metaChildren = meta.getChildren(null);
        if (!CommonUtils.isEmpty(metaChildren)) {
            for (DBXTreeNode child : metaChildren) {
                if (child.isInline()) {
                    inlineMetas.add(child);
                }
            }
        }
    }

    protected Class<?>[] getListBaseTypes()
    {
        List<Class<?>> baseTypes;
        // Collect base types for root node
        if (getRootNode() instanceof DBNDatabaseNode) {
            DBNDatabaseNode dbNode = (DBNDatabaseNode) getRootNode();
            baseTypes = dbNode.getChildrenTypes();
        } else {
            baseTypes = null;
        }

        // Collect base types for inline children
        return baseTypes == null || baseTypes.isEmpty() ? null : baseTypes.toArray(new Class<?>[baseTypes.size()]);
    }

    public Viewer getNavigatorViewer()
    {
        return getItemsViewer();
    }

    public DBNNode getRootNode() {
        return node;
    }

    public DBXTreeNode getNodeMeta()
    {
        return nodeMeta;
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
                NavigatorHandlerObjectOpen.openEntityEditor(node, null, getWorkbenchPart().getSite().getWorkbenchWindow());
            }
        }
    }

    public void nodeChanged(final DBNEvent event)
    {
        if (isDisposed()) {
            return;
        }
        if (event.getNode().isChildOf(getRootNode())) {
            if (event.getAction() != DBNEvent.Action.UPDATE) {
                // Add or remove - just reload list content
                loadData();
            } else {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run()
                    {
                        getItemsViewer().update(event.getNode(), null);
                    }
                });
            }
        }
    }

}