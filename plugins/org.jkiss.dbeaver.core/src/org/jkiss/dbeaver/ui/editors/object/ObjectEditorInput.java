/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.object;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNTreeObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * FolderEditorInput
 */
public class ObjectEditorInput extends DatabaseEditorInput<DBNTreeObject>
{
    public ObjectEditorInput(DBNTreeObject dbmNode)
    {
        super(dbmNode);
    }

    public ImageDescriptor getImageDescriptor()
    {
        DBNTreeObject node = getTreeNode();
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