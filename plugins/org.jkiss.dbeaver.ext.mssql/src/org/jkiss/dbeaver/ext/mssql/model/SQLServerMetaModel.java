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
package org.jkiss.dbeaver.ext.mssql.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLServerMetaModel
 */
public class SQLServerMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    static final Log log = Log.getLog(SQLServerMetaModel.class);

    public SQLServerMetaModel(IConfigurationElement cfg) {
        super(cfg);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        return extractSource(monitor, sourceObject.getDataSource(), sourceObject.getCatalog().getName(), sourceObject.getSchema().getName(), sourceObject.getName());
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return extractSource(monitor, sourceObject.getDataSource(), sourceObject.getCatalog().getName(), sourceObject.getSchema().getName(), sourceObject.getName());
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<? extends GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTable table) throws DBException {
        assert table != null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read triggers")) {
            String schema = getSystemSchema(getServerType(monitor, container.getDataSource()));
            String catalog = table.getCatalog().getName();
            String query =
                "SELECT triggers.name FROM " + catalog + "." + schema + ".sysobjects tables, " + catalog + "." + schema + ".sysobjects triggers\n" +
                "WHERE triggers.type = 'TR'\n" +
                "AND triggers.deltrig = tables.id\n" +
                "AND user_name(tables.uid) = ? AND tables.name = ?";

            try (JDBCPreparedStatement dbStat = session.prepareStatement(query)) {
                dbStat.setString(1, table.getSchema().getName());
                dbStat.setString(2, table.getName());
                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        GenericTrigger trigger = new GenericTrigger(container, table, name, null);
                        result.add(trigger);
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @NotNull
    private String getSystemSchema(ServerType serverType) {
        return serverType == ServerType.SQL_SERVER ? "sys" : "dbo";
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        GenericTable table = trigger.getTable();
        assert table != null;
        return extractSource(monitor, table.getDataSource(), table.getCatalog().getName(), table.getSchema().getName(), trigger.getName());
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //return new QueryTransformerTop();
        }
        return null;
    }

    private String extractSource(DBRProgressMonitor monitor, GenericDataSource dataSource, String catalog, String schema, String name) throws DBException {
        ServerType serverType = getServerType(monitor, dataSource);
        String systemSchema = getSystemSchema(serverType);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read source code")) {
            String mdQuery = serverType == ServerType.SQL_SERVER ?
                catalog + "." + systemSchema + ".sp_helptext '" + schema + "." + name + "'"
                :
                "SELECT sc.text\n" +
                "FROM " + catalog + "." + systemSchema + ".sysobjects so\n" +
                "INNER JOIN " + catalog + "." + systemSchema + ".syscomments sc on sc.id = so.id\n" +
                "WHERE user_name(so.uid)=? AND so.name=?";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(mdQuery)) {
                if (serverType == ServerType.SYBASE) {
                    dbStat.setString(1, schema);
                    dbStat.setString(2, name);
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    public static ServerType getServerType(DBRProgressMonitor monitor, DBPDataSource dataSource) {
        JDBCExecutionContext context = (JDBCExecutionContext) dataSource.getDefaultContext(true);
        try {
            Connection connection = context.getConnection(monitor);
            String connectionClass = connection.getClass().getName();
            if (connectionClass.contains("jtds")) {
                try {
                    Integer serverType = (Integer) connection.getClass().getMethod("getServerType").invoke(connection);
                    if (serverType == 1) {
                        return ServerType.SQL_SERVER;
                    } else {
                        return ServerType.SYBASE;
                    }
                } catch (Throwable e) {
                    log.debug("Can't determine JTDS driver type", e);
                    return ServerType.SQL_SERVER;
                }
            } else if (connectionClass.contains("microsoft")) {
                return ServerType.SQL_SERVER;
            } else {
                return ServerType.SYBASE;
            }
        } catch (SQLException e) {
            return ServerType.UNKNOWN;
        }
    }

}
