/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.core;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.debug.core.model.DatabaseStackFrame;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugUtils {

    private static Log log = Log.getLog(DebugUtils.class);

    public static CoreException abort(String message, Throwable th) {
        return new CoreException(newErrorStatus(message, th));
    }

    public static CoreException abort(String message) {
        return abort(message, null);
    }

    public static boolean canLaunch(ILaunchConfiguration configuration, String mode) {
        if (configuration == null || !configuration.exists()) {
            return false;
        }
        try {
            return configuration.supportsMode(mode);
        } catch (CoreException e) {
            String message = NLS.bind(DebugCoreMessages.DebugCore_e_unable_to_retrieve_modes, configuration);
            log.error(message, e);
            return false;
        }
    }

    public static List<DBSObject> extractLaunchable(Object[] scope) {
        List<DBSObject> extracted = new ArrayList<>();
        if (scope == null) {
            return extracted;
        }
        for (Object object : scope) {
            DBSObject adapted = GeneralUtils.adapt(object, DBSObject.class, true);
            if (adapted != null) {
                extracted.add(adapted);
            }
        }
        return extracted;
    }

    public static Status newErrorStatus(String message, Throwable th) {
        return new Status(IStatus.ERROR, DBGConstants.BUNDLE_SYMBOLIC_NAME, message, th);
    }

    public static Status newErrorStatus(String message) {
        return newErrorStatus(message, null);
    }

    public static DBSObject resolveDatabaseObject(DBPDataSourceContainer container, Map<String, Object> context,
            Object identifier, DBRProgressMonitor monitor) throws DBException {
        DBGResolver finder = GeneralUtils.adapt(container, DBGResolver.class);
        if (finder == null) {
            return null;
        }
        return finder.resolveObject(context, identifier, monitor);
    }

    public static Map<String, Object> resolveDatabaseContext(DBSObject databaseObject) {
        Map<String, Object> result = new HashMap<>();
        if (databaseObject == null) {
            return result;
        }
        DBPDataSource dataSource = databaseObject.getDataSource();
        if (dataSource == null) {
            return result;
        }
        DBGResolver finder = GeneralUtils.adapt(dataSource.getContainer(), DBGResolver.class);
        if (finder == null) {
            return result;
        }
        Map<String, Object> context = finder.resolveContext(databaseObject);
        result.putAll(context);
        return result;
    }

    public static String getSourceName(Object object) throws CoreException {
        if (object instanceof DatabaseStackFrame) {
            DatabaseStackFrame frame = (DatabaseStackFrame) object;
            Object sourceIdentifier = frame.getSourceIdentifier();
            DBSObject dbsObject;
            try {
                dbsObject = findDatabaseObject(frame.getController(), sourceIdentifier, new VoidProgressMonitor());
            } catch (DBException e) {
                Status error = DebugUtils.newErrorStatus(e.getMessage(), e);
                throw new CoreException(error);
            }
            if (dbsObject == null) {
                return null;
            }
            final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
            DBNDatabaseNode node = navigatorModel.getNodeByObject(new VoidProgressMonitor(), dbsObject, false);
            if (node != null) {
                return node.getNodeItemPath();
            }
        }
        if (object instanceof String) {
            // well, let's be positive and assume it's a node path already
            return (String) object;
        }
        return null;
    }

    public static Map<String, Object> toBreakpointDescriptor(Map<String, Object> attributes) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(IMarker.LINE_NUMBER, attributes.get(IMarker.LINE_NUMBER));
        return result;
    }
    
    public static DBSObject findDatabaseObject(DBGController controller, Object identifier, DBRProgressMonitor monitor) throws DBException {
        DBPDataSourceContainer container = controller.getDataSourceContainer();
        Map<String, Object> context = controller.getDebugConfiguration();
        return resolveDatabaseObject(container, context, identifier, monitor);
    }

    public static void putContextInConfiguration(ILaunchConfigurationWorkingCopy configuration, Map<String, Object> attrs) {
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                configuration.removeAttribute(entry.getKey());
            } else if (value instanceof Integer) {
                configuration.setAttribute(entry.getKey(), (Integer) value);
            } else if (value instanceof Boolean) {
                configuration.setAttribute(entry.getKey(), (Boolean) value);
            } else if (value instanceof List) {
                configuration.setAttribute(entry.getKey(), (List<String>)value);
            } else {
                configuration.setAttribute(entry.getKey(), value.toString());
            }
        }
    }

    /**
     * Fires the given debug event.
     *
     * @param event
     *            debug event to fire
     */
    public static void fireEvent(DebugEvent event) {
        DebugPlugin manager = DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[] { event });
        }
    }

    /**
     * Fires a terminate event.
     */
    public static void fireTerminate(Object source) {
        fireEvent(new DebugEvent(source, DebugEvent.TERMINATE));
    }

    @NotNull
    public static DBPDataSourceContainer getDataSourceContainer(ILaunchConfiguration configuration) throws CoreException {
        String projectName = configuration.getAttribute(DBGConstants.ATTR_PROJECT_NAME, (String)null);
        String datasourceId = configuration.getAttribute(DBGConstants.ATTR_DATASOURCE_ID, (String)null);
        DBPDataSourceContainer datasourceDescriptor = DBUtils.findDataSource(projectName, datasourceId);
        if (datasourceDescriptor == null) {
            String message = NLS.bind("Unable to find data source with id {0}", datasourceId);
            throw new CoreException(newErrorStatus(message));
        }
        return datasourceDescriptor;
    }
}
