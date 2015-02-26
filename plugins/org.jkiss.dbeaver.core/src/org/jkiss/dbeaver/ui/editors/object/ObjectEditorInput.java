/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
//        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
//        if (editorDescriptor != null) {
//            return editorDescriptor.getImageDescriptor();
//        } else {
            return ImageDescriptor.createFromImage(node.getNodeIconDefault());
//        }
    }

    @Override
    public String getToolTipText()
    {
        return getTreeNode().getMeta().getDescription();
    }

}