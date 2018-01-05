/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
import java.util.List;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGControllerRegistry;
import org.jkiss.dbeaver.debug.DBGProcedureController;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreActivator;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;

public class DebugCore {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.debug.core"; //$NON-NLS-1$

    public static final String EP_PROCEDURE_CONTROLLERS_ID = "procedureControllers"; //$NON-NLS-1$
    public static final String EP_PROCEDURE_CONTROLLERS_CONTROLLER = "controller"; //$NON-NLS-1$
    public static final String EP_PROCEDURE_CONTROLLERS_CONTROLLER_PROVIDER_ID = "providerId"; //$NON-NLS-1$
    public static final String EP_PROCEDURE_CONTROLLERS_CONTROLLER_CLASS = "class"; //$NON-NLS-1$

    // FIXME: AF: revisit, looks like we can live without it
    public static final String MODEL_IDENTIFIER_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "database"; //$NON-NLS-1$
    public static final String MODEL_IDENTIFIER_PROCEDURE = BUNDLE_SYMBOLIC_NAME + '.' + "procedure"; //$NON-NLS-1$

    public static final String BREAKPOINT_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "databaseBreakpointMarker"; //$NON-NLS-1$
    public static final String BREAKPOINT_DATABASE_LINE = BUNDLE_SYMBOLIC_NAME + '.' + "databaseLineBreakpointMarker"; //$NON-NLS-1$

    public static final String ATTR_DATASOURCE = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DATASOURCE"; //$NON-NLS-1$
    public static final String ATTR_DATASOURCE_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DATABASE"; //$NON-NLS-1$
    public static final String ATTR_DATABASE_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_OID = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_OID"; //$NON-NLS-1$
    public static final String ATTR_OID_DEFAULT = ""; //$NON-NLS-1$

    private static Log log = Log.getLog(DebugCore.class);

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
        for (int i = 0; i < scope.length; i++) {
            Object object = scope[i];
            DBSObject adapted = Adapters.adapt(object, DBSObject.class, true);
            if (adapted != null) {
                extracted.add(adapted);
            }
        }
        return extracted;
    }

    public static String extractDatasourceId(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_DATASOURCE, ATTR_DATASOURCE_DEFAULT);
    }

    public static String extractDatabaseName(ILaunchConfiguration configuration) {
        return extractStringAttribute(configuration, ATTR_DATABASE, ATTR_DATABASE_DEFAULT);
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

    // FIXME: AF: move to org.jkiss.dbeaver.Log
    public static void log(Log delegate, IStatus status) {
        if (delegate == null) {
            // no way to log
            return;
        }
        if (status == null) {
            // nothing to log
            return;
        }
        int severity = status.getSeverity();
        String message = status.getMessage();
        Throwable exception = status.getException();
        switch (severity) {
        case IStatus.CANCEL:
            delegate.debug(message, exception);
            break;
        case IStatus.ERROR:
            delegate.error(message, exception);
            break;
        case IStatus.WARNING:
            delegate.warn(message, exception);
            break;
        case IStatus.INFO:
            delegate.info(message, exception);
            break;
        case IStatus.OK:
            delegate.trace(message, exception);
            break;
        default:
            break;
        }
    }

    public static Status newErrorStatus(String message, Throwable th) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, th);
    }

    public static Status newErrorStatus(String message) {
        return newErrorStatus(message, null);
    }

    public static String extractProviderId(DataSourceDescriptor datasourceDescriptor) {
        if (datasourceDescriptor == null) {
            return null;
        }
        DriverDescriptor driverDescriptor = datasourceDescriptor.getDriver();
        if (driverDescriptor == null) {
            return null;
        }
        DataSourceProviderDescriptor providerDescriptor = driverDescriptor.getProviderDescriptor();
        if (providerDescriptor == null) {
            return null;
        }
        return providerDescriptor.getId();
    }

    public static DBGControllerRegistry<DBGProcedureController> getProcedureControllerRegistry() {
        return DebugCoreActivator.getDefault().getProcedureControllerRegistry();
    }

    public static DBGProcedureController findProcedureController(DataSourceDescriptor datasourceDescriptor) {
        String providerId = extractProviderId(datasourceDescriptor);
        DBGControllerRegistry<DBGProcedureController> controllerRegistry = getProcedureControllerRegistry();
        return controllerRegistry.createController(providerId);
    }

}
