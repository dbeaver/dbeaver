/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import net.sf.jkiss.utils.CommonUtils;
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
import java.util.*;

/**
 * Composite objects cache.
 * Each composite object contains from several rows.
 * Each row object refers to some other DB objects.
 * Each composite object belongs to some parent object (table usually) and it's name is unique within it's parent.
 * Each row object name is unique within main object.
 *
 * Examples: table index, constraint.
 */
public abstract class JDBCCompositeCache<
    PARENT extends DBSObject,
    OBJECT extends DBSObject,
    ROW_REF extends DBSObject>
    implements JDBCAbstractCache<OBJECT>
{
    protected static final Log log = LogFactory.getLog(JDBCCompositeCache.class);

    private final Map<String, ObjectInfo> PRECACHED_MARK = new HashMap<String, ObjectInfo>();

    private JDBCStructCache<?,?> parentCache;
    private Class<PARENT> parentType;
    private List<OBJECT> objectList;
    private Map<String, OBJECT> objectMap;
    private final String parentColumnName;
    private final String objectColumnName;

    protected JDBCCompositeCache(
        JDBCStructCache<?,?> parentCache,
        Class<PARENT> parentType,
        String parentColumnName,
        String objectColumnName)
    {
        this.parentCache = parentCache;
        this.parentType = parentType;
        this.parentColumnName = parentColumnName;
        this.objectColumnName = objectColumnName;
    }

    abstract protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, PARENT forParent)
        throws SQLException, DBException;

    abstract protected OBJECT fetchObject(JDBCExecutionContext context, ResultSet resultSet, PARENT parent, String childName)
        throws SQLException, DBException;

    abstract protected ROW_REF fetchObjectRow(JDBCExecutionContext context, ResultSet resultSet, PARENT parent, OBJECT forObject)
        throws SQLException, DBException;

    abstract protected boolean isObjectsCached(PARENT parent);
    
    abstract protected void cacheObjects(PARENT parent, List<OBJECT> objects);

    abstract protected void cacheChildren(OBJECT object, List<ROW_REF> rows);

    private class ObjectInfo {
        final OBJECT object;
        final List<ROW_REF> rows = new ArrayList<ROW_REF>();
        public ObjectInfo(OBJECT object)
        {
            this.object = object;
        }
    }

    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource)
        throws DBException
    {
        return getObjects(monitor, dataSource, null);
    }

    public Collection<OBJECT> getCachedObjects()
    {
        synchronized (this) {
            return objectList == null ? Collections.<OBJECT>emptyList() : objectList;
        }
    }

    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource, PARENT forParent)
        throws DBException
    {
        if (!isCached()) {
            loadObjects(monitor, dataSource, forParent);
        }
        return getCachedObjects();
    }

    public OBJECT getObject(DBRProgressMonitor monitor, JDBCDataSource dataSource, String objectName)
        throws DBException
    {
        if (!isCached()) {
            loadObjects(monitor, dataSource, null);
        }
        return getCachedObject(objectName);
    }

    public OBJECT getCachedObject(String name)
    {
        synchronized (this) {
            return objectMap == null ? null : objectMap.get(name);
        }
    }

    public boolean isCached()
    {
        synchronized (this) {
            return objectMap != null;
        }
    }

    public void cacheObject(OBJECT object)
    {
        synchronized (this) {
            if (this.objectList != null) {
                this.objectList.add(object);
                this.objectMap.put(object.getName(), object);
            }
        }
    }

    public void setCache(Collection<OBJECT> objects)
    {
        synchronized (this) {
            objectList = new ArrayList<OBJECT>(objects);
            objectMap = new LinkedHashMap<String, OBJECT>();
            for (OBJECT object : objects) {
                objectMap.put(object.getName(), object);
            }
        }
    }

    public void clearCache()
    {
        synchronized (this) {
            if (this.objectList != null) {
                for (OBJECT object : this.objectList) {
                    cacheChildren(object, null);
                }
                this.objectList = null;
                this.objectMap = null;
            }
        }
    }

    protected void loadObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource, PARENT forParent)
        throws DBException
    {
        if (isCached()) {
            return;
        }

        // Load tables and columns first
        if (forParent == null) {
            parentCache.loadObjects(monitor, dataSource);
            parentCache.getChildren(monitor, dataSource, null);
        } else if (!forParent.isPersisted() || isObjectsCached(forParent)) {
            return;
        }

        Map<PARENT, Map<String, ObjectInfo>> parentObjectMap = new LinkedHashMap<PARENT, Map<String, ObjectInfo>>();

        // Load index columns
        JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Load composite objects");
        try {

            JDBCPreparedStatement dbStat = prepareObjectsStatement(context, forParent);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String parentName = JDBCUtils.safeGetString(dbResult, parentColumnName);
                        String objectName = JDBCUtils.safeGetString(dbResult, objectColumnName);

                        if (CommonUtils.isEmpty(objectName) || CommonUtils.isEmpty(parentName)) {
                            // Bad object - can't evaluate it
                            continue;
                        }
                        PARENT parent = forParent;
                        if (parent == null) {
                            parent = parentCache.getObject(monitor, dataSource, parentName, parentType);
                            if (parent == null) {
                                log.debug("Object '" + objectName + "' owner '" + parentName + "' not found");
                                continue;
                            }
                        }
                        if (isObjectsCached(parent)) {
                            // Already read
                            parentObjectMap.put(parent, PRECACHED_MARK);
                            continue;
                        }
                        // Add to map
                        Map<String, ObjectInfo> objectMap = parentObjectMap.get(parent);
                        if (objectMap == null) {
                            objectMap = new TreeMap<String, ObjectInfo>();
                            parentObjectMap.put(parent, objectMap);
                        }

                        ObjectInfo objectInfo = objectMap.get(objectName);
                        if (objectInfo == null) {
                            OBJECT object = fetchObject(context, dbResult, parent, objectName);
                            if (object == null) {
                                // Could not fetch object
                                continue;
                            }
                            objectInfo = new ObjectInfo(object);
                            objectMap.put(objectName, objectInfo);
                        }
                        ROW_REF rowRef = fetchObjectRow(context, dbResult, parent, objectInfo.object);
                        if (rowRef == null) {
                            continue;
                        }
                        objectInfo.rows.add(rowRef);
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

        // Fill global cache
        synchronized (this) {
            if (forParent != null || !parentObjectMap.isEmpty()) {
                if (forParent == null) {
                    // Cache global object list
                    objectList = new ArrayList<OBJECT>();
                    objectMap = new LinkedHashMap<String, OBJECT>();
                    for (Map<String, ObjectInfo> objMap : parentObjectMap.values()) {
                        for (ObjectInfo info : objMap.values()) {
                            objectList.add(info.object);
                            objectMap.put(info.object.getName(), info.object);
                        }
                    }
                }
            }
        }

        // Cache data in individual objects only if we have read something or have certain parent object
        // Otherwise we assume that this function is not supported for mass data reading

        // All objects are read. Now assign them to parents
        for (Map.Entry<PARENT,Map<String,ObjectInfo>> colEntry : parentObjectMap.entrySet()) {
            if (colEntry.getValue() == PRECACHED_MARK) {
                // Do not overwrite this object's cache
                continue;
            }
            Collection<ObjectInfo> objectInfos = colEntry.getValue().values();
            ArrayList<OBJECT> objects = new ArrayList<OBJECT>(objectInfos.size());
            for (ObjectInfo objectInfo : objectInfos) {
                cacheChildren(objectInfo.object, objectInfo.rows);
                objects.add(objectInfo.object);
            }
            cacheObjects(colEntry.getKey(), objects);
        }
        // Now set empty object list for other parents
        if (forParent == null) {
            for (PARENT tmpParent : parentCache.getObjects(monitor, dataSource, parentType)) {
                if (!parentObjectMap.containsKey(tmpParent)) {
                    cacheObjects(tmpParent, new ArrayList<OBJECT>());
                }
            }
        } else if (!parentObjectMap.containsKey(forParent)) {
            cacheObjects(forParent, new ArrayList<OBJECT>());
        }

    }

}