/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.object;

import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * ObjectEditorInput
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
        DBNDatabaseObject node = getNavigatorNode();
//        IEditorDescriptor editorDescriptor = node.getEditorDescriptor();
//        if (editorDescriptor != null) {
//            return editorDescriptor.getImageDescriptor();
//        } else {
            return DBeaverIcons.getImageDescriptor(node.getNodeIconDefault());
//        }
    }

    @Override
    public String getToolTipText()
    {
        return getNavigatorNode().getMeta().getDescription();
    }

}