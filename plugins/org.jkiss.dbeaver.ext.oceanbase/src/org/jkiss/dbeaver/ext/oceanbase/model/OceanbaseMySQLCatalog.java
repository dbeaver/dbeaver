/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class OceanbaseMySQLCatalog extends MySQLCatalog {

    private static final Log log = Log.getLog(OceanbaseMySQLCatalog.class);

    private final MySQLDataSource dataSource;
    private final OceanbaseProceduresCache oceanbaseProceduresCache = new OceanbaseProceduresCache();
    private final OceanbaseTableCache oceanbaseTableCache = new OceanbaseTableCache();

    private List<String> proceduresNames = new ArrayList<>();

    public OceanbaseMySQLCatalog(MySQLDataSource dataSource, ResultSet dbResult) {
        super(dataSource, dbResult);
        this.dataSource = dataSource;
        oceanbaseTableCache.setCaseSensitive(false);
    }

    public OceanbaseProceduresCache getProceduresCache() {
        return oceanbaseProceduresCache;
    }

    public OceanbaseTableCache getTableCache() {
        return this.oceanbaseTableCache;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        proceduresNames.clear();
        return super.refreshObject(monitor);
    }

    static class OceanbaseTableCache extends TableCache {

        OceanbaseTableCache() {
            super();
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @Nullable MySQLTableBase forTable
        ) throws SQLException {
            if (forTable instanceof MySQLView) {
                return session.prepareStatement("desc " + owner.getName() + "." + forTable.getName());
            }
            return super.prepareChildrenStatement(session, owner, forTable);
        }

        @Override
        protected MySQLTableColumn fetchChild(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @NotNull MySQLTableBase table,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            if (table instanceof MySQLView) {
                return new OceanbaseMySQLViewColumn(table, dbResult);
            }
            return super.fetchChild(session, owner, table, dbResult);
        }
    }

    class OceanbaseProceduresCache extends ProceduresCache {

        OceanbaseProceduresCache() {
            super();
        }

        @Override
        protected MySQLProcedure fetchObject(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            String routineName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_NAME);
            if (CommonUtils.isEmpty(routineName)) {
                log.debug("Can't read routine name.");
                return null;
            }
            if (proceduresNames.contains(routineName)) {
                // For some reason OceanBase information_schema.ROUTINES can store duplicates names.
                // And OceanBase doesn't support procedures overriding.
                // So let's avoid duplicates.
                log.debug("Skipped duplicate routine name " + routineName);
                return null;
            } else {
                proceduresNames.add(routineName);
            }
            return super.fetchObject(session, owner, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @Nullable MySQLProcedure procedure
        ) throws SQLException {
            if (procedure != null && procedure.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
                return session.getMetaData().getProcedureColumns(owner.getName(), null,
                        JDBCUtils.escapeWildCards(session, procedure.getName()), "%").getSourceStatement();
            } else {
                String queryFunctionString = "select * from mysql.proc where db=? and type='FUNCTION'" +
                    (procedure != null ? " and name=?" : "");
                JDBCPreparedStatement statement = session.prepareStatement(queryFunctionString);
                statement.setString(1, owner.getName());
                if (procedure != null) {
                    statement.setString(2, procedure.getName());
                }
                return statement;
            }
        }

        @Override
        protected MySQLProcedureParameter fetchChild(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @NotNull MySQLProcedure parent,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            if (parent.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
                return super.fetchChild(session, owner, parent, dbResult);
            } else {
                String returnString = JDBCUtils.safeGetString(dbResult, "returns");
                if (returnString == null) {
                    return null;
                }
                String paramListString = JDBCUtils.safeGetString(dbResult, "param_list");
                DBSObjectCache<MySQLProcedure, MySQLProcedureParameter> paramCache
                    = owner.getProceduresCache().getChildrenCache(parent);
                if (CommonUtils.isNotEmpty(paramListString)) {
                    String[] strings = paramListString.split(",");
                    List<MySQLProcedureParameter> funcParams = new ArrayList<>();
                    for (int i = 0; i < strings.length; i++) {
                        String[] argPart = strings[i].trim().split(" ");
                        if (argPart.length > 1) {
                            String argName = argPart[0];
                            String dataType = argPart[1];
                            DBSDataType type;
                            Integer typeSize = null;
                            if (CommonUtils.isNotEmpty(dataType) && dataType.contains("(") && dataType.contains(")")) {
                                String typeName = dataType.substring(0, dataType.indexOf("("));
                                String typeLength = dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")"));
                                typeSize = CommonUtils.toInt(typeLength, -1);
                                type = getDataSource().getLocalDataType(typeName);
                            } else {
                                type = getDataSource().getLocalDataType(dataType);
                            }
                            funcParams.add(new MySQLProcedureParameter(
                                parent,
                                DBSProcedureParameterKind.IN.getTitle(),
                                DBUtils.getUnQuotedIdentifier(getDataSource(), argName),
                                type == null ? Types.INTEGER : type.getTypeID(),
                                i,
                                typeSize != null ? typeSize : 42,
                                null,
                                null,
                                true,
                                DBSProcedureParameterKind.IN));
                        }
                    }
                    for (MySQLProcedureParameter param : funcParams) {
                        paramCache.cacheObject(param);
                    }
                }

                String[] returnParamsList = returnString.split("\\(");
                int columnSize = Integer.parseInt(returnParamsList[1].split("\\)")[0]);

                MySQLProcedureParameter parameter = new MySQLProcedureParameter(
                    parent,
                    "RETURN",
                    returnParamsList[0],
                    STRUCT_ATTRIBUTES,
                    0,
                    columnSize,
                    null,
                    null,
                    true,
                    DBSProcedureParameterKind.RETURN);
                paramCache.cacheObject(parameter);
                return parameter;
            }
        }

    }

}
