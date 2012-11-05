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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
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

    abstract protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, OWNER owner)
        throws SQLException;

    abstract protected OBJECT fetchObject(JDBCExecutionContext context, OWNER owner, ResultSet resultSet)
        throws SQLException, DBException;

    @Override
    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException
    {
        if (!isCached()) {
            loadObjects(monitor, owner);
        }
        return getCachedObjects();
    }

    @Override
    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String name)
        throws DBException
    {
        if (!isCached()) {
            this.loadObjects(monitor, owner);
        }
        return getCachedObject(name);
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
                monitor.subTask("Execute query");
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

    private void detectCaseSensitivity(DBSObject object) {
        if (this.caseSensitive && object.getDataSource().getInfo().storesUnquotedCase() == DBPIdentifierCase.MIXED) {
            this.caseSensitive = false;
        }
    }

}
