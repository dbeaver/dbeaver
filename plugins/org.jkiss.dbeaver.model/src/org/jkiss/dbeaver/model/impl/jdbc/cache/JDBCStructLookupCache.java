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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Struct cache with ability to load/search single object by name.
 */
public abstract class JDBCStructLookupCache<OWNER extends DBSObject, OBJECT extends DBSObject, CHILD extends DBSObject>
    extends JDBCStructCache<OWNER, OBJECT, CHILD>
    implements JDBCObjectLookup<OWNER, OBJECT>
{
    private final Set<String> missingNames = new HashSet<>();

    public JDBCStructLookupCache(Object objectNameColumn) {
        super(objectNameColumn);
    }

    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @NotNull String name)
        throws DBException
    {
        OBJECT cachedObject = getCachedObject(name);
        if (cachedObject != null) {
            return cachedObject;
        }
        if (isFullyCached() || missingNames.contains(name)) {
            return null;
        }
        // Now cache just one object
        OBJECT object = reloadObject(monitor, owner, null, name);
        if (object != null) {
            cacheObject(object);
        } else {
            // Not found!
            missingNames.add(name);
        }
        return object;
    }

    public OBJECT refreshObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @NotNull OBJECT oldObject)
        throws DBException
    {
        String objectName = oldObject.getName();
        if (!isFullyCached()) {
            this.loadObjects(monitor, owner);
        } else {
            OBJECT newObject = this.reloadObject(monitor, owner, oldObject, null);
            if (isChildrenCached(oldObject)) {
                clearChildrenCache(oldObject);
            }
            if (newObject != null) {
                deepCopyCachedObject(newObject, oldObject);
            } else {
                removeObject(oldObject, false);
            }
//            removeObject(oldObject, false);
//            if (newObject != null) {
//                cacheObject(newObject);
//            }
            return oldObject;
        }
        return getCachedObject(objectName);
    }


    protected OBJECT reloadObject(@NotNull DBRProgressMonitor monitor, @NotNull OWNER owner, @Nullable OBJECT object, @Nullable String objectName)
        throws DBException
    {
        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner,
            object == null ?
                "Load object '" + objectName + "' from " + owner.getName() :
                "Reload object '" + object + "' from " + owner.getName()))
        {
            try (JDBCStatement dbStat = prepareLookupStatement(session, owner, object, objectName)) {
                dbStat.setFetchSize(1);
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                if (dbResult != null) {
                    try {
                        if (dbResult.next()) {
                            return fetchObject(session, owner, dbResult);
                        }
                    } finally {
                        dbResult.close();
                    }
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DBException("Error loading object metadata from database", ex, dataSource);
        }
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner)
        throws SQLException
    {
        return prepareLookupStatement(session, owner, null, null);
    }

    @Override
    public void setCache(List<OBJECT> objects) {
        super.setCache(objects);
        this.missingNames.clear();
    }

    @Override
    public void clearCache() {
        super.clearCache();
        this.missingNames.clear();
    }

}
