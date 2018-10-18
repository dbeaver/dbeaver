/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Session table
 */
class SessionTable<SESSION_TYPE extends DBAServerSession> extends DatabaseObjectListControl<SESSION_TYPE> {

    private static final Log log = Log.getLog(SessionTable.class);

    private DBAServerSessionManager<SESSION_TYPE> sessionManager;

    SessionTable(Composite parent, int style, IWorkbenchSite site, DBAServerSessionManager<SESSION_TYPE> sessionManager)
    {
        super(parent, style, site, CONTENT_PROVIDER);
        this.sessionManager = sessionManager;
        //setFitWidth(true);
    }

    public DBAServerSessionManager<SESSION_TYPE> getSessionManager() {
        return sessionManager;
    }

    @NotNull
    @Override
    protected String getListConfigId(List<Class<?>> classList) {
        return "Sessions/" + sessionManager.getDataSource().getContainer().getDriver().getId();
    }

    @Override
    protected LoadingJob<Collection<SESSION_TYPE>> createLoadService()
    {
        return LoadingJob.createService(
            new LoadSessionsService(),
            new ObjectsLoadVisualizer());
    }

    LoadingJob<Void> createAlterService(List<SESSION_TYPE> sessions, Map<String, Object> options)
    {
        return LoadingJob.createService(
            new KillSessionsService(sessions, options),
            new ObjectActionVisualizer());
    }

    public void init(DBAServerSessionManager<SESSION_TYPE> sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    protected Map<String, Object> getSessionOptions() {
        return null;
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

    private class LoadSessionsService extends DatabaseLoadService<Collection<SESSION_TYPE>> {

        LoadSessionsService()
        {
            super("Load sessions", sessionManager.getDataSource());
        }

        @Override
        public Collection<SESSION_TYPE> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = sessionManager.getDataSource().getDefaultInstance().openIsolatedContext(monitor, "View sessions")) {
                    try (DBCSession session = isolatedContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Retrieve server sessions")) {
                        return sessionManager.getSessions(session, getSessionOptions());
                    }
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }

    private class KillSessionsService extends DatabaseLoadService<Void> {
        private final List<SESSION_TYPE> sessions;
        private final Map<String, Object> options;

        KillSessionsService(List<SESSION_TYPE> sessions, Map<String, Object> options)
        {
            super("Kill session", sessionManager.getDataSource());
            this.sessions = sessions;
            this.options = options;
        }

        @Override
        public Void evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCExecutionContext isolatedContext = sessionManager.getDataSource().getDefaultInstance().openIsolatedContext(monitor, "View sessions")) {
                    try (DBCSession session = isolatedContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Kill server session")) {
                        Throwable lastError = null;
                        for (SESSION_TYPE dbaSession : this.sessions) {
                            try {
                                sessionManager.alterSession(session, dbaSession, options);
                            } catch (Exception e) {
                                log.error("Error killing session " + session, e);
                                lastError = e;
                            }
                        }
                        if (lastError != null) {
                            throw new InvocationTargetException(lastError);
                        }
                        return null;
                    }
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }

    }

}
