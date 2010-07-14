/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.object;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.dbeaver.model.meta.DBMTreeObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * FolderEditorInput
 */
public class ObjectEditorInput extends DatabaseEditorInput<DBMTreeObject>
{
    public ObjectEditorInput(DBMTreeObject dbmNode)
    {
        super(dbmNode);
    }

    public ImageDescriptor getImageDescriptor()
    {
        DBMTreeObject node = getTreeNode();
        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
        if (editorDescriptor != null) {
            return editorDescriptor.getImageDescriptor();
        } else {
            return ImageDescriptor.createFromImage(node.getNodeIconDefault());
        }
    }

    public String getToolTipText()
    {
        return getTreeNode().getMeta().getDescription();
    }

}