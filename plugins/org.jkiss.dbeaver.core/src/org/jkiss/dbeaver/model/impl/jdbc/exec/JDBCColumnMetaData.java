/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * JDBCColumnMetaData
 */
public class JDBCColumnMetaData implements DBCAttributeMetaData, IObjectImageProvider
{
    static final Log log = LogFactory.getLog(JDBCColumnMetaData.class);

    public static final String PROP_CATEGORY_COLUMN = "Column";

    private int ordinalPosition;
    private boolean notNull;
    private long displaySize;
    private String label;
    private String name;
    private int precision;
    private int scale;
    private String tableName;
    private int typeID;
    private String typeName;
    private boolean readOnly;
    private boolean writable;
    private boolean sequence;
    private JDBCTableMetaData tableMetaData;
    private DBSEntityAttribute tableColumn;
    private final DBPDataKind dataKind;
    private DBDPseudoAttribute pseudoAttribute;

    protected JDBCColumnMetaData(JDBCResultSetMetaData resultSetMeta, int ordinalPosition)
        throws SQLException
    {
        DBCStatement rsSource = resultSetMeta.getResultSet().getSourceStatement();
        Object statementSource = rsSource != null ? rsSource.getStatementSource() : null;
        DBSEntity ownerEntity = null;
        if (statementSource instanceof DBSEntity) {
            ownerEntity = (DBSEntity)statementSource;
        }
        this.ordinalPosition = ordinalPosition;

        this.label = resultSetMeta.getColumnLabel(ordinalPosition);
        this.name = resultSetMeta.getColumnName(ordinalPosition);
        this.readOnly = resultSetMeta.isReadOnly(ordinalPosition);
        this.writable = resultSetMeta.isWritable(ordinalPosition);

        String fetchedTableName = null;
        try {
            fetchedTableName = resultSetMeta.getTableName(ordinalPosition);
        } catch (SQLException e) {
            log.debug(e);
        }
        String fetchedCatalogName = null;
        try {
            fetchedCatalogName = resultSetMeta.getCatalogName(ordinalPosition);
        } catch (SQLException e) {
            log.debug(e);
        }
        String fetchedSchemaName = null;
        try {
            fetchedSchemaName = resultSetMeta.getSchemaName(ordinalPosition);
        } catch (SQLException e) {
            log.debug(e);
        }

        // Check for tables name
        // Sometimes [DBSPEC: Informix] it contains schema/catalog name inside
        if (!CommonUtils.isEmpty(fetchedTableName) && CommonUtils.isEmpty(fetchedCatalogName) && CommonUtils.isEmpty(fetchedSchemaName)) {
            final DBPDataSource dataSource = resultSetMeta.getResultSet().getSession().getDataSource();
            if (dataSource instanceof SQLDataSource) {
                SQLDialect sqlDialect = ((SQLDataSource) dataSource).getSQLDialect();
                if (!DBUtils.isQuotedIdentifier(dataSource, fetchedTableName)) {
                    final String catalogSeparator = sqlDialect.getCatalogSeparator();
                    final int catDivPos = fetchedTableName.indexOf(catalogSeparator);
                    if (catDivPos != -1 && (sqlDialect.getCatalogUsage() & SQLDialect.USAGE_DML) != 0) {
                        // Catalog in table name - extract it
                        fetchedCatalogName = fetchedTableName.substring(0, catDivPos);
                        fetchedTableName = fetchedTableName.substring(catDivPos + catalogSeparator.length());
                    }
                    final char structSeparator = sqlDialect.getStructSeparator();
                    final int schemaDivPos = fetchedTableName.indexOf(structSeparator);
                    if (schemaDivPos != -1 && (sqlDialect.getSchemaUsage() & SQLDialect.USAGE_DML) != 0) {
                        // Schema in table name - extract it
                        fetchedSchemaName = fetchedTableName.substring(0, schemaDivPos);
                        fetchedTableName = fetchedTableName.substring(schemaDivPos + 1);
                    }
                }
            }
        }

        if (ownerEntity != null) {
            this.tableName = ownerEntity.getName();
            try {
                this.tableColumn = ownerEntity.getAttribute(resultSetMeta.getResultSet().getSession().getProgressMonitor(), name);
            }
            catch (DBException e) {
                log.warn(e);
            }
            try {
                this.tableMetaData = resultSetMeta.getTableMetaData(ownerEntity);
            }
            catch (DBException e) {
                log.warn(e);
            }
        } else {
            this.tableName = fetchedTableName;
        }

        if (this.tableColumn == null) {
            this.notNull = resultSetMeta.isNullable(ordinalPosition) == ResultSetMetaData.columnNoNulls;
            try {
                this.displaySize = resultSetMeta.getColumnDisplaySize(ordinalPosition);
            } catch (SQLException e) {
                this.displaySize = 0;
            }
            this.typeID = resultSetMeta.getColumnType(ordinalPosition);
            this.typeName = resultSetMeta.getColumnTypeName(ordinalPosition);
            this.sequence = resultSetMeta.isAutoIncrement(ordinalPosition);

            try {
                this.precision = resultSetMeta.getPrecision(ordinalPosition);
            } catch (Exception e) {
                // NumberFormatException occurred in Oracle on BLOB columns
                this.precision = 0;
            }
            try {
                this.scale = resultSetMeta.getScale(ordinalPosition);
            } catch (Exception e) {
                this.scale = 0;
            }
        }

        if (this.tableMetaData == null) {
            try {
                if (!CommonUtils.isEmpty(this.tableName)) {
                    this.tableMetaData = resultSetMeta.getTableMetaData(fetchedCatalogName, fetchedSchemaName, tableName);
                }
            }
            catch (DBException e) {
                log.warn(e);
            }
        }
        if (this.tableMetaData != null) {
            this.tableMetaData.addAttribute(this);
        }

        dataKind = JDBCUtils.resolveDataKind(resultSetMeta.getResultSet().getSourceStatement().getSession().getDataSource(), typeName, typeID);
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 1)
    @Override
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 2)
    @Override
    public String getName()
    {
        return name;
    }

    //@Property(category = PROP_CATEGORY_COLUMN, order = 3)
    @NotNull
    @Override
    public String getLabel()
    {
        return label;
    }

    @Nullable
    @Property(category = PROP_CATEGORY_COLUMN, order = 4)
    @Override
    public String getEntityName()
    {
        return tableMetaData != null ? tableMetaData.getEntityName() : tableName;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 30)
    @Override
    public boolean isRequired()
    {
        return tableColumn == null || pseudoAttribute != null ? notNull : tableColumn.isRequired();
    }

    @Override
    public boolean isAutoGenerated() {
        return tableColumn == null || pseudoAttribute != null ? sequence : tableColumn.isAutoGenerated();
    }

    @Override
    public boolean isPseudoAttribute() {
        return pseudoAttribute != null;
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 20)
    @Override
    public long getMaxLength()
    {
        return tableColumn == null || pseudoAttribute != null ? displaySize : tableColumn.getMaxLength();
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 21)
    @Override
    public int getPrecision()
    {
        return tableColumn == null || pseudoAttribute != null ? precision : tableColumn.getPrecision();
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 22)
    @Override
    public int getScale()
    {
        return tableColumn == null || pseudoAttribute != null ? scale : tableColumn.getScale();
    }

    @Override
    public int getTypeID()
    {
        return tableColumn == null || pseudoAttribute != null ? typeID : tableColumn.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return tableColumn == null || pseudoAttribute != null ? dataKind : tableColumn.getDataKind();
    }

    @Property(category = PROP_CATEGORY_COLUMN, order = 4)
    @Override
    public String getTypeName()
    {
        return tableColumn == null || pseudoAttribute != null ? typeName : tableColumn.getTypeName();
    }

    @Override
    public boolean isReadOnly()
    {
        return readOnly;
    }

    @Nullable
    @Override
    public DBDPseudoAttribute getPseudoAttribute()
    {
        return pseudoAttribute;
    }

    public void setPseudoAttribute(DBDPseudoAttribute pseudoAttribute)
    {
        this.pseudoAttribute = pseudoAttribute;
    }

    @Nullable
    @Override
    public JDBCTableMetaData getEntityMetaData()
    {
        return tableMetaData;
    }

    @Nullable
    @Override
    public DBSEntityAttribute getEntityAttribute(DBRProgressMonitor monitor)
        throws DBException
    {
        if (tableColumn != null) {
            return tableColumn;
        }
        if (tableMetaData == null) {
            return null;
        }
        DBSEntity entity = tableMetaData.getEntity(monitor);
        if (entity == null) {
            return null;
        }
        if (pseudoAttribute != null) {
            tableColumn = pseudoAttribute.createFakeAttribute(entity, this);
        } else {
            tableColumn = entity.getAttribute(monitor, name);
        }
        return tableColumn;
    }

    @Nullable
    @Override
    public Image getObjectImage()
    {
        if (tableColumn instanceof IObjectImageProvider) {
            return ((IObjectImageProvider) tableColumn).getObjectImage();
        }
        return DBUtils.getDataIcon(this).getImage();
    }

    @Override
    public String toString()
    {
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
