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
package org.jkiss.dbeaver.ext.postgresql.model.generic;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreGenericDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreGenericTrigger;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreGenericTypeCache;
import org.jkiss.dbeaver.ext.postgresql.model.plan.PostgreQueryPlaner;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreMetaModel
 */
public class PostgreMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("\\n\\s*Position: ([0-9]+)");

    public PostgreMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new PostgreGenericDataSource(monitor, container, this);
    }

    @Override
    public JDBCBasicDataTypeCache createDataTypeCache(@NotNull GenericStructContainer container) {
        return new PostgreGenericTypeCache(container);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject.getDataSource(), "Read view definition")) {
            return JDBCUtils.queryString(session, "SELECT definition FROM PG_CATALOG.PG_VIEWS WHERE SchemaName=? and ViewName=?", sourceObject.getContainer().getName(), sourceObject.getName());
        } catch (SQLException e) {
            throw new DBException(e, sourceObject.getDataSource());
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject.getDataSource(), "Read procedure definition")) {
            return JDBCUtils.queryString(session, "SELECT pg_get_functiondef(p.oid) FROM PG_CATALOG.PG_PROC P, PG_CATALOG.PG_NAMESPACE NS\n" +
                "WHERE ns.oid=p.pronamespace and ns.nspname=? AND p.proname=?", sourceObject.getContainer().getName(), sourceObject.getName());
        } catch (SQLException e) {
            throw new DBException(e, sourceObject.getDataSource());
        }
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        Version databaseVersion = dataSource.getInfo().getDatabaseVersion();
        return databaseVersion.getMajor() >= 9 || databaseVersion.getMajor() == 8 && databaseVersion.getMinor() >= 4;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read procedure definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema=?")) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<GenericSequence> result = new ArrayList<>();
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        try (JDBCPreparedStatement dbSeqStat = session.prepareStatement("SELECT last_value,min_value,max_value,increment_by from " + container.getName() + "." + name)) {
                            try (JDBCResultSet seqResults = dbSeqStat.executeQuery()) {
                                seqResults.next();
                                GenericSequence sequence = new GenericSequence(
                                    container,
                                    name,
                                    PostgreUtils.getObjectComment(monitor, container.getDataSource(), container.getName(), name),
                                    JDBCUtils.safeGetLong(seqResults, 1),
                                    JDBCUtils.safeGetLong(seqResults, 2),
                                    JDBCUtils.safeGetLong(seqResults, 3),
                                    JDBCUtils.safeGetLong(seqResults, 4));
                                result.add(sequence);
                            }
                        }
                    }
                    return result;
                }
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
    public List<PostgreGenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTable table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read triggers")) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT trigger_name,event_manipulation,action_order,action_condition,action_statement,action_orientation,action_timing\n" +
                "FROM INFORMATION_SCHEMA.TRIGGERS\n" +
                "WHERE ");
            if (table == null) {
                sql.append("trigger_schema=? AND event_object_table IS NULL");
            } else {
                sql.append("event_object_schema=? AND event_object_table=?");
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
                if (table == null) {
                    dbStat.setString(1, container.getSchema().getName());
                } else {
                    dbStat.setString(1, table.getSchema().getName());
                    dbStat.setString(2, table.getName());
                }
                Map<String, PostgreGenericTrigger> result = new LinkedHashMap<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "trigger_name");
                        if (name == null) {
                            continue;
                        }
                        String manipulation = JDBCUtils.safeGetString(dbResult, "event_manipulation");
                        PostgreGenericTrigger trigger = result.get(name);
                        if (trigger != null) {
                            trigger.addManipulation(manipulation);
                            continue;
                        }
                        String description = "";
                        trigger = new PostgreGenericTrigger(
                            container,
                            table,
                            name,
                            description,
                            manipulation,
                            JDBCUtils.safeGetString(dbResult, "action_orientation"),
                            JDBCUtils.safeGetString(dbResult, "action_timing"),
                            JDBCUtils.safeGetString(dbResult, "action_statement"));
                        result.put(name, trigger);
                    }
                }
                return new ArrayList<>(result.values());

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        // Never be here
        return null;
    }

    @Override
    public DBCQueryPlanner getQueryPlanner(@NotNull GenericDataSource dataSource) {
        return new PostgreQueryPlaner(dataSource);
    }

    @Override
    public DBPErrorAssistant.ErrorPosition getErrorPosition(@NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.position = Integer.parseInt(matcher.group(1)) - 1;
                return pos;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false, true);
        }
        return null;
    }

}
