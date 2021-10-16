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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.vertica.VerticaConstants;
import org.jkiss.dbeaver.ext.vertica.VerticaUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VerticaMetaModel
 */
public class VerticaMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private static final Log log = Log.getLog(VerticaMetaModel.class);

    private Boolean childObjectColumnAvailable;

    public VerticaMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new VerticaDataSource(monitor, container, this);
    }

    @Override
    public GenericSchema createSchemaImpl(@NotNull GenericDataSource dataSource, GenericCatalog catalog, @NotNull String schemaName) throws DBException {
        return new VerticaSchema(dataSource, catalog, schemaName);
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, GenericTableBase table, String tableName) throws SQLException {
        JDBCPreparedStatement dbStat;
        if (owner instanceof VerticaSchema && ((VerticaSchema) owner).isSystem()) {
            String sql = "SELECT st.table_schema as table_schem, st.table_description as remarks, 'SYSTEM TABLE' as table_type, st.*\n" +
                "from v_catalog.system_tables st where st.table_schema =?" + (table != null ? " and st.table_name=?" : "");
            dbStat = session.prepareStatement(sql);
            dbStat.setString(1, owner.getName());
            if (table != null) {
                dbStat.setString(2, table.getName());
            }
        } else {
            String sql = "SELECT t.table_schema as table_schem, t.table_name, t.create_time, t.is_temp_table, t.is_system_table, t.has_aggregate_projection, " +
                "t.partition_expression, case when is_flextable = true THEN 'FLEX TABLE' else 'TABLE' end as table_type, com.\"comment\" as remarks\n" +
                "FROM v_catalog.tables t LEFT JOIN v_catalog.comments com ON com.object_type = 'TABLE'\n" +
                "AND com.object_id = t.table_id\n" +
                (owner instanceof VerticaSchema ? "WHERE t.table_schema=?" + (table != null || CommonUtils.isNotEmpty(tableName) ? " and t.table_name = ?" : "") : "")+
                "\nUNION ALL\n" +
                "SELECT v.table_schema as table_schem, v.table_name, v.create_time, v.is_local_temp_view as is_temp_table, v.is_system_view as is_system_table, false as has_aggregate_projection, " +
                "null as partition_expression, 'VIEW' as table_type, com.\"comment\" as remarks\n" +
                "FROM v_catalog.views v LEFT JOIN v_catalog.comments com ON com.object_type = 'VIEW'\n" +
                "AND com.object_id = v.table_id\n" +
                (owner instanceof VerticaSchema ? "WHERE v.table_schema=?" + (table != null || CommonUtils.isNotEmpty(tableName) ? " and v.table_name = ?" : "") : "");
            dbStat = session.prepareStatement(sql);
            if (owner instanceof VerticaSchema) {
                dbStat.setString(1, owner.getName());
                if (table != null || CommonUtils.isNotEmpty(tableName)) {
                    dbStat.setString(2, table != null ? table.getName() : tableName);
                    dbStat.setString(3, owner.getName());
                    dbStat.setString(4, table != null ? table.getName() : tableName);
                } else {
                    dbStat.setString(2, owner.getName());
                }
            }
        }

        return dbStat;
    }

    @Override
    public GenericTableBase createTableImpl(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        VerticaSchema schema = (VerticaSchema) container;
        if (CommonUtils.isNotEmpty(tableType)) {
            if (isView(tableType)) {
                return new VerticaView(schema, tableName, tableType, dbResult);
            } else if (tableType.equals(VerticaConstants.TYPE_FLEX_TABLE)) {
                return new VerticaFlexTable(schema, tableName, tableType, dbResult);
            } else if (tableType.equals(VerticaConstants.TYPE_SYSTEM_TABLE)) {
                return new VerticaSystemTable(schema, tableName, tableType, dbResult);
            } else {
                return new VerticaTable(schema, tableName, tableType, dbResult);
            }
        } else {
            return new VerticaTable(schema, tableName, tableType, dbResult);
        }
    }

    @Override
    public JDBCStatement prepareTableColumnLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException {
        JDBCPreparedStatement dbStat;
        boolean supportsCommentReading = isChildCommentColumnAvailable(session.getProgressMonitor(), owner.getDataSource());
        StringBuilder ddl = new StringBuilder();
        ddl.append("SELECT ");
        if (supportsCommentReading) ddl.append("com.comment AS REMARKS,");
        ddl.append("col.*, col.sql_type_id AS SOURCE_DATA_TYPE, col.data_type_name AS TYPE_NAME, col.column_default AS COLUMN_DEF " +
            "FROM v_catalog.odbc_columns col ");
        if (supportsCommentReading) {
            ddl.append("LEFT JOIN v_catalog.comments com ON com.object_type = 'COLUMN' " +
                "AND com.object_schema = col.schema_name " +
                "AND com.object_name = col.table_name " +
                "AND com.child_object = col.column_name ");
        }
        ddl.append("WHERE col.schema_name=? ");
        if (forTable != null) {
            ddl.append("AND col.table_name=? ");
        }
        dbStat = session.prepareStatement(ddl.toString());
        if (forTable != null) {
            dbStat.setString(1, forTable.getSchema().getName());
            dbStat.setString(2, forTable.getName());
        } else {
            dbStat.setString(1, owner.getSchema().getName());
        }
        return dbStat;
    }

    private boolean isChildCommentColumnAvailable(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource) {
        // child_object is very helpful column in v_catalog.comments table, but it's not childObjectColumnAvailable in Vertica versions < 9.3 and in some other cases
        if (childObjectColumnAvailable == null) {
            try {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Check child comment column existence")) {
                    try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                            "SELECT child_object FROM v_catalog.comments WHERE 1<>1"))
                    {
                        dbStat.setFetchSize(1);
                        dbStat.execute();
                        childObjectColumnAvailable = true;
                    }
                }
            } catch (Exception e) {
                childObjectColumnAvailable = false;
            }
        }
        return childObjectColumnAvailable;
    }

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        // From Vertica documentation: "Column constraints AUTO_INCREMENT and IDENTITY are synonyms that associate a column with a sequence"
        if (dbResult != null) {
            autoIncrement = JDBCUtils.safeGetBoolean(dbResult, "is_identity");
        }
        return new VerticaTableColumn(table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, notNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTableBase sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        if (sourceObject.isPersisted()) {
            return VerticaUtils.getObjectDDL(monitor, dataSource, sourceObject);
        } else {
            return super.getTableDDL(monitor, sourceObject, options);
        }
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        return getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Vertica procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT function_definition FROM v_catalog.user_functions WHERE schema_name=? AND function_name=?")) {
                dbStat.setString(1, sourceObject.getSchema().getName());
                dbStat.setString(2, sourceObject.getName());
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

    @Override
    public boolean supportsOverloadedProcedureNames() {
        return true;
    }

    @Override
    public boolean showProcedureParamNames() {
        return true;
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT s.*, c.comment FROM v_catalog.sequences s\n" +
                    "LEFT JOIN v_catalog.comments c\n" +
                    "ON s.sequence_id = c.object_id\n" +
                    "WHERE sequence_schema=? ORDER BY sequence_name")) {
                dbStat.setString(1, container.getSchema().getName());
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "sequence_name");
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        VerticaSequence sequence = new VerticaSequence(
                            container,
                            name,
                            JDBCUtils.safeGetString(dbResult, "comment"),
                            JDBCUtils.safeGetLong(dbResult, "current_value"),
                            JDBCUtils.safeGetLong(dbResult, "minimum"),
                            JDBCUtils.safeGetLong(dbResult, "maximum"),
                            JDBCUtils.safeGetLong(dbResult, "increment_by"),
                            JDBCUtils.safeGetString(dbResult, "identity_table_name"),
                            JDBCUtils.safeGetLong(dbResult, "session_cache_count"),
                            JDBCUtils.safeGetBoolean(dbResult, "allow_cycle")
                        );
                        result.add(sequence);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimitVertica();
        }
        return null;
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException, DBException {
        JDBCPreparedStatement dbStat;
        dbStat = session.prepareStatement("SELECT col.constraint_name as PK_NAME, col.table_name as TABLE_NAME, col.column_name as COLUMN_NAME, " +
                "c.ordinal_position as KEY_SEQ, col.constraint_type, tc.predicate, com.comment, col.is_enabled \n" +
                "FROM v_catalog.constraint_columns col\n" +
                "LEFT JOIN v_catalog.columns c ON\n" +
                "c.table_id = col.table_id\n" +
                "LEFT JOIN v_catalog.comments com ON\n" +
                "com.object_id = col.constraint_id\n" +
                "JOIN v_catalog.table_constraints tc ON\n" +
                "tc.constraint_id = col.constraint_id \n" +
                "AND col.column_name = c.column_name \n" +
                "WHERE col.constraint_type IN ('u','p','c')\n" +
                "AND col.table_schema = ?" + (forParent != null ? " AND col.table_name = ?" : "") +
                " ORDER BY col.table_id, KEY_SEQ, PK_NAME");
        if (forParent != null) {
            dbStat.setString(1, forParent.getSchema().getName());
            dbStat.setString(2, forParent.getName());
        } else {
            dbStat.setString(1, owner.getName());
        }
        return dbStat;
    }
    @Override
    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) throws DBException, SQLException {
        String type = JDBCUtils.safeGetString(dbResult, "constraint_type");
        if (type != null) {
            if ("u".equals(type)) {
                return DBSEntityConstraintType.UNIQUE_KEY;
            }
            if ("c".equals(type)) {
                return DBSEntityConstraintType.CHECK;
            }
            return DBSEntityConstraintType.PRIMARY_KEY;
        }
        return super.getUniqueConstraintType(dbResult);
    }

    @Override
    public GenericUniqueKey createConstraintImpl(GenericTableBase table, String constraintName, DBSEntityConstraintType constraintType, JDBCResultSet dbResult, boolean persisted) {
        String checkExpression = "";
        String comment = null;
        boolean isEnabled = false;
        if (dbResult != null) {
            checkExpression = JDBCUtils.safeGetString(dbResult, "predicate");
            comment = JDBCUtils.safeGetString(dbResult, "comment");
            isEnabled = JDBCUtils.safeGetBoolean(dbResult, "is_enabled");
        }
        return new VerticaConstraint(table, constraintName, comment, constraintType, persisted, CommonUtils.notEmpty(checkExpression).trim(), isEnabled);
    }

    @Override
    public boolean supportsCheckConstraints() {
        return true;
    }
}
