/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * FolderEditorInput
 */
public class EntityEditorInput implements IDatabaseEditorInput
{
    private DBMNode node;
    private String defaultPageId;

    public EntityEditorInput(DBMNode node)
    {
        this.node = node;
    }

    public DBMNode getNode()
    {
        return node;
    }

    public boolean exists()
    {
        return false;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return ImageDescriptor.createFromImage(node.getNodeIconDefault());
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
        //setPageText(index, );
        StringBuilder toolTip = new StringBuilder();
        if (node instanceof DBMTreeNode) {
            toolTip.append(((DBMTreeNode)node).getMeta().getLabel()).append(" ");
        }
        toolTip.append(node.getNodeName());
        if (!CommonUtils.isEmpty(node.getNodeDescription())) {
            toolTip.append("\n").append(node.getNodeDescription());
        }
        return toolTip.toString();
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
            (obj instanceof EntityEditorInput && ((EntityEditorInput)obj).node.equals(node));
    }
}