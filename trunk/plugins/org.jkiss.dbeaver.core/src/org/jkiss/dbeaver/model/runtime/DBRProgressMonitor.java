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

package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Database progress monitor.
 * Similar to IProgressMonitor but with DBP specific features
 */
public interface DBRProgressMonitor {

    /**
     * Obtains eclipse progress monitor.
     * Can be used to pass to eclipse API.
     * @return
     */
    public IProgressMonitor getNestedMonitor();

    public void beginTask(String name, int totalWork);

    public void done();

    public void subTask(String name);

    public void worked(int work);

    public boolean isCanceled();

    public void startBlock(DBRBlockingObject object, String taskName);

    public void endBlock();

    public DBRBlockingObject getActiveBlock();

}
