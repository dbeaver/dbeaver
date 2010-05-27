/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;
import org.jkiss.dbeaver.model.struct.DBSUtils;

public class SetActiveObjectAction extends NavigatorAction
{
    static Log log = LogFactory.getLog(SetActiveObjectAction.class);

    public void run(IAction action)
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode != null) {
            DBSStructureContainerActive activeContainer = DBSUtils.queryParentInterface(
                DBSStructureContainerActive.class, selectedNode.getObject());
            try {
                activeContainer.setActiveChild(selectedNode.getObject());
            }
            catch (DBException e) {
                log.error("Could not set active object", e);
            }
        }
    }

}