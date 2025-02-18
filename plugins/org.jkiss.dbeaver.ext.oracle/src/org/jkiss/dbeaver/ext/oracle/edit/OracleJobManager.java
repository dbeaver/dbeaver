/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleJob;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OracleJobManager extends SQLObjectEditor<OracleJob, OracleSchema> {

    private static final String DATE_REGEX = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]) ([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";

    @Override
    protected OracleJob createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container, Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        OracleJob oracleJob = new OracleJob((OracleSchema) container, "NEW_JOB");
        oracleJob.setObjectDefinitionText("");
        return oracleJob;
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        Map<Object, Object> properties = command.getProperties();
        OracleJob oracleJob = command.getObject();
        String action = SQLUtils.quoteString(oracleJob, String.valueOf(properties.get("action")));
        String interval = SQLUtils.quoteString(oracleJob, String.valueOf(properties.get("interval")));
        String isBroken = String.valueOf(properties.get("broken")).toUpperCase(Locale.ROOT);
        String nextDate = formatDate(oracleJob, String.valueOf(properties.get("nextDate")));
        String sql = String.format(
            """
                DECLARE
                  l_job NUMBER;
                BEGIN
                  DBMS_JOB.SUBMIT(
                    job       => l_job,
                    what      => %s,
                    next_date => %s,
                    interval  => %s
                  );
                  COMMIT;
                  DBMS_JOB.BROKEN(l_job, %s);
                END;
                """, action, nextDate, interval, isBroken
        );
        Object nlsEnv = properties.get("nlsEnv");
        if (nlsEnv != null) {
            actions.add(modifyNlsEnv(nlsEnv));
        }
        actions.add(new SQLDatabasePersistAction("Create New Job", sql)); //$NON-NLS-1$
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull SQLObjectEditor<OracleJob, OracleSchema>.ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        OracleJob oracleJob = command.getObject();
        Map<Object, Object> properties = command.getProperties();
        String action = SQLUtils.quoteString(oracleJob, String.valueOf(properties.getOrDefault("action", oracleJob.getAction())));
        String interval = SQLUtils.quoteString(oracleJob, String.valueOf(properties.getOrDefault("interval", oracleJob.getInterval())));
        String isBroken = String.valueOf(properties.get("broken")).toUpperCase(Locale.ROOT);
        long job = oracleJob.getJob();
        String nextDate = formatDate(oracleJob, String.valueOf(properties.get("nextDate")));
        String sql = String.format(
            """
                BEGIN
                  DBMS_JOB.CHANGE(
                    job       => %s,
                    what      => %s,
                    next_date => %s,
                    interval  => %s
                  );
                  DBMS_JOB.BROKEN(%s, %s);
                  COMMIT;
                END;
                """,
            job, action, nextDate, interval, job, isBroken
        );
        Object nlsEnv = properties.get("nlsEnv");
        if (nlsEnv != null) {
            actionList.add(modifyNlsEnv(nlsEnv));
        }
        actionList.add(new SQLDatabasePersistAction("Update Job", sql)); //$NON-NLS-1$
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull SQLObjectEditor<OracleJob, OracleSchema>.ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        String sql = String.format(
            """
                BEGIN
                  DBMS_JOB.REMOVE(%s);
                  COMMIT;
                END;
                """, command.getObject().getJob()
        );
        actions.add(new SQLDatabasePersistAction("Delete Job", sql)); //$NON-NLS-1$
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleJob> getObjectsCache(OracleJob object) {
        return object.getSchema().jobCache;
    }

    private SQLDatabasePersistAction modifyNlsEnv(@NotNull Object nlsEnv) {
        String sql = String.format("ALTER SESSION SET %s", nlsEnv);
        return new SQLDatabasePersistAction("Edit NLS ENV", sql);
    }

    private String formatDate(OracleJob oracleJob, String date) {
        return date.matches(DATE_REGEX)
            ? "TO_DATE(%s,'yyyy-mm-dd hh24:mi:ss')".formatted(SQLUtils.quoteString(oracleJob, date))
            : date;
    }
}
