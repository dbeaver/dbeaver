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
package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Various objects cache.
 * Simple cache which may read objects from database and keep them.
 */
public abstract class JDBCObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> extends AbstractObjectCache<OWNER, OBJECT>
{

    protected JDBCObjectCache() {
    }

    abstract protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner)
        throws SQLException;

    @Nullable
    abstract protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull ResultSet resultSet)
        throws SQLException, DBException;

    @NotNull
    @Override
    public Collection<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException
    {
        if (!isCached()) {
            loadObjects(monitor, owner);
        }
        return getCachedObjects();
    }

    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner, @NotNull String name)
        throws DBException
    {
        if (!isCached()) {
            this.loadObjects(monitor, owner);
        }
        return getCachedObject(name);
    }

    protected void loadObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        if (isCached() || monitor.isCanceled()) {
            return;
        }

        List<OBJECT> tmpObjectList = new ArrayList<OBJECT>();

        DBPDataSource dataSource = owner.getDataSource();
        if (dataSource == null) {
            throw new DBException("Not connected to database");
        }
        JDBCSession session = (JDBCSession) dataSource.getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Load objects from " + owner.getName());
        try {
            JDBCStatement dbStat = prepareObjectsStatement(session, owner);
            try {
                monitor.subTask("Execute query");
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

                            monitor.subTask(object.getName());
                        }
                    } finally {
                        dbResult.close();
                    }
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex, session.getDataSource());
        }
        finally {
            session.close();
        }

        Comparator<OBJECT> comparator = getListOrderComparator();
        if (comparator != null) {
            Collections.sort(tmpObjectList, comparator);
        }

        synchronized (this) {
            detectCaseSensitivity(owner);
            setCache(tmpObjectList);
            this.invalidateObjects(monitor, owner, new CacheIterator());
        }
    }


}
