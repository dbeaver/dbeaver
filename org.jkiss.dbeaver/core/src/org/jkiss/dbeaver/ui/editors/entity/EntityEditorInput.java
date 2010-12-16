/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * EntityEditorInput
 */
public class EntityEditorInput extends DatabaseEditorInput<DBNNode>
{
    private DBOManager<?> objectManager;

    public EntityEditorInput(DBNNode dbmNode)
    {
        super(dbmNode);
    }

    public EntityEditorInput(DBNNode dbnNode, DBOManager<?> objectManager)
    {
        super(dbnNode);
        this.objectManager = objectManager;
    }

    public DBOManager<?> getObjectManager()
    {
        return objectManager;
    }

    public String getToolTipText()
    {
        DBNNode node = getTreeNode();
        //setPageText(index, );
        StringBuilder toolTip = new StringBuilder();
        if (node instanceof DBNTreeNode) {
            toolTip.append(((DBNTreeNode)node).getMeta().getItemLabel()).append(" ");
        }
        toolTip.append(node.getNodeName());
        if (!CommonUtils.isEmpty(node.getNodeDescription())) {
            toolTip.append("\n").append(node.getNodeDescription());
        }
        return toolTip.toString();
    }

}