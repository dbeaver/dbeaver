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
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.debug.core.model.DatabaseDebugTarget;
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
import org.jkiss.dbeaver.runtime.ide.core.DBeaverIDECore;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DebugCore {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.debug.core"; //$NON-NLS-1$

    public static final String MODEL_IDENTIFIER_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "database"; //$NON-NLS-1$

    public static final String BREAKPOINT_ID_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "databaseBreakpointMarker"; //$NON-NLS-1$
    public static final String BREAKPOINT_ID_DATABASE_LINE = BUNDLE_SYMBOLIC_NAME + '.'
            + "databaseLineBreakpointMarker"; //$NON-NLS-1$

    public static final String BREAKPOINT_ATTRIBUTE_DATASOURCE_ID = DBeaverIDECore.MARKER_ATTRIBUTE_DATASOURCE_ID;
    public static final String BREAKPOINT_ATTRIBUTE_DATABASE_NAME = BUNDLE_SYMBOLIC_NAME + '.' + "databaseName"; //$NON-NLS-1$
    public static final String BREAKPOINT_ATTRIBUTE_SCHEMA_NAME = BUNDLE_SYMBOLIC_NAME + '.' + "schemaName"; //$NON-NLS-1$
    public static final String BREAKPOINT_ATTRIBUTE_PROCEDURE_NAME = BUNDLE_SYMBOLIC_NAME + '.' + "procedureName"; //$NON-NLS-1$
    public static final String BREAKPOINT_ATTRIBUTE_PROCEDURE_OID = BUNDLE_SYMBOLIC_NAME + '.' + "procedureOid"; //$NON-NLS-1$
    public static final String BREAKPOINT_ATTRIBUTE_NODE_PATH = DBeaverIDECore.MARKER_ATTRIBUTE_NODE_PATH;

    public static final String SOURCE_CONTAINER_TYPE_DATASOURCE = BUNDLE_SYMBOLIC_NAME + '.'
            + "datasourceSourceContainerType"; //$NON-NLS-1$

    public static final String ATTR_DRIVER_ID = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DRIVER_ID"; //$NON-NLS-1$
    public static final String ATTR_DRIVER_ID_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_DATASOURCE_ID = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DATASOURCE_ID"; //$NON-NLS-1$
    public static final String ATTR_DATASOURCE_ID_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_DATABASE_NAME = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DATABASE_NAME"; //$NON-NLS-1$
    public static final String ATTR_DATABASE_NAME_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_SCHEMA_NAME = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_SCHEMA_NAME"; //$NON-NLS-1$
    public static final String ATTR_SCHEMA_NAME_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_PROCEDURE_OID = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_PROCEDURE_OID"; //$NON-NLS-1$
    public static final String ATTR_PROCEDURE_OID_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_PROCEDURE_NAME = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_PROCEDURE_NAME"; //$NON-NLS-1$
    public static final String ATTR_PROCEDURE_NAME_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_ATTACH_PROCESS = BUNDLE_SYMBOLIC_NAME + '.' + "ATTACH_PROCESS"; //$NON-NLS-1$
    public static final String ATTR_ATTACH_PROCESS_DEFAULT = DBGController.ATTACH_PROCESS_ANY;

    public static final String ATTR_ATTACH_KIND = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_ATTACH_KIND"; //$NON-NLS-1$
    public static final String ATTR_ATTACH_KIND_DEFAULT = DBGController.ATTACH_KIND_LOCAL;

    public static final String ATTR_SCRIPT_EXECUTE = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_SCRIPT_EXECUTE"; //$NON-NLS-1$
    public static final String ATTR_SCRIPT_EXECUTE_DEFAULT = Boolean.FALSE.toString();

    public static final String ATTR_SCRIPT_TEXT = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_SCRIPT_TEXT"; //$NON-NLS-1$
    public static final String ATTR_SCRIPT_TEXT_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_NODE_PATH = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_NODE_PATH"; //$NON-NLS-1$
    public static final String ATTR_NODE_PATH_DEFAULT = ""; //$NON-NLS-1$

    private static Log log = Log.getLog(DebugCore.class);

    public static void log(IStatus status) {
        Log.log(log, status);
    }

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
        String call = sb.toString();
        return call;
    }

    public static String composeScriptText(DBSProcedure procedure) {
        try {
            return composeProcedureCall(procedure, new VoidProgressMonitor());
        } catch (DBException e) {
            String message = NLS.bind("Failed to compose call for {0}", procedure);
            log.error(message, e);
            return ATTR_SCRIPT_TEXT_DEFAULT;
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

    public static String extractDriverId(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_DRIVER_ID, ATTR_DRIVER_ID_DEFAULT);
    }

    public static String extractDatasourceId(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_DATASOURCE_ID, ATTR_DATASOURCE_ID_DEFAULT);
    }

    public static String extractDatabaseName(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_DATABASE_NAME, ATTR_DATABASE_NAME_DEFAULT);
    }

    public static String extractSchemaName(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_SCHEMA_NAME, ATTR_SCHEMA_NAME_DEFAULT);
    }

    public static String extractProcedureOid(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_PROCEDURE_OID, ATTR_PROCEDURE_OID_DEFAULT);
    }

    public static String extractProcedureName(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_PROCEDURE_NAME, ATTR_PROCEDURE_NAME_DEFAULT);
    }

    public static String extractAttachProcess(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_ATTACH_PROCESS, ATTR_ATTACH_PROCESS_DEFAULT);
    }

    public static String extractAttachKind(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_ATTACH_KIND, ATTR_ATTACH_KIND_DEFAULT);
    }

    public static String extractScriptExecute(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_SCRIPT_EXECUTE, ATTR_SCRIPT_EXECUTE_DEFAULT);
    }

    public static String extractScriptText(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_SCRIPT_TEXT, ATTR_SCRIPT_TEXT_DEFAULT);
    }

    public static String extractNodePath(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_NODE_PATH, ATTR_NODE_PATH_DEFAULT);
    }

    public static String extractStringAttribute(ILaunchConfiguration configuration, String attributeName,
            String defaultValue) {
        if (configuration == null) {
            String message = NLS.bind(DebugCoreMessages.DebugCore_e_read_attribute_null, attributeName);
            log.error(message);
            return defaultValue;
        }
        try {
            return configuration.getAttribute(attributeName, defaultValue);
        } catch (CoreException e) {
            String message = NLS.bind(DebugCoreMessages.DebugCore_e_read_attribute_generic, attributeName,
                    configuration);
            log.error(message, e);
            return defaultValue;
        }
    }

    public static Status newErrorStatus(String message, Throwable th) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, th);
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
        Map<String, Object> result = new HashMap<String, Object>();
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

    public static DBGController findProcedureController(DBPDataSourceContainer dataSourceContainer)
            throws DBGException {
        DBGController controller = Adapters.adapt(dataSourceContainer, DBGController.class);
        if (controller != null) {
            return controller;
        }
        String providerId = dataSourceContainer.getDriver().getProviderId();
        String message = NLS.bind("Unable to find controller for datasource \"{0}\"", providerId);
        throw new DBGException(message);
    }

    public static String getSourceName(Object object) throws CoreException {
        if (object instanceof DatabaseStackFrame) {
            DatabaseStackFrame frame = (DatabaseStackFrame) object;
            Object sourceIdentifier = frame.getSourceIdentifier();
            DatabaseDebugTarget debugTarget = frame.getDatabaseDebugTarget();
            DBSObject dbsObject = null;
            try {
                dbsObject = debugTarget.findDatabaseObject(sourceIdentifier, new VoidProgressMonitor());
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
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put(DBGController.BREAKPOINT_LINE_NUMBER, attributes.get(IMarker.LINE_NUMBER));
        result.put(DBGController.PROCEDURE_OID, attributes.get(BREAKPOINT_ATTRIBUTE_PROCEDURE_OID));
        return result;
    }
    
    public static void postDebuggerSourceEvent(String nodePath) {
        String encoded = GeneralUtils.encodeTopic(DBPScriptObject.OPTION_DEBUGGER_SOURCE);
        DebugCoreActivator.getDefault().postEvent(encoded, nodePath);
    }

}
