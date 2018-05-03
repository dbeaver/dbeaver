/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.debug.core.model.DatabaseStackFrame;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreActivator;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DebugCore {

    private static Log log = Log.getLog(DebugCore.class);

    public static CoreException abort(String message, Throwable th) {
        return new CoreException(newErrorStatus(message, th));
    }

    public static CoreException abort(String message) {
        return abort(message, null);
    }

    public static String composeProcedureCall(DBSProcedure procedure, DBRProgressMonitor monitor) throws DBException {
        StringBuilder sb = new StringBuilder();
        sb.append("select").append(' ').append(procedure.getName());
        sb.append('(');
        Collection<? extends DBSProcedureParameter> parameters = procedure.getParameters(monitor);
        int size = parameters.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Object value = '?';
                sb.append(value);
                sb.append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(')');
        return sb.toString();
    }

    public static String composeScriptText(DBSProcedure procedure) {
        try {
            return composeProcedureCall(procedure, new VoidProgressMonitor());
        } catch (DBException e) {
            String message = NLS.bind("Failed to compose call for {0}", procedure);
            log.error(message, e);
            return "";
        }
    }

    public static ILaunchConfigurationWorkingCopy createConfiguration(IContainer container, String typeName,
            String name) throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(typeName);
        ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(container, name);
        return workingCopy;
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
        for (int i = 0; i < scope.length; i++) {
            Object object = scope[i];
            DBSObject adapted = Adapters.adapt(object, DBSObject.class, true);
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
        DBGResolver finder = Adapters.adapt(container, DBGResolver.class);
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
        DBGResolver finder = Adapters.adapt(dataSource.getContainer(), DBGResolver.class);
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
                Status error = DebugCore.newErrorStatus(e.getMessage(), e);
                throw new CoreException(error);
            }
            if (dbsObject == null) {
                return null;
            }
            final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
            DBNDatabaseNode node = navigatorModel.getNodeByObject(new VoidProgressMonitor(), dbsObject, false);
            if (node != null) {
                String nodePath = node.getNodeItemPath();
                DebugCore.postDebuggerSourceEvent(nodePath);
                return nodePath;
            }
        }
        if (object instanceof String) {
            // well, let's be positive and assume it's a node path already
            String nodePath = (String) object;
            DebugCore.postDebuggerSourceEvent(nodePath);
            return nodePath;
        }
        return null;
    }

    public static Map<String, Object> toBreakpointDescriptor(Map<String, Object> attributes) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(IMarker.LINE_NUMBER, attributes.get(IMarker.LINE_NUMBER));
        //result.put(PostgreDebugConstants.ATTR_PROCEDURE_OID, attributes.get(PostgreDebugConstants.ATTR_PROCEDURE_OID));
        return result;
    }
    
    public static void postDebuggerSourceEvent(String nodePath) {
        String encoded = GeneralUtils.encodeTopic(DBPScriptObject.OPTION_DEBUGGER_SOURCE);
        DebugCoreActivator.getDefault().postEvent(encoded, nodePath);
    }

    public static DBSObject findDatabaseObject(DBGController controller, Object identifier, DBRProgressMonitor monitor) throws DBException {
        DBPDataSourceContainer container = controller.getDataSourceContainer();
        Map<String, Object> context = controller.getDebugConfiguration();
        return resolveDatabaseObject(container, context, identifier, monitor);
    }
}
