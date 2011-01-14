/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeObject;
import org.jkiss.dbeaver.ui.ICommandIds;

/**
 * DBNDatabaseObject
 */
public class DBNDatabaseObject extends DBNDatabaseNode implements DBSObject
{
    private DBXTreeObject meta;
    private IEditorDescriptor editorDescriptor;
    private Image image;

    DBNDatabaseObject(DBNNode parent, DBXTreeObject meta)
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

    protected void dispose(boolean reflect)
    {
        if (image != null) {
            image.dispose();
            image = null;
        }
        if (this.getModel() != null) {
            this.getModel().removeNode(this, reflect);
        }
        super.dispose(reflect);
    }

    public IEditorDescriptor getEditorDescriptor()
    {
        return editorDescriptor;
    }

    public DBXTreeObject getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        // do nothing
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
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
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


    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_OBJECT_OPEN;
    }

    @Property(name = "Name", viewable = true, order = 1)
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
        return getParentNode() instanceof DBNDatabaseNode ? ((DBSWrapper)getParentNode()).getObject() : null;
    }

    public DBPDataSource getDataSource()
    {
        DBSObject parentObject = getParentObject();
        return parentObject == null ? null : parentObject.getDataSource();
    }

    public boolean isPersisted()
    {
        return true;
    }

}