/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Various objects cache
 */
public abstract class JDBCObjectCache<OBJECT extends DBSObject> {

    protected final JDBCDataSource dataSource;
    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;
    private boolean caseSensitive;
    private Comparator<OBJECT> listOrderComparator;

    protected JDBCObjectCache(JDBCDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    JDBCDataSource getDataSource()
    {
        return dataSource;
    }

    protected void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public void setListOrderComparator(Comparator<OBJECT> listOrderComparator)
    {
        this.listOrderComparator = listOrderComparator;
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
        return objectMap.get(caseSensitive ? name : name.toUpperCase());
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

        List<OBJECT> tmpObjectList = new ArrayList<OBJECT>();
        Map<String, OBJECT> tmpObjectMap = new HashMap<String, OBJECT>();

        JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Load objects");
        try {
            JDBCPreparedStatement dbStat = prepareObjectsStatement(context);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {

                        OBJECT object = fetchObject(context, dbResult);
                        if (object == null) {
                            continue;
                        }
                        tmpObjectList.add(object);
                        tmpObjectMap.put(caseSensitive ? object.getName() : object.getName().toUpperCase(), object);

                        monitor.subTask(object.getName());
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

        if (listOrderComparator != null) {
            Collections.sort(tmpObjectList, listOrderComparator);
        }
        this.objectList = tmpObjectList;
        this.objectMap = tmpObjectMap;
    }

}
