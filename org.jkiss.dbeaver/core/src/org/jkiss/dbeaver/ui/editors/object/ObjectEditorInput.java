/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.object;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMTreeObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * FolderEditorInput
 */
public class ObjectEditorInput implements IDatabaseEditorInput
{
    private DBMTreeObject node;
    private String defaultPageId;

    public ObjectEditorInput(DBMTreeObject node)
    {
        this.node = node;
    }

    public DBMTreeObject getNode()
    {
        return node;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
        if (editorDescriptor != null) {
            return editorDescriptor.getImageDescriptor();
        } else {
            return ImageDescriptor.createFromImage(node.getNodeIconDefault());
        }
    }

    public String getName()
    {
        return node.getNodePathName();
    }

    public IPersistableElement getPersistable()
    {
        return null;
    }

    public String getToolTipText()
    {
        return node.getMeta().getDescription();
    }

    public Object getAdapter(Class adapter)
    {
        return null;
    }

    public DBMModel getModel()
    {
        return node.getModel();
    }

    public DBSObject getDatabaseObject()
    {
        return node.getObject();
    }

    public String getDefaultPageId()
    {
        return defaultPageId;
    }

    public void setDefaultPageId(String defaultPageId)
    {
        this.defaultPageId = defaultPageId;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj == this ||
            (obj instanceof ObjectEditorInput && ((ObjectEditorInput)obj).node.equals(node));
    }
}