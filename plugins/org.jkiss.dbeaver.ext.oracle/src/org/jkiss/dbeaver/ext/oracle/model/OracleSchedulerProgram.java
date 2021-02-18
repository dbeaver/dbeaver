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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Oracle scheduler program
 */
public class OracleSchedulerProgram extends OracleSchemaObject {

    private static final String CAT_ADVANCED = "Advanced";

    private String owner;
	private String programName;
	private String programType;
	private String programAction;
	private long numberOfArguments;
	private String enabled;
	private String detached;
	private String scheduleLimit;
	private String priority;
	private String weight;
	private long maxRuns;
	private long maxFailures;
	private String maxRunDuration;
	private String nlsEnv;
    private String comments;
    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    OracleSchedulerProgram(OracleSchema schema, ResultSet dbResult) {
        super(schema, JDBCUtils.safeGetString(dbResult, "PROGRAM_NAME"), true);

        owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        programName = JDBCUtils.safeGetString(dbResult, "PROGRAM_NAME");
        programType = JDBCUtils.safeGetString(dbResult, "PROGRAM_TYPE");
        programAction = JDBCUtils.safeGetString(dbResult, "PROGRAM_ACTION");
        numberOfArguments = JDBCUtils.safeGetLong(dbResult, "NUMBER_OF_ARGUMENTS");
        enabled = JDBCUtils.safeGetString(dbResult, "ENABLED");
        detached = JDBCUtils.safeGetString(dbResult, "DETACHED");
        maxRuns = JDBCUtils.safeGetLong(dbResult, "MAX_RUNS");
        maxFailures = JDBCUtils.safeGetLong(dbResult, "MAX_FAILURES");
        scheduleLimit = JDBCUtils.safeGetString(dbResult, "SCHEDULE_LIMIT");
        priority = JDBCUtils.safeGetString(dbResult, "PRIORITY");
        weight = JDBCUtils.safeGetString(dbResult, "WEIGHT");
        maxRunDuration = JDBCUtils.safeGetString(dbResult, "MAX_RUN_DURATION");
        nlsEnv = JDBCUtils.safeGetString(dbResult, "NLS_ENV");
        comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
    }

    @Property(viewable = false, order = 10)
    public String getOwner() {
        return owner;
    }

    @Property(viewable = true, order = 16)
    public String getProgramName() {
        return programName;
    }

    @Property(viewable = true, order = 17)
    public String getProgramType() {
        return programType;
    }

    @Property(viewable = false, order = 18)
    public String getProgramAction() {
        return programAction;
    }

    @Property(viewable = false, order = 19)
    public long getNumberOfArguments() {
        return numberOfArguments;
    }

    @Property(viewable = true, order = 34)
    public String getEnabled() {
        return enabled;
    }

    @Property(viewable = true, order = 35)
    public String getDetached() {
        return detached;
    }

    @Property(viewable = false, order = 40)
    public long getMaxRuns() {
        return maxRuns;
    }

    @Property(viewable = false, order = 42)
    public long getMaxFailures() {
        return maxFailures;
    }


    @Property(viewable = false, order = 47)
    public String getScheduleLimit() {
        return scheduleLimit;
    }

    @Property(viewable = true, order = 48)
    public String getPriority() {
        return priority;
    }

    @Property(viewable = false, order = 49)
    public String getWeight() {
        return weight;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 48)
    public String getMaxRunDuration() {
        return maxRunDuration;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 55)
    public String getNlsEnv() {
        return nlsEnv;
    }

    @Property(viewable = true, order = 200)
    @Nullable
    @Override
    public String getDescription() {
        return comments;
    }

    @Association
    public Collection<OracleSchedulerProgramArgument> getArguments(DBRProgressMonitor monitor) throws DBException
    {
        return argumentsCache.getAllObjects(monitor, this);
    }

    static class ArgumentsCache extends JDBCObjectCache<OracleSchedulerProgram, OracleSchedulerProgramArgument> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchedulerProgram program) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM ALL_SCHEDULER_PROGRAM_ARGS " +
                        "WHERE OWNER=? AND PROGRAM_NAME=? " +
                        "ORDER BY ARGUMENT_POSITION");
            dbStat.setString(1, program.getSchema().getName());
            dbStat.setString(2, program.getName());
            return dbStat;
        }

        @Override
        protected OracleSchedulerProgramArgument fetchObject(@NotNull JDBCSession session, @NotNull OracleSchedulerProgram program, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSchedulerProgramArgument(program, resultSet);
        }

    }
}
