/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.controls.ObjectEditorHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private ObjectEditorHandler objectEditorHandler;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node,
        DBXTreeNode metaNode)
    {
        super(parent, style, workbenchPart, node, metaNode);
    }

    @Override
    public void dispose()
    {
        if (objectEditorHandler != null) {
            objectEditorHandler.dispose();
            objectEditorHandler = null;
        }
        super.dispose();
    }

    @Override
    protected Composite createProgressPanel(Composite container)
    {
        Composite panel = super.createProgressPanel(container);

        if (getWorkbenchPart() instanceof IDatabaseObjectEditor) {
            objectEditorHandler = new ObjectEditorHandler((IDatabaseObjectEditor)getWorkbenchPart());
            objectEditorHandler.createEditorControls(panel);
        }

        return panel;
    }

    @Override
    protected LoadingJob<Collection<DBNNode>> createLoadService()
    {
        return LoadingUtils.createService(
            new ItemLoadService(getNodeMeta()),
            new ObjectsLoadVisualizer());
    }

    private class ItemLoadService extends DatabaseLoadService<Collection<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items", getRootNode() instanceof DBSWrapper ? (DBSWrapper)getRootNode() : null);
            this.metaNode = metaNode;
        }

        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBNNode> items = new ArrayList<DBNNode>();
                List<? extends DBNNode> children = getRootNode().getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
                    if (item instanceof DBNDatabaseFolder) {
                        continue;
                    }
                    if (metaNode != null) {
                        if (!(item instanceof DBNDatabaseNode)) {
                            continue;
                        }
                        if (((DBNDatabaseNode)item).getMeta() != metaNode) {
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
