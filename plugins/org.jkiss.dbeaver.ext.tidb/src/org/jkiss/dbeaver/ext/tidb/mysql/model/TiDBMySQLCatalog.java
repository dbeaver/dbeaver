/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.tidb.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
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
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TiDBMySQLCatalog extends MySQLCatalog {
    private final TiDBMySQLDataSource dataSource;
    private final TiDBTableCache tidbTableCache = new TiDBTableCache();

    TiDBMySQLCatalog(TiDBMySQLDataSource dataSource, ResultSet dbResult) {
        super(dataSource, dbResult);
        this.dataSource = dataSource;
        tidbTableCache.setCaseSensitive(false);
    }

    public TiDBTableCache getTiDBTableCache() {
        return this.tidbTableCache;
    }

    @Override
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName) throws DBException {
        return null;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        tidbTableCache.clearCache();
        return this;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return dataSource;
    }

    /**
     * getTables use tidbTableCache to get all tables
     *
     * @param monitor Database progress monitor.
     * @return tables message
     * @throws DBException raise all exception from getTypedObjects() function
     */
    @Association
    public Collection<MySQLTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tidbTableCache.getTypedObjects(monitor, this, MySQLTable.class);
    }

    /**
     * getTable get particular table from tidbTableCache by name
     *
     * @param monitor Database progress monitor.
     * @param name table name
     * @return table message
     * @throws DBException raise all exception from getObject() function
     */
    public MySQLTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tidbTableCache.getObject(monitor, this, name, MySQLTable.class);
    }

    /**
     * getViews use tidbTableCache to get all views
     *
     * @param monitor Database progress monitor.
     * @return views message
     * @throws DBException raise all exception from getTypedObjects() function
     */
    @Association
    public Collection<MySQLView> getViews(DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(tidbTableCache.getTypedObjects(monitor, this, TiDBMySQLView.class));
    }

    @Override
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tidbTableCache.getAllObjects(monitor, this);
    }

    @Override
    public MySQLTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return tidbTableCache.getObject(monitor, this, childName);
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        monitor.subTask("Cache tables");
        tidbTableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tidbTableCache.loadChildren(monitor, this, null);
        }
        super.cacheStructure(monitor, scope);
    }

    static class TiDBTableCache
            extends JDBCStructLookupCache<TiDBMySQLCatalog, MySQLTableBase, MySQLTableColumn> {

        TiDBTableCache() {
            super(JDBCConstants.TABLE_NAME);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull TiDBMySQLCatalog owner,
                @Nullable MySQLTableBase object, @Nullable String objectName) throws SQLException {
            MySQLDataSource dataSource = owner.getDataSource();
            StringBuilder sql = new StringBuilder("SHOW FULL TABLES FROM ")
                    .append(DBUtils.getQuotedIdentifier(owner));
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
                                if (hasCond) {
                                    sql.append(" OR ");
                                }

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
                                if (hasCond) {
                                    sql.append(" OR ");
                                }
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
        protected MySQLTableBase fetchObject(@NotNull JDBCSession session, @NotNull TiDBMySQLCatalog owner,
                @NotNull JDBCResultSet dbResult) {
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType != null && tableType.contains("VIEW")) {
                return new TiDBMySQLView(owner, dbResult);
            } else {
                return new MySQLTable(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session,
                @NotNull TiDBMySQLCatalog owner, @Nullable MySQLTableBase forTable) throws SQLException {
            if (forTable instanceof TiDBMySQLView) {
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
        protected MySQLTableColumn fetchChild(@NotNull JDBCSession session, @NotNull TiDBMySQLCatalog owner,
                @NotNull MySQLTableBase table, @NotNull JDBCResultSet dbResult) throws DBException {
            if (table instanceof TiDBMySQLView) {
                return new TiDBMySQLViewColumn(table, dbResult);
            }
            return new MySQLTableColumn(table, dbResult);
        }

    }
}
