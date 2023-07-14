/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.server;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dpi.api.DPIContext;
import org.jkiss.dbeaver.model.dpi.api.DPIController;
import org.jkiss.dbeaver.model.dpi.api.DPISession;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.rest.RestServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DPIControllerImpl implements DPIController {

    private static final Log log = Log.getLog(DPIControllerImpl.class);

    private final DPIContext context;
    private final Map<String, DPISession> sessions = new LinkedHashMap<>();
    private RestServer<?> server;

    public DPIControllerImpl(DPIContext context) {
        this.context = context;
    }

    @Override
    public String ping() throws DBException {
        return "pong";
    }

    @Override
    public DPISession openSession(String projectId) {
        DPISession session = new DPISession(UUID.randomUUID().toString());
        this.sessions.put(session.getSessionId(), session);
        return session;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull String session,
        @NotNull String projectId,
        @NotNull String container,
        @Nullable Map<String, String> credentials
    ) throws DBException {
        DPISession dpiSession = getSession(session);

        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProjectById(projectId);
        if (project == null) {
            throw new DBException("Project '" + projectId + "' not found");
        }
        DBPDataSourceContainer dataSourceContainer = project.getDataSourceRegistry().getDataSource(container);
        if (dataSourceContainer == null) {
            throw new DBException("Data source '" + container + "' not found");
        }
        LoggingProgressMonitor monitor = new LoggingProgressMonitor(log);

        dataSourceContainer.connect(monitor, true, false);

        DBPDataSource dataSource = dataSourceContainer.getDataSource();

        return dataSource;
    }

    @Override
    public void closeSession(@NotNull String sessionId) throws DBException {
        getSession(sessionId);
        sessions.remove(sessionId);

        if (sessions.isEmpty() && server != null) {
            new AbstractJob("Stop detached server") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    server.stop(1);
                    server = null;
                    return Status.OK_STATUS;
                }
            }.schedule(200);
        }
    }

    @Override
    public Object callMethod(@NotNull String objectId, @NotNull String method, @Nullable Object[] args) throws DBException {
        Object object = context.getObject(objectId);
        if (object == null) {
            throw new DBException("DPI object '" + objectId + "' not found");
        }
        for (Method objMethod : object.getClass().getMethods()) {
            if (objMethod.getName().equals(method)) {
                Class<?>[] argTypes = objMethod.getParameterTypes();
                if ((ArrayUtils.isEmpty(args) && ArrayUtils.isEmpty(argTypes)) ||
                    (args != null && argTypes.length == args.length)
                ) {
                    if (args != null) {
                        boolean argsMatch = true;
                        for (int i = 0; i < argTypes.length; i++) {
                            if (args[i] != null && !argTypes[i].isInstance(args[i])) {
                                argsMatch = false;
                                break;
                            }
                        }
                        if (argsMatch) {
                            return invokeObjectMethod(object, objMethod, args);
                        }
                    } else {
                        // No args
                        return invokeObjectMethod(object, objMethod, null);
                    }
                }
            }
        }
        throw new DBException("Method '" + method + "' not found in DPI object '" + objectId + "'");
    }

    private Object invokeObjectMethod(Object object, Method method, Object[] args) throws DBException {
        try {
            log.debug("Invoke DPI method " + method + " on " + object.getClass());
            return method.invoke(object, args);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw new DBException("Error invoking DPI method", e);
        }
    }

    private DPISession getSession(@NotNull String sessionId) throws DBException {
        DPISession session = sessions.get(sessionId);
        if (session == null) {
            throw new DBException("Session '" + sessionId + "' not found");
        }
        return session;
    }

    @Override
    public void close() {

    }

    public void setServer(RestServer<?> server) {
        this.server = server;
    }

    public RestServer<?> getServer() {
        return server;
    }
}
