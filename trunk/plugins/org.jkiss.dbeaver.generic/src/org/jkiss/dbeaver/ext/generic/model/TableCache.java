/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ext.generic.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetadataReader;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Tables cache implementation
 */
public class TableCache extends JDBCStructCache<GenericStructContainer, GenericTable, GenericTableColumn> {

    static final Log log = LogFactory.getLog(TableCache.class);

    // Tables types which are not actually a table
    // This is needed for some strange JDBC drivers which returns not a table objects
    // in DatabaseMetaData.getTables method (PostgreSQL especially)
    private static final Set<String> INVALID_TABLE_TYPES = new HashSet<String>();

    static {
        // [JDBC: PostgreSQL]
        INVALID_TABLE_TYPES.add("INDEX");
        INVALID_TABLE_TYPES.add("SEQUENCE");
        INVALID_TABLE_TYPES.add("SYSTEM INDEX");
        INVALID_TABLE_TYPES.add("SYSTEM SEQUENCE");
        // [JDBC: SQLite]
        INVALID_TABLE_TYPES.add("TRIGGER");
    }

    TableCache()
    {
        super(JDBCConstants.TABLE_NAME);
        setListOrderComparator(DBUtils.<GenericTable>nameComparator());
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, GenericStructContainer owner)
        throws SQLException
    {
        return context.getMetaData().getTables(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            null,
            null).getSource();
    }

    @Override
    protected GenericTable fetchObject(JDBCExecutionContext context, GenericStructContainer owner, ResultSet dbResult)
        throws SQLException, DBException
    {
        GenericMetadataReader.TableInfo tableInfo = owner.getDataSource().getMetadataReader().fetchTable(dbResult, owner);
        if (tableInfo == null) {
            return null;
        }
        if (CommonUtils.isEmpty(tableInfo.tableName)) {
            log.debug("Empty table name in container " + owner.getName());
            return null;
        }

        if (tableInfo.tableType != null && INVALID_TABLE_TYPES.contains(tableInfo.tableType)) {
            // Bad table type. Just skip it
            return null;
        }

        if (tableInfo.systemTable && !owner.getDataSource().getContainer().isShowSystemObjects()) {
            // Filter system tables
            return null;
        }

        return new GenericTable(
            owner,
            tableInfo,
            true);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, GenericStructContainer owner, GenericTable forTable)
        throws SQLException
    {
        return context.getMetaData().getColumns(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            forTable == null ? null : forTable.getName(),
            null).getSource();
    }

    @Override
    protected GenericTableColumn fetchChild(JDBCExecutionContext context, GenericStructContainer owner, GenericTable table, ResultSet dbResult)
        throws SQLException, DBException
    {
        GenericMetadataReader.TableColumnInfo column = table.getDataSource().getMetadataReader().fetchTableColumn(dbResult, table);
        if (column == null) {
            return null;
        }

        return new GenericTableColumn(table, column);
    }
}
