/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndex;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * GenericStructureContainer
 */
public abstract class JDBCIndexCache<
    TABLE extends DBSTable,
    COLUMN extends DBSTableColumn,
    INDEX extends DBSIndex>
{
    static Log log = LogFactory.getLog(JDBCIndexCache.class);

    private List<INDEX> indexList;
    private boolean indexesCached = false;

/*
    void cacheIndexes(DBRProgressMonitor monitor, TABLE forTable)
        throws DBException
    {
        if (this.indexesCached) {
            return;
        }

        // Load tables and columns first
        cacheColumns(monitor, forTable);
        if (forTable != null && forTable.isIndexesCached()) {
            return;
        }

        // Load index columns
        try {
            Map<TABLE, Map<String, INDEX>> tableIndexMap = new HashMap<TABLE, Map<String, INDEX>>();

            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            // Load indexes
            ResultSet dbResult = metaData.getIndexInfo(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                // oracle fails if unquoted complex identifier specified
                // but other DBs (and logically it's correct) do not want quote chars in this query
                // so let's fix it in oracle plugin
                forTable == null ? null : forTable.getName(), //DBSUtils.getQuotedIdentifier(getDataSource(), forTable.getName()),
                false,
                false);
            try {
                while (dbResult.next()) {
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String indexName = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_NAME);
                    boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
                    String indexQualifier = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_QUALIFIER);
                    int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);

                    int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

                    if (CommonUtils.isEmpty(indexName) || CommonUtils.isEmpty(tableName)) {
                        // Bad index - can't evaluate it
                        continue;
                    }
                    DBSIndexType indexType;
                    switch (indexTypeNum) {
                        case DatabaseMetaData.tableIndexStatistic: indexType = DBSIndexType.STATISTIC; break;
                        case DatabaseMetaData.tableIndexClustered: indexType = DBSIndexType.CLUSTERED; break;
                        case DatabaseMetaData.tableIndexHashed: indexType = DBSIndexType.HASHED; break;
                        case DatabaseMetaData.tableIndexOther: indexType = DBSIndexType.OTHER; break;
                        default: indexType = DBSIndexType.UNKNOWN; break;
                    }
                    TABLE table = forTable;
                    if (table == null) {
                        table = tableMap.get(tableName);
                        if (table == null) {
                            log.warn("Index '" + indexName + "' owner table '" + tableName + "' not found");
                            continue;
                        }
                    }
                    if (table.isIndexesCached()) {
                        // Already read
                        continue;
                    }
                    // Add to map
                    Map<String, INDEX> indexMap = tableIndexMap.get(table);
                    if (indexMap == null) {
                        indexMap = new TreeMap<String, INDEX>();
                        tableIndexMap.put(table, indexMap);
                    }

                    INDEX index = indexMap.get(indexName);
                    if (index == null) {
                        index = new INDEX(
                            table,
                            isNonUnique,
                            indexQualifier,
                            indexName,
                            indexType);
                        indexMap.put(indexName, index);
                    }
                    COLUMN tableColumn = table.getColumn(monitor, columnName);
                    if (tableColumn == null) {
                        log.warn("Column '" + columnName + "' not found in table '" + this.getName() + "'");
                        continue;
                    }
                    index.addColumn(
                        new INDEXColumn(
                            index,
                            tableColumn,
                            ordinalPosition,
                            !"D".equalsIgnoreCase(ascOrDesc)));
                }

                // All indexes are read. Now assign them to tables
                for (Map.Entry<TABLE,Map<String,INDEX>> colEntry : tableIndexMap.entrySet()) {
                    colEntry.getKey().setIndexes(new ArrayList<INDEX>(colEntry.getValue().values()));
                }
                // Now set empty index list for other tables
                if (forTable == null) {
                    for (TABLE tmpTable : tableList) {
                        if (!tableIndexMap.containsKey(tmpTable)) {
                            tmpTable.setIndexes(new ArrayList<INDEX>());
                        }
                    }
                } else if (!tableIndexMap.containsKey(forTable)) {
                    forTable.setIndexes(new ArrayList<INDEX>());
                }

                if (forTable == null) {
                    this.indexesCached = true;
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public boolean refreshCache(DBRProgressMonitor monitor)
        throws DBException
    {
        this.indexList = null;
        this.indexesCached = false;
        return true;
    }
*/

}