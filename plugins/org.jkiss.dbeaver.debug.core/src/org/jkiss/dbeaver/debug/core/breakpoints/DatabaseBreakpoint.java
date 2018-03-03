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

package org.jkiss.dbeaver.debug.core.breakpoints;

import static org.jkiss.dbeaver.debug.core.DebugCore.BREAKPOINT_ATTRIBUTE_DATABASE_NAME;
import static org.jkiss.dbeaver.debug.core.DebugCore.BREAKPOINT_ATTRIBUTE_DATASOURCE_ID;
import static org.jkiss.dbeaver.debug.core.DebugCore.BREAKPOINT_ATTRIBUTE_NODE_PATH;
import static org.jkiss.dbeaver.debug.core.DebugCore.BREAKPOINT_ATTRIBUTE_PROCEDURE_NAME;
import static org.jkiss.dbeaver.debug.core.DebugCore.BREAKPOINT_ATTRIBUTE_PROCEDURE_OID;
import static org.jkiss.dbeaver.debug.core.DebugCore.BREAKPOINT_ATTRIBUTE_SCHEMA_NAME;
import static org.jkiss.dbeaver.debug.core.DebugCore.MODEL_IDENTIFIER_DATABASE;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.Breakpoint;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DatabaseBreakpoint extends Breakpoint implements IDatabaseBreakpoint {

    @Override
    public String getModelIdentifier() {
        return MODEL_IDENTIFIER_DATABASE;
    }

    @Override
    public String getDatasourceId() throws CoreException {
        return ensureMarker().getAttribute(BREAKPOINT_ATTRIBUTE_DATASOURCE_ID, null);
    }

    @Override
    public String getDatabaseName() throws CoreException {
        return ensureMarker().getAttribute(BREAKPOINT_ATTRIBUTE_DATABASE_NAME, null);
    }

    @Override
    public String getSchemaName() throws CoreException {
        return ensureMarker().getAttribute(BREAKPOINT_ATTRIBUTE_SCHEMA_NAME, null);
    }

    @Override
    public String getProcedureName() throws CoreException {
        return ensureMarker().getAttribute(BREAKPOINT_ATTRIBUTE_PROCEDURE_NAME, null);
    }

    @Override
    public String getProcedureOid() throws CoreException {
        return ensureMarker().getAttribute(BREAKPOINT_ATTRIBUTE_PROCEDURE_OID, null);
    }

    @Override
    public String getNodePath() throws CoreException {
        return ensureMarker().getAttribute(BREAKPOINT_ATTRIBUTE_NODE_PATH, null);
    }

    protected void setDatasourceId(String datasourceId) throws CoreException {
        setAttribute(BREAKPOINT_ATTRIBUTE_DATASOURCE_ID, datasourceId);
    }

    protected void setDatabaseName(String databaseName) throws CoreException {
        setAttribute(BREAKPOINT_ATTRIBUTE_DATABASE_NAME, databaseName);
    }

    protected void setSchemaName(String schemaName) throws CoreException {
        setAttribute(BREAKPOINT_ATTRIBUTE_SCHEMA_NAME, schemaName);
    }

    protected void setProcedureName(String procedureName) throws CoreException {
        setAttribute(BREAKPOINT_ATTRIBUTE_PROCEDURE_NAME, procedureName);
    }

    protected void setProcedureOid(String procedureOid) throws CoreException {
        setAttribute(BREAKPOINT_ATTRIBUTE_PROCEDURE_OID, procedureOid);
    }

    protected void setNodePath(String nodePath) throws CoreException {
        setAttribute(BREAKPOINT_ATTRIBUTE_NODE_PATH, nodePath);
    }

    protected void register(boolean register) throws CoreException {
        if (register) {
            DebugPlugin plugin = DebugPlugin.getDefault();
            if (plugin != null) {
                plugin.getBreakpointManager().addBreakpoint(this);
            }
        } else {
            setRegistered(false);
        }
    }

    protected void addDatabaseBreakpointAttributes(Map<String, Object> attributes, DBSObject databaseObject, DBNNode node) {
        String datasourceId = databaseObject.getDataSource().getContainer().getId();
        attributes.put(BREAKPOINT_ATTRIBUTE_DATASOURCE_ID, datasourceId);

        String nodePath = node.getNodeItemPath();
        attributes.put(BREAKPOINT_ATTRIBUTE_NODE_PATH, nodePath);

        Map<String, Object> context = DebugCore.resolveDatabaseContext(databaseObject);
        if (context.isEmpty()) {
            return;
        }
        attributes.put(BREAKPOINT_ATTRIBUTE_DATABASE_NAME, context.get(DBGController.DATABASE_NAME));
        attributes.put(BREAKPOINT_ATTRIBUTE_SCHEMA_NAME, context.get(DBGController.SCHEMA_NAME));
        attributes.put(BREAKPOINT_ATTRIBUTE_PROCEDURE_NAME, context.get(DBGController.PROCEDURE_NAME));
        Object oid = context.get(DBGController.PROCEDURE_OID);
        attributes.put(BREAKPOINT_ATTRIBUTE_PROCEDURE_OID, String.valueOf(oid));
    }
}
