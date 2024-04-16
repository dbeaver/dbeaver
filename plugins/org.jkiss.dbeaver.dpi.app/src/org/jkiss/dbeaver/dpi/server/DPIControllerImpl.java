/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.dpi.app.DPIApplication;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.dpi.model.adapters.DPISerializer;
import org.jkiss.dbeaver.dpi.model.client.DPISmartObjectResponse;
import org.jkiss.dbeaver.dpi.model.client.DPISmartObjectWrapper;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dpi.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.rest.RestServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

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
    public DPISession openSession() {
        DPISession session = new DPISession(UUID.randomUUID().toString());
        this.sessions.put(session.getSessionId(), session);
        return session;
    }

    @NotNull
    @Override
    public synchronized DBPDataSource openDataSource(
        @NotNull DPIDataSourceParameters parameters
    ) throws DBException {
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            throw new DBException("Active project not found");
        }
        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        if (!(registry instanceof DataSourcePersistentRegistry persistentRegistry)) {
            throw new DBException("Cannot load datasource from " + registry.getClass().getName());
        }
        DBPDataSourceConfigurationStorage storage =
            new DataSourceMemoryStorage(parameters.getContainerConfiguration().getBytes(StandardCharsets.UTF_8));
        DataSourceConfigurationManager manager = new DataSourceConfigurationManagerBuffer();
        persistentRegistry.loadDataSources(
            List.of(storage),
            manager,
            List.of(),
            true,
            false
        );
        DBPDataSourceContainer dataSourceContainer =
            project.getDataSourceRegistry().getDataSources().stream().findFirst().orElse(null);

        if (dataSourceContainer == null) {
            throw new DBException("Data source not found");
        }
        ((DPIApplication) DPIApplication.getInstance()).addDriverLibsLocation(
            dataSourceContainer.getDriver().getId(), parameters.getDriverLibraries()
        );
        LoggingProgressMonitor monitor = new LoggingProgressMonitor(log);

        ((DataSourceDescriptor) dataSourceContainer).openDataSource(monitor, true);

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
        log.debug(MessageFormat.format("Invoke method: {0} object: {1}", method, objectId));
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

    @Override
    public Object readProperty(@NotNull String objectId, @NotNull String propertyName) throws DBException {
        Object object = context.getObject(objectId);
        if (object == null) {
            throw new DBException("DPI object '" + objectId + "' not found");
        }
        Method method = DBXTreeItem.findPropertyReadMethod(object.getClass(), propertyName);
        if (method == null) {
            throw new DBException("Property '" + propertyName + "' not found in object '" + object.getClass() + "'");
        }
        return invokeObjectMethod(object, method, null);
    }

    private Object invokeObjectMethod(Object object, Method method, Object[] args) throws DBException {
        boolean originalAccessible = method.canAccess(object);
        method.setAccessible(true);
        try {
            log.debug("DPI Server: invoke DPI method " + method + " on " + object.getClass());
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (args == null) {
                // Simple method or property read
                if (parameterTypes.length == 1 && parameterTypes[0] == DBRProgressMonitor.class) {
                    // Lazy property read
                    args = new Object[] { context.getProgressMonitor() };
                }
                return method.invoke(object, args);
            }
            // Deserialize arguments
            Object[] realArgs = new Object[args.length];
            Gson gson = context.getGson();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] instanceof String && !CharSequence.class.isAssignableFrom(parameterTypes[i])) {
                    realArgs[i] = gson.fromJson((String) args[i], parameterTypes[i]);
                } else if (args[i] instanceof Map<?,?> && !Map.class.isAssignableFrom(parameterTypes[i])) {
                    // Double convert of map
                    realArgs[i] = gson.fromJson(gson.toJsonTree(args[i], Map.class), parameterTypes[i]);
                } else if (args[i] instanceof Number) {
                    realArgs[i] = gson.fromJson(gson.toJson(args[i], parameterTypes[i]), parameterTypes[i]);
                } else {
                    realArgs[i] = args[i];
                }
            }
            Object result = method.invoke(object, realArgs);
            List<DPISmartObjectWrapper> smartObjects = new ArrayList<>();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (DPISerializer.isSmartObject(parameterType)) {
                    Object serverSideSmartObject = realArgs[i];
                    if (serverSideSmartObject instanceof DPIServerSmartObject serverSmartObject) {
                        DPISmartCallback callback = serverSmartObject.getCallback();
                        var dpiWrapper = new DPISmartObjectWrapper(
                            callback.getClass(),
                            i,
                            callback
                        );
                        smartObjects.add(dpiWrapper);
                    }
                }
            }
            if (smartObjects.isEmpty()) {
                return result;
            } else {
                log.debug("Smart arguments detected, return smart objects wrapper instead of original method results");
                return new DPISmartObjectResponse(result, smartObjects);
            }
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            throw new DBException("Error invoking DPI method "
                + object.getClass() + "#"
                + method.getName() + " : "
                + e.getMessage(),
                e
            );
        } finally {
            method.setAccessible(originalAccessible);
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
