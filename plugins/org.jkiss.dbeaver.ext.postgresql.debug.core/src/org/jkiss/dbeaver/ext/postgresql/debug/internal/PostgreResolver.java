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
package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import java.util.HashMap;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class PostgreResolver implements DBGResolver {

    private final PostgreDataSource dataSource;

    public PostgreResolver(PostgreDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObject resolveObject(Map<String, Object> context, Object identifier, DBRProgressMonitor monitor)
            throws DBException {
        Long oid = null;
        final String errorIdentifier = String.format("Unknown procedure identifier %s", identifier);
        if (identifier instanceof Number) {
            Number number = (Number) identifier;
            oid = number.longValue();
        } else if (identifier instanceof String) {
            String string = (String) identifier;
            try {
                oid = Long.parseLong(string);
            } catch (NumberFormatException e) {
                throw new DBException(errorIdentifier, e, dataSource);
            }
        }
        if (oid == null) {
            throw new DBException(errorIdentifier);
        }
        String databaseName = String.valueOf(context.get(DBGController.DATABASE_NAME));
        PostgreDatabase database = dataSource.getDatabase(databaseName);
        if (database == null) {
            return null;
        }
        String schemaName = String.valueOf(context.get(DBGController.SCHEMA_NAME));
        PostgreSchema schema = null;
        schema = database.getSchema(monitor, schemaName);
        if (schema == null) {
            return null;
        }
        PostgreProcedure procedure = schema.getProcedure(monitor, oid);
        return procedure;
    }

    @Override
    public Map<String, Object> resolveContext(DBSObject databaseObject) {
        HashMap<String, Object> context = new HashMap<String, Object>();
        if (databaseObject instanceof PostgreProcedure) {
            PostgreProcedure procedure = (PostgreProcedure) databaseObject;
            context.put(DBGController.PROCEDURE_OID, procedure.getObjectId());
            context.put(DBGController.PROCEDURE_NAME, procedure.getName());
            PostgreSchema schema = procedure.getContainer();
            if (schema != null) {
                context.put(DBGController.SCHEMA_NAME, schema.getName());
            }
            PostgreDatabase database = procedure.getDatabase();
            if (database != null) {
                context.put(DBGController.DATABASE_NAME, database.getName());
            }
        }
        return context;
    }

}
