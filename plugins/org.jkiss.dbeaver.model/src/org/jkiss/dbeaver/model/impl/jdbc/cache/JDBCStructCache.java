/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.DBSStructCache;
import org.jkiss.dbeaver.model.impl.SimpleObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * JDBC structured objects cache.
 * Stores objects themselves and their child objects.
 */
public abstract class JDBCStructCache<OWNER extends DBSObject, OBJECT extends DBSObject, CHILD extends DBSObject>
    extends JDBCObjectCache<OWNER, OBJECT>
    implements DBSStructCache<OWNER, OBJECT, CHILD>
{
    private static final Log log = Log.getLog(JDBCStructCache.class);

    private final Object objectNameColumn;
    private volatile boolean childrenCached = false;
    private final Map<OBJECT, SimpleObjectCache<OBJECT, CHILD>> childrenCache = new IdentityHashMap<>();

    abstract protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OWNER owner, @Nullable OBJECT forObject)
        throws SQLException;

    abstract protected CHILD fetchChild(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull OBJECT parent, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException;

    protected JDBCStructCache(Object objectNameColumn)
    {
        this.objectNameColumn = objectNameColumn;
    }

    /**
     * Reads children objects from database
     * 
     * @param monitor
     *            monitor
     * @param forObject
     *            object for which to read children. If null then reads children for all objects in this container.
     * @throws org.jkiss.dbeaver.DBException
     *             on error
     */
    public synchronized void loadChildren(DBRProgressMonitor monitor, OWNER owner, @Nullable final OBJECT forObject) throws DBException
    {
        if ((forObject == null && this.childrenCached)
            || (forObject != null && (!forObject.isPersisted() || isChildrenCached(forObject))) || monitor.isCanceled()) {
            return;
        }
        if (forObject == null) {
            // If we have some child objects read before that - do not clear them.
            // We have to reuse them because there could be some references in cached model
            //clearChildrenCache(null);
            super.loadObjects(monitor, owner);
        }

        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException("Not connected to database");
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load child objects")) {
            Map<OBJECT, List<CHILD>> objectMap = new HashMap<>();

            // Load columns
            try (JDBCStatement dbStat = prepareChildrenStatement(session, owner, forObject)) {
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                if (dbResult != null) {
                    try {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            String objectName;
                            if (objectNameColumn instanceof Number) {
                                objectName = JDBCUtils.safeGetString(dbResult, ((Number) objectNameColumn).intValue());
                            } else {
                                objectName = JDBCUtils.safeGetStringTrimmed(dbResult, objectNameColumn.toString());
                            }
                            if (objectName == null) {
                                log.debug("NULL object name in " + this);
                                continue;
                            }
                            OBJECT object = forObject;
                            if (object == null) {
                                object = super.getCachedObject(objectName);
                                if (object == null) {
                                    log.debug("Object '" + objectName + "' not found");
                                    continue;
                                }
                            }
                            if (isChildrenCached(object)) {
                                // Already read
                                continue;
                            }
                            CHILD child = fetchChild(session, owner, object, dbResult);
                            if (child == null) {
                                continue;
                            }

                            // Add to map
                            List<CHILD> children = objectMap.get(object);
                            if (children == null) {
                                children = new ArrayList<>();
                                objectMap.put(object, children);
                            }
                            children.add(child);
                        }

                        if (monitor.isCanceled()) {
                            return;
                        }

                        // All children are read. Now assign them to parents
                        for (Map.Entry<OBJECT, List<CHILD>> colEntry : objectMap.entrySet()) {
                            if (!isChildrenCached(colEntry.getKey())) {
                                // isChildrenCached may return true if the same cache was read in other thread
                                // just skip
                                cacheChildren(colEntry.getKey(), colEntry.getValue());
                            }
                        }
                        if (forObject == null) {
                            if (objectMap.isEmpty()) {
                                // Nothing was read. May be it means empty list of children
                                // but possibly this feature is not supported [JDBC: SQLite]
                            } else {
                                // Now set empty column list for other tables
                                for (OBJECT tmpObject : getAllObjects(monitor, owner)) {
                                    if (!isChildrenCached(tmpObject) && !objectMap.containsKey(tmpObject)) {
                                        cacheChildren(tmpObject, new ArrayList<CHILD>());
                                    }
                                }
                                this.childrenCached = true;
                            }
                        } else if (!objectMap.containsKey(forObject)) {
                            cacheChildren(forObject, new ArrayList<CHILD>());
                        }
                    } finally {
                        dbResult.close();
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }
    }

    @Override
    public void removeObject(@NotNull OBJECT object, boolean resetFullCache)
    {
        super.removeObject(object, resetFullCache);
        clearChildrenCache(object);
    }

    @Override
    public void clearCache()
    {
        this.clearChildrenCache(null);
        super.clearCache();
    }

    /**
     * Returns cache for child objects. Creates cache i it doesn't exists
     * 
     * @param forObject
     *            parent object
     * @return cache
     */
    public DBSObjectCache<OBJECT, CHILD> getChildrenCache(final OBJECT forObject)
    {
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(forObject);
            if (nestedCache == null) {
                // Create new empty children cache
                // This may happen only when invoked for newly created object (e.g. when we create new column
                // in a new created table)
                nestedCache = new SimpleObjectCache<>();
                nestedCache.setCache(new ArrayList<CHILD>());
                childrenCache.put(forObject, nestedCache);
            }
            return nestedCache;
        }
    }

    @Nullable
    public List<CHILD> getChildren(DBRProgressMonitor monitor, OWNER owner, final OBJECT forObject) throws DBException
    {
        loadChildren(monitor, owner, forObject);
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(forObject);
            return nestedCache == null ? null : nestedCache.getAllObjects(monitor, null);
        }
    }

    @Nullable
    public CHILD getChild(DBRProgressMonitor monitor, OWNER owner, final OBJECT forObject, String objectName) throws DBException
    {
        loadChildren(monitor, owner, forObject);
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(forObject);
            return nestedCache == null ? null : nestedCache.getObject(monitor, forObject, objectName);
        }
    }

    public void clearChildrenCache(OBJECT forParent)
    {
        synchronized (childrenCache) {
            if (forParent != null) {
                this.childrenCache.remove(forParent);
            } else {
                this.childrenCache.clear();
            }
            childrenCached = false;
        }
    }

    protected boolean isChildrenCached(OBJECT parent)
    {
        synchronized (childrenCache) {
            return childrenCache.containsKey(parent);
//            SimpleObjectCache<OBJECT, CHILD> chCache = childrenCache.get(parent);
//            return chCache != null && !CommonUtils.isEmpty(chCache.getCachedObjects());
        }
    }

    protected void cacheChildren(OBJECT parent, List<CHILD> children)
    {
        synchronized (childrenCache) {
            SimpleObjectCache<OBJECT, CHILD> nestedCache = childrenCache.get(parent);
            if (nestedCache == null) {
                nestedCache = new SimpleObjectCache<>();
                nestedCache.setCaseSensitive(caseSensitive);
                childrenCache.put(parent, nestedCache);
            }
            nestedCache.setCache(children);
        }
    }

}