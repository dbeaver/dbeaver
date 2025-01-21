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
package org.jkiss.dbeaver.ext.cubrid.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.*;
import org.jkiss.dbeaver.ext.cubrid.model.plan.CubridQueryPlanner;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CubridMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private static final Log log = Log.getLog(CubridMetaModel.class);

    @Nullable
    public String getTableOrViewName(@Nullable GenericTableBase base) {
        if (base != null) {
            if (base.isView()) {
                return ((CubridView) base).getUniqueName();
            } else {
                return ((CubridTable) base).getUniqueName();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public List<GenericSchema> loadSchemas(@NotNull JDBCSession session, @NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog)
            throws DBException {
        List<GenericSchema> users = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement("select * from db_user")) {
            dbStat.executeStatement();
            try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                while (dbResult.next()) {
                    String name = JDBCUtils.safeGetStringTrimmed(dbResult, CubridConstants.NAME);
                    String description = JDBCUtils.safeGetStringTrimmed(dbResult, CubridConstants.COMMENT);
                    CubridUser user = new CubridUser(dataSource, name, description);
                    users.add(user);
	            }
            }
        } catch (SQLException e) {
            log.error("Cannot load user", e);
        }
        return users;
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase object,
            @Nullable String objectName)
            throws SQLException {
        String sql = "select a.*,a.class_name as TABLE_NAME, case when class_type = 'CLASS' then 'TABLE'\r\n"
                + " when class_type = 'VCLASS' then 'VIEW' end as TABLE_TYPE,\r\n"
                + " a.comment as REMARKS, b.current_val from db_class a LEFT JOIN\r\n"
                + " db_serial b on a.class_name = b.class_name\r\n"
                + " left join db_partition p on a.class_name = p.partition_class_name\r\n"
                + " where a.owner_name = ? and p.partition_class_name is null";

        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, owner.getName());
        return dbStat;
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableColumnLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable)
            throws SQLException {
        boolean multiSchema = forTable != null && ((CubridDataSource) forTable.getDataSource()).getSupportMultiSchema();
        StringBuilder sql = new StringBuilder();
        sql.append("select *, def_order + 1 as ref_order from db_attribute ");
        if (forTable != null) {
            sql.append("where class_name = ? ");
            if (multiSchema) {
                sql.append("and owner_name = ? ");
            }
        }
        sql.append("order by def_order");
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
        if (forTable != null) {
            dbStat.setString(1, forTable.getName());
            if (multiSchema) {
                dbStat.setString(2, owner.getName());
            }
        }
        return dbStat;
    }

    @NotNull
    @Override
    public boolean supportsUniqueKeys() {
        return true;
    }

    @NotNull
    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable)
            throws SQLException, DBException {
        CubridTable table = (CubridTable) forTable;
        String sql = "select *, t1.index_name as PK_NAME from db_index t1 join db_index_key t2 \n"
                + "on t1.index_name = t2.index_name where is_unique = 'YES' and t1.class_name = ? \n"
                + (table.getDataSource().getSupportMultiSchema() ? "and t1.owner_name = ?" : "");
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, table.getName());
        if (table.getDataSource().getSupportMultiSchema()) {
            dbStat.setString(2, table.getSchema().getName());
        }
        return dbStat;
    }

    @Nullable
    @Override
    public GenericTableConstraintColumn[] createConstraintColumnsImpl(
            @NotNull JDBCSession session,
            @NotNull GenericTableBase parent,
            @Nullable GenericUniqueKey object,
            @Nullable GenericMetaObject pkObject,
            @NotNull JDBCResultSet dbResult)
            throws DBException {
        String name = JDBCUtils.safeGetStringTrimmed(dbResult, "key_attr_name");
        Integer keyOrder = JDBCUtils.safeGetInteger(dbResult, "key_order") + 1;
        GenericTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), name);
        return new GenericTableConstraintColumn[] {
              new GenericTableConstraintColumn(object, tableColumn, keyOrder) };
    }

    @NotNull
    @Override
    public DBSEntityConstraintType getUniqueConstraintType(@NotNull JDBCResultSet dbResult) throws DBException {
        String isPrimary = JDBCUtils.safeGetString(dbResult, "is_primary_key");
        if (isPrimary.equals("YES")) {
            return DBSEntityConstraintType.PRIMARY_KEY;
        } else {
            return DBSEntityConstraintType.UNIQUE_KEY;
        }
    }

    @NotNull
    @Override
    public JDBCStatement prepareForeignKeysLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer owner,
            @Nullable GenericTableBase forTable)
            throws SQLException {
        return session.getMetaData().getImportedKeys(null, null, this.getTableOrViewName(forTable)).getSourceStatement();
    }

    @Nullable
    @Override
    public GenericTableBase createTableImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull GenericMetaObject tableObject,
        @NotNull JDBCResultSet dbResult) {
        String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
        String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);
        GenericTableBase table = createTableOrViewImpl(owner, tableName, tableType, dbResult);
        return table;
    }

    @Nullable
    @Override
    public GenericTableBase createTableOrViewImpl(
            @NotNull GenericStructContainer container,
            @Nullable String tableName,
            @Nullable String tableType,
            @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new CubridView(container, tableName, tableType, dbResult);
        }
        return new CubridTable(container, tableName, tableType, dbResult);
    }

    @NotNull
    public boolean isTableCommentEditable() {
        return true;
    }

    @NotNull
    public boolean isTableColumnCommentEditable() {
        return true;
    }

    @NotNull
    @Override
    public JDBCStatement prepareSequencesLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container)
            throws SQLException {
        String sql = "select *, owner.name from db_serial where owner.name = ?";
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Nullable
    @Override
    public GenericSequence createSequenceImpl(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container,
            @NotNull JDBCResultSet dbResult)
            throws DBException {
        String name = JDBCUtils.safeGetStringTrimmed(dbResult, CubridConstants.NAME);
        return new CubridSequence(container, name, dbResult);
    }

    @NotNull
    @Override
    public JDBCStatement prepareSynonymsLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container)
            throws SQLException {
        String sql = "select * from db_synonym where synonym_owner_name = ?";
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Nullable
    @Override
    public GenericSynonym createSynonymImpl(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container,
            @NotNull JDBCResultSet dbResult)
            throws DBException {
        String name = JDBCUtils.safeGetStringTrimmed(dbResult, "synonym_name");
        String description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
        return new CubridSynonym(container, name, description, dbResult);
    }

    @NotNull
    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @NotNull
    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container,
            @Nullable GenericTableBase table)
            throws SQLException {
        boolean supportMultiSchema = ((CubridDataSource) table.getDataSource()).getSupportMultiSchema();
        String sql = "select t1.*, t2.*, t1.owner.name from db_trigger as t1, db_trig as t2 \n"
                + "where t1.name = t2.trigger_name and t1.owner.name = ? and t2.target_class_name = ? \n"
                + (supportMultiSchema ? "and t1.owner.name = t2.owner_name" : "");
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, container.getName());
        dbStat.setString(2, table.getName());
        return dbStat;
    }

    @Nullable
    @Override
    public CubridTrigger createTableTriggerImpl(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container,
            @Nullable GenericTableBase table,
            @Nullable String triggerName,
            @NotNull JDBCResultSet dbResult)
            throws DBException {
        String name = JDBCUtils.safeGetString(dbResult, CubridConstants.NAME);
        String description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
        return new CubridTrigger(table, name, description, dbResult);
    }

    @NotNull
    @Override
    public JDBCStatement prepareContainerTriggersLoadStatement(
            @NotNull JDBCSession session,
            @NotNull GenericStructContainer container)
            throws SQLException {
        boolean supportMultiSchema = ((CubridDataSource) container.getDataSource()).getSupportMultiSchema();
        String sql = "select t1.*, t2.*, t1.owner.name from db_trigger as t1, db_trig as t2 \n"
                + "where t1.name = t2.trigger_name and t1.owner.name = ?\n"
                + (supportMultiSchema ? "and t1.owner.name = t2.owner_name" : "");
        final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Nullable
    @Override
    public CubridTrigger createContainerTriggerImpl(
            @NotNull GenericStructContainer container,
            @NotNull JDBCResultSet dbResult)
            throws DBException {
        String name = JDBCUtils.safeGetString(dbResult, CubridConstants.NAME);
        String description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
        String tableName = JDBCUtils.safeGetString(dbResult, "target_class_name");
        String owner = JDBCUtils.safeGetString(dbResult, "target_owner_name");
        DBRProgressMonitor monitor = dbResult.getSession().getProgressMonitor();
        CubridTable cubridTable = (CubridTable) container.getDataSource().findTable(monitor, null, owner, tableName);
        return new CubridTrigger(cubridTable, name, description, dbResult);
    }

    @Override
    public void loadProcedures(
            @NotNull DBRProgressMonitor monitor,
            @NotNull GenericObjectContainer container)
            throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load procedures")) {
            String sql = "select * from db_stored_procedure where owner = ?";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String procedureName = JDBCUtils.safeGetString(dbResult, "sp_name");
                        String description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
                        String type = JDBCUtils.safeGetString(dbResult, "sp_type");
                        String returnType = JDBCUtils.safeGetString(dbResult, "return_type");
                        DBSProcedureType procedureType;
                        if (type.equalsIgnoreCase(CubridConstants.TERM_PROCEDURE)) {
                            procedureType = DBSProcedureType.PROCEDURE;
                        } else if (type.equalsIgnoreCase(CubridConstants.TERM_FUNCTION)) {
                            procedureType = DBSProcedureType.FUNCTION;
                        } else {
                            procedureType = DBSProcedureType.UNKNOWN;
                        }
                        container.addProcedure(new CubridProcedure(container, procedureName, description, procedureType, returnType));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Load procedures failed", e);
        }
    }

    @Nullable
    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView object, @NotNull Map<String, Object> options) throws DBException {
        String ddl = "-- View definition not available";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load view ddl")) {
            String sql = String.format("show create view %s", ((CubridView) object).getUniqueName());
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        ddl = "create or replace view " + dbResult.getString("View") + " as " + dbResult.getString("Create View");
                        ddl = SQLFormatUtils.formatSQL(object.getDataSource(), ddl);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Cannot load view ddl", e);
        }
        return ddl;
    }

    @Override
    public DBCQueryPlanner getQueryPlanner(@NotNull GenericDataSource dataSource) {
        return new CubridQueryPlanner((CubridDataSource) dataSource);
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimitCubrid();
        }
        return null;
    }
}
