/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    static final Log log = LogFactory.getLog(ItemListControl.class);

    private DBNNode node;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node)
    {
        super(parent, style, workbenchPart, node);
    }

    public void fillData()
    {
        this.fillData(null);
    }

    public void fillData(DBXTreeNode metaNode)
    {
        LoadingJob<Collection<DBNNode>> loadingJob = LoadingUtils.createService(
            new ItemLoadService(metaNode),
            new ObjectsLoadVisualizer());
        super.loadData(loadingJob);
    }

    private class ItemLoadService extends DatabaseLoadService<Collection<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items", node);
            this.metaNode = metaNode;
        }

        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBNNode> items = new ArrayList<DBNNode>();
                List<? extends DBNNode> children = node.getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
                    if (item instanceof DBNTreeFolder) {
                        continue;
                    }
                    if (metaNode != null) {
                        if (!(item instanceof DBNTreeNode)) {
                            continue;
                        }
                        if (((DBNTreeNode)item).getMeta() != metaNode) {
                            continue;
                        }
                    }
                    items.add(item);
                }
                return items;
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }


}
