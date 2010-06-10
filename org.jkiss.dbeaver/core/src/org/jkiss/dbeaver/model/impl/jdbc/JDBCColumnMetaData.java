/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * JDBCColumnMetaData
 */
public class JDBCColumnMetaData implements DBCColumnMetaData
{
    static Log log = LogFactory.getLog(JDBCColumnMetaData.class);

    private JDBCResultSetMetaData resultSetMeta;
    private int index;
    private boolean autoIncrement;
    private boolean nullable;
    private int displaySize;
    private String label;
    private String name;
    private int precision;
    private int scale;
    private String catalogName;
    private String schemaName;
    private String tableName;
    private int type;
    private String typeName;
    private boolean readOnly;
    private boolean writable;
    private JDBCTableMetaData tableMetaData;
    private DBSTableColumn tableColumn;

    JDBCColumnMetaData(JDBCResultSetMetaData resultSetMeta, int index)
        throws SQLException
    {
        this.resultSetMeta = resultSetMeta;
        this.index = index;

        ResultSetMetaData metaData = resultSetMeta.getJdbcMetaData();
        try {
            autoIncrement = metaData.isAutoIncrement(index);
        } catch (SQLException e) {
            autoIncrement = false;
        }
        nullable = metaData.isNullable(index) > 0;
        displaySize = metaData.getColumnDisplaySize(index);
        label = metaData.getColumnLabel(index);
        name = metaData.getColumnName(index);
        catalogName = metaData.getCatalogName(index);
        schemaName = metaData.getSchemaName(index);
        tableName = metaData.getTableName(index);
        type = metaData.getColumnType(index);
        typeName = metaData.getColumnTypeName(index);
        readOnly = metaData.isReadOnly(index);
        writable = metaData.isWritable(index);

        try {
            precision = metaData.getPrecision(index);
        } catch (Exception e) {
            // NumberFormatException occured in Oracle on BLOB columns
            precision = 0;
        }
        try {
            scale = metaData.getScale(index);
        } catch (Exception e) {
            scale = 0;
        }

        try {
            if (!CommonUtils.isEmpty(tableName)) {
                tableMetaData = resultSetMeta.getTableMetaData(catalogName, schemaName, tableName);
                if (tableMetaData != null) {
                    tableMetaData.addColumn(this);
                }
            } else {
                DBSObject dataContainer = getResultSetMeta().getResultSet().getStatement().getDataContainer();
                if (dataContainer instanceof DBSTable) {
                    tableMetaData = resultSetMeta.getTableMetaData((DBSTable)dataContainer);
                    if (tableMetaData != null) {
                        tableMetaData.addColumn(this);
                    }
                }
            }
        }
        catch (DBException e) {
            log.warn(e);
        }
    }

    JDBCResultSetMetaData getResultSetMeta()
    {
        return resultSetMeta;
    }

    public int getIndex()
    {
        return index;
    }

    public boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    public boolean isNullable()
    {
        return nullable;
    }

    public int getDisplaySize()
    {
        return displaySize;
    }

    public String getLabel()
    {
        return label;
    }

    public String getColumnName()
    {
        return name;
    }

    public int getPrecision()
    {
        return precision;
    }

    public int getScale()
    {
        return scale;
    }

    public String getTableName()
    {
        return tableMetaData != null ? tableMetaData.getTableName() : tableName;
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public int getValueType()
    {
        return type;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public boolean isWritable()
    {
        return writable;
    }

    public JDBCTableMetaData getTable()
    {
        return tableMetaData;
    }

    public DBSTableColumn getTableColumn(DBRProgressMonitor monitor)
        throws DBException
    {
        if (tableColumn != null) {
            return tableColumn;
        }
        if (tableMetaData == null) {
            return null;
        }
        tableColumn = tableMetaData.getTable(monitor).getColumn(name);
        return tableColumn;
    }

    public boolean isReference(DBRProgressMonitor monitor)
        throws DBException
    {
        if (getTableColumn(monitor) == null) {
            return false;
        }
        Collection<? extends DBSForeignKey> foreignKeys = getTable().getTable(monitor).getImportedKeys();
        for (DBSForeignKey fk : foreignKeys) {
            if (fk.getColumn(tableColumn) != null) {
                return true;
            }
        }
        return false;
    }

    public List<DBSForeignKey> getReferences()
        throws DBException
    {
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder db = new StringBuilder();
        if (!CommonUtils.isEmpty(catalogName)) {
            db.append(catalogName).append('.');
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            db.append(schemaName).append('.');
        }
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
