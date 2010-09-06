/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.AbstractJob;

import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements IActionFilter
{
    static final Log log = LogFactory.getLog(DBNNode.class);

    private DBNModel model;
    private DBNNode parentNode;
    private boolean locked;

    protected DBNNode(DBNModel model)
    {
        this.model = model;
        this.parentNode = null;
    }

    protected DBNNode(DBNNode parentNode)
    {
        this.model = parentNode.getModel();
        this.parentNode = parentNode;
    }

    public boolean isDisposed()
    {
        return model == null;
    }

    void dispose()
    {
        this.model = null;
        this.parentNode = null;
    }

    public DBNModel getModel()
    {
        return model;
    }

    public DBNNode getParentNode()
    {
        return parentNode;
    }

    void setParentNode(DBNNode parentNode)
    {
        this.parentNode = parentNode;
    }

    public boolean isLocked() {
        return locked || parentNode != null && parentNode.isLocked();
    }

    public abstract DBSObject getObject();

    public abstract Object getValueObject();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract Image getNodeIcon();

    public Image getNodeIconDefault()
    {
        Image image = getNodeIcon();
        if (image == null) {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
            if (this.hasChildren()) {
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            }
            image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
        return image;
    }

    public String getNodePathName()
    {
        StringBuilder pathName = new StringBuilder();
        pathName.append(getNodeName());
        for (DBNNode parent = getParentNode(); parent != null && !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            if (parent instanceof DBNTreeFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
            if (!CommonUtils.isEmpty(parentName)) {
                pathName.insert(0, '.').insert(0, parentName);
            }
        }
        return pathName.toString();
    }

    public abstract boolean hasChildren();

    public abstract boolean hasNavigableChildren();
    
    public abstract List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)  throws DBException;

    void clearNode()
    {

    }

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually changes navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     * @param monitor progress monitor
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public DBNNode refreshNode(DBRProgressMonitor monitor) throws DBException
    {
        if (getObject() instanceof DBSEntity && ((DBSEntity)getObject()).refreshEntity(monitor)) {
            refreshNodeContent(monitor);
            return this;
        } else if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor);
        } else {
            return null;
        }
    }

    private void refreshNodeContent(final DBRProgressMonitor monitor)
        throws DBException
    {
        if (model == null) {
            return;
        }
        new RefreshJob("Refresh node " + getNodeName()).schedule();
    }

    public abstract Class<? extends IActionDelegate> getDefaultAction();

    public abstract boolean isLazyNode();

    public static Object[] convertNodesToObjects(List<? extends DBNNode> children)
    {
        if (CommonUtils.isEmpty(children)) {
            return new Object[0];
        }
        Object[] result = new Object[children.size()];
        for (int i = 0; i < children.size(); i++) {
            DBNNode child = children.get(i);
            result[i] = child.getObject();
        }
        return result;
    }

    private class RefreshJob extends AbstractJob {

        protected RefreshJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            DBNNode node = DBNNode.this;
            // Lock node and it's children
            node.locked = true;
            try {
                model.fireNodeUpdate(this, node, DBNEvent.NodeChange.LOCK);

                if (DBNNode.this instanceof DBNTreeNode) {
                    try {
                        ((DBNTreeNode)DBNNode.this).reloadChildren(monitor);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }

                model.fireNodeUpdate(this, node, DBNEvent.NodeChange.REFRESH);
            } finally {
                node.locked = false;

                // Unlock node
                model.fireNodeUpdate(this, node, DBNEvent.NodeChange.UNLOCK);
            }

            return Status.OK_STATUS;
        }
    }

    public boolean testAttribute(Object target, String name, String value) {
        if (name.equals("targetType")) {
            try {
                Class<?> targetClass = Class.forName(value);
                return targetClass.isAssignableFrom(getObject().getClass());
            } catch (ClassNotFoundException e) {
                log.warn("Unknown target type: " + value);
            }
        }
        return false;
    }
}
