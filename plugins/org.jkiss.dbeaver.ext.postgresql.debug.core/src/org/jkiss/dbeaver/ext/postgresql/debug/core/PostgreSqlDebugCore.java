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
package org.jkiss.dbeaver.ext.postgresql.debug.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.PostgreDebugCoreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class PostgreSqlDebugCore {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.ext.postgresql.debug.core"; //$NON-NLS-1$

    public static final String CONFIGURATION_TYPE = BUNDLE_SYMBOLIC_NAME + '.' + "pgSQL";//$NON-NLS-1$

    public static ILaunchConfigurationWorkingCopy createConfiguration(DBSObject launchable) throws CoreException {
        boolean isInstance = launchable instanceof PostgreProcedure;
        if (!isInstance) {
            throw DebugCore.abort(PostgreDebugCoreMessages.PostgreSqlDebugCore_e_procedure_required);
        }
        PostgreProcedure procedure = (PostgreProcedure) launchable;
        PostgreDataSource dataSource = procedure.getDataSource();
        DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();
        PostgreDatabase database = procedure.getDatabase();
        PostgreSchema schema = procedure.getContainer();

        String databaseName = database.getName();
        String schemaName = schema.getName();
        String procedureName = procedure.getName();
        Object[] bindings = new Object[] { dataSourceContainer.getName(), databaseName, procedureName, schemaName };
        String name = NLS.bind(PostgreDebugCoreMessages.PostgreSqlDebugCore_launch_configuration_name, bindings);
        // Let's use metadata area for storage
        IContainer container = null;
        ILaunchConfigurationWorkingCopy workingCopy = DebugCore.createConfiguration(container, CONFIGURATION_TYPE,
                name);
        workingCopy.setAttribute(DBGConstants.ATTR_DATASOURCE_ID, dataSourceContainer.getId());
        workingCopy.setAttribute(PostgreDebugConstants.ATTR_FUNCTION_OID, String.valueOf(procedure.getObjectId()));

        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
        DBNDatabaseNode node = navigatorModel.getNodeByObject(new VoidProgressMonitor(), procedure, false);
        workingCopy.setAttribute(DBGConstants.ATTR_NODE_PATH, node.getNodeItemPath());
        return workingCopy;
    }

    public static PostgreProcedure resolveFunction(DBRProgressMonitor monitor, DBPDataSourceContainer dsContainer, Map<String, Object> configuration) throws DBException {
        if (!dsContainer.isConnected()) {
            dsContainer.connect(monitor, true, true);
        }
        long functionId = CommonUtils.toLong(configuration.get(PostgreDebugConstants.ATTR_FUNCTION_OID));
        String databaseName = (String)configuration.get(PostgreDebugConstants.ATTR_DATABASE_NAME);
        String schemaName = (String)configuration.get(PostgreDebugConstants.ATTR_SCHEMA_NAME);
        PostgreDataSource ds = (PostgreDataSource) dsContainer.getDataSource();
        PostgreDatabase database = ds.getDatabase(databaseName);
        if (database != null) {
            PostgreSchema schema = database.getSchema(monitor, schemaName);
            if (schema != null) {
                PostgreProcedure function = schema.getProcedure(monitor, functionId);
                if (function != null) {
                    return function;
                }
                throw new DBException("Function " + functionId + " not found in schema " + schemaName);
            } else {
                throw new DBException("Schema '" + schemaName + "' not found in database " + databaseName);
            }
        } else {
            throw new DBException("Database '" + databaseName + "' not found");
        }
    }
}
