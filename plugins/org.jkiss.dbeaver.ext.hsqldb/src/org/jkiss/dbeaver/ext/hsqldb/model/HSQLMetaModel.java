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
package org.jkiss.dbeaver.ext.hsqldb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.hsqldb.model.plan.HSQLQueryPlanner;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HSQLMetaModel
 */
public class HSQLMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(HSQLMetaModel.class);
    private static final Pattern PROHIBITED_PATTERN = Pattern.compile("jdbc:hsqldb:(file|mem|res)");

    public HSQLMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            String url = container.getConnectionConfiguration().getUrl();
            if (!container.getDriver().isEmbedded() && url != null) {
                final Matcher matcher = PROHIBITED_PATTERN.matcher(url);
                if (matcher.find()) {
                    throw new DBException("File access is not allowed for this driver. " +
                        "Please use the embedded driver to access server files.");
                }
            }
        }
        return new HSQLDataSource(monitor, container, this);
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
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
            throw new DBDatabaseException(e, dataSource);
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
            throw new DBDatabaseException(e, dataSource);
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
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA=?");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        String name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        return new GenericSequence(
            container,
            name,
            null,
            JDBCUtils.safeGetLong(dbResult, "NEXT_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "MINIMUM_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "MAXIMUM_VALUE"),
            JDBCUtils.safeGetLong(dbResult, "INCREMENT")
        );
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
    public GenericTrigger createTableTriggerImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer genericStructContainer,
        @NotNull GenericTableBase genericTableBase,
        String triggerName,
        @NotNull JDBCResultSet resultSet
    ) throws DBException {
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
            throw new DBDatabaseException(e, container.getDataSource());
        }
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        return ((HSQLTrigger)trigger).getStatement();
    }

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareSynonymsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_SYNONYMS\n" +
                "WHERE SYNONYM_SCHEMA=?\n" +
                "ORDER BY SYNONYM_NAME");
        dbStat.setString(1, container.getName());
        return dbStat;
    }

    @Override
    public GenericSynonym createSynonymImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        return new HSQLSynonym(container, dbResult);
    }

    @Override
    public DBCQueryPlanner getQueryPlanner(@NotNull GenericDataSource dataSource) {
        return new HSQLQueryPlanner((HSQLDataSource) dataSource);
    }
}
