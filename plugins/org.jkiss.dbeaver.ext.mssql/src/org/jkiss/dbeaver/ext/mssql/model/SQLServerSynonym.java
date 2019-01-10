/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
    private SQLServerSchema schema;
    private String name;
    private String targetObjectName;
    private String description;

    private boolean persisted;

    protected SQLServerSynonym(SQLServerSchema schema, long objectId, String name, String targetObjectName, boolean persisted) {
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

    @Nullable
    @Override
    public SQLServerSchema getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return schema.getDataSource();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            schema.getDatabase(),
            schema,
            this);
    }

    @Property(viewable = true, order = 20)
    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        int divPos = targetObjectName.indexOf("].[");
        if (divPos == -1) {
            log.debug("Bad target object name '" + targetObjectName + "' for synonym '" + getName() + "'");
            return null;
        }
        String schemaName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(0, divPos + 1));
        String objectName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(divPos + 2));
        SQLServerSchema targetSchema = schema.getDatabase().getSchema(monitor, schemaName);
        if (targetSchema == null) {
            log.debug("Schema '" + schemaName + "' not found for synonym '" + getName() + "'");
            return null;
        }
        return targetSchema.getChild(monitor, objectName);
    }

}
