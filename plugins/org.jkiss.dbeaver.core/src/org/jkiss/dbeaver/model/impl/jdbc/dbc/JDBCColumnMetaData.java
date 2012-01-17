/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBCColumnMetaData
 */
public class JDBCColumnMetaData implements DBCColumnMetaData, IObjectImageProvider
{
    static final Log log = LogFactory.getLog(JDBCColumnMetaData.class);

    private JDBCResultSetMetaData resultSetMeta;
    private int index;
    private boolean notNull;
    private long displaySize;
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

        DBPObject rsSource = this.resultSetMeta.getResultSet().getSource();
        DBSObject dataContainer = rsSource instanceof DBCStatement ? ((DBCStatement)rsSource).getDataContainer() : null;
        DBSTable ownerTable = null;
        if (dataContainer instanceof DBSTable) {
            ownerTable = (DBSTable)dataContainer;
        }
        this.index = index;

        ResultSetMetaData metaData = resultSetMeta.getJdbcMetaData();
        this.label = metaData.getColumnLabel(index);
        this.name = metaData.getColumnName(index);
        boolean hasData = false;

        String fetchedTableName = metaData.getTableName(index);
        String fetchedCatalogName = metaData.getCatalogName(index);
        String fetchedSchemaName = metaData.getSchemaName(index);
        // Check for tables name
        // Sometimes [DBSPEC: Informix] it contains schema/catalog name inside
        if (!CommonUtils.isEmpty(fetchedTableName) && CommonUtils.isEmpty(fetchedCatalogName) && CommonUtils.isEmpty(fetchedSchemaName)) {
            final DBPDataSource dataSource = resultSetMeta.getResultSet().getContext().getDataSource();
            final DBPDataSourceInfo dsInfo = dataSource.getInfo();
            if (!DBUtils.isQuotedIdentifier(dataSource, fetchedTableName)) {
                final String catalogSeparator = dsInfo.getCatalogSeparator();
                final int catDivPos = fetchedTableName.indexOf(catalogSeparator);
                if (catDivPos != -1 && (dsInfo.getCatalogUsage() & DBPDataSourceInfo.USAGE_DML) != 0) {
                    // Catalog in table name - extract it
                    fetchedCatalogName = fetchedTableName.substring(0, catDivPos);
                    fetchedTableName = fetchedTableName.substring(catDivPos + catalogSeparator.length());
                }
                final String structSeparator = dsInfo.getStructSeparator();
                final int schemaDivPos = fetchedTableName.indexOf(structSeparator);
                if (schemaDivPos != -1 && (dsInfo.getSchemaUsage() & DBPDataSourceInfo.USAGE_DML) != 0) {
                    // Schema in table name - extract it
                    fetchedSchemaName = fetchedTableName.substring(0, schemaDivPos);
                    fetchedTableName = fetchedTableName.substring(schemaDivPos + structSeparator.length());
                }
            }
        }

        if (ownerTable != null) {
            // Get column using void monitor because all columns MUST be already read
            try {
                this.tableColumn = ownerTable.getColumn(VoidProgressMonitor.INSTANCE, name);
            }
            catch (DBException e) {
                log.warn(e);
            }
            if (this.tableColumn != null) {
                this.notNull = this.tableColumn.isNotNull();
                this.displaySize = this.tableColumn.getMaxLength();
                DBSObject tableParent = ownerTable.getParentObject();
                DBSObject tableGrandParent = tableParent == null ? null : tableParent.getParentObject();
                this.catalogName = tableParent instanceof DBSCatalog ? tableParent.getName() : tableGrandParent instanceof DBSCatalog ? tableGrandParent.getName() : null;
                this.schemaName = tableParent instanceof DBSSchema ? tableParent.getName() : null;
                this.tableName = fetchedTableName;
                this.type = this.tableColumn.getValueType();
                this.typeName = this.tableColumn.getTypeName();
                this.readOnly = false;
                this.writable = true;
                this.precision = this.tableColumn.getPrecision();
                this.scale = this.tableColumn.getScale();

                try {
                    this.tableMetaData = resultSetMeta.getTableMetaData(ownerTable);
                    if (this.tableMetaData != null) {
                        this.tableMetaData.addColumn(this);
                    }
                }
                catch (DBException e) {
                    log.warn(e);
                }
                hasData = true;
            }

        }

