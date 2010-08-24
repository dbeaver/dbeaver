/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;

public class SetActiveObjectAction extends NavigatorAction
{
    static final Log log = LogFactory.getLog(SetActiveObjectAction.class);

    public void run(IAction action)
    {
        final DBMNode selectedNode = getSelectedNode();
        if (selectedNode != null) {
            final DBSEntitySelector activeContainer = DBUtils.queryParentInterface(
                DBSEntitySelector.class, selectedNode.getObject());
            DBeaverCore.getInstance().runAndWait(true, true, new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        activeContainer.setActiveChild(monitor, selectedNode.getObject());
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
    }

}