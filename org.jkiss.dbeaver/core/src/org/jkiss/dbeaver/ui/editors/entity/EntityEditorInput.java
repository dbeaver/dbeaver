/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeNode;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * EntityEditorInput
 */
public class EntityEditorInput extends DatabaseEditorInput<DBMNode>
{
    public EntityEditorInput(DBMNode dbmNode)
    {
        super(dbmNode);
    }

    public String getToolTipText()
    {
        DBMNode node = getTreeNode();
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

}