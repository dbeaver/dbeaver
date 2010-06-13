/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC structured objects cache
 */
public abstract class JDBCStructCache<
    OBJECT extends DBSStructureContainer,
    CHILD extends DBSObject>
    extends JDBCObjectCache<OBJECT>
{
    static Log log = LogFactory.getLog(JDBCStructCache.class);

    private boolean childrenCached = false;

    private final String objectNameColumn;
    private final String childListName;

    abstract protected boolean isChildrenCached(OBJECT parent);

    abstract protected void cacheChildren(OBJECT parent, List<CHILD> children);

    abstract protected PreparedStatement prepareChildrenStatement(DBRProgressMonitor monitor, OBJECT forObject)
        throws SQLException, DBException;

    abstract protected CHILD fetchChild(DBRProgressMonitor monitor, OBJECT parent, ResultSet dbResult)
        throws SQLException, DBException;

    protected JDBCStructCache(String objectListName, String childListName, String objectNameColumn
    )
    {
        super(objectListName);
        this.objectNameColumn = objectNameColumn;
        this.childListName = childListName;
    }

    /**
     * Reads children objects from database
     * @param monitor monitor
     * @param forObject object for which to read children. If null then reads children for all objects in this container.
     * @throws org.jkiss.dbeaver.DBException on error
     */
    public void loadChildren(DBRProgressMonitor monitor, final OBJECT forObject)
        throws DBException
    {
        if (this.childrenCached) {
            return;
        }
        if (forObject == null) {
            super.cacheObjects(monitor);
        } else if (isChildrenCached(forObject)) {
            return;
        }

        try {
            Map<OBJECT, List<CHILD>> columnMap = new HashMap<OBJECT, List<CHILD>>();

            // Load columns
            PreparedStatement dbStat = prepareChildrenStatement(monitor, forObject);
            monitor.startBlock(JDBCUtils.makeBlockingObject(dbStat), "Load " + childListName);
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String tableName = JDBCUtils.safeGetString(dbResult, objectNameColumn);

                        OBJECT table = forObject;
                        if (table == null) {
                            table = super.getObject(monitor, tableName);
                            if (table == null) {
                                log.warn("Object '" + tableName + "' not found");
                                continue;
                            }
                        }
                        if (isChildrenCached(table)) {
                            // Already read
                            continue;
                        }
                        CHILD tableColumn = fetchChild(monitor, table, dbResult);
                        if (tableColumn == null) {
                            continue;
                        }

                        // Add to map
                        List<CHILD> columns = columnMap.get(table);
                        if (columns == null) {
                            columns = new ArrayList<CHILD>();
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

                    // All children are read. Now assign them to parents
                    for (Map.Entry<OBJECT, List<CHILD>> colEntry : columnMap.entrySet()) {
                        cacheChildren(colEntry.getKey(), colEntry.getValue());
                    }
                    // Now set empty column list for other tables
                    if (forObject == null) {
                        for (OBJECT tmpTable : getObjects(monitor)) {
                            if (!columnMap.containsKey(tmpTable)) {
                                cacheChildren(tmpTable, new ArrayList<CHILD>());
                            }
                        }
                    } else if (!columnMap.containsKey(forObject)) {
                        cacheChildren(forObject, new ArrayList<CHILD>());
                    }

                    if (forObject == null) {
                        this.childrenCached = true;
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

    public void clearCache()
    {
        super.clearCache();
        this.childrenCached = false;
    }

}