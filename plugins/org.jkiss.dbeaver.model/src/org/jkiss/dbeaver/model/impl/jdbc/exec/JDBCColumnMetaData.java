/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
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
public class JDBCColumnMetaData implements DBCAttributeMetaData, DBPImageProvider {
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
    private final int typeID;
    private String typeName;
    private boolean readOnly;
    private boolean writable;
    private boolean sequence;
    private final DBPDataKind dataKind;
    private JDBCTableMetaData tableMetaData;
    private DBDPseudoAttribute pseudoAttribute;
    private Object source;
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
        } catch (SQLException e) {
            log.debug(e);
        }

        String fetchedTableName = null;
        try {
            fetchedTableName = resultSetMeta.getTableName(ordinalPosition + 1);
        } catch (SQLException e) {
            log.debug(e);
        }
        try {
            catalogName = resultSetMeta.getCatalogName(ordinalPosition + 1);
        } catch (SQLException e) {
            log.debug(e);
        }
        try {
            schemaName = resultSetMeta.getSchemaName(ordinalPosition + 1);
        } catch (SQLException e) {
            log.debug(e);
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
        } catch (SQLException e) {
            this.notNull = false;
            log.debug(e);
        }
        try {
            this.displaySize = resultSetMeta.getColumnDisplaySize(ordinalPosition + 1);
        } catch (SQLException e) {
            this.displaySize = 0;
        }
        try {
            this.typeName = resultSetMeta.getColumnTypeName(ordinalPosition + 1);
        } catch (SQLException e) {
            log.debug(e);
            this.typeName = "unknown";
        }
        {
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
        }

        try {
            this.sequence = resultSetMeta.isAutoIncrement(ordinalPosition + 1);
        } catch (SQLException e) {
            this.sequence = false;
            log.debug(e);
        }
        try {
            this.precision = resultSetMeta.getPrecision(ordinalPosition + 1);
        } catch (Exception e) {
            // NumberFormatException occurred in Oracle on BLOB columns
            this.precision = 0;
        }
        try {
            this.scale = resultSetMeta.getScale(ordinalPosition + 1);
        } catch (Exception e) {
            this.scale = 0;
        }

        this.tableName = fetchedTableName;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 1)
    @Override
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @NotNull
    @Property(category = PROP_CATEGORY_COLUMN, order = 2)
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getSource() {
        return source;
    }

    //@Property(category = PROP_CATEGORY_COLUMN, order = 3)
    @NotNull
    @Override
    public String getLabel() {
        return label;
    }

    @Nullable
    @Property(category = PROP_CATEGORY_COLUMN, order = 4)
    @Override
    public String getEntityName() {
        return tableName;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 30)
    @Override
    public boolean isRequired() {
        return notNull;
    }

    @Override
    public boolean isAutoGenerated() {
        return sequence;
    }

    @Override
    public boolean isPseudoAttribute() {
        return pseudoAttribute != null;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 20)
    @Override
    public long getMaxLength() {
        return displaySize;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 21)
    @Override
    public int getPrecision() {
        return precision;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 22)
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

    @Property(category = PROP_CATEGORY_COLUMN, order = 4)
    @Override
    public String getTypeName() {
        return typeName;
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
    public DBDPseudoAttribute getPseudoAttribute() {
        return pseudoAttribute;
    }

    public void setPseudoAttribute(DBDPseudoAttribute pseudoAttribute) {
        this.pseudoAttribute = pseudoAttribute;
    }

    @Nullable
    @Override
    public JDBCTableMetaData getEntityMetaData() {
        return tableMetaData;
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBUtils.getDataIcon(this);
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
