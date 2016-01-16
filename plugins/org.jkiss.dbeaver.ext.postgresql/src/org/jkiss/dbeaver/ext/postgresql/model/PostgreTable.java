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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.SimpleObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreTable
 */
public class PostgreTable extends PostgreTableBase
{

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private String tablespaceName;

        @Property(viewable = false, editable = true, updatable = true, order = 5)
        public String getTablespaceName() {
            return tablespaceName;
        }

        public void setTablespaceName(String tablespaceName) {
            this.tablespaceName = tablespaceName;
        }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<PostgreTable> {
        @Override
        public boolean isPropertyCached(PostgreTable object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private SimpleObjectCache<PostgreTable, PostgreTableForeignKey> foreignKeys = new SimpleObjectCache<>();

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public PostgreTable(PostgreSchema catalog)
    {
        super(catalog);
    }

    public PostgreTable(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return getSchema().indexCache.getObjects(monitor, getSchema(), this);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);
        getContainer().indexCache.clearObjectCache(this);
        foreignKeys.clearCache();
        synchronized (additionalInfo) {
            additionalInfo.loaded = false;
        }
        return true;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }

        PostgreDataSource dataSource = getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM pg_catalog.pg_tables t WHERE t.schemaname=? AND t.tablename=?")) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.tablespaceName = JDBCUtils.safeGetString(dbResult, "tablespace");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
        additionalInfo.loaded = true;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return "";
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

}
