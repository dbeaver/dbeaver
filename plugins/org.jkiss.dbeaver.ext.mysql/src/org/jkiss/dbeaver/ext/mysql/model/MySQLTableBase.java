/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * MySQLTable base
 */
public abstract class MySQLTableBase extends JDBCTable<MySQLDataSource, MySQLCatalog>
    implements DBPNamedObject2,DBPRefreshableObject, MySQLSourceObject
{
    private static final Log log = Log.getLog(MySQLTableBase.class);

    protected MySQLTableBase(MySQLCatalog catalog)
    {
        super(catalog, false);
    }

    // Copy constructor
    protected MySQLTableBase(DBRProgressMonitor monitor, MySQLCatalog catalog, DBSEntity source) throws DBException {
        super(catalog, source, false);

        DBSObjectCache<MySQLTableBase, MySQLTableColumn> colCache = getContainer().getTableCache().getChildrenCache(this);
        // Copy columns
        for (DBSEntityAttribute srcColumn : CommonUtils.safeCollection(source.getAttributes(monitor))) {
            if (DBUtils.isHiddenObject(srcColumn)) {
                continue;
            }
            MySQLTableColumn column = new MySQLTableColumn(this, srcColumn);
            colCache.cacheObject(column);
        }
    }

    protected MySQLTableBase(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, 1), true);
    }

    @Override
    public JDBCStructCache<MySQLCatalog, ? extends JDBCTable, ? extends JDBCTableColumn> getCache()
    {
        return getContainer().getTableCache();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @Override
    public Collection<MySQLTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public MySQLTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!isPersisted()) {
            return JDBCUtils.generateTableDDL(monitor, this, false);
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Retrieve table DDL")) {
            try (PreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE " + (isView() ? "VIEW" : "TABLE") + " " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (ResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        byte[] ddl;
                        if (isView()) {
                            ddl = dbResult.getBytes("Create View");
                        } else {
                            ddl = dbResult.getBytes("Create Table");
                        }
                        if (ddl == null) {
                            return null;
                        } else {
                            try {
                                return new String(ddl, getContainer().getDefaultCharset().getName());
                            } catch (UnsupportedEncodingException e) {
                                log.debug(e);
                                return new String(ddl);
                            }
                        }
                    } else {
                        return "DDL is not available";
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DBException(ex, getDataSource());
        }
    }

}
