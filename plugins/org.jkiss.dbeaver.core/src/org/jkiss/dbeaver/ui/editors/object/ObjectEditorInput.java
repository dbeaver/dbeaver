/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.object;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * FolderEditorInput
 */
public class ObjectEditorInput extends DatabaseEditorInput<DBNDatabaseObject>
{
    public ObjectEditorInput(DBNDatabaseObject dbmNode)
    {
        super(dbmNode);
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        DBNDatabaseObject node = getTreeNode();
        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
        if (editorDescriptor != null) {
            return editorDescriptor.getImageDescriptor();
        } else {
            return ImageDescriptor.createFromImage(node.getNodeIconDefault());
        }
    }

    @Override
    public String getToolTipText()
    {
        return getTreeNode().getMeta().getDescription();
    }

}