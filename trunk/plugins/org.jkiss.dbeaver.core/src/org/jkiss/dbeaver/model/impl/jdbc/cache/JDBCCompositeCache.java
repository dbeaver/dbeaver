/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Composite objects cache.
 * Each composite object consists from several rows.
 * Each row object refers to some other DB objects.
 * Each composite object belongs to some parent object (table usually) and it's name is unique within it's parent.
 * Each row object name is unique within main object.
 *
 * Examples: table index, constraint.
 */
public abstract class JDBCCompositeCache<
    OWNER extends DBSObject,
    PARENT extends DBSObject,
    OBJECT extends DBSObject,
    ROW_REF extends DBSObject>
    extends AbstractObjectCache<OWNER, OBJECT>
{
    protected static final Log log = LogFactory.getLog(JDBCCompositeCache.class);

    private final JDBCStructCache<OWNER,?,?> parentCache;
    private final Class<PARENT> parentType;
    private final String parentColumnName;
    private final String objectColumnName;

    private final Map<PARENT, List<OBJECT>> objectCache = new IdentityHashMap<PARENT, List<OBJECT>>();

    protected JDBCCompositeCache(
        JDBCStructCache<OWNER,?,?> parentCache,
        Class<PARENT> parentType,
        String parentColumnName,
        String objectColumnName)
    {
        this.parentCache = parentCache;
        this.parentType = parentType;
        this.parentColumnName = parentColumnName;
        this.objectColumnName = objectColumnName;
    }

    abstract protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, OWNER owner, PARENT forParent)
        throws SQLException;

    abstract protected OBJECT fetchObject(JDBCExecutionContext context, OWNER owner, PARENT parent, String childName, ResultSet resultSet)
        throws SQLException, DBException;

    abstract protected ROW_REF fetchObjectRow(JDBCExecutionContext context, PARENT parent, OBJECT forObject, ResultSet resultSet)
        throws SQLException, DBException;

    protected PARENT getParent(OBJECT object)
    {
        return (PARENT) object.getParentObject();
    }

    abstract protected void cacheChildren(OBJECT object, List<ROW_REF> children);

    @Override
    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        return getObjects(monitor, owner, null);
    }

    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner, PARENT forParent)
        throws DBException
    {
        loadObjects(monitor, owner, forParent);
        return getCachedObjects(forParent);
    }

    public Collection<OBJECT> getCachedObjects(PARENT forParent)
    {
        if (forParent == null) {
            return getCachedObjects();
        } else {
            synchronized (objectCache) {
                return objectCache.get(forParent);
            }
        }
    }

    @Override
    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String objectName)
        throws DBException
    {
        loadObjects(monitor, owner, null);

        return getCachedObject(objectName);
    }

    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, PARENT forParent, String objectName)
        throws DBException
    {
        loadObjects(monitor, owner, forParent);
        if (forParent == null) {
            return getCachedObject(objectName);
        } else {
            synchronized (objectCache) {
                return DBUtils.findObject(objectCache.get(forParent), objectName);
            }
        }
    }

    @Override
    public void cacheObject(OBJECT object)
    {
        super.cacheObject(object);
        synchronized (objectCache) {
            List<OBJECT> objects = objectCache.get(getParent(object));
            if (!CommonUtils.isEmpty(objects)) {
                objects.add(object);
            }
        }
    }

    @Override
    public void removeObject(OBJECT object)
    {
        super.removeObject(object);
        objectCache.remove(getParent(object));
    }

    public void clearObjectCache(PARENT forParent)
    {
        if (forParent == null) {
            super.clearCache();
        } else {
            objectCache.remove(forParent);
        }
    }

    private class ObjectInfo {
        final OBJECT object;
        final List<ROW_REF> rows = new ArrayList<ROW_REF>();
        public ObjectInfo(OBJECT object)
        {
            this.object = object;
        }
    }

    protected void loadObjects(DBRProgressMonitor monitor, OWNER owner, PARENT forParent)
        throws DBException
    {
        synchronized (objectCache) {
            if ((forParent == null && isCached()) ||
                (forParent != null && (!forParent.isPersisted() || objectCache.containsKey(forParent))))
            {
                return;
            }
        }

        // Load tables and columns first
        if (forParent == null) {
            parentCache.loadObjects(monitor, owner);
            parentCache.loadChildren(monitor, owner, null);
        }

        Map<PARENT, Map<String, ObjectInfo>> parentObjectMap = new LinkedHashMap<PARENT, Map<String, ObjectInfo>>();
        List<OBJECT> precachedObjects = new ArrayList<OBJECT>();

        // Load index columns
        JDBCExecutionContext context = (JDBCExecutionContext) owner.getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load composite objects");
        try {

            JDBCStatement dbStat = prepareObjectsStatement(context, owner, forParent);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            try {
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                try {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String parentName = JDBCUtils.safeGetString(dbResult, parentColumnName);
                        String objectName = JDBCUtils.safeGetString(dbResult, objectColumnName);

                        if (CommonUtils.isEmpty(objectName) || CommonUtils.isEmpty(parentName)) {
                            // Bad object - can't evaluate it
                            continue;
                        }
                        PARENT parent = forParent;
                        if (parent == null) {
                            parent = parentCache.getObject(monitor, owner, parentName, parentType);
                            if (parent == null) {
                                log.debug("Object '" + objectName + "' owner '" + parentName + "' not found");
                                continue;
                            }
                        }
                        synchronized (objectCache) {
                            final Collection<OBJECT> objectsCache = objectCache.get(parent);
                            if (objectsCache != null) {
                                // Already read
                                parentObjectMap.put(parent, null);
                                precachedObjects.addAll(objectsCache);
                                continue;
                            }
                        }
                        // Add to map
                        Map<String, ObjectInfo> objectMap = parentObjectMap.get(parent);
                        if (objectMap == null) {
                            objectMap = new TreeMap<String, ObjectInfo>();
                            parentObjectMap.put(parent, objectMap);
                        }

                        ObjectInfo objectInfo = objectMap.get(objectName);
                        if (objectInfo == null) {
                            OBJECT object = fetchObject(context, owner, parent, objectName, dbResult);
                            if (object == null) {
                                // Could not fetch object
                                continue;
                            }
                            objectInfo = new ObjectInfo(object);
                            objectMap.put(objectName, objectInfo);
                        }
                        ROW_REF rowRef = fetchObjectRow(context, parent, objectInfo.object, dbResult);
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

        if (monitor.isCanceled()) {
            return;
        }

        // Fill global cache
        synchronized (this) {
            if (forParent != null || !parentObjectMap.isEmpty()) {
                if (forParent == null) {
                    // Cache global object list
                    List<OBJECT> globalCache = new ArrayList<OBJECT>();
                    for (Map<String, ObjectInfo> objMap : parentObjectMap.values()) {
                        if (objMap != null) {
                            for (ObjectInfo info : objMap.values()) {
                                globalCache.add(info.object);
                            }
                        }
                    }
                    // Add precached objects to global cache too
                    globalCache.addAll(precachedObjects);
                    this.setCache(globalCache);
                    this.invalidateObjects(monitor, owner, new CacheIterator());
                }
            }
        }

        synchronized (objectCache) {
            // Cache data in individual objects only if we have read something or have certain parent object
            // Otherwise we assume that this function is not supported for mass data reading

            // All objects are read. Now assign them to parents
            for (Map.Entry<PARENT,Map<String,ObjectInfo>> colEntry : parentObjectMap.entrySet()) {
                if (colEntry.getValue() == null) {
                    // Do not overwrite this object's cache
                    continue;
                }
                Collection<ObjectInfo> objectInfos = colEntry.getValue().values();
                ArrayList<OBJECT> objects = new ArrayList<OBJECT>(objectInfos.size());
                for (ObjectInfo objectInfo : objectInfos) {
                    cacheChildren(objectInfo.object, objectInfo.rows);
                    objects.add(objectInfo.object);
                }
                objectCache.put(colEntry.getKey(), objects);
            }
            // Now set empty object list for other parents
            if (forParent == null) {
                for (PARENT tmpParent : parentCache.getTypedObjects(monitor, owner, parentType)) {
                    if (!parentObjectMap.containsKey(tmpParent)) {
                        objectCache.put(tmpParent, new ArrayList<OBJECT>());
                    }
                }
            } else if (!parentObjectMap.containsKey(forParent)) {
                objectCache.put(forParent, new ArrayList<OBJECT>());
            }
        }

    }

}
