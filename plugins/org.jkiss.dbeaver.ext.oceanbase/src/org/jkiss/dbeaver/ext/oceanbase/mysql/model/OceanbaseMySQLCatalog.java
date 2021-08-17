/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedureParameter;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

public class OceanbaseMySQLCatalog extends MySQLCatalog {
    private final OceanbaseMySQLDataSource dataSource;
    private final OceanbaseProceduresCache oceanbaseProceduresCache = new OceanbaseProceduresCache();
    private final OceanbaseTableCache oceanbaseTableCache = new OceanbaseTableCache();

    OceanbaseMySQLCatalog(OceanbaseMySQLDataSource dataSource, ResultSet dbResult) {
        super(dataSource, dbResult);
        this.dataSource = dataSource;
        oceanbaseTableCache.setCaseSensitive(false);
    }

    public OceanbaseProceduresCache getOceanbaseProceduresCache() {
        return this.oceanbaseProceduresCache;
    }

    public OceanbaseTableCache getOceanbaseTableCache() {
        return this.oceanbaseTableCache;
    }

    @Override
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().supportsInformationSchema()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(oceanbaseProceduresCache.getAllObjects(monitor, this));
    }

    @Override
    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName) throws DBException {
        return oceanbaseProceduresCache.getObject(monitor, this, procName);
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        oceanbaseProceduresCache.clearCache();
        oceanbaseTableCache.clearCache();
        return this;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return dataSource;
    }

    @Association
    public Collection<MySQLTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return oceanbaseTableCache.getTypedObjects(monitor, this, MySQLTable.class);
    }

    public MySQLTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return oceanbaseTableCache.getObject(monitor, this, name, MySQLTable.class);
    }

    @Association
    public Collection<MySQLView> getViews(DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(oceanbaseTableCache.getTypedObjects(monitor, this, OceanbaseMySQLView.class));
    }

    @Override
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return oceanbaseTableCache.getAllObjects(monitor, this);
    }

    @Override
    public MySQLTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return oceanbaseTableCache.getObject(monitor, this, childName);
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        monitor.subTask("Cache tables");
        oceanbaseTableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            oceanbaseTableCache.loadChildren(monitor, this, null);
        }
        super.cacheStructure(monitor, scope);
    }

    static class OceanbaseTableCache
            extends JDBCStructLookupCache<OceanbaseMySQLCatalog, MySQLTableBase, MySQLTableColumn> {

        OceanbaseTableCache() {
            super(JDBCConstants.TABLE_NAME);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OceanbaseMySQLCatalog owner,
                @Nullable MySQLTableBase object, @Nullable String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder("SHOW ");
            MySQLDataSource dataSource = owner.getDataSource();
            if (session.getMetaData().getDatabaseMajorVersion() > 4) {
                sql.append("FULL ");
            }
            sql.append("TABLES FROM ").append(DBUtils.getQuotedIdentifier(owner));
            if (!session.getDataSource().getContainer().getPreferenceStore()
                    .getBoolean(ModelPreferences.META_USE_SERVER_SIDE_FILTERS)) {
                // Client side filter
                if (object != null || objectName != null) {
                    sql.append(" LIKE ").append(SQLUtils.quoteString(session.getDataSource(),
                            object != null ? object.getName() : objectName));
                }
            } else {
                String tableNameCol = DBUtils.getQuotedIdentifier(dataSource, "Tables_in_" + owner.getName());
                if (object != null || objectName != null) {
                    sql.append(" WHERE ").append(tableNameCol).append(" LIKE ").append(SQLUtils
                            .quoteString(session.getDataSource(), object != null ? object.getName() : objectName));
                    if (dataSource.supportsSequences()) {
                        sql.append(" AND Table_type <> 'SEQUENCE'");
                    }
                } else {
                    DBSObjectFilter tableFilters = dataSource.getContainer().getObjectFilter(MySQLTable.class, owner,
                            true);
                    if (tableFilters != null && !tableFilters.isNotApplicable()) {
                        sql.append(" WHERE ");
                        if (!CommonUtils.isEmpty(tableFilters.getInclude())) {
                            sql.append("(");
                            boolean hasCond = false;
                            for (String incName : tableFilters.getInclude()) {
                                if (hasCond)
                                    sql.append(" OR ");
                                hasCond = true;
                                sql.append(tableNameCol).append(" LIKE ").append(
                                        SQLUtils.quoteString(session.getDataSource(), SQLUtils.makeSQLLike(incName)));
                            }
                            sql.append(")");
                        }
                        if (!CommonUtils.isEmpty(tableFilters.getExclude())) {
                            if (!CommonUtils.isEmpty(tableFilters.getInclude())) {
                                sql.append(" AND ");
                            }
                            sql.append("(");
                            boolean hasCond = false;
                            for (String incName : tableFilters.getExclude()) {
                                if (hasCond)
                                    sql.append(" OR ");
                                hasCond = true;
                                sql.append(tableNameCol).append(" NOT LIKE ")
                                        .append(SQLUtils.quoteString(session.getDataSource(), incName));
                            }
                            sql.append(")");
                        }
                    } else if (dataSource.supportsSequences()) {
                        sql.append(" WHERE Table_type <> 'SEQUENCE'");
                    }
                }
            }

            return session.prepareStatement(sql.toString());
        }

        @Override
        protected MySQLTableBase fetchObject(@NotNull JDBCSession session, @NotNull OceanbaseMySQLCatalog owner,
                @NotNull JDBCResultSet dbResult) {
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType != null && tableType.contains("VIEW")) {
                return new OceanbaseMySQLView(owner, dbResult);
            } else {
                return new MySQLTable(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session,
                @NotNull OceanbaseMySQLCatalog owner, @Nullable MySQLTableBase forTable) throws SQLException {
            if (forTable instanceof OceanbaseMySQLView) {
                JDBCPreparedStatement dbStat = session
                        .prepareStatement("desc " + owner.getName() + "." + forTable.getName());
                return dbStat;
            }
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM ").append(MySQLConstants.META_TABLE_COLUMNS).append(" WHERE ")
                    .append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableColumn fetchChild(@NotNull JDBCSession session, @NotNull OceanbaseMySQLCatalog owner,
                @NotNull MySQLTableBase table, @NotNull JDBCResultSet dbResult) throws DBException {
            if (table instanceof OceanbaseMySQLView) {
                return new OceanbaseMySQLViewColumn(table, dbResult);
            }
            return new MySQLTableColumn(table, dbResult);
        }

    }

    static class OceanbaseProceduresCache
            extends JDBCStructLookupCache<OceanbaseMySQLCatalog, OceanbaseMySQLProcedure, MySQLProcedureParameter> {

        OceanbaseProceduresCache() {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session, OceanbaseMySQLCatalog owner,
                OceanbaseMySQLProcedure object, String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM "
                    + MySQLConstants.META_TABLE_ROUTINES + "\nWHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?"
                    + (object == null && objectName == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_NAME + "=?")
                    + " AND ROUTINE_TYPE" + (object == null ? " IN ('PROCEDURE','FUNCTION')" : "=?") + "\nORDER BY "
                    + MySQLConstants.COL_ROUTINE_NAME);
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
                if (object != null) {
                    dbStat.setString(3, String.valueOf(object.getProcedureType()));
                }
            }
            return dbStat;
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCSession session, OceanbaseMySQLCatalog owner,
                OceanbaseMySQLProcedure procedure) throws SQLException {
            if (procedure.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
                return session.getMetaData().getProcedureColumns(owner.getName(), null,
                        JDBCUtils.escapeWildCards(session, procedure.getName()), "%").getSourceStatement();
            } else {
                String queryFunctionString = "select * from mysql.proc where db=? and type='FUNCTION' and name=?";
                JDBCPreparedStatement statement = session.prepareStatement(queryFunctionString);
                statement.setString(1, owner.getName());
                statement.setString(2, procedure.getName());
                return statement;
            }
        }

        @Override
        protected MySQLProcedureParameter fetchChild(JDBCSession session, OceanbaseMySQLCatalog owner,
                OceanbaseMySQLProcedure parent, JDBCResultSet dbResult) {
            if (parent.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
                String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
                int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
                boolean notNull = JDBCUtils.safeGetInt(dbResult,
                        JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
                int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
                int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
                DBSProcedureParameterKind parameterType;
                switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn:
                    parameterType = DBSProcedureParameterKind.IN;
                    break;
                case DatabaseMetaData.procedureColumnInOut:
                    parameterType = DBSProcedureParameterKind.INOUT;
                    break;
                case DatabaseMetaData.procedureColumnOut:
                    parameterType = DBSProcedureParameterKind.OUT;
                    break;
                case DatabaseMetaData.procedureColumnReturn:
                    parameterType = DBSProcedureParameterKind.RETURN;
                    break;
                case DatabaseMetaData.procedureColumnResult:
                    parameterType = DBSProcedureParameterKind.RESULTSET;
                    break;
                default:
                    parameterType = DBSProcedureParameterKind.UNKNOWN;
                    break;
                }
                if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
                    columnName = "RETURN";
                }
                return new MySQLProcedureParameter(parent, columnName, typeName, valueType, position, columnSize, scale,
                        precision, notNull, parameterType);
            } else {
                String returnString = JDBCUtils.safeGetString(dbResult, "returns");
                if (returnString == null) {
                    return null;
                }
                String[] paramList = returnString.split("\\(");
                int columnSize = Integer.parseInt(paramList[1].split("\\)")[0]);

                return new MySQLProcedureParameter(parent, "RETURN", paramList[0], STRUCT_ATTRIBUTES, 0, columnSize,
                        null, null, true, null);
            }
        }

        @Override
        protected OceanbaseMySQLProcedure fetchObject(JDBCSession session, OceanbaseMySQLCatalog owner,
                JDBCResultSet resultSet) {
            return new OceanbaseMySQLProcedure(owner, resultSet);
        }

    }

}
