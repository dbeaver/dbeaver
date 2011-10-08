/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Various objects cache
 */
public abstract class JDBCObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> implements JDBCAbstractCache<OWNER, OBJECT> {

    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;
    private boolean caseSensitive = true;
    private Comparator<OBJECT> listOrderComparator;

    protected JDBCObjectCache() {
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public void setListOrderComparator(Comparator<OBJECT> listOrderComparator)
    {
        this.listOrderComparator = listOrderComparator;
    }

    abstract protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, OWNER owner)
        throws SQLException;

    abstract protected OBJECT fetchObject(JDBCExecutionContext context, OWNER owner, ResultSet resultSet)
        throws SQLException, DBException;

    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        if (!isCached()) {
            loadObjects(monitor, owner);
        }
        return getCachedObjects();
    }

    public Collection<OBJECT> getCachedObjects()
    {
        synchronized (this) {
            return objectList == null ? Collections.<OBJECT>emptyList() : objectList;
        }
    }

    public <SUB_TYPE> Collection<SUB_TYPE> getObjects(DBRProgressMonitor monitor, OWNER owner, Class<SUB_TYPE> type)
        throws DBException
    {
        List<SUB_TYPE> result = new ArrayList<SUB_TYPE>();
        for (OBJECT object : getObjects(monitor, owner)) {
            if (type.isInstance(object)) {
                result.add(type.cast(object));
            }
        }
        return result;
    }

    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String name)
        throws DBException
    {
        if (!isCached()) {
            this.loadObjects(monitor, owner);
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
                detectCaseSensitivity(object);
                this.objectList.add(object);
                this.objectMap.put(caseSensitive ? object.getName() : object.getName().toUpperCase(), object);
            }
        }
    }

    public <SUB_TYPE> SUB_TYPE getObject(DBRProgressMonitor monitor, OWNER owner, String name, Class<SUB_TYPE> type)
        throws DBException
    {
        final OBJECT object = getObject(monitor, owner, name);
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

    public void loadObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        if (isCached() || monitor.isCanceled()) {
            return;
        }

        List<OBJECT> tmpObjectList = new ArrayList<OBJECT>();

        JDBCExecutionContext context = (JDBCExecutionContext)owner.getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load objects");
        try {
            JDBCStatement dbStat = prepareObjectsStatement(context, owner);
            try {
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }

                        OBJECT object = fetchObject(context, owner, dbResult);
                        if (object == null) {
                            continue;
                        }
                        tmpObjectList.add(object);

                        monitor.subTask(object.getName());
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

        synchronized (this) {
            detectCaseSensitivity(owner);

            this.objectList = tmpObjectList;
            this.objectMap = new LinkedHashMap<String, OBJECT>();
            for (OBJECT object : tmpObjectList) {
                this.objectMap.put(caseSensitive ? object.getName() : object.getName().toUpperCase(), object);
            }
            this.invalidateObjects(monitor, owner, new CacheIterator());
        }
        if (listOrderComparator != null) {
            Collections.sort(tmpObjectList, listOrderComparator);
        }
    }

    private void detectCaseSensitivity(DBSObject object) {
        if (this.caseSensitive && object.getDataSource().getInfo().storesUnquotedCase() == DBPIdentifierCase.MIXED) {
            this.caseSensitive = false;
        }
    }

    protected void invalidateObjects(DBRProgressMonitor monitor, OWNER owner, Iterator<OBJECT> objectIter)
    {

    }

    private class CacheIterator implements Iterator<OBJECT> {
        private Iterator<OBJECT> listIterator = objectList.iterator();
        private OBJECT curObject;
        private CacheIterator()
        {
        }

        public boolean hasNext()
        {
            return listIterator.hasNext();
        }

        public OBJECT next()
        {
            return (curObject = listIterator.next());
        }

        public void remove()
        {
            listIterator.remove();
            objectMap.remove(caseSensitive ? curObject.getName() : curObject.getName().toUpperCase());
        }
    }
}
