/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode
{
    static final Log log = LogFactory.getLog(DBNNode.class);

    private DBNModel model;
    private DBNNode parentNode;

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
    public abstract DBNNode refreshNode(DBRProgressMonitor monitor) throws DBException;

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
}
