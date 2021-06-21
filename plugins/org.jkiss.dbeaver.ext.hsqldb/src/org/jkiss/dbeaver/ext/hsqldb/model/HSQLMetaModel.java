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
package org.jkiss.dbeaver.ext.hsqldb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HSQLMetaModel
 */
public class HSQLMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(HSQLMetaModel.class);

    public HSQLMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new HSQLDataSource(monitor, container, this);
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HSQLDB view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS " +
                    "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return "CREATE VIEW AS " + dbResult.getString(1);
                    }
                    return "-- HSQLDB view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        GenericDataSource dataSource = container.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read HSQLDB procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA=?"))
            {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        DBSProcedureType routineType = DBSProcedureType.PROCEDURE;
                        try {
                            routineType = DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "ROUTINE_TYPE"));
                        } catch (IllegalArgumentException e) {
                            log.warn(e);
                        }
                        final GenericProcedure procedure = createProcedureImpl(
                            container,
                            JDBCUtils.safeGetString(dbResult, "ROUTINE_NAME"),
                            JDBCUtils.safeGetString(dbResult, "SPECIFIC_NAME"),
                            null,
                            routineType,
                            null);
                        container.addProcedure(procedure);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HSQLDB procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ROUTINE_DEFINITION FROM INFORMATION_SCHEMA.ROUTINES " +
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
                    return "-- HSQLDB procedure definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA=?")) {
                dbStat.setString(1, container.getName());
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
                        if (name == null) {
                            continue;
                        }
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            null,
                            JDBCUtils.safeGetLong(dbResult, "NEXT_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "MINIMUM_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "MAXIMUM_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "INCREMENT")
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
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "IDENTITY";
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer genericStructContainer, @Nullable GenericTableBase forParent) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT EVENT_OBJECT_TABLE AS OWNER, T.* FROM INFORMATION_SCHEMA.TRIGGERS T\n" +
                        "WHERE EVENT_OBJECT_SCHEMA=?" + (forParent != null ? " AND EVENT_OBJECT_TABLE=?" : ""));
            dbStat.setString(1, genericStructContainer.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
        return dbStat;
    }

    @Override
    public GenericTrigger createTableTriggerImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer genericStructContainer, @NotNull GenericTableBase genericTableBase, String triggerName, @NotNull JDBCResultSet resultSet) throws DBException {
        if (CommonUtils.isEmpty(triggerName)) {
            triggerName = JDBCUtils.safeGetString(resultSet, "TRIGGER_NAME");
        }
        if (triggerName == null) {
            return null;
        }
        triggerName = triggerName.trim();
        return new HSQLTrigger(
                genericTableBase,
                triggerName,
                resultSet);
    }

    @Override
    public List<GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws DBException {
        if (table == null) {
            throw new DBException("Database level triggers aren't supported for HSQLDB");
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM INFORMATION_SCHEMA.TRIGGERS\n" +
                            "WHERE EVENT_OBJECT_SCHEMA=? AND EVENT_OBJECT_TABLE=?")) {
                dbStat.setString(1, container.getName());
                dbStat.setString(2, table.getName());

                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME");
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        HSQLTrigger trigger = new HSQLTrigger(
                                table,
                                name,
                                dbResult);
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
        return ((HSQLTrigger)trigger).getStatement();
    }

    @Override
    public boolean supportsSynonyms(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<HSQLSynonym> loadSynonyms(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_SYNONYMS\n" +
                    "WHERE SYNONYM_SCHEMA=?\n" +
                    "ORDER BY SYNONYM_NAME")) {
                dbStat.setString(1, container.getName());

                List<HSQLSynonym> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        HSQLSynonym trigger = new HSQLSynonym(
                            container,
                            dbResult);
                        result.add(trigger);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }
}
