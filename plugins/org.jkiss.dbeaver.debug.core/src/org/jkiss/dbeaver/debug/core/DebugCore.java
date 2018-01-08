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
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DebugCore {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.debug.core"; //$NON-NLS-1$

    // FIXME: AF: revisit, looks like we can live without it
    public static final String MODEL_IDENTIFIER_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "database"; //$NON-NLS-1$
    public static final String MODEL_IDENTIFIER_PROCEDURE = BUNDLE_SYMBOLIC_NAME + '.' + "procedure"; //$NON-NLS-1$

    public static final String BREAKPOINT_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "databaseBreakpointMarker"; //$NON-NLS-1$
    public static final String BREAKPOINT_DATABASE_LINE = BUNDLE_SYMBOLIC_NAME + '.' + "databaseLineBreakpointMarker"; //$NON-NLS-1$

    public static final String ATTR_DRIVER = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DRIVER"; //$NON-NLS-1$
    public static final String ATTR_DRIVER_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_DATASOURCE = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DATASOURCE"; //$NON-NLS-1$
    public static final String ATTR_DATASOURCE_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_DATABASE = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_DATABASE"; //$NON-NLS-1$
    public static final String ATTR_DATABASE_DEFAULT = ""; //$NON-NLS-1$

    public static final String ATTR_OID = BUNDLE_SYMBOLIC_NAME + '.' + "ATTR_OID"; //$NON-NLS-1$
    public static final String ATTR_OID_DEFAULT = ""; //$NON-NLS-1$

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
        return extractStringAttribute(configuration, ATTR_DRIVER, ATTR_DRIVER_DEFAULT);
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

    public static Status newErrorStatus(String message, Throwable th) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message, th);
    }

    public static Status newErrorStatus(String message) {
        return newErrorStatus(message, null);
    }

    public static DBGController findProcedureController(DBPDataSourceContainer dataSourceContainer) throws DBGException {
        DBGController controller = Adapters.adapt(dataSourceContainer, DBGController.class);
        if (controller != null) {
            return controller;
        }
        String providerId = dataSourceContainer.getDriver().getProviderId();
        String message = NLS.bind("Unable to find controller for datasource \"{0}\"", providerId);
        throw new DBGException(message);
    }

}
