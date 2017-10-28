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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    public GenericDataSource createDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new FireBirdDataSource(monitor, container, this);
    }

    @Override
    public FireBirdDataTypeCache createDataTypeCache(@NotNull GenericStructContainer container) {
        return new FireBirdDataTypeCache(container);
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT RDB$GENERATOR_NAME,RDB$DESCRIPTION FROM RDB$GENERATORS")) {
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        String description = JDBCUtils.safeGetString(dbResult, 2);
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            description,
                            -1,
                            0,
                            -1,
                            1
                        );
                        result.add(sequence);
                    }
                }

                // Obtain sequence values
                for (GenericSequence sequence : result) {
                    try (JDBCPreparedStatement dbSeqStat = session.prepareStatement("SELECT GEN_ID(" + sequence.getName() + ", 0) from RDB$DATABASE")) {
                        try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                            seqResults.next();
                            sequence.setLastValue(JDBCUtils.safeGetLong(seqResults, 1));
                        }
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
    public List<GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTable table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT RDB$TRIGGER_NAME,RDB$TRIGGER_SEQUENCE,RDB$TRIGGER_TYPE,RDB$DESCRIPTION FROM RDB$TRIGGERS\n" +
                    "WHERE RDB$RELATION_NAME" + (table == null ? " IS NULL" : "=?"))) {
                if (table != null) {
                    dbStat.setString(1, table.getName());
                }
                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        int sequence = JDBCUtils.safeGetInt(dbResult, 2);
                        int type = JDBCUtils.safeGetInt(dbResult, 3);
                        String description = JDBCUtils.safeGetString(dbResult, 4);
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
    public boolean isSystemTable(GenericTable table) {
        final String tableName = table.getName();
        return tableName.contains("$");    // [JDBC: Firebird]
    }

    @Override
    public GenericTableColumn createTableColumnImpl(JDBCSession session, JDBCResultSet dbResult, GenericTable table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new FireBirdTableColumn(session, table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, notNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

}
