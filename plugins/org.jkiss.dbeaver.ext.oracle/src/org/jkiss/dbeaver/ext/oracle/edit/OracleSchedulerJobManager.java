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
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerJob;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerJob.JobState;
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
import java.util.Set;

public class OracleSchedulerJobManager extends SQLObjectEditor<OracleSchedulerJob, OracleSchema> {

    private static final String TIMESTAMP_FORMAT = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]) " +
                                                   "([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d\\.\\d{6} [+-]\\d{1,2}:[0-5]\\d$";

    @Override
    protected OracleSchedulerJob createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container, Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        return new OracleSchedulerJob(
            (OracleSchema) container,
            "NEW_SCHEDULER_JOB",
            JobState.DISABLED.name(),
            ""
        );
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        Map<Object, Object> properties = command.getProperties();
        OracleSchedulerJob schedulerJob = command.getObject();
        String owner = schedulerJob.getParentObject().getName();

        String jobFullNameQuoted = getJobFullNameQuoted(schedulerJob, owner);
        String jobType = properties.get("jobType").toString();
        String actionStr = properties.get("jobAction").toString();
        Object nlsEnv = properties.get("nlsEnv");
        if (nlsEnv != null) {
            actionList.add(modifyNlsEnv(nlsEnv));
        }
        String createJobSQL = String.format(
            """
                BEGIN
                  DBMS_SCHEDULER.CREATE_JOB(
                    job_name            => %s,
                    job_type            => %s,
                    job_action          => %s
                  );
                """,
            jobFullNameQuoted,
            SQLUtils.quoteString(schedulerJob, jobType),
            SQLUtils.quoteString(schedulerJob, actionStr)
        );

        Set<String> mainParams = Set.of("name", "jobType", "jobAction", "enabled", "nlsEnv");
        StringBuilder setAttrPlsql = new StringBuilder(createJobSQL);
        for (var entry : properties.entrySet()) {
            String attribute = "description".equals(entry.getKey().toString()) ? "comments" : entry.getKey().toString();
            if (mainParams.contains(attribute)) {
                continue;
            }
            setDbmsSchedulerJobAttribute(schedulerJob, jobFullNameQuoted, setAttrPlsql, entry, attribute);
        }

        enableOrDisableIfPresent(properties, setAttrPlsql, jobFullNameQuoted);

        setAttrPlsql.append("  COMMIT;\nEND;");
        String setAttrSQL = setAttrPlsql.toString();
        actionList.add(new SQLDatabasePersistAction("Create scheduler job", setAttrSQL));
        if (setAttrSQL.contains("DBMS_SCHEDULER.SET_ATTRIBUTE")) {
            actionList.add(new SQLDatabasePersistAction("Set Additional Scheduler Job Attributes", setAttrSQL));
        }
    }

    private void setDbmsSchedulerJobAttribute(
        OracleSchedulerJob schedulerJob,
        String jobFullNameQuoted,
        StringBuilder setAttrPlsql,
        Map.Entry<Object, Object> entry,
        String attribute
    ) {
        Object valueObj = entry.getValue();
        if (valueObj == null) {
            return;
        }
        String formattedValue = getFormattedValue(valueObj, attribute, schedulerJob);
        String attributeUnderScore = SQLUtils.quoteString(schedulerJob, camelCaseToUnderScore(attribute));
        setAttrPlsql.append(String.format(
            """
                DBMS_SCHEDULER.SET_ATTRIBUTE(
                  name      => %s,
                  attribute => %s,
                  value     => %s
                );
                """,
            jobFullNameQuoted,
            attributeUnderScore,
            formattedValue
        ));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull SQLObjectEditor<OracleSchedulerJob, OracleSchema>.ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        Map<Object, Object> properties = command.getProperties();
        OracleSchedulerJob schedulerJob = command.getObject();
        String owner = schedulerJob.getParentObject().getName();
        Object nlsEnv = properties.get("nlsEnv");
        if (nlsEnv != null) {
            actionList.add(modifyNlsEnv(nlsEnv));
        }
        String jobFullNameQuoted = getJobFullNameQuoted(schedulerJob, owner);
        StringBuilder plsql = new StringBuilder("BEGIN\n");

        for (var entry : properties.entrySet()) {
            String attribute = "description".equals(entry.getKey().toString()) ? "comments" : entry.getKey().toString();
            if (attribute.equals("enabled")) {
                continue;
            }
            setDbmsSchedulerJobAttribute(schedulerJob, jobFullNameQuoted, plsql, entry, attribute);
        }
        enableOrDisableIfPresent(properties, plsql, jobFullNameQuoted);
        plsql.append("  COMMIT;\nEND;");
        String result = plsql.toString();
        actionList.add(new SQLDatabasePersistAction("Update Scheduler Job", result));
    }

    private String getFormattedValue(
        Object valueObj,
        String attribute,
        OracleSchedulerJob schedulerJob
    ) {
        String formattedValue = "";
        if (valueObj instanceof Number numberVal) {
            formattedValue = String.valueOf(numberVal);
        } else if (attribute.toLowerCase(Locale.ROOT).contains("date")) {
            formattedValue = formatTimestamp(schedulerJob, valueObj.toString());
        } else if (valueObj instanceof String stringVal) {
            if ("instanceStickiness".equals(attribute)
                || "loggingLevel".equals(attribute)
                || "autoDrop".equals(attribute)
                || "restartable".equals(attribute)
                || "instanceId".equals(attribute)) {
                formattedValue = stringVal;
            } else {
                formattedValue = SQLUtils.quoteString(schedulerJob, stringVal);
            }
        } else {
            formattedValue = valueObj.toString();
        }

        return formattedValue;
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull SQLObjectEditor<OracleSchedulerJob, OracleSchema>.ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        OracleSchedulerJob schedulerJob = command.getObject();
        String owner = schedulerJob.getParentObject().getName();
        String jobFullNameQuoted = getJobFullNameQuoted(schedulerJob, owner);
        String sql = String.format(
            """
                BEGIN
                  DBMS_SCHEDULER.DROP_JOB(%s);
                END;
                """, jobFullNameQuoted
        );
        actions.add(new SQLDatabasePersistAction("Delete Scheduled Job", sql)); //$NON-NLS-1$
    }

    private String getJobFullNameQuoted(OracleSchedulerJob schedulerJob, String owner) {
        return SQLUtils.quoteString(schedulerJob, owner + "." + schedulerJob.getName());
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleSchedulerJob> getObjectsCache(OracleSchedulerJob object) {
        return object.getSchema().schedulerJobCache;
    }

    private String formatTimestamp(OracleSchedulerJob oracleJob, String timestamp) {
        return timestamp.matches(TIMESTAMP_FORMAT)
            ? "TO_TIMESTAMP_TZ(%s, 'yyyy-mm-dd hh24:mi:ss.ff tzr')".formatted(SQLUtils.quoteString(oracleJob, timestamp))
            : timestamp;
    }

    private void enableOrDisableIfPresent(
        Map<Object, Object> properties,
        StringBuilder setAttrPlsql,
        String jobFullNameQuoted
    ) {
        Object enabled = properties.get("enabled");
        if (enabled != null) {
            String enabledVal = enabled.toString();
            if (enabledVal.equalsIgnoreCase("true")) {
                setAttrPlsql.append("  DBMS_SCHEDULER.ENABLE(")
                    .append(jobFullNameQuoted)
                    .append(");\n");
            } else {
                setAttrPlsql.append("  DBMS_SCHEDULER.DISABLE(")
                    .append(jobFullNameQuoted)
                    .append(");\n");
            }
        }
    }

    private SQLDatabasePersistAction modifyNlsEnv(@NotNull Object nlsEnv) {
        String sql = String.format("ALTER SESSION SET %s", nlsEnv);
        return new SQLDatabasePersistAction("Edit NLS ENV", sql);
    }

    private static String camelCaseToUnderScore(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
