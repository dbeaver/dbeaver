/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database.load;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * TreeLoadService
 */
public class TreeLoadService extends DatabaseLoadService<Object[]> {

    private DBNNode parentNode;

    public TreeLoadService(String serviceName, DBNNode parentNode)
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
            List<? extends DBNNode> children = parentNode.getChildren(getProgressMonitor());
            return CommonUtils.isEmpty(children) ? new Object[0] : children.toArray(); 
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                throw (InvocationTargetException)ex;
            } else {
                throw new InvocationTargetException(ex);
            }
        }
    }

}