        if (!hasData) {
            this.notNull = metaData.isNullable(index) == ResultSetMetaData.columnNoNulls;
            try {
                this.displaySize = metaData.getColumnDisplaySize(index);
            } catch (SQLException e) {
                this.displaySize = 0;
            }
            this.catalogName = fetchedCatalogName;
            this.schemaName = fetchedSchemaName;
            this.tableName = fetchedTableName;
            this.type = metaData.getColumnType(index);
            this.typeName = metaData.getColumnTypeName(index);
            this.readOnly = metaData.isReadOnly(index);
            this.writable = metaData.isWritable(index);

            try {
                this.precision = metaData.getPrecision(index);
            } catch (Exception e) {
                // NumberFormatException occurred in Oracle on BLOB columns
                this.precision = 0;
            }
            try {
                this.scale = metaData.getScale(index);
            } catch (Exception e) {
                this.scale = 0;
            }

            try {
                if (!CommonUtils.isEmpty(this.tableName)) {
                    this.tableMetaData = resultSetMeta.getTableMetaData(catalogName, schemaName, tableName);
                    if (this.tableMetaData != null) {
                        this.tableMetaData.addColumn(this);
                    }
                }
            }
            catch (DBException e) {
                log.warn(e);
            }
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

    public boolean isNotNull()
    {
        return notNull;
    }

    public long getMaxLength()
    {
        return displaySize;
    }

    public String getLabel()
    {
        return label;
    }

    public String getName()
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
        tableColumn = tableMetaData.getTable(monitor).getColumn(monitor, name);
        return tableColumn;
    }

    public boolean isForeignKey(DBRProgressMonitor monitor)
        throws DBException
    {
        DBSTableColumn tableColumn = getTableColumn(monitor);
        if (tableColumn == null) {
            return false;
        }
        DBSTable table = tableMetaData.getTable(monitor);
        if (table == null) {
            return false;
        }
        Collection<? extends DBSForeignKey> foreignKeys = table.getAssociations(monitor);
        if (foreignKeys != null) {
            for (DBSForeignKey fk : foreignKeys) {
                if (fk.getColumn(monitor, tableColumn) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<DBSForeignKey> getForeignKeys(DBRProgressMonitor monitor)
        throws DBException
    {
        List<DBSForeignKey> refs = new ArrayList<DBSForeignKey>();
        DBSTableColumn tableColumn = getTableColumn(monitor);
        if (tableColumn == null) {
            return refs;
        }
        DBSTable table = tableMetaData.getTable(monitor);
        if (table == null) {
            return refs;
        }
        Collection<? extends DBSForeignKey> foreignKeys = table.getAssociations(monitor);
        if (foreignKeys != null) {
            for (DBSForeignKey fk : foreignKeys) {
                if (fk.getColumn(monitor, tableColumn) != null) {
                    refs.add(fk);
                }
            }
        }
        return refs;
    }

    public Image getObjectImage()
    {
        if (tableColumn instanceof IObjectImageProvider) {
            return ((IObjectImageProvider) tableColumn).getObjectImage();
        }
        return JDBCUtils.getDataIcon(this).getImage();
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


    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JDBCColumnMetaData)) {
            return false;
        }
        JDBCColumnMetaData col = (JDBCColumnMetaData)obj;
        return
            index == col.index &&
            notNull == col.notNull &&
            displaySize == col.displaySize &&
            CommonUtils.equalObjects(label, col.label) &&
            CommonUtils.equalObjects(name, col.name) &&
            precision == col.precision &&
            scale == col.scale &&
            CommonUtils.equalObjects(catalogName, col.catalogName) &&
            CommonUtils.equalObjects(schemaName, col.schemaName) &&
            CommonUtils.equalObjects(tableName, col.tableName) &&
            type == col.type &&
            CommonUtils.equalObjects(typeName, col.typeName) &&
            readOnly == col.readOnly &&
            writable == col.writable;
    }

}
