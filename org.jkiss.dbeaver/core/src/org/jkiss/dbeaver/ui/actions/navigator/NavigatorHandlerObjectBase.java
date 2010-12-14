/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;

public abstract class NavigatorHandlerObjectBase extends AbstractHandler {

    static final Log log = LogFactory.getLog(NavigatorHandlerObjectBase.class);

    public static DBNNode getNodeByObject(DBSObject object)
    {
        DBNModel model = DBeaverCore.getInstance().getNavigatorModel();
        DBNNode node = model.findNode(object);
        if (node == null) {
            NodeLoader nodeLoader = new NodeLoader(model, object);
            try {
                DBeaverCore.getInstance().runAndWait2(nodeLoader);
            } catch (InvocationTargetException e) {
                log.warn("Could not load node for object '" + object.getName() + "'", e.getTargetException());
            } catch (InterruptedException e) {
                // do nothing
            }
            node = nodeLoader.node;
        }
        return node;
    }

    private static class NodeLoader implements DBRRunnableWithProgress {
        private final DBNModel model;
        private final DBSObject object;
        private DBNNode node;

        public NodeLoader(DBNModel model, DBSObject object)
        {
            this.model = model;
            this.object = object;
        }

        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            node = model.getNodeByObject(monitor, object, true);
        }
    }
}