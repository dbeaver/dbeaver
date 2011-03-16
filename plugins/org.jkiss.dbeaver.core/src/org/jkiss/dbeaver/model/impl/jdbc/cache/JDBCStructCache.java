/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

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
    OBJECT extends DBSObject,
    CHILD extends DBSObject>
    extends JDBCObjectCache<OBJECT>
{
    static final Log log = LogFactory.getLog(JDBCStructCache.class);

    private boolean childrenCached = false;

    private final String objectNameColumn;

    abstract protected boolean isChildrenCached(OBJECT parent);

    abstract protected void cacheChildren(OBJECT parent, List<CHILD> children);

    abstract protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OBJECT forObject)
        throws SQLException, DBException;

    abstract protected CHILD fetchChild(JDBCExecutionContext context, OBJECT parent, ResultSet dbResult)
        throws SQLException, DBException;

    protected JDBCStructCache(JDBCDataSource dataSource, String objectNameColumn)
    {
        super(dataSource);
        this.objectNameColumn = objectNameColumn;
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
            super.loadObjects(monitor);
        } else if (isChildrenCached(forObject)) {
            return;
        }

        JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Load child objects");
        try {
            Map<OBJECT, List<CHILD>> columnMap = new HashMap<OBJECT, List<CHILD>>();

            // Load columns
            JDBCPreparedStatement dbStat = prepareChildrenStatement(context, forObject);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String objectName = JDBCUtils.safeGetString(dbResult, objectNameColumn);

                        OBJECT table = forObject;
                        if (table == null) {
                            table = super.getObject(monitor, objectName);
                            if (table == null) {
                                log.debug("Object '" + objectName + "' not found");
                                continue;
                            }
                        }
                        if (isChildrenCached(table)) {
                            // Already read
                            continue;
                        }
                        CHILD tableColumn = fetchChild(context, table, dbResult);
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
                        for (OBJECT tmpObject : getObjects(monitor)) {
                            if (!isChildrenCached(tmpObject) && !columnMap.containsKey(tmpObject)) {
                                cacheChildren(tmpObject, new ArrayList<CHILD>());
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
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    public void clearCache()
    {
        super.clearCache();
        this.childrenCached = false;
    }

}