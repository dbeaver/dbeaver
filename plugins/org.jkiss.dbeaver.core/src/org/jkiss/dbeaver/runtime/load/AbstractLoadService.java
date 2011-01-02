/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public abstract class AbstractLoadService<RESULT> implements ILoadService<RESULT> {
    private String serviceName;
    private DBRProgressMonitor progressMonitor;

    protected AbstractLoadService(String serviceName)
    {
        this.serviceName = serviceName;
    }

    protected AbstractLoadService()
    {
        this("Loading");
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public DBRProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    public void setProgressMonitor(DBRProgressMonitor monitor)
    {
        this.progressMonitor = monitor;
    }

    public boolean cancel() throws InvocationTargetException
    {
        // Invoke nested service cancel
        DBRBlockingObject block = progressMonitor.getActiveBlock();
        if (block != null) {
            try {
                block.cancelBlock();
                return true;
            }
            catch (DBException e) {
                throw new InvocationTargetException(e, "Could not cancel blocking object");
            }
        }
        return false;
    }

}