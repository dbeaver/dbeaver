/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.PreparedStatement;
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

    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;

    protected JDBCObjectCache()
    {
    }

    abstract protected PreparedStatement prepareObjectsStatement(DBRProgressMonitor monitor)
        throws SQLException, DBException;

    abstract protected OBJECT fetchObject(DBRProgressMonitor monitor, ResultSet resultSet)
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
        try {
            PreparedStatement dbStat = prepareObjectsStatement(monitor);
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {

                        OBJECT table = fetchObject(monitor, dbResult);
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
                JDBCUtils.safeClose(dbStat);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }

        this.objectList = tmpTableList;
        this.objectMap = tmpTableMap;
    }

}
