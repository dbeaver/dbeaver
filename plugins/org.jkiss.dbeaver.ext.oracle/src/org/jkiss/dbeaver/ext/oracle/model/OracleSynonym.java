/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Oracle synonym
 */
public class OracleSynonym extends OracleSchemaObject implements DBSAlias {

    private String objectOwner;
    private String objectTypeName;
    private String objectName;
    private String dbLink;

    public OracleSynonym(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"), true);
        this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        this.objectOwner = JDBCUtils.safeGetString(dbResult, "TABLE_OWNER");
        this.objectName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.dbLink = JDBCUtils.safeGetString(dbResult, "DB_LINK");
    }

    public OracleObjectType getObjectType()
    {
        return OracleObjectType.getByType(objectTypeName);
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public String getObjectTypeName()
    {
        return objectTypeName;
    }

    @Property(viewable = true, order = 3)
    public Object getObjectOwner()
    {
        final OracleSchema schema = getDataSource().schemaCache.getCachedObject(objectOwner);
        return schema == null ? objectOwner : schema;
    }

    @Property(viewable = true, linkPossible = true, order = 4)
    public Object getObject(DBRProgressMonitor monitor) throws DBException
    {
        if (objectTypeName == null) {
            return null;
        }
        return OracleObjectType.resolveObject(
            monitor,
            getDataSource(),
            dbLink,
            objectTypeName,
            objectOwner,
            objectName);
    }

    @Property(viewable = true, order = 5)
    public Object getDbLink(DBRProgressMonitor monitor) throws DBException
    {
        return OracleDBLink.resolveObject(monitor, getSchema(), dbLink);
    }

    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        Object object = getObject(monitor);
        if (object instanceof DBSObject) {
            return (DBSObject) object;
        }
        return null;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        if (OracleConstants.USER_PUBLIC.equals(getSchema().getName())) {
            return DBUtils.getQuotedIdentifier(this);
        }
        return super.getFullyQualifiedName(context);
    }
}
