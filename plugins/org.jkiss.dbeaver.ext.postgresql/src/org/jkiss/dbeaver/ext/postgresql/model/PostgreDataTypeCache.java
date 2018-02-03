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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.LongKeyMap;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * PostgreDataTypeCache
 */
public class PostgreDataTypeCache extends JDBCObjectCache<PostgreSchema, PostgreDataType>
{
    private LongKeyMap<PostgreDataType> dataTypeMap = new LongKeyMap<>();

    PostgreDataTypeCache() {
        setListOrderComparator(DBUtils.nameComparator());
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, PostgreSchema postgreSchema) throws DBException {
        super.loadObjects(monitor, postgreSchema);

        // Cache aliases
        if (postgreSchema.isCatalogSchema()) {
            mapDataTypeAliases(PostgreConstants.DATA_TYPE_ALIASES);
            mapDataTypeAliases(PostgreConstants.SERIAL_TYPES);
        }
    }

    private void mapDataTypeAliases(Map<String, String> aliases) {
        // Add serial data types
        for (Map.Entry<String,String> aliasMapping : aliases.entrySet()) {
            PostgreDataType realType = getCachedObject(aliasMapping.getValue());
            if (realType != null) {
                PostgreDataType serialType = new PostgreDataType(realType, aliasMapping.getKey());
                cacheObject(serialType);
            }
        }
    }


    @Override
    public void clearCache() {
        super.clearCache();
        dataTypeMap.clear();
    }

    @Override
    public void removeObject(@NotNull PostgreDataType object, boolean resetFullCache) {
        super.removeObject(object, resetFullCache);
        dataTypeMap.remove(object.getObjectId());
    }

    @Override
    public void cacheObject(@NotNull PostgreDataType object) {
        if (getCachedObject(object.getName()) != null) {

        } else {
            super.cacheObject(object);
            if (!object.isAlias()) {
                dataTypeMap.put(object.getObjectId(), object);
            }
        }
    }

    @Override
    public void setCache(List<PostgreDataType> postgreDataTypes) {
        super.setCache(postgreDataTypes);
        for (PostgreDataType dt : postgreDataTypes) {
            if (!dt.isAlias()) {
                dataTypeMap.put(dt.getObjectId(), dt);
            }
        }
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT t.oid,t.* \n" +
                "FROM pg_catalog.pg_type t WHERE typnamespace=?\n" +
                "ORDER by t.oid");
        dbStat.setLong(1, owner.getObjectId());
        return dbStat;
    }

    @Override
    protected PostgreDataType fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return PostgreDataType.readDataType(session, owner, dbResult);
    }

    public PostgreDataType getDataType(long oid) {
        return dataTypeMap.get(oid);
    }

}
