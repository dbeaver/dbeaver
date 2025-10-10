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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQLProcedureManager
 */
public class MySQLProcedureManager extends SQLObjectEditor<MySQLProcedure, MySQLCatalog> {

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLProcedure> getObjectsCache(MySQLProcedure object)
    {
        return object.getContainer().getProceduresCache();
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getDeclaration())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    @Override
    protected MySQLProcedure createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        return new MySQLProcedure((MySQLCatalog) container);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop procedure", "DROP " + command.getObject().getProcedureType() + " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, MySQLProcedure procedure)
    {
        if (procedure.getDataSource().isMariaDB()) {
            String txt = SQLUtils.replaceCreateToCreateOrReplace(procedure.getDeclaration());
            actions.add(new SQLDatabasePersistAction("Create procedure", txt, true));
        } else {

            String original = procedure.getDeclaration();
            String tempName = "tmp_proc_"
                + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                + "_" + UUID.randomUUID().toString().substring(0, 6);

            String newFullName = buildFullName(
                extractSchema(procedure.getFullyQualifiedName(DBPEvaluationContext.DDL)), tempName
            );

            String createTemp = withNewProcName(
                procedure.getDataSource().getSQLDialect(),
                procedure.getDeclaration(),
                newFullName
            );

            actions.add(new SQLDatabasePersistAction("Create procedure (temp)", createTemp, true));

            actions.add(
                new SQLDatabasePersistAction("Drop procedure", "DROP "
                    + procedure.getProcedureType()
                    + " IF EXISTS "
                    + procedure.getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$ //$NON-NLS-3$

            actions.add(new SQLDatabasePersistAction("Create procedure (final)", original, true));

            actions.add(new SQLDatabasePersistAction(
                "Drop temp procedure",
                "DROP " + procedure.getProcedureType() + " IF EXISTS " + newFullName
            ));
        }

    }

    private static final Pattern CREATE_PROC_HEAD = Pattern.compile(
        "^(\\s*CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:DEFINER\\s*=\\s*(?:`[^`]+`|[^\\s]+)\\s+)?PROCEDURE\\s+)"
            +
            "(?:(`[^`]+`|[\\w$]+)\\.)?"
            +
            "(`[^`]+`|[\\w$]+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final String PROCEDURE_REPLACE_COMMENT = """
        /*
          MySQL does not support `CREATE OR REPLACE PROCEDURE`,
          and DDL is non-transactional.\s
          Therefore, we first create a temporary procedure to validate\s
          the new definition before replacing the existing one.
        */
        """;

    @NotNull
    private static String withNewProcName(
        @NotNull SQLDialect dialect,
        @NotNull String ddl,
        @NotNull String newFullName
    ) {
        int start = SQLUtils.skipLeadingComments(dialect, ddl);
        Matcher m = CREATE_PROC_HEAD.matcher(ddl.substring(start));
        if (!m.find()) {
            throw new IllegalArgumentException("CREATE PROCEDURE statement not recognized");
        }

        String head = m.group(1);
        return PROCEDURE_REPLACE_COMMENT + ddl.substring(0, start) + head + newFullName + ddl.substring(start + m.end());
    }


    private static final Pattern FQN_WITH_SCHEMA = Pattern.compile(
        "^\\s*((`[^`]+`)|([\\w$]+))\\s*\\.\\s*((`[^`]+`)|([\\w$]+))\\s*$"
    );

    @Nullable
    static String extractSchema(String fullyQualifiedName) {
        Matcher m = FQN_WITH_SCHEMA.matcher(fullyQualifiedName);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    @NotNull
    static String buildFullName(@Nullable String schemaRaw, @NotNull String tempName) {
        return (schemaRaw != null && !schemaRaw.isEmpty())
            ? schemaRaw + "." + tempName
            : tempName;
    }




}

