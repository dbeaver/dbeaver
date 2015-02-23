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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Progress monitor null implementation
 */
public class VoidProgressMonitor implements DBRProgressMonitor {

    public static final VoidProgressMonitor INSTANCE = new VoidProgressMonitor();

    private static final IProgressMonitor NESTED_INSTANCE = new NullProgressMonitor();

    protected VoidProgressMonitor() {
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return NESTED_INSTANCE;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
    }

    @Override
    public void done()
    {
    }

    @Override
    public void subTask(String name)
    {
    }

    @Override
    public void worked(int work)
    {
    }

    @Override
    public boolean isCanceled()
    {
        return false;
    }

    @Override
    public void startBlock(DBRBlockingObject object, String taskName)
    {
        // do nothing
    }

    @Override
    public void endBlock()
    {
        // do nothing
    }

    @Override
    public DBRBlockingObject getActiveBlock()
    {
        return null;
    }

}