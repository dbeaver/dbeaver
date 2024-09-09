/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL Server data type
 */
public class SQLServerDataType implements DBSDataType, SQLServerObject, DBPQualifiedObject, DBPScriptObject, DBSObjectWithScript, DBSEntity {

    private static final Log log = Log.getLog(SQLServerDataType.class);

    private final DBSObject owner;
    private final String name;
    private final int valueType;
    private final DBPDataKind dataKind;
    private final int systemTypeId;
    private final int userTypeId;
    private long schemaId;
    private int maxLength;
    private int scale;
    private int precision;
    private boolean nullable;
    private boolean userType;
    private boolean assemblyType;
    private String collationName;
    private int tableTypeId = 0;
    private int tableTypeSchemaId = 0;

    private boolean persisted;

    public SQLServerDataType(DBSObject owner, ResultSet dbResult) {
        this.owner = owner;

        String nameValue = JDBCUtils.safeGetString(dbResult, "name");
        this.name = DBUtils.getUnQuotedIdentifier(owner.getDataSource(), CommonUtils.notEmpty(nameValue));
        this.systemTypeId = JDBCUtils.safeGetInt(dbResult, "system_type_id");
        this.userTypeId = JDBCUtils.safeGetInt(dbResult, "user_type_id");
        this.schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
        this.maxLength = JDBCUtils.safeGetInt(dbResult, "max_length");
        this.scale = JDBCUtils.safeGetInt(dbResult, "scale");
        this.precision = JDBCUtils.safeGetInt(dbResult, "precision");

        this.nullable = JDBCUtils.safeGetInt(dbResult, "is_nullable") != 0;
        this.userType = JDBCUtils.safeGetInt(dbResult, "is_user_defined") != 0;
        this.assemblyType = JDBCUtils.safeGetInt(dbResult, "is_assembly_type") != 0;
        boolean isTableType = JDBCUtils.safeGetInt(dbResult, "is_table_type") != 0;

        this.collationName = JDBCUtils.safeGetString(dbResult, "collation_name");

        if (isTableType) {
            this.tableTypeId = JDBCUtils.safeGetInt(dbResult, "type_table_object_id");
            this.tableTypeSchemaId = JDBCUtils.safeGetInt(dbResult, "type_table_schema_id");
        }

        if (userType) {
            if (systemTypeId == SQLServerConstants.TABLE_TYPE_SYSTEM_ID) { // 243 - ID of user defined table types
                this.dataKind = DBPDataKind.OBJECT;
                this.valueType = Types.OTHER;
            } else {
                SQLServerDataType systemDataType = getSystemDataType();
                this.dataKind = systemDataType == null ? DBPDataKind.UNKNOWN : systemDataType.getDataKind();
                this.valueType = systemDataType == null ? Types.OTHER : systemDataType.getTypeID();
            }
        } else {
            this.dataKind = getDataKindByName(this.name);
            this.valueType = getDataTypeIDByName(this.name);
        }

        this.persisted = true;
    }

    public SQLServerDataType(DBSObject owner, String name, int systemId, DBPDataKind dataKind, int valueType) {
        this.owner = owner;
        this.name = name;
        this.systemTypeId = userTypeId = systemId;
        this.dataKind = dataKind;
        this.valueType = valueType;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return (SQLServerDataSource) owner.getDataSource();
    }

    @Property(order = 80)
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

    @Property(order = 5)
    @Nullable
    public SQLServerSchema getSchema(DBRProgressMonitor monitor) throws DBException {
        return owner instanceof SQLServerDatabase db ? db.getSchema(monitor, schemaId) : null;
    }

    @Nullable
    public SQLServerSchema getSysSchema(DBRProgressMonitor monitor) throws DBException {
        if (owner instanceof SQLServerDatabase db) {
            return db.getSysSchema(monitor);
        } else {
            if (owner instanceof SQLServerDataSource ds) {
                return ds.getDefaultDatabase(monitor) != null ? ds.getDefaultDatabase(monitor).getSysSchema(monitor) : null;
            }
        }
        return null;
    }

    public SQLServerDataType getSystemDataType() {
        if (userType) {
            return getDataSource().getSystemDataType(systemTypeId);
        }
        return this;
    }

