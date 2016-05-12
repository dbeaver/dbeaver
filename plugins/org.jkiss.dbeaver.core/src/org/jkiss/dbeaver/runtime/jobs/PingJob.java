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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;

/**
 * PingJob
 */
public class PingJob extends AbstractJob
{
    private static final Log log = Log.getLog(PingJob.class);

    private final DBPDataSource dataSource;

    public PingJob(DBPDataSource dataSource)
    {
        super("Connection ping");
        setUser(false);
        setSystem(true);
        this.dataSource = dataSource;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        log.debug("Ping connection " + dataSource.getContainer().getId());
        for (final DBCExecutionContext context : new ArrayList<>(dataSource.getAllContexts())) {
            try {
                context.isContextAlive(monitor);
            } catch (Exception e) {
                log.debug("Context [" + dataSource.getName() + "::" + context.getContextName() + "] check failed: " + e.getMessage());
            }
        }
        return Status.OK_STATUS;
    }

}