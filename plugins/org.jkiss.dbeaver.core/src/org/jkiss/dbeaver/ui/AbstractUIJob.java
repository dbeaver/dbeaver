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
package org.jkiss.dbeaver.ui;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

/**
 * Abstract Database Job
 */
public abstract class AbstractUIJob extends UIJob
{
    static protected final Log log = Log.getLog(AbstractUIJob.class);

    protected AbstractUIJob(String name)
    {
        super(name);
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor)
    {
        return this.runInUIThread(RuntimeUtils.makeMonitor(monitor));
    }

    protected abstract IStatus runInUIThread(DBRProgressMonitor monitor);

}