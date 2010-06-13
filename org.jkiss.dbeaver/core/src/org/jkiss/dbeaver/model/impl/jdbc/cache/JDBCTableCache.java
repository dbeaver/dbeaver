/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GenericStructureContainer
 */
public abstract class JDBCTableCache<
    TABLE extends DBSTable, 
    COLUMN extends DBSTableColumn>
{
    static Log log = LogFactory.getLog(JDBCTableCache.class);

    private List<TABLE> tableList;
    private Map<String, TABLE> tableMap;
    private boolean columnsCached = false;

    private final String tableNameColumn;

    abstract protected PreparedStatement prepareTablesStatement(DBRProgressMonitor monitor)
        throws SQLException, DBException;

    abstract protected TABLE fetchTable(DBRProgressMonitor monitor, ResultSet resultSet)
        throws SQLException, DBException;

    abstract protected boolean isTableColumnsCached(TABLE table);

    abstract protected void cacheTableColumns(TABLE table, List<COLUMN> columns);

    abstract protected PreparedStatement prepareColumnsStatement(DBRProgressMonitor monitor, TABLE forTable)
        throws SQLException, DBException;

    abstract protected COLUMN fetchColumn(DBRProgressMonitor monitor, TABLE table, ResultSet resultSet)
        throws SQLException, DBException;

    protected JDBCTableCache(String tableNameColumn)
    {
        this.tableNameColumn = tableNameColumn;
    }

    public List<TABLE> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        this.cacheTables(monitor);
        return tableList;
    }

    public TABLE getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        this.cacheTables(monitor);
        return tableMap.get(name);
    }

    private void cacheTables(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.tableList != null) {
            return;
        }

        List<TABLE> tmpTableList = new ArrayList<TABLE>();
        Map<String, TABLE> tmpTableMap = new HashMap<String, TABLE>();
        try {
            PreparedStatement dbStat = prepareTablesStatement(monitor);
            monitor.startBlock(JDBCUtils.makeBlockingObject(dbStat), "Load tables");
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {

                        TABLE table = fetchTable(monitor, dbResult);
                        if (table == null) {
                            continue;
                        }
                        tmpTableList.add(table);
                        tmpTableMap.put(table.getName(), table);

                        monitor.subTask(table.getName());
                        if (monitor.isCanceled()) {
                            break;
                        }
                    }
                }
                finally {
                    JDBCUtils.safeClose(dbResult);
                }
            }
            finally {
                JDBCUtils.closeStatement(monitor, dbStat);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }

        this.tableList = tmpTableList;
        this.tableMap = tmpTableMap;
    }

    /**
     * Reads table columns from database
     * @param monitor monitor
     * @param forTable table for which to read columns. If null then reads columns for all tables in this container.
     * @throws org.jkiss.dbeaver.DBException on error
     */
    public void cacheColumns(DBRProgressMonitor monitor, final TABLE forTable)
        throws DBException
    {
        if (this.columnsCached) {
            return;
        }
        if (forTable == null) {
            cacheTables(monitor);
        } else if (isTableColumnsCached(forTable)) {
            return;
        }

        try {
            Map<TABLE, List<COLUMN>> columnMap = new HashMap<TABLE, List<COLUMN>>();

            // Load columns
            PreparedStatement dbStat = prepareColumnsStatement(monitor, forTable);
            monitor.startBlock(JDBCUtils.makeBlockingObject(dbStat), "Load columns");
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String tableName = JDBCUtils.safeGetString(dbResult, tableNameColumn);

                        TABLE table = forTable;
                        if (table == null) {
                            table = tableMap.get(tableName);
                            if (table == null) {
                                log.warn("Column owner table '" + tableName + "' not found");
                                continue;
                            }
                        }
                        if (isTableColumnsCached(table)) {
                            // Already read
                            continue;
                        }
                        COLUMN tableColumn = fetchColumn(monitor, table, dbResult);
                        if (tableColumn == null) {
                            continue;
                        }

                        // Add to map
                        List<COLUMN> columns = columnMap.get(table);
                        if (columns == null) {
                            columns = new ArrayList<COLUMN>();
                            columnMap.put(table, columns);
                        }
                        columns.add(tableColumn);

                        if (monitor.isCanceled()) {
                            break;
                        }
                    }

                    if (monitor.isCanceled()) {
                        return;
                    }

                    // All columns are read. Now assign them to tables
                    for (Map.Entry<TABLE, List<COLUMN>> colEntry : columnMap.entrySet()) {
                        cacheTableColumns(colEntry.getKey(), colEntry.getValue());
                    }
                    // Now set empty column list for other tables
                    if (forTable == null) {
                        for (TABLE tmpTable : tableList) {
                            if (!columnMap.containsKey(tmpTable)) {
                                cacheTableColumns(tmpTable, new ArrayList<COLUMN>());
                            }
                        }
                    } else if (!columnMap.containsKey(forTable)) {
                        cacheTableColumns(forTable, new ArrayList<COLUMN>());
                    }

                    if (forTable == null) {
                        this.columnsCached = true;
                    }
                }
                finally {
                    JDBCUtils.safeClose(dbResult);
                }
            }
            finally {
                JDBCUtils.closeStatement(monitor, dbStat);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public boolean clearCache()
    {
        this.tableList = null;
        this.tableMap = null;
        this.columnsCached = false;
        return true;
    }

}