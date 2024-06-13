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
package org.jkiss.dbeaver.ext.db2.i.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * DB2IMetaModel
 */
public class DB2IMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(DB2IMetaModel.class);

    public DB2IMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DB2IDataSource(monitor, container, this);
    }

    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        String schemaName = schema.getName();
        return "QSYS".equals(schemaName) || "QSYS2".equals(schemaName) || "SYSIBM".equals(schemaName) || "SYSPROC".equals(schemaName) || "SYSTOOLS".equals(schemaName);
    }

    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new GenericView(container, tableName, tableType, dbResult);
        }
        return new DB2ITable(container, tableName, tableType, dbResult);
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read DB2I view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT VIEW_DEFINITION FROM QSYS2.SYSVIEWS " +
                    "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?")) {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        String definition = dbResult.getString(1);
                        if (CommonUtils.isNotEmpty(definition)) {
                            definition = "CREATE VIEW " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS " +  SQLFormatUtils.formatSQL(dataSource, definition);
                        } else {
                            // SYS views as example can have empty definition
                            definition = "-- View definition not available";
                        }
                        return definition;
                    }
                    return "-- DB2 i view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException {
        JDBCPreparedStatement dbStat;
        dbStat = session.prepareStatement("SELECT K.CONSTRAINT_NAME AS PK_NAME, K.TABLE_NAME, K.COLUMN_POSITION AS KEY_SEQ, K.COLUMN_NAME, tc.CONSTRAINT_TYPE, '' AS CHECK_CLAUSE FROM QSYS2.SYSKEYCST K\n" +
            "LEFT OUTER JOIN \"SYSIBM\".TABLE_CONSTRAINTS tc ON tc.CONSTRAINT_SCHEMA = K.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = K.CONSTRAINT_NAME\n" +
            "WHERE tc.CONSTRAINT_TYPE <> 'FOREIGN KEY' AND K.CONSTRAINT_SCHEMA = ?" +
            (forParent != null ? " AND K.TABLE_NAME = ?" : "") +
            "\nUNION ALL" +
            "\nSELECT cc.CONSTRAINT_NAME AS PK_NAME, tc.TABLE_NAME, 0 as KEY_SEQ, '' as COLUMN_NAME, 'CHECK' AS CONSTRAINT_TYPE, cc.CHECK_CLAUSE FROM QSYS2.CHECK_CONSTRAINTS cc\n" +
            "LEFT OUTER JOIN \"SYSIBM\".TABLE_CONSTRAINTS tc ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME\n" +
            "WHERE tc.CONSTRAINT_TYPE = 'CHECK' AND cc.CONSTRAINT_SCHEMA = ?" +
            (forParent != null ? " AND tc.TABLE_NAME = ?" : ""));
        if (forParent != null) {
            String schemaName = forParent.getSchema().getName();
            String tableName = forParent.getName();
            dbStat.setString(1, schemaName);
            dbStat.setString(2, tableName);
            dbStat.setString(3, schemaName);
            dbStat.setString(4, tableName);
        } else {
            String ownerName = owner.getName();
            dbStat.setString(1, ownerName);
            dbStat.setString(2, ownerName);
        }
        return dbStat;
    }

    @Override
    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) {
        String constraintType = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE");
        if ("UNIQUE".equals(constraintType)) {
            return DBSEntityConstraintType.UNIQUE_KEY;
        } else if ("CHECK".equals(constraintType)) {
            return DBSEntityConstraintType.CHECK;
        }
        return DBSEntityConstraintType.PRIMARY_KEY;
    }

    @Override
    public GenericUniqueKey createConstraintImpl(GenericTableBase table, String constraintName, DBSEntityConstraintType constraintType, JDBCResultSet dbResult, boolean persisted) {
        String checkClause = null;
        if (dbResult != null) {
            checkClause = JDBCUtils.safeGetString(dbResult, "CHECK_CLAUSE");
        }
        return new DB2IConstraint(table, constraintName, null, constraintType, persisted, checkClause);
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
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read DB2 for i procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ROUTINE_DEFINITION FROM QSYS2.SYSROUTINES " +
                    "WHERE ROUTINE_SCHEMA=? AND ROUTINE_NAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        String definition = dbResult.getString(1);
                        if (definition != null) {
                            definition = SQLFormatUtils.formatSQL(dataSource, definition);
                        }
                        return definition;
                    }
                    return "-- DB2 i procedure definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT * FROM \"SYSIBM\".SEQUENCES\n" +
                "WHERE SEQUENCE_SCHEMA=? ORDER BY SEQUENCE_NAME");
        dbStat.setString(1, container.getSchema().getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        String sequenceName = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
        if (CommonUtils.isEmpty(sequenceName)) {
            return null;
        }
        return new GenericSequence(
            container,
            sequenceName,
            "",
            null,
            JDBCUtils.safeGetLong(dbResult, "MINIMUM_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "MAXIMUM_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "INCREMENT"));
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return true;
    }
}
