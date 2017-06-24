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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * JDBCColumnMetaData
 */
public class JDBCColumnMetaData implements DBCAttributeMetaData {
    private static final Log log = Log.getLog(JDBCColumnMetaData.class);

    public static final String PROP_CATEGORY_COLUMN = "Column";

    private final int ordinalPosition;
    private boolean notNull;
    private long displaySize;
    private final String label;
    private final String name;
    private int precision;
    private int scale;
    private final String tableName;
    private int typeID;
    private String typeName;
    private boolean readOnly;
    private boolean writable;
    private boolean sequence;
    private DBPDataKind dataKind;
    private JDBCTableMetaData tableMetaData;
    private DBCExecutionSource source;
    private String catalogName;
    private String schemaName;

    public JDBCColumnMetaData(JDBCResultSetMetaDataImpl resultSetMeta, int ordinalPosition)
        throws SQLException
    {
        this(resultSetMeta.getResultSet().getSession().getDataSource(), resultSetMeta, ordinalPosition);
        DBCStatement rsSource = resultSetMeta.getResultSet().getSourceStatement();
        this.source = rsSource != null ? rsSource.getStatementSource() : null;

        if (!CommonUtils.isEmpty(this.tableName)) {
            this.tableMetaData = resultSetMeta.getTableMetaData(catalogName, schemaName, tableName);
        } else {
            this.tableMetaData = null;
        }

        if (this.tableMetaData != null) {
            this.tableMetaData.addAttribute(this);
        }
    }

