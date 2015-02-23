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
package org.jkiss.dbeaver.ui.editors.entity;

import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * EntityEditorInput
 */
public class EntityEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{
    public EntityEditorInput(DBNDatabaseNode dbmNode)
    {
        super(dbmNode);
    }

    public EntityEditorInput(DBNDatabaseNode dbnDatabaseNode, DBECommandContext commandContext)
    {
        super(dbnDatabaseNode, commandContext);
    }

    @Override
    public String getToolTipText()
    {
        StringBuilder toolTip = new StringBuilder();

        for (DBNNode node = getTreeNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBSFolder) {
                continue;
            }
            toolTip.append(node.getNodeType());
            toolTip.append(": ");
            toolTip.append(node.getNodeName());
            toolTip.append(" \n");
            if (node instanceof DBNDataSource) {
                break;
            }
        }

        return toolTip.toString();
    }

}