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
package org.jkiss.dbeaver.dpi.server;

import com.google.gson.Gson;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.dpi.model.DPIController;
import org.jkiss.dbeaver.dpi.model.DPISession;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
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
    public synchronized DBPDataSource openDataSource(
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
    public synchronized Object callMethod(@NotNull String objectId, @NotNull String method, @Nullable Object[] args) throws DBException {
        Object object = context.getObject(objectId);
        if (object == null) {
            throw new DBException("DPI object '" + objectId + "' not found");
        }
        for (Method objMethod : object.getClass().getMethods()) {
            if (objMethod.getName().equals(method)) {
                Class<?>[] argTypes = objMethod.getParameterTypes();
                if (args != null && argTypes.length > 0 && argTypes.length != args.length && argTypes[0] == DBRProgressMonitor.class) {
                    // Maybe some implicit parameters missing
                    Object[] modifiedArgs = new Object[args.length + 1];
                    modifiedArgs[0] = context.getProgressMonitor();
                    System.arraycopy(args, 0, modifiedArgs, 1, args.length);
                    args = modifiedArgs;
                }
                if ((ArrayUtils.isEmpty(args) && ArrayUtils.isEmpty(argTypes)) ||
                    (args != null && argTypes.length == args.length)
                ) {
                    return invokeObjectMethod(object, objMethod, args);
                }
            }
        }
        throw new DBException("Method '" + method + "' not found in DPI object '" + objectId + "'");
    }

    private Object invokeObjectMethod(Object object, Method method, Object[] args) throws DBException {
        try {
            log.debug("DPI Server: invoke DPI method " + method + " on " + object.getClass());
            if (args == null) {
                return method.invoke(object);
            }
            // Deserialize arguments
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] realArgs = new Object[args.length];
            Gson gson = context.getGson();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] instanceof String && !CharSequence.class.isAssignableFrom(parameterTypes[i])) {
                    realArgs[i] = gson.fromJson((String) args[i], parameterTypes[i]);
                } else if (args[i] instanceof Map<?,?> && !Map.class.isAssignableFrom(parameterTypes[i])) {
                    // Double convert of map
                    realArgs[i] = gson.fromJson(gson.toJsonTree(args[i], Map.class), parameterTypes[i]);
                } else {
                    realArgs[i] = args[i];
                }
            }
            return method.invoke(object, realArgs);
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
