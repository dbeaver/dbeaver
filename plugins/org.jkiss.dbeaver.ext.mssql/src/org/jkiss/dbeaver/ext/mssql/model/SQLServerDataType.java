/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;
import java.util.Map;

/**
 * Oracle data type
 */
public class SQLServerDataType implements DBSDataType, SQLServerObject, DBPQualifiedObject, DBPScriptObject {

    private static final Log log = Log.getLog(SQLServerDataType.class);

    private DBSObject owner;
    private String name;
    private DBPDataKind dataKind;
    private int systemTypeId;
    private int userTypeId;
    private long schemaId;
    private int maxLength;
    private int scale;
    private int precision;
    private boolean nullable;
    private boolean userType;
    private boolean assemblyType;
    private String collationName;

    private boolean persisted;

    public SQLServerDataType(DBSObject owner, ResultSet dbResult) {
        this.owner = owner;

        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.systemTypeId = JDBCUtils.safeGetInt(dbResult, "system_type_id");
        this.userTypeId = JDBCUtils.safeGetInt(dbResult, "user_type_id");
        this.schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
        this.maxLength = JDBCUtils.safeGetInt(dbResult, "max_length");
        this.scale = JDBCUtils.safeGetInt(dbResult, "scale");
        this.precision = JDBCUtils.safeGetInt(dbResult, "precision");

        this.nullable = JDBCUtils.safeGetInt(dbResult, "is_nullable") != 0;
        this.userType = JDBCUtils.safeGetInt(dbResult, "is_user_defined") != 0;
        this.assemblyType = JDBCUtils.safeGetInt(dbResult, "is_assembly_type") != 0;

        this.collationName = JDBCUtils.safeGetString(dbResult, "collation_name");

        this.dataKind = userType ? getSystemDataType().getDataKind() : getDataKindByName(this.name);

        this.persisted = true;
    }

    @Override
    public SQLServerDataSource getDataSource() {
        return (SQLServerDataSource) owner.getDataSource();
    }

    @Property(viewable = false, order = 80)
    @Override
    public long getObjectId() {
        return userTypeId;
    }

    public boolean isUserType() {
        return userType;
    }

    long getSchemaId() {
        return schemaId;
    }

    @Property(viewable = false, order = 5)
    @Nullable
    public SQLServerSchema getSchema(DBRProgressMonitor monitor) throws DBException {
        return owner instanceof SQLServerDatabase ? ((SQLServerDatabase) owner).getSchema(monitor, schemaId) : null;
    }

    public SQLServerDataType getSystemDataType() {
        if (userType) {
            return (SQLServerDataType) getDataSource().getLocalDataType(systemTypeId);
        }
        return this;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
        return "";
    }

    @Override
    public String getTypeName() {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @Property(viewable = false, order = 70)
    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID() {
        return userTypeId;
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    @Property(viewable = false, order = 20)
    @Override
    public Integer getScale() {
        return scale == 0 ? null : scale;
    }

    @Property(viewable = false, order = 21)
    @Override
    public Integer getPrecision() {
        return precision == 0 ? null : precision;
    }

    @Property(viewable = false, order = 22)
    @Override
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public int getMinScale() {
        return scale;
    }

    @Override
    public int getMaxScale() {
        return scale;
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        return DBUtils.getDefaultOperators(this);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return userTypeId;
    }

    public SQLServerDataType getComponentType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return owner instanceof SQLServerSchema ?
            DBUtils.getFullQualifiedName(getDataSource(), ((SQLServerSchema) owner).getDatabase(), owner, this) :
            name;
    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    public static DBPDataKind getDataKindByName(String systemTypeName) {
        switch (systemTypeName) {
            case "char":
            case "nchar":
            case "ntext":
            case "nvarchar":
            case "varchar":
            case "text":
            case "sysname":
                return DBPDataKind.STRING;

            case "tinyint":
            case "bigint":
            case "bit":
            case "int":
            case "numeric":
            case "real":
            case "smallint":
            case "decimal":
            case "float":
                return DBPDataKind.NUMERIC;

            case "date":
            case "datetime":
            case "datetime2":
            case "datetimeoffset":
            case "smalldatetime":
            case "time":
            case "timestamp":
                return DBPDataKind.DATETIME;

            case "binary":
                return DBPDataKind.BINARY;

            case "image":
            case "varbinary":
                return DBPDataKind.CONTENT;

            case "uniqueidentifier":
                return DBPDataKind.STRING;

            case "geography":
            case "geometry":
            case "hierarchyid":

            case "money":
            case "smallmoney":

            case "sql_variant":
                return DBPDataKind.OBJECT;

            case "xml":
                return DBPDataKind.CONTENT;

            default:
                return DBPDataKind.OBJECT;
        }
    }

}
