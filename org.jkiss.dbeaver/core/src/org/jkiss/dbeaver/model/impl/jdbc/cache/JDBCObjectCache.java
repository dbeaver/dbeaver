/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Various objects cache
 */
public abstract class JDBCObjectCache<OBJECT extends DBSObject> {

    protected final JDBCConnector connector;
    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;

    protected JDBCObjectCache(JDBCConnector connector)
    {
        this.connector = connector;
    }

    JDBCConnector getConnector()
    {
        return connector;
    }

    abstract protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
        throws SQLException, DBException;

    abstract protected OBJECT fetchObject(JDBCExecutionContext context, ResultSet resultSet)
        throws SQLException, DBException;

    public List<OBJECT> getObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        if (objectList == null) {
            loadObjects(monitor);
        }
        return objectList;
    }

    public OBJECT getObject(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        if (objectMap == null) {
            this.loadObjects(monitor);
        }
        return objectMap.get(name);
    }

    public void clearCache()
    {
        this.objectList = null;
        this.objectMap = null;
    }

    public synchronized void loadObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.objectList != null) {
            return;
        }

        List<OBJECT> tmpTableList = new ArrayList<OBJECT>();
        Map<String, OBJECT> tmpTableMap = new HashMap<String, OBJECT>();

        JDBCExecutionContext context = connector.openContext(monitor);
        try {
            JDBCPreparedStatement dbStat = prepareObjectsStatement(context);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {

                        OBJECT table = fetchObject(context, dbResult);
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

        this.objectList = tmpTableList;
        this.objectMap = tmpTableMap;
    }

}
