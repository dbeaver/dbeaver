/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.registry.tree.DBXTreeObject;
import org.jkiss.dbeaver.ui.actions.OpenObjectEditorAction;
import net.sf.jkiss.utils.CommonUtils;

/**
 * DBMTreeItem
 */
public class DBMTreeObject extends DBMTreeNode implements DBSObject
{
    private DBXTreeObject meta;
    private IEditorDescriptor editorDescriptor;
    private Image image;

    DBMTreeObject(DBMNode parent, DBXTreeObject meta)
    {
        super(parent);
        this.meta = meta;
        if (this.getModel() != null) {
            this.getModel().addNode(this);
        }
        this.editorDescriptor = DBeaverCore.getActiveWorkbenchWindow().getWorkbench().getEditorRegistry().findEditor(meta.getEditorId());
        if (this.editorDescriptor != null) {
            this.image = this.editorDescriptor.getImageDescriptor().createImage();
        }
    }

    protected void dispose()
    {
        if (this.getModel() != null) {
            this.getModel().removeNode(this);
        }
        super.dispose();
    }

    public IEditorDescriptor getEditorDescriptor()
    {
        return editorDescriptor;
    }

    public DBXTreeObject getMeta()
    {
        return meta;
    }

    public DBSObject getObject()
    {
        return this;
    }

    public Object getValueObject()
    {
        return this;
    }

    public Image getNodeIcon()
    {
        if (image != null) {
            return image;
        }
        return super.getNodeIcon();
    }

    public String getNodePathName()
    {
        StringBuilder pathName = new StringBuilder();
        for (DBMNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent instanceof DBMTreeFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
            if (!CommonUtils.isEmpty(parentName)) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '.');
                }
                pathName.insert(0, parentName);
            }
        }
        pathName.insert(0, getNodeName() + " (");
        pathName.append(")");
        return pathName.toString();
    }


    public DBMNode refreshNode(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor);
        } else {
            return null;
        }
    }

    public Class<OpenObjectEditorAction> getDefaultAction()
    {
        return OpenObjectEditorAction.class;
    }

    public String getName()
    {
        return meta.getLabel();
    }

    public String getDescription()
    {
        return meta.getDescription();
    }

    public DBSObject getParentObject()
    {
        return getParentNode().getObject();
    }

    public DBPDataSource getDataSource()
    {
        return getParentObject().getDataSource();
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }
}