/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleJavaClass;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedureStandalone;
import org.jkiss.dbeaver.ext.oracle.model.OracleQueue;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerProgram;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchemaTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndexColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YashanDBSchema
 */
public class YashanDBSchema extends OracleSchema {

    private final YashanDBTableCache yashanDBTableCache = new YashanDBTableCache();
    private final YashanDBForeignKeyCache yashanDBForeignKeyCache = new YashanDBForeignKeyCache();
    private final YashanDBConstraintCache yashanDBConstraintCache = new YashanDBConstraintCache();
    private final YashanDBIndexCache yashanDBIndexCache = new YashanDBIndexCache();
    private final YashanDBTableTriggerCache YashanDBTableTriggerCache = new YashanDBTableTriggerCache();
    private final YashanDBDataTypeCache yashanDBDataTypeCache = new YashanDBDataTypeCache();
    private final YashanDBPackageCache yashanDBPackageCache = new YashanDBPackageCache();
    private final YashanDBProceduresCache yashanDBProceduresCache = new YashanDBProceduresCache();
    private final YashanDBSchedulerJobCache yashanDBSchedulerJobCache = new YashanDBSchedulerJobCache();

    public YashanDBSchema(@NotNull OracleDataSource dataSource, @NotNull ResultSet dbResult) {
        super(dataSource, dbResult);
    }

    public YashanDBSchema(OracleDataSource dataSource, long id, String name) {
        super(dataSource, id, name);
    }

    @NotNull
    @Override
    public YashanDBTableCache getTableCache() {
        return this.yashanDBTableCache;
    }

    @NotNull
    @Override
    public YashanDBConstraintCache getConstraintCache() {
        return this.yashanDBConstraintCache;
    }

    @NotNull
    @Override
    public YashanDBForeignKeyCache getForeignKeyCache() {
        return this.yashanDBForeignKeyCache;
    }

    @NotNull
    @Override
    public YashanDBIndexCache getIndexCache() {
        return this.yashanDBIndexCache;
    }

    @NotNull
    @Override
    public YashanDBTableTriggerCache getTableTriggerCache() {
        return this.YashanDBTableTriggerCache;
    }

    @NotNull
    @Override
    public YashanDBDataTypeCache getDataTypeCache() {
        return this.yashanDBDataTypeCache;
    }

    @NotNull
    @Override
    public YashanDBPackageCache getPackageCache() {
        return this.yashanDBPackageCache;
    }

    @NotNull
    @Override
    public YashanDBProceduresCache getProceduresCache() {
        return this.yashanDBProceduresCache;
    }

    @NotNull
    @Override
    public SchedulerJobCache getSchedulerJobCache() {
        return this.yashanDBSchedulerJobCache;
    }

    @Override
    public YashanDBTable createTableImpl(@NotNull DBRProgressMonitor monitor, @NotNull OracleSchema owner,
                                         @NotNull JDBCResultSet dbResult) {
        return new YashanDBTable(monitor, owner, dbResult);
    }

    @Override
    public Collection<OracleQueue> getQueues(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support
        return Collections.emptyList();
    }

    @Override
    public Collection<OracleJavaClass> getJavaClasses(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support
        return Collections.emptyList();
    }

    @Override
    public Collection<OracleSchemaTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support
        return Collections.emptyList();
    }

    @Override
    public Collection<OracleSchedulerProgram> getSchedulerPrograms(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support
        return Collections.emptyList();
    }

    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options)
            throws DBException {
        // YashanDB not support
        return "-- EMPTY DDL";
    }

    class YashanDBTableCache extends TableCache {

        YashanDBTableCache() {
            super();
        }

        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                                    @Nullable OracleTableBase object, @Nullable String objectName) throws SQLException {
            // YashanDB custom
            JDBCPreparedStatement dbStat = null;
            String sql = "SELECT  O.*,t.OWNER,t.TABLE_TYPE,t.TABLESPACE_NAME,t.PARTITIONED,t.TEMPORARY,t.NUM_ROWS "
                + "FROM (SELECT * FROM "
                + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "OBJECTS")
                + " o where O.OWNER= ? AND O.OBJECT_NAME NOT LIKE '%BIN$%' and O.OBJECT_TYPE IN ('TABLE','VIEW','MATERIALIZED VIEW')"
                + (object == null && objectName == null ? "" : " AND O.OBJECT_NAME = ?")
                + (object instanceof OracleTable ? " AND O.OBJECT_TYPE='TABLE'" : "")
                + (object instanceof OracleView ? " AND O.OBJECT_TYPE='VIEW'" : "")
                + (object instanceof OracleMaterializedView ? " AND O.OBJECT_TYPE='MATERIALIZED VIEW'" : "")
                + ") O LEFT JOIN "
                + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "TABLES") + " t "
                + "on t.OWNER= O.OWNER AND t.TABLE_NAME = o.OBJECT_NAME";

            dbStat = session.prepareStatement(sql);
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }

        @Override
        protected OracleTableBase fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                              @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            final String tableType = JDBCUtils.safeGetString(dbResult, OracleConstants.COLUMN_OBJECT_TYPE);
            if ("TABLE".equals(tableType)) {
                return owner.createTableImpl(session.getProgressMonitor(), owner, dbResult);
            } else if ("MATERIALIZED VIEW".equals(tableType)) {
                return new YashanDBMaterializedView(owner, dbResult);
            } else {
                return new YashanDBView(owner, dbResult);
            }
        }

        @Override
        protected YashanDBTableColumn fetchChild(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                                 @NotNull OracleTableBase table, @NotNull JDBCResultSet dbResult) throws DBException {
            return new YashanDBTableColumn(session.getProgressMonitor(), table, dbResult);
        }
    }

    class YashanDBForeignKeyCache extends ForeignKeyCache {

        YashanDBForeignKeyCache() {
            super(yashanDBTableCache);
        }

        @Override
        protected OracleTableForeignKeyColumn[] fetchObjectRow(@NotNull JDBCSession session,
                                                               @NotNull OracleTable parent, @NotNull OracleTableForeignKey object,
                                                               @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException {

            if (JDBCUtils.safeGetString(dbResult, "COLUMN_NAMES_NUMS") != null) {
                List<SpecialPosition> positions = parsePositions(
                    JDBCUtils.safeGetString(dbResult, "COLUMN_NAMES_NUMS"));
                OracleTableForeignKeyColumn[] result = new OracleTableForeignKeyColumn[positions.size()];
                for (int idx = 0; idx < positions.size(); idx++) {
                    OracleTableColumn column = getTableColumn(session, parent, dbResult,
                        positions.get(idx).getColumn());
                    if (column == null) {
                        continue;
                    }
                    // YashanDB position start 0
                    result[idx] = new OracleTableForeignKeyColumn(object, column, positions.get(idx).getPos() + 1);
                }
                return result;

            } else {
                OracleTableColumn column = getTableColumn(session, parent, dbResult,
                    JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME"));
                if (column == null) {
                    return null;
                }
                // YashanDB position start 0
                return new OracleTableForeignKeyColumn[] {new OracleTableForeignKeyColumn(object, column,
                    JDBCUtils.safeGetInt(dbResult, "POSITION") + 1)};
            }
        }
    }

    class YashanDBConstraintCache extends ConstraintCache {

        protected YashanDBConstraintCache() {
            super(yashanDBTableCache);
        }
    }

    class YashanDBIndexCache extends IndexCache {

        protected YashanDBIndexCache() {
            super(yashanDBTableCache);
        }

        @Nullable
        @Override
        protected OracleTableIndexColumn[] fetchObjectRow(@NotNull JDBCSession session, @NotNull OracleTableBase parent,
                                                          @NotNull OracleTableIndex object, @NotNull JDBCResultSet dbResult)
                throws DBException {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
            // YashanDB position start 0
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION") + 1;
            boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND"));
            String columnExpression = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_EXPRESSION");

            OracleTableColumn tableColumn = columnName == null ? null
                : parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '"
                    + object.getName() + "'");
                return null;
            }

            return new OracleTableIndexColumn[] {
                new OracleTableIndexColumn(object, tableColumn, ordinalPosition, isAscending, columnExpression)};
        }
    }

    class YashanDBTableTriggerCache extends TableTriggerCache {

        protected YashanDBTableTriggerCache() {
            super(yashanDBTableCache);
        }

        @Nullable
        @Override
        protected YashanDBTableTrigger fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema schema,
                                                   @NotNull OracleTableBase table, @NotNull String childName,
                                                   @NotNull JDBCResultSet resultSet)
                throws SQLException, DBException {
            return new YashanDBTableTrigger(table, resultSet);
        }
    }

    static class YashanDBDataTypeCache extends DataTypeCache {

        @Override
        protected YashanDBDataType fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                               @NotNull JDBCResultSet resultSet) throws SQLException {
            return new YashanDBDataType(owner, resultSet);
        }
    }

    static class YashanDBPackageCache extends PackageCache {

        @Override
        protected YashanDBPackage fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                              @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new YashanDBPackage(owner, dbResult);
        }
    }

    static class YashanDBProceduresCache extends ProceduresCache {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                                    @Nullable OracleProcedureStandalone object, @Nullable String objectName)
                throws SQLException {
            // YashanDB function -> UDF
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT "
                + OracleUtils.getSysCatalogHint(owner.getDataSource())
                + " CASE WHEN OBJECT_TYPE = 'UDF' THEN 'FUNCTION' ELSE OBJECT_TYPE END AS OBJECT_TYPE, OBJECT_NAME ,OBJECT_ID ,STATUS FROM "
                + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner.getDataSource(), "OBJECTS")
                + " " + "WHERE OBJECT_TYPE IN ('PROCEDURE','UDF') " + "AND OWNER=? "
                + (object == null && objectName == null ? "" : "AND OBJECT_NAME=? ") + "ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }

        @Override
        protected YashanDBProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                                          @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new YashanDBProcedureStandalone(owner, dbResult);
        }
    }

    static class YashanDBSchedulerJobCache extends SchedulerJobCache {

        @Override
        protected YashanDBSchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner,
                                                   @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new YashanDBSchedulerJob(owner, dbResult);
        }

    }
}