    public SQLServerDatabase getContainer() {
        if (owner instanceof SQLServerDatabase) return (SQLServerDatabase) owner;
        return null;
    }

    @Override
    public SQLServerDatabase getDatabase() {
        if (owner instanceof SQLServerDatabase) {
            return (SQLServerDatabase) owner;
        }
        return ((SQLServerDataSource) owner).getDefaultDatabase(new VoidProgressMonitor()); // Monitor is not significant here, so we can use Void monitor
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- DROP TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(";\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
        if (tableTypeId == 0) {
            sql.append("CREATE TYPE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append("\n").append("FROM ");
            SQLServerDataType systemDataType = getSystemDataType();
            String typeName = systemDataType.getName();
            sql.append(typeName.toUpperCase(Locale.ENGLISH));
            SQLServerTypedObject serverTypedObjects = new SQLServerTypedObject(typeName, getDataTypeIDByName(typeName), dataKind, scale, precision, maxLength);
            String modifiers = SQLUtils.getColumnTypeModifiers(getDataSource(), serverTypedObjects, typeName, dataKind);
            if (modifiers != null) {
                sql.append(modifiers);
            }
            if (!nullable) {
                sql.append(" NOT NULL;");
            }
        } else {
            try {
                SQLServerTableType tableType = getTableType(monitor);
                if (tableType != null) {
                    options.put(DBPScriptObject.OPTION_USE_SPECIAL_NAME, name);
                    String objectDefinitionText = tableType.getObjectDefinitionText(monitor, options);
                    sql.append(objectDefinitionText);
                } else {
                    return
                        "-- Error generating table DDL\n" +
                        "-- Table type " + tableTypeId + " not found in system catalog (" + tableTypeSchemaId + ")";
                }
            } catch (DBException e) {
                log.debug("Cannot load table type definition", e);
            }
        }
        return sql.toString();
    }

    @NotNull
    @Override
    public String getTypeName() {
        return getFullyQualifiedName(DBPEvaluationContext.DDL);
    }

    @NotNull
    @Property(order = 70)
    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID() {
        return valueType;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    @Nullable
    @Override
    @Property(order = 20)
    public Integer getScale() {
        return scale == 0 ? null : scale;
    }

    @Nullable
    @Override
    @Property(order = 21)
    public Integer getPrecision() {
        return precision == 0 ? null : precision;
    }

    @Override
    @Property(order = 22)
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public long getTypeModifiers() {
        return 0;
    }

    @Override
    public int getMinScale() {
        return scale;
    }

    @Override
    public int getMaxScale() {
        return scale;
    }

    @Property(order = 23)
    public boolean isNullable() {
        return nullable;
    }

    @Property(order = 24)
    public boolean isAssemblyType() {
        return assemblyType;
    }

    @Property(order = 25)
    public boolean isTableType() {
        return tableTypeId != 0;
    }

    @Property(order = 26)
    public String getCollationName() {
        return collationName;
    }

    public int getTableTypeId() {
        return tableTypeId;
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

    public SQLServerDataType getComponentType(@NotNull DBRProgressMonitor monitor) {
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
            case SQLServerConstants.TYPE_CHAR:
            case SQLServerConstants.TYPE_NCHAR:
            case SQLServerConstants.TYPE_NTEXT:
            case SQLServerConstants.TYPE_NVARCHAR:
            case SQLServerConstants.TYPE_VARCHAR:
            case SQLServerConstants.TYPE_TEXT:
            case "sysname":
                return DBPDataKind.STRING;

            case SQLServerConstants.TYPE_TINYINT:
            case SQLServerConstants.TYPE_BIGINT:
            case SQLServerConstants.TYPE_BIT:
            case SQLServerConstants.TYPE_INT:
            case SQLServerConstants.TYPE_NUMERIC:
            case SQLServerConstants.TYPE_REAL:
            case SQLServerConstants.TYPE_SMALLINT:
            case SQLServerConstants.TYPE_DECIMAL:
            case SQLServerConstants.TYPE_FLOAT:
            case SQLServerConstants.TYPE_MONEY:
            case SQLServerConstants.TYPE_SMALLMONEY:
                return DBPDataKind.NUMERIC;

            case SQLServerConstants.TYPE_DATETIME:
            case SQLServerConstants.TYPE_DATETIME2:
            case SQLServerConstants.TYPE_SMALLDATETIME:
            case SQLServerConstants.TYPE_DATE:
            case SQLServerConstants.TYPE_TIME:
                return DBPDataKind.DATETIME;

            case SQLServerConstants.TYPE_DATETIMEOFFSET:
                return DBPDataKind.DATETIME;

            case SQLServerConstants.TYPE_BINARY:
            case SQLServerConstants.TYPE_VARBINARY:
            case SQLServerConstants.TYPE_TIMESTAMP:
                return DBPDataKind.BINARY;

            case SQLServerConstants.TYPE_IMAGE:
                return DBPDataKind.CONTENT;

            case SQLServerConstants.TYPE_UNIQUEIDENTIFIER:
                return DBPDataKind.STRING;

            case SQLServerConstants.TYPE_GEOGRAPHY:
            case SQLServerConstants.TYPE_GEOMETRY:
            case SQLServerConstants.TYPE_HIERARCHYID:
                return DBPDataKind.BINARY;

            case SQLServerConstants.TYPE_SQL_VARIANT:
                return DBPDataKind.OBJECT;

            case SQLServerConstants.TYPE_XML:
                return DBPDataKind.CONTENT;

            default:
                return DBPDataKind.OBJECT;
        }
    }

    public static int getDataTypeIDByName(String systemTypeName) {
        switch (systemTypeName) {
            case "char":
                return Types.CHAR;
            case "nchar":
                return Types.NCHAR;
            case "ntext":
            case "nvarchar":
                return Types.NVARCHAR;
            case "varchar":
            case "text":
                return Types.VARCHAR;

            case "sysname":
                return Types.VARCHAR;

            case "tinyint":
                return Types.TINYINT;
            case "bigint":
                return Types.BIGINT;
            case "bit":
                return Types.BIT;
            case "int":
                return Types.INTEGER;
            case "numeric":
                return Types.NUMERIC;
            case "real":
                return Types.REAL;
            case "smallint":
                return Types.SMALLINT;
            case "decimal":
                return Types.DECIMAL;
            case "float":
                return Types.FLOAT;

            case "date":
                return Types.DATE;
            case "datetime":
            case "datetime2":
                return Types.TIMESTAMP;
            case "smalldatetime":
                return Types.TIMESTAMP;
            case "time":
                return Types.TIME;
            case "timestamp":
                return Types.TIMESTAMP;
            case "datetimeoffset":
                return Types.VARCHAR;
            case "binary":
                return Types.BINARY;
            case "image":
            case "varbinary":
                return Types.VARBINARY;
            case "uniqueidentifier":
                return Types.VARBINARY;

            case "geography":
            case "geometry":
            case "hierarchyid":
                return Types.BINARY;
            case "money":
            case "smallmoney":
            case "sql_variant":
                return Types.OTHER;

            case "xml":
                return Types.SQLXML;

            default:
                return Types.OTHER;
        }
    }

    @Override
    public void setObjectDefinitionText(String source) {
        
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TYPE;
    }

    @Nullable
    @Override
    public List<SQLServerTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        SQLServerTableType type = getTableType(monitor);
        if (type != null) {
            return type.getAttributes(monitor);
        }
        return null;
    }

    @Nullable
    @Override
    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        SQLServerTableType type = getTableType(monitor);
        if (type != null) {
            return type.getAttribute(monitor, attributeName);
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<SQLServerTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        SQLServerTableType type = getTableType(monitor);
        if (type != null) {
            return type.getConstraints(monitor);
        }
        return null;
    }

    private SQLServerTableType getTableType(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!isTableType()) {
            return null;
        }
        SQLServerSchema sysSchema = owner instanceof SQLServerDatabase db ? db.getSchema(monitor, tableTypeSchemaId) : null;
        if (sysSchema != null) {
            SQLServerTableType tableType = sysSchema.getTableType(monitor, tableTypeId);
            if (tableType != null) {
                return tableType;
            } else {
                log.debug("Table type " + tableTypeId + " not found in schema " + sysSchema.getName());
            }
        } else {
            log.debug("Table type " + tableTypeId + " schema " + tableTypeSchemaId + " not found in database");
        }
        return null;
    }

    @Override
    public Collection<SQLServerTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<SQLServerTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }


}
