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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        List<MySQLTableColumn> childColumns = getContainer().tableCache.getChildren(monitor, getContainer(), this);
        if (childColumns == null) {
            return Collections.emptyList();
        }
        List<MySQLTableColumn> columns = new ArrayList<>(childColumns);
        Collections.sort(columns, DBUtils.orderComparator());
        return columns;
    }

    @Override
    public MySQLTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    public List<MySQLTableColumn> getCachedAttributes()
    {
        DBSObjectCache<MySQLTableBase, MySQLTableColumn> childrenCache = getContainer().tableCache.getChildrenCache(this);
        if (childrenCache != null) {
            return childrenCache.getCachedObjects();
        }
        return Collections.emptyList();
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
                        if (isView()) {
                            return dbResult.getString("Create View");
                        } else {
                            return dbResult.getString("Create Table");
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
