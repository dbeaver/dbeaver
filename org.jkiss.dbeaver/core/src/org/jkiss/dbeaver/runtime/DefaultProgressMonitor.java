/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBRProgressMonitor {

    static Log log = LogFactory.getLog(DefaultProgressMonitor.class);

    private IProgressMonitor nestedMonitor;
    private List<DBRBlockingObject> blocks = null;

    public DefaultProgressMonitor(IProgressMonitor nestedMonitor)
    {
        this.nestedMonitor = nestedMonitor;
    }

    public IProgressMonitor getNestedMonitor()
    {
        return nestedMonitor;
    }

    public void beginTask(String name, int totalWork)
    {
        nestedMonitor.beginTask(name, totalWork);
    }

    public void done()
    {
        nestedMonitor.done();
    }

    public void subTask(String name)
    {
        nestedMonitor.subTask(name);
    }

    public void worked(int work)
    {
        nestedMonitor.worked(work);
    }

    public boolean isCanceled()
    {
        return nestedMonitor.isCanceled();
    }

    public synchronized void startBlock(DBRBlockingObject object)
    {
        if (blocks == null) {
            blocks = new ArrayList<DBRBlockingObject>();
        }
        blocks.add(object);
    }

    public synchronized void endBlock()
    {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("End block invoked while no blocking objects are in stack");
        }
    }

    public DBRBlockingObject getActiveBlock()
    {
        return blocks == null || blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
    }

}
