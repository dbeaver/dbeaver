/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FireBirdDataSource
 */
public class FireBirdMetaModel extends GenericMetaModel
{
    private Pattern ERROR_POSITION_PATTERN = Pattern.compile(" line ([0-9]+), column ([0-9]+)");

    public FireBirdMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new FireBirdDataSource(monitor, container, this);
    }

    @Override
    public FireBirdDataTypeCache createDataTypeCache(@NotNull GenericStructContainer container) {
        return new FireBirdDataTypeCache(container);
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        return FireBirdUtils.getViewSource(monitor, sourceObject);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        return FireBirdUtils.getProcedureSource(monitor, sourceObject);
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM RDB$GENERATORS")) {
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$GENERATOR_NAME");
                        if (name == null) {
                            continue;
                        }
                        String description = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$DESCRIPTION");
                        FireBirdSequence sequence = new FireBirdSequence(
                            container,
                            name,
                            description,
                            null,
                            0,
                            -1,
                            1
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

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM RDB$TRIGGERS\n" +
                    "WHERE RDB$RELATION_NAME" + (table == null ? " IS NULL" : "=?"))) {
                if (table != null) {
                    dbStat.setString(1, table.getName());
                }
                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$TRIGGER_NAME");
                        if (name == null) {
                            continue;
                        }
                        int sequence = JDBCUtils.safeGetInt(dbResult, "RDB$TRIGGER_SEQUENCE");
                        int type = JDBCUtils.safeGetInt(dbResult, "RDB$TRIGGER_TYPE");
                        String description = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$DESCRIPTION");
                        FireBirdTrigger trigger = new FireBirdTrigger(
                            container,
                            table,
                            name,
                            description,
                            FireBirdTriggerType.getByType(type),
                            sequence);
                        result.add(trigger);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        return FireBirdUtils.getTriggerSource(monitor, (FireBirdTrigger)trigger);
    }

    @Override
    public DBPErrorAssistant.ErrorPosition getErrorPosition(@NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                pos.position = Integer.parseInt(matcher.group(2)) - 1;
                return pos;
            }
        }
        return null;
    }

    @Override
    public boolean isSystemTable(GenericTableBase table) {
        String tableName = table.getName();
        tableName = tableName.toUpperCase(Locale.ENGLISH);
        return tableName.startsWith("RDB$") || tableName.startsWith("MON$");    // [JDBC: Firebird]
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase object, @Nullable String objectName) throws SQLException {
        return session.prepareStatement("SELECT * FROM RDB$RELATIONS ORDER BY RDB$RELATION_NAME");
    }

    @Override
    public GenericTableBase createTableImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$RELATION_NAME");
        int relType = JDBCUtils.safeGetInt(dbResult, "RDB$RELATION_TYPE");
        boolean isSystem = JDBCUtils.safeGetInt(dbResult, "RDB$SYSTEM_FLAG") != 0;
        GenericTableBase table;
        if (relType == 1) {
            table = new FireBirdView(owner, tableName, isSystem ? "SYSTEM VIEW" : "VIEW", null);
        } else {
            String tableType;
            switch (relType) {
                case 2:
                    tableType = "EXTERNAL TABLE";
                    break;
                case 3:
                    tableType = "MONITORING TABLE";
                    break;
                case 4:
                    tableType = "CONNECTION-LEVEL GTT";
                    break;
                case 5:
                    tableType = "TRANSACTION-LEVEL GTT";
                    break;
                default:
                    tableType = isSystem ? "SYSTEM TABLE" : "TABLE";
                    break;
            }
            table = new FireBirdTable(owner, tableName, tableType, dbResult);
        }
        table.setPersisted(true);
        table.setSystem(isSystem);
        table.setDescription(JDBCUtils.safeGetStringTrimmed(dbResult, "RDB$DESCRIPTION"));
        return table;
    }

    @Override
    public GenericTableColumn createTableColumnImpl(DBRProgressMonitor monitor, GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new FireBirdTableColumn(monitor, table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, notNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException {
        return session.prepareStatement(
            "select " +
                "RC.RDB$RELATION_NAME TABLE_NAME," +
                "ISGMT.RDB$FIELD_NAME as COLUMN_NAME," +
                "CAST((ISGMT.RDB$FIELD_POSITION + 1) as SMALLINT) as KEY_SEQ," +
                "RC.RDB$CONSTRAINT_NAME as PK_NAME," +
                "RC.RDB$CONSTRAINT_TYPE as CONSTRAINT_TYPE " +
                "FROM " +
                "RDB$RELATION_CONSTRAINTS RC " +
                "INNER JOIN RDB$INDEX_SEGMENTS ISGMT ON RC.RDB$INDEX_NAME = ISGMT.RDB$INDEX_NAME " +
                "where RC.RDB$CONSTRAINT_TYPE IN ('PRIMARY KEY','UNIQUE') " +
                (forParent == null ? "" : "AND RC.RDB$RELATION_NAME = '" + forParent.getName()) + "' " +
                "ORDER BY ISGMT.RDB$FIELD_NAME ");
    }

    @Override
    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) throws DBException, SQLException {
        String constraintType = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE");
        return "PRIMARY KEY".equals(constraintType) ? DBSEntityConstraintType.PRIMARY_KEY : DBSEntityConstraintType.UNIQUE_KEY;
    }
}
