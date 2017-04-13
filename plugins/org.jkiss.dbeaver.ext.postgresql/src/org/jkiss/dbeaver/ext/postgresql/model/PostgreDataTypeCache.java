/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.IntKeyMap;

import java.sql.SQLException;
import java.util.List;

/**
 * PostgreDataTypeCache
 */
public class PostgreDataTypeCache extends JDBCObjectCache<PostgreDatabase, PostgreDataType>
{
    static final Log log = Log.getLog(PostgreDataTypeCache.class);
    private IntKeyMap<PostgreDataType> dataTypeMap = new IntKeyMap<>();

    @Override
    public void clearCache() {
        super.clearCache();
        dataTypeMap.clear();
    }

    @Override
    public void removeObject(@NotNull PostgreDataType object) {
        super.removeObject(object);
        dataTypeMap.remove(object.getObjectId());
    }

    @Override
    public void cacheObject(@NotNull PostgreDataType object) {
        super.cacheObject(object);
        dataTypeMap.put(object.getObjectId(), object);
    }

    @Override
    public void setCache(List<PostgreDataType> postgreDataTypes) {
        super.setCache(postgreDataTypes);
        for (PostgreDataType dt : postgreDataTypes) {
            dataTypeMap.put(dt.getObjectId(), dt);
        }
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreDatabase owner) throws SQLException
    {
        final JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT t.oid,t.* \n" +
                "FROM pg_catalog.pg_type t\n" +
                "ORDER by t.oid");
        //dbStat.setInt(1, owner.getObjectId());
        return dbStat;
    }

    @Override
    protected PostgreDataType fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        return PostgreDataType.readDataType(owner, dbResult);
    }

    public PostgreDataType getDataType(int oid) {
        return dataTypeMap.get(oid);
    }

}
