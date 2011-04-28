/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
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

    public String getToolTipText()
    {
        DBNNode node = getTreeNode();
        //setPageText(index, );
        StringBuilder toolTip = new StringBuilder();
        if (node instanceof DBNDatabaseNode) {
            toolTip.append(((DBNDatabaseNode)node).getMeta().getItemLabel()).append(" ");
        }
        toolTip.append(node.getNodeName());
        if (!CommonUtils.isEmpty(node.getNodeDescription())) {
            toolTip.append("\n").append(node.getNodeDescription());
        }
        return toolTip.toString();
    }

}