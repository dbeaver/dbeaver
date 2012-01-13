/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database.load;

import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * TreeLoadService
 */
public class TreeLoadService extends DatabaseLoadService<Object[]> {

    private DBNNode parentNode;

    public TreeLoadService(String serviceName, DBNDatabaseNode parentNode)
    {
        super(serviceName, parentNode);
        this.parentNode = parentNode;
    }

    public DBNNode getParentNode() {
        return parentNode;
    }

    public Object[] evaluate()
        throws InvocationTargetException, InterruptedException
    {
        try {
            List<? extends DBNNode> children = filterNavigableChildren(
                parentNode.getChildren(getProgressMonitor()));
            return CommonUtils.isEmpty(children) ? new Object[0] : children.toArray(); 
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                throw (InvocationTargetException)ex;
            } else {
                throw new InvocationTargetException(ex);
            }
        }
    }

    public static List<? extends DBNNode> filterNavigableChildren(List<? extends DBNNode> children)
    {
        if (CommonUtils.isEmpty(children)) {
            return children;
        }
        List<DBNNode> filtered = null;
        for (int i = 0; i < children.size(); i++) {
            DBNNode node = children.get(i);
            if (node instanceof DBNDatabaseNode && !((DBNDatabaseNode) node).getMeta().isNavigable()) {
                if (filtered == null) {
                    filtered = new ArrayList<DBNNode>(children.size());
                    for (int k = 0; k < i; k++) {
                        filtered.add(children.get(k));
                    }
                }
            } else if (filtered != null) {
                filtered.add(node);
            }
        }
        return filtered == null ? children : filtered;
    }

}
