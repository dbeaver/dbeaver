/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.tree;

import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;

/**
 * TreeLoadService
 */
public class TreeLoadService extends AbstractLoadService<Object[]> {

    private DBSObject parent;
    private DBMNode parentNode;

    public TreeLoadService(String serviceName, DBSObject parent, DBMNode parentNode)
    {
        super(serviceName);
        this.parent = parent;
        this.parentNode = parentNode;
    }

    public DBSObject getParent()
    {
        return parent;
    }

    public Object[] evaluate()
        throws InvocationTargetException, InterruptedException
    {
        try {
            return DBMNode.convertNodesToObjects(
                parentNode.getChildren(getProgressMonitor()));
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                throw (InvocationTargetException)ex;
            } else {
                throw new InvocationTargetException(ex);
            }
        }
    }

}
