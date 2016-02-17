/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

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