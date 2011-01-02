/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSWrapper;

import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements IActionFilter, DBSWrapper
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

    public final boolean isDisposed()
    {
        return model == null;
    }

    void dispose(boolean reflect)
    {
        this.model = null;
        this.parentNode = null;
    }

    public final DBNModel getModel()
    {
        return model;
    }

    public final DBNNode getParentNode()
    {
        return parentNode;
    }

    public final boolean isLocked()
    {
        return locked || parentNode != null && parentNode.isLocked();
    }

    public boolean isManagable()
    {
        return false;
    }

    public abstract Object getValueObject();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract Image getNodeIcon();

    public final Image getNodeIconDefault()
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

    void clearNode(boolean reflect)
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
        if (isLocked()) {
            log.warn("Attempt to refresh locked node '" + getNodeName() + "'");
            return null;
        }
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
        this.locked = true;
        try {
            model.fireNodeUpdate(this, this, DBNEvent.NodeChange.LOCK);

            if (DBNNode.this instanceof DBNTreeNode) {
                try {
                    ((DBNTreeNode)DBNNode.this).reloadChildren(monitor);
                } catch (DBException e) {
                    log.error(e);
                }
            }

            model.fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
        } finally {
            this.locked = false;

            // Unlock node
            model.fireNodeUpdate(this, this, DBNEvent.NodeChange.UNLOCK);
        }
        //new RefreshJob("Refresh node " + getNodeName()).schedule();
    }

    public abstract String getDefaultCommandId();

    public abstract boolean isLazyNode();

    public final boolean isChildOf(DBNNode node)
    {
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent == node) {
                return true;
            }
        }
        return false;
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
