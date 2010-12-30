/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
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
{
    static final Log log = LogFactory.getLog(JDBCCompositeCache.class);

    private JDBCObjectCache<PARENT> parentCache;
    private List<OBJECT> objectList;
    private final String parentColumnName;
    private final String objectColumnName;

    protected JDBCCompositeCache(
        JDBCObjectCache<PARENT> parentCache,
        String parentColumnName,
        String objectColumnName)
    {
        this.parentCache = parentCache;
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

    abstract protected void cacheRows(OBJECT object, List<ROW_REF> rows);

    private class ObjectInfo {
        final OBJECT object;
        final List<ROW_REF> rows = new ArrayList<ROW_REF>();
        public ObjectInfo(OBJECT object)
        {
            this.object = object;
        }
    }

    public List<OBJECT> getObjects(DBRProgressMonitor monitor, PARENT forParent)
        throws DBException
    {
        if (objectList == null) {
            loadObjects(monitor, forParent);
        }
        return objectList;
    }

    public boolean isCached()
    {
        return objectList != null;
    }

    public void setCache(List<OBJECT> objects)
    {
        objectList = objects;
    }

    public void clearCache()
    {
        this.objectList = null;
    }

    protected synchronized void loadObjects(DBRProgressMonitor monitor, PARENT forParent)
        throws DBException
    {
        if (this.objectList != null) {
            return;
        }

        // Load tables and columns first
        if (forParent == null) {
            parentCache.loadObjects(monitor);
        } else if (isObjectsCached(forParent)) {
            return;
        }

        // Load index columns
        JDBCExecutionContext context = parentCache.getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load composite objects");
        try {
            Map<PARENT, Map<String, ObjectInfo>> parentObjectMap = new HashMap<PARENT, Map<String, ObjectInfo>>();

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
                            parent = parentCache.getObject(monitor, parentName);
                            if (parent == null) {
                                log.warn("Object '" + objectName + "' owner '" + parentName + "' not found");
                                continue;
                            }
                        }
                        if (isObjectsCached(parent)) {
                            // Already read
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

                    // All objects are read. Now assign them to parents
                    for (Map.Entry<PARENT,Map<String,ObjectInfo>> colEntry : parentObjectMap.entrySet()) {
                        Collection<ObjectInfo> objectInfos = colEntry.getValue().values();
                        ArrayList<OBJECT> objects = new ArrayList<OBJECT>(objectInfos.size());
                        for (ObjectInfo objectInfo : objectInfos) {
                            cacheRows(objectInfo.object, objectInfo.rows);
                            objects.add(objectInfo.object);
                        }
                        cacheObjects(colEntry.getKey(), objects);
                    }
                    // Now set empty object list for other parents
                    if (forParent == null) {
                        for (PARENT tmpParent : parentCache.getObjects(monitor)) {
                            if (!parentObjectMap.containsKey(tmpParent)) {
                                cacheObjects(tmpParent, new ArrayList<OBJECT>());
                            }
                        }
                    } else if (!parentObjectMap.containsKey(forParent)) {
                        cacheObjects(forParent, new ArrayList<OBJECT>());
                    }

                    if (forParent == null) {
                        // Cache global object list
                        objectList = new ArrayList<OBJECT>();
                        for (Map<String, ObjectInfo> objMap : parentObjectMap.values()) {
                            for (ObjectInfo info : objMap.values()) {
                                objectList.add(info.object);
                            }
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
    }

}