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
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * Session table
 */
class SessionTable extends DatabaseObjectListControl<DBAServerSession> {

    private DBAServerSessionManager<DBAServerSession> sessionManager;

    public SessionTable(Composite parent, int style, DBAServerSessionManager<DBAServerSession> sessionManager)
    {
        super(parent, style, CONTENT_PROVIDER);
        this.sessionManager = sessionManager;
        //setFitWidth(true);
    }

    @Override
    protected LoadingJob<Collection<DBAServerSession>> createLoadService()
    {
        return RuntimeUtils.createService(
            new LoadSessionsService(),
            new ObjectsLoadVisualizer());
    }

    protected LoadingJob<Void> createAlterService(DBAServerSession session, Map<String, Object> options)
    {
        return RuntimeUtils.createService(
            new KillSessionService(session, options),
            new ObjectActionVisualizer());
    }

    public void init(DBAServerSessionManager<DBAServerSession> sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    private static IStructuredContentProvider CONTENT_PROVIDER = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

    private class LoadSessionsService extends DatabaseLoadService<Collection<DBAServerSession>> {

        protected LoadSessionsService()
        {
            super("Load sessions", sessionManager.getDataSource());
        }

        @Override
        public Collection<DBAServerSession> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = sessionManager.getDataSource().openIsolatedContext(getProgressMonitor(), "View sessions")) {
                    try (DBCSession session = isolatedContext.openSession(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Retrieve server sessions")) {
                        return sessionManager.getSessions(session, null);
                    }
                }
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

    private class KillSessionService extends DatabaseLoadService<Void> {
        private final DBAServerSession session;
        private final Map<String, Object> options;

        protected KillSessionService(DBAServerSession session, Map<String, Object> options)
        {
            super("Kill session", sessionManager.getDataSource());
            this.session = session;
            this.options = options;
        }

        @Override
        public Void evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = sessionManager.getDataSource().openIsolatedContext(getProgressMonitor(), "View sessions")) {
                    try (DBCSession session = isolatedContext.openSession(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Kill server session")) {
                        sessionManager.alterSession(session, this.session, options);
                        return null;
                    }
                }
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }

    }

}
