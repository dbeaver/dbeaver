/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * SQL Server synonym.
 */
public class SQLServerSynonym implements DBSAlias, DBSObject, DBPQualifiedObject, SQLServerObject
{
    private static final Log log = Log.getLog(SQLServerSynonym.class);

    private long objectId;
    @NotNull
    private SQLServerSchema schema;
    @NotNull
    private String name;
    @NotNull
    private String targetObjectName;
    private String description;

    private boolean persisted;

    protected SQLServerSynonym(@NotNull SQLServerSchema schema, long objectId, @NotNull String name, @NotNull String targetObjectName, boolean persisted) {
        this.schema = schema;
        this.objectId = objectId;
        this.name = name;
        this.targetObjectName = targetObjectName;

        this.persisted = persisted;
    }

    @Property(viewable = false, order = 80)
    @Override
    public long getObjectId() {
        return objectId;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Nullable
    @Override
    //@Property(viewable = true, multiline = true, order = 10)
    public String getDescription() {
        return description;
    }

    @NotNull
    @Override
    public SQLServerSchema getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return schema.getDataSource();
    }

    @Nullable
    @Override
    public SQLServerDatabase getDatabase() {
        return schema.getDatabase();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        if (context == DBPEvaluationContext.DDL ||
            !SQLServerUtils.supportsCrossDatabaseQueries(getDataSource()))
        {
            return DBUtils.getFullQualifiedName(getDataSource(), schema, this);
        }
        return DBUtils.getFullQualifiedName(getDataSource(),
            schema.getDatabase(),
            schema,
            this);
    }

    @Property(viewable = true, order = 20)
    @Override
    public DBSObject getTargetObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        String databaseName;
        String schemaName;
        String objectName;

        int divPos = targetObjectName.indexOf("].[");
        if (divPos == -1) {
            databaseName = schema.getDatabase().getName();
            schemaName = schema.getName();
            objectName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName);
        } else {
            int divPos2 = targetObjectName.indexOf("].[", divPos + 1);
            if (divPos2 == -1) {
                databaseName = schema.getDatabase().getName();
                schemaName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(0, divPos + 1));
                objectName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(divPos + 2));
            } else {
                databaseName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(0, divPos + 1));
                schemaName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(divPos + 2, divPos2 + 1));
                objectName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(divPos2 + 2));
            }
        }
        SQLServerDatabase database = schema.getDataSource().getDatabase(databaseName);
        if (database == null) {
            log.debug("Database '" + databaseName + "' not found for synonym '" + getName() + "'");
            return null;
        }
        SQLServerSchema targetSchema = database.getSchema(monitor, schemaName);
        if (targetSchema == null) {
            log.debug("Schema '" + schemaName + "' not found for synonym '" + getName() + "'");
            return null;
        }
        return targetSchema.getChild(monitor, objectName);
    }

}
