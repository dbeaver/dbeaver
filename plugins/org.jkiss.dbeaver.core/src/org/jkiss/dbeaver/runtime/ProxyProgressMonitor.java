/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor default implementation
 */
public class ProxyProgressMonitor implements DBRProgressMonitor {

    private DBRProgressMonitor original;

    public ProxyProgressMonitor(DBRProgressMonitor original)
    {
        this.original = original;
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return original.getNestedMonitor();
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        original.beginTask(name, totalWork);
    }

    @Override
    public void done()
    {
        original.done();
    }

    @Override
    public void subTask(String name)
    {
        original.subTask(name);
    }

    @Override
    public void worked(int work)
    {
        original.worked(work);
    }

    @Override
    public boolean isCanceled()
    {
        return original.isCanceled();
    }

    @Override
    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        original.startBlock(object, taskName);
    }

    @Override
    public synchronized void endBlock()
    {
        original.endBlock();
    }

    @Override
    public DBRBlockingObject getActiveBlock()
    {
        return original.getActiveBlock();
    }

}
