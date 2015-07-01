/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBRProgressMonitor {

    static final Log log = Log.getLog(DefaultProgressMonitor.class);

    private IProgressMonitor nestedMonitor;
    private List<DBRBlockingObject> blocks = null;

    public DefaultProgressMonitor(IProgressMonitor nestedMonitor)
    {
        this.nestedMonitor = nestedMonitor;
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return nestedMonitor;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        nestedMonitor.beginTask(name, totalWork);
    }

    @Override
    public void done()
    {
        nestedMonitor.done();
    }

    @Override
    public void subTask(String name)
    {
        nestedMonitor.subTask(name);
    }

    @Override
    public void worked(int work)
    {
        nestedMonitor.worked(work);
    }

    @Override
    public boolean isCanceled()
    {
        return nestedMonitor.isCanceled();
    }

    @Override
    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        if (taskName != null) {
            subTask(taskName);
        }
        if (blocks == null) {
            blocks = new ArrayList<DBRBlockingObject>();
        }
        blocks.add(object);
    }

    @Override
    public synchronized void endBlock()
    {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("End block invoked while no blocking objects are in stack"); //$NON-NLS-1$
            return;
        }
        //if (blocks.size() == 1) {
        //    this.done();
        //}
        blocks.remove(blocks.size() - 1);
    }

    @Override
    public synchronized DBRBlockingObject getActiveBlock()
    {
        return blocks == null || blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
    }

}
