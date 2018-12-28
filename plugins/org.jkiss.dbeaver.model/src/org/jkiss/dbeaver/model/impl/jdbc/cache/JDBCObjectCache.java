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
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.*;

/**
 * Various objects cache.
 * Simple cache which may read objects from database and keep them.
 */
public abstract class JDBCObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> extends AbstractObjectCache<OWNER, OBJECT>
{
    public static final int DEFAULT_MAX_CACHE_SIZE = 1000000;

    private static final Log log = Log.getLog(JDBCObjectCache.class);

    // Maximum number of objects in cache
    private int maximumCacheSize = DEFAULT_MAX_CACHE_SIZE;

    protected JDBCObjectCache() {
    }

    public void setMaximumCacheSize(int maximumCacheSize) {
        this.maximumCacheSize = maximumCacheSize;
    }

    abstract protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner)
        throws SQLException;

    @Nullable
    abstract protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull JDBCResultSet resultSet)
        throws SQLException, DBException;

    @NotNull
    @Override
    public Collection<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException
    {
        if (!isFullyCached()) {
            loadObjects(monitor, owner);
        }
        return getCachedObjects();
    }

    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @NotNull String name)
        throws DBException
    {
        if (!isFullyCached()) {
            this.loadObjects(monitor, owner);
        }
        return getCachedObject(name);
    }

    protected synchronized void loadObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        if (isFullyCached() || monitor.isCanceled()) {
            return;
        }

        List<OBJECT> tmpObjectList = new ArrayList<>();

        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Load objects from " + owner.getName())) {
                try (JDBCStatement dbStat = prepareObjectsStatement(session, owner)) {
                    monitor.subTask("Load " + getCacheName());
                    dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                    dbStat.executeStatement();
                    JDBCResultSet dbResult = dbStat.getResultSet();
                    if (dbResult != null) {
                        try {
                            while (dbResult.next()) {
                                if (monitor.isCanceled()) {
                                    break;
                                }

                                OBJECT object = fetchObject(session, owner, dbResult);
                                if (object == null) {
                                    continue;
                                }
                                tmpObjectList.add(object);

                                // Do not log every object load. This overheats UI in case of long lists
                                //monitor.subTask(object.getName());
                                if (tmpObjectList.size() == maximumCacheSize) {
                                    log.warn("Maximum cache size exceeded (" + maximumCacheSize + ") in " + this);
                                    break;
                                }
                            }
                        } finally {
                            dbResult.close();
                        }
                    }
                }
            } catch (SQLException ex) {
                throw new DBException(ex, dataSource);
            }
        } catch (DBException e) {
            if (!handleCacheReadError(e)) {
                throw e;
            }
        }

        Comparator<OBJECT> comparator = getListOrderComparator();
        if (comparator != null) {
            tmpObjectList.sort(comparator);
        }

        detectCaseSensitivity(owner);
        mergeCache(tmpObjectList);
        this.invalidateObjects(monitor, owner, new CacheIterator());
    }

    protected String getCacheName() {
        return getClass().getSimpleName();
    }

    // Can be implemented to provide custom cache error handler
    protected boolean handleCacheReadError(DBException error) {
        return false;
    }

}
