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
package org.jkiss.dbeaver.ext.h2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * H2MetaModel
 */
public class H2MetaModel extends GenericMetaModel
{
    public H2MetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new H2DataSource(monitor, container, new H2MetaModel());
    }

    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        return schema.getName().equals("INFORMATION_SCHEMA");
    }

    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new GenericView(container, tableName, tableType, dbResult);
        }
        return new H2Table(container, tableName, tableType, dbResult);
    }

    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject, @NotNull Map<String, Object> options) throws DBException {
        // We tried here using SELECT SQL FROM INFORMATION_SCHEMA.TABLES, but it is not good
        // And this SQL result does not have info about keys or indexes
        return super.getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        // Since version 2 H2 keeps part of data in the system views.
        // But VIEW_DEFINITION field is empty for system views in the INFORMATION_SCHEMA.VIEWS
        // Maybe someday something will change, but until we will show anything for system views
        if (sourceObject.getSchema() != null && !sourceObject.getSchema().isSystem()) {
            GenericDataSource dataSource = sourceObject.getDataSource();
            try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read H2 view source")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS " +
                        "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?")) {
                    dbStat.setString(1, sourceObject.getContainer().getName());
                    dbStat.setString(2, sourceObject.getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.nextRow()) {
                            return dbResult.getString(1);
                        }
                        return "-- H2 view definition not found";
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, dataSource);
            }
        }
        return super.getViewDDL(monitor, sourceObject, options);
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException, DBException {
        if (!owner.getDataSource().isServerVersionAtLeast(2, 0)) {
            JDBCPreparedStatement dbStat;
            dbStat = session.prepareStatement("SELECT c.*, c.CONSTRAINT_NAME AS PK_NAME FROM INFORMATION_SCHEMA.\"CONSTRAINTS\" c \n" +
                "WHERE c.CONSTRAINT_TYPE <> 'REFERENTIAL' AND c.CONSTRAINT_SCHEMA = ? "
                + (forParent != null ? "AND c.TABLE_NAME = ?" : ""));
            dbStat.setString(1, owner.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
            return dbStat;
        } else {
            JDBCPreparedStatement dbStat;
            dbStat = session.prepareStatement("SELECT tc.*, tc.CONSTRAINT_NAME AS PK_NAME, ccu.COLUMN_NAME, cc.CHECK_CLAUSE AS CHECK_EXPRESSION FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc LEFT JOIN\n" +
                "INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu ON tc.CONSTRAINT_SCHEMA = ccu.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME\n" +
                "LEFT JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME\n" +
                "WHERE tc.CONSTRAINT_TYPE NOT IN ('REFERENTIAL', 'FOREIGN KEY') AND tc.CONSTRAINT_SCHEMA = ?"
                + (forParent != null ? "AND tc.TABLE_NAME = ?" : ""));
            dbStat.setString(1, owner.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
            return dbStat;
        }
    }

    @Override
    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) throws DBException, SQLException {
        String type = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE");
        if (CommonUtils.isNotEmpty(type)) {
            if ("UNIQUE".equals(type)) {
                return DBSEntityConstraintType.UNIQUE_KEY;
            }
            if ("CHECK".equals(type)) {
                return DBSEntityConstraintType.CHECK;
            }
            return DBSEntityConstraintType.PRIMARY_KEY;
        }
        return super.getUniqueConstraintType(dbResult);
    }

    @Override
    public GenericUniqueKey createConstraintImpl(GenericTableBase table, String constraintName, DBSEntityConstraintType constraintType, JDBCResultSet dbResult, boolean persisted) {
        if (dbResult != null) {
            String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
            String checkExpression = JDBCUtils.safeGetString(dbResult, "CHECK_EXPRESSION");
            return new H2Constraint(table, constraintName, description, constraintType, persisted, checkExpression);
        }
        return new H2Constraint(table, constraintName, null, constraintType, persisted, null);
    }

    @Override
    public GenericTableConstraintColumn[] createConstraintColumnsImpl(JDBCSession session, GenericTableBase parent, GenericUniqueKey object, GenericMetaObject pkObject, JDBCResultSet dbResult) throws DBException {
        GenericDataSource dataSource = parent.getDataSource();
        if (!dataSource.isServerVersionAtLeast(2, 0) && dbResult != null) { // H2 Version 2 has COLUMN_NAME and works fine
            List<GenericTableConstraintColumn> constraintColumns = new ArrayList<>();
            String columnList = JDBCUtils.safeGetString(dbResult, "COLUMN_LIST");
            List<? extends GenericTableColumn> attributes = parent.getAttributes(dbResult.getSession().getProgressMonitor());
            if (CommonUtils.isNotEmpty(columnList) && !CommonUtils.isEmpty(attributes)) {
                if (columnList.contains(",")) {
                    // We have a few columns in the key. Let's find them all.
                    String[] strings = columnList.split(",");
                    for (String columnName : strings) {
                        findConstraintColumns(object, dataSource, constraintColumns, columnName, attributes);
                    }
                } else {
                    // It can be only one key column
                    findConstraintColumns(object, dataSource, constraintColumns, columnList, attributes);
                }
            }
            return ArrayUtils.toArray(GenericTableConstraintColumn.class, constraintColumns);
        }
        return super.createConstraintColumnsImpl(session, parent, object, pkObject, dbResult);
    }

    private void findConstraintColumns(GenericUniqueKey object, GenericDataSource dataSource, List<GenericTableConstraintColumn> constraintColumns, String columnList, List<? extends GenericTableColumn> attributes) {
        Optional<? extends GenericTableColumn> match = attributes.stream().filter(item -> DBUtils.getUnQuotedIdentifier(dataSource, item.getName()).equals(columnList)).findFirst();
        if (match.isPresent()) {
            GenericTableColumn tableColumn = match.get();
            constraintColumns.add(new GenericTableConstraintColumn(object, tableColumn, tableColumn.getOrdinalPosition()));
        }
    }

    @Override
    public boolean supportsCheckConstraints() {
        return true;
    }

    @Override
    public boolean supportsUniqueKeys() {
        return true;
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container)
        throws SQLException {
        JDBCPreparedStatement statement = session.prepareStatement(
            "SELECT * FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA=?");
        statement.setString(1, container.getSchema().getName());
        return statement;
    }

    @Override
    public GenericSequence createSequenceImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        String name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
        boolean isVersion2 = container.getDataSource().isServerVersionAtLeast(2, 0);

        // Some columns are changed in H2 version 2 and some new columns added
        return new H2Sequence(
            container,
            name,
            description,
            isVersion2 ? JDBCUtils.safeGetLong(dbResult, "BASE_VALUE") : JDBCUtils.safeGetLong(dbResult, "CURRENT_VALUE"),
            isVersion2 ? JDBCUtils.safeGetLong(dbResult, "MINIMUM_VALUE") : JDBCUtils.safeGetLong(dbResult, "MIN_VALUE"),
            isVersion2 ? JDBCUtils.safeGetLong(dbResult, "MAXIMUM_VALUE") : JDBCUtils.safeGetLong(dbResult, "MAX_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "INCREMENT"),
            dbResult
        );
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "AUTO_INCREMENT";
/*
        if (!column.isPersisted()) {
            return "AUTO_INCREMENT";
        } else {
            // For existing columns auto-increment will in DEFAULT clause
            return null;
        }
*/
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return true;
    }

    @Override
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        String sql;
        boolean new2H2 = container.getDataSource().isServerVersionAtLeast(2, 0);
        if (new2H2) {
            sql = "SELECT * FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA = ?";
        } else {
            sql = "SELECT * FROM INFORMATION_SCHEMA.FUNCTION_ALIASES WHERE ALIAS_SCHEMA = ?";
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load functions aliases")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        String aliasName = JDBCUtils.safeGetString(dbResult, new2H2 ? "ROUTINE_NAME" : "ALIAS_NAME");
                        if (CommonUtils.isEmpty(aliasName)) {
                            continue;
                        }
                        String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
                        DBSProcedureType type = DBSProcedureType.PROCEDURE;
                        GenericFunctionResultType resultType = null;
                        if (new2H2) {
                            String routineType = JDBCUtils.safeGetString(dbResult, "ROUTINE_TYPE");
                            if ("FUNCTION".equals(routineType)) {
                                type = DBSProcedureType.FUNCTION;
                                resultType = GenericFunctionResultType.UNKNOWN;
                            }
                        } else {
                            int procType = JDBCUtils.safeGetInt(dbResult, "RETURNS_RESULT");
                            if (procType == 2) {
                                type = DBSProcedureType.FUNCTION;
                                resultType = GenericFunctionResultType.UNKNOWN;
                            }
                        }
                        H2RoutineAlias routineAlias = new H2RoutineAlias(container, aliasName, description, type, resultType, dbResult);
                        container.addProcedure(routineAlias);
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, container.getDataSource());
            }
        }
    }
}
