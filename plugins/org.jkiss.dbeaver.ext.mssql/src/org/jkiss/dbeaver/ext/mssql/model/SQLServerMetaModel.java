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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
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
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLServerMetaModel
 */
public class SQLServerMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private static final Log log = Log.getLog(SQLServerMetaModel.class);

    public SQLServerMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new SQLServerDataSource(monitor, container, this);
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
            String catalog = DBUtils.getQuotedIdentifier(table.getCatalog());
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
        catalog = DBUtils.getQuotedIdentifier(dataSource, catalog);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read source code")) {
            String mdQuery = serverType == ServerType.SQL_SERVER ?
                catalog + "." + systemSchema + ".sp_helptext '" + DBUtils.getQuotedIdentifier(dataSource, schema) + "." + DBUtils.getQuotedIdentifier(dataSource, name) + "'"
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

    @Override
    public GenericTableIndex createIndexImpl(GenericTable table, boolean nonUnique, String qualifier, long cardinality, String indexName, DBSIndexType indexType, boolean persisted) {
        return new SQLServerIndex(table, nonUnique, qualifier, cardinality, indexName, indexType, persisted);
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "IDENTITY(1,1)";
    }

    @Override
    public boolean useCatalogInObjectNames() {
        return false;
    }
}