    public JDBCColumnMetaData(DBPDataSource dataSource, ResultSetMetaData resultSetMeta, int ordinalPosition)
        throws SQLException
    {
        this.ordinalPosition = ordinalPosition;

        this.label = resultSetMeta.getColumnLabel(ordinalPosition + 1);
        this.name = resultSetMeta.getColumnName(ordinalPosition + 1);
        // TODO: some drivers (DB2) always mark all columns as read only. Dunno why. So let's ignore this property
        // read-only connections are detected separately.
        this.readOnly = false;//resultSetMeta.isReadOnly(ordinalPosition + 1);

        try {
            this.writable = resultSetMeta.isWritable(ordinalPosition + 1);
        } catch (Throwable e) {
            log.debug("Can't get column writable flag: " + e.getMessage());
        }

        String fetchedTableName = null;
        try {
            fetchedTableName = resultSetMeta.getTableName(ordinalPosition + 1);
        } catch (Throwable e) {
            log.debug("Can't get column table name: " + e.getMessage());
        }
        try {
            catalogName = resultSetMeta.getCatalogName(ordinalPosition + 1);
        } catch (Throwable e) {
            log.debug("Can't get column catalog name: " + e.getMessage());
        }
        try {
            schemaName = resultSetMeta.getSchemaName(ordinalPosition + 1);
        } catch (Throwable e) {
            log.debug("Can't get column schema name: " + e.getMessage());
        }

        // Check for tables name
        // Sometimes [DBSPEC: Informix] it contains schema/catalog name inside
        if (!CommonUtils.isEmpty(fetchedTableName) && CommonUtils.isEmpty(catalogName) && CommonUtils.isEmpty(schemaName)) {
            if (dataSource instanceof SQLDataSource) {
                SQLDialect sqlDialect = ((SQLDataSource) dataSource).getSQLDialect();
                if (!DBUtils.isQuotedIdentifier(dataSource, fetchedTableName)) {
                    final String catalogSeparator = sqlDialect.getCatalogSeparator();
                    final int catDivPos = fetchedTableName.indexOf(catalogSeparator);
                    if (catDivPos != -1 && (sqlDialect.getCatalogUsage() & SQLDialect.USAGE_DML) != 0) {
                        // Catalog in table name - extract it
                        catalogName = fetchedTableName.substring(0, catDivPos);
                        fetchedTableName = fetchedTableName.substring(catDivPos + catalogSeparator.length());
                    }
                    final char structSeparator = sqlDialect.getStructSeparator();
                    final int schemaDivPos = fetchedTableName.indexOf(structSeparator);
                    if (schemaDivPos != -1 && (sqlDialect.getSchemaUsage() & SQLDialect.USAGE_DML) != 0) {
                        // Schema in table name - extract it
                        schemaName = fetchedTableName.substring(0, schemaDivPos);
                        fetchedTableName = fetchedTableName.substring(schemaDivPos + 1);
                    }
                }
            }
        }

        try {
            this.notNull = resultSetMeta.isNullable(ordinalPosition + 1) == ResultSetMetaData.columnNoNulls;
        } catch (Throwable e) {
            this.notNull = false;
            log.debug("Can't get column nullability: " + e.getMessage());
        }
        try {
            this.displaySize = resultSetMeta.getColumnDisplaySize(ordinalPosition + 1);
        } catch (Throwable e) {
            this.displaySize = 0;
        }
        try {
            this.typeName = resultSetMeta.getColumnTypeName(ordinalPosition + 1);
        } catch (Throwable e) {
            log.debug("Can't get column type name: " + e.getMessage());
            this.typeName = "unknown";
        }
        try {
            int typeID = resultSetMeta.getColumnType(ordinalPosition + 1);
            DBPDataKind dataKind = null;
            if (dataSource instanceof DBPDataTypeProvider) {
                DBSDataType dataType = ((DBPDataTypeProvider) dataSource).getLocalDataType(typeName);
                if (dataType != null) {
                    typeID = dataType.getTypeID();
                    dataKind = dataType.getDataKind();
                }
            }
            if (dataKind == null) {
                dataKind = JDBCUtils.resolveDataKind(dataSource, typeName, typeID);
            }
            this.typeID = typeID;
            this.dataKind = dataKind;
        } catch (Throwable e) {
            log.debug("Can't get column type ID: " + e.getMessage());
            this.typeID = -1;
            this.dataKind = DBPDataKind.UNKNOWN;
        }

        try {
            this.sequence = resultSetMeta.isAutoIncrement(ordinalPosition + 1);
        } catch (Throwable e) {
            this.sequence = false;
            log.debug("Can't get column auto increment: " + e.getMessage());
        }
        try {
            this.precision = resultSetMeta.getPrecision(ordinalPosition + 1);
        } catch (Throwable e) {
            // NumberFormatException occurred in Oracle on BLOB columns
            this.precision = 0;
        }
        try {
            this.scale = resultSetMeta.getScale(ordinalPosition + 1);
        } catch (Throwable e) {
            this.scale = 0;
        }

        this.tableName = fetchedTableName;
    }

    @Override
    public DBCExecutionSource getSource() {
        return source;
    }

    @NotNull
    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 1)
    @Override
    public String getName() {
        return name;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 2)
    @NotNull
    @Override
    public String getLabel() {
        return label;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 3)
    @Override
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Nullable
    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 4)
    @Override
    public String getEntityName() {
        return tableName;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 30)
    @Override
    public boolean isRequired() {
        return notNull;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 30)
    @Override
    public boolean isAutoGenerated() {
        return sequence;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 20)
    @Override
    public long getMaxLength() {
        return displaySize;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 21)
    @Override
    public int getPrecision() {
        return precision;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 22)
    @Override
    public int getScale() {
        return scale;
    }

    @Override
    public int getTypeID() {
        return typeID;
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    @Property(viewable = true, category = PROP_CATEGORY_COLUMN, order = 4)
    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isWritable() {
        return writable;
    }

    @Nullable
    @Override
    public JDBCTableMetaData getEntityMetaData() {
        return tableMetaData;
    }

    @Override
    public String toString() {
        StringBuilder db = new StringBuilder();
        if (!CommonUtils.isEmpty(tableName)) {
            db.append(tableName).append('.');
        }
        if (!CommonUtils.isEmpty(name)) {
            db.append(name);
        }
        if (!CommonUtils.isEmpty(label)) {
            db.append(" as ").append(label);
        }
        return db.toString();
    }

}
