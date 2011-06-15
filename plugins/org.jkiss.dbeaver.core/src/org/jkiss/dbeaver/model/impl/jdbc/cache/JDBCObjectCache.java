/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
public abstract class JDBCObjectCache<OBJECT extends DBSObject> implements JDBCAbstractCache<OBJECT> {

    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;
    private boolean caseSensitive = true;
    private Comparator<OBJECT> listOrderComparator;

    public void setCaseSensitive(boolean caseSensitive)
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

    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource)
        throws DBException
    {
        if (!isCached()) {
            loadObjects(monitor, dataSource);
        }
        return getCachedObjects();
    }

    public Collection<OBJECT> getCachedObjects()
    {
        return objectList == null ? Collections.<OBJECT>emptyList() : objectList;
    }

    public <SUB_TYPE> Collection<SUB_TYPE> getObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource, Class<SUB_TYPE> type)
        throws DBException
    {
        List<SUB_TYPE> result = new ArrayList<SUB_TYPE>();
        for (OBJECT object : getObjects(monitor, dataSource)) {
            if (type.isInstance(object)) {
                result.add(type.cast(object));
            }
        }
        return result;
    }

    public OBJECT getObject(DBRProgressMonitor monitor, JDBCDataSource dataSource, String name)
        throws DBException
    {
        if (!isCached()) {
            this.loadObjects(monitor, dataSource);
        }
        return getCachedObject(name);
    }

    public OBJECT getCachedObject(String name)
    {
        synchronized (this) {
            return objectMap == null ? null : objectMap.get(caseSensitive ? name : name.toUpperCase());
        }
    }

    public void cacheObject(OBJECT object)
    {
        synchronized (this) {
            if (this.objectList != null) {
                this.objectList.add(object);
                this.objectMap.put(caseSensitive ? object.getName() : object.getName().toUpperCase(), object);
            }
        }
    }

    public <SUB_TYPE> SUB_TYPE getObject(DBRProgressMonitor monitor, JDBCDataSource dataSource, String name, Class<SUB_TYPE> type)
        throws DBException
    {
        final OBJECT object = getObject(monitor, dataSource, name);
        return type.isInstance(object) ? type.cast(object) : null;
    }

    public boolean isCached()
    {
        synchronized (this) {
            return objectMap != null;
        }
    }

    public void clearCache()
    {
        synchronized (this) {
            this.objectList = null;
            this.objectMap = null;
        }
    }

    public void loadObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource)
        throws DBException
    {
        if (isCached()) {
            return;
        }

        List<OBJECT> tmpObjectList = new ArrayList<OBJECT>();

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
        synchronized (this) {
            this.objectList = tmpObjectList;
            this.objectMap = new LinkedHashMap<String, OBJECT>();
            for (OBJECT object : tmpObjectList) {
                this.objectMap.put(caseSensitive ? object.getName() : object.getName().toUpperCase(), object);
            }
            this.invalidateObjects(monitor, objectList);
        }
    }

    protected void invalidateObjects(DBRProgressMonitor monitor, Collection<OBJECT> objectList)
    {

    }

}
