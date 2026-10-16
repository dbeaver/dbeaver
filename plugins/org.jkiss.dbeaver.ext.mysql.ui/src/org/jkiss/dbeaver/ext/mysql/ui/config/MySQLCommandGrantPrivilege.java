/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends DBECommandAbstract<MySQLUser> {

    private boolean grant;
    private boolean withGrantOption;
    private MySQLCatalog schema;
    private MySQLTableBase table;
    private MySQLProcedure procedure;
    private List<MySQLTableColumn> columns;
    // Stored schema LIKE pattern (e.g. tenant\_%) of a wildcard schema grant shown under a concrete schema:
    // the statement must address that stored grant row, not the concrete schema
    @Nullable
    private String schemaPattern;
    // Commands on the same object merge into one GRANT/REVOKE statement with several privileges
    private final List<MySQLPrivilege> privileges = new ArrayList<>();

    public MySQLCommandGrantPrivilege(
        @NotNull MySQLUser user,
        boolean grant,
        boolean withGrantOption,
        @Nullable MySQLCatalog schema,
        @Nullable MySQLTableBase table,
        @NotNull MySQLPrivilege privilege
    ) {
        super(user, grant
            ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege
            : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.table = table;
        addPrivilege(privilege);
    }

    /**
     * Grant/revoke a user-level (global or schema-wide) privilege with no specific table or procedure.
     */
    public MySQLCommandGrantPrivilege(
        @NotNull MySQLUser user,
        boolean grant,
        boolean withGrantOption,
        @Nullable MySQLCatalog schema,
        @NotNull MySQLPrivilege privilege
    ) {
        this(user, grant, withGrantOption, schema, (MySQLTableBase) null, privilege);
    }

    public MySQLCommandGrantPrivilege(
        @NotNull MySQLUser user,
        boolean grant,
        boolean withGrantOption,
        @Nullable MySQLCatalog schema,
        @Nullable MySQLProcedure procedure,
        @NotNull MySQLPrivilege privilege
    ) {
        super(user, grant
            ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege
            : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.procedure = procedure;
        addPrivilege(privilege);
    }

    public MySQLCommandGrantPrivilege(
        @NotNull MySQLUser user,
        boolean grant,
        boolean withGrantOption,
        @Nullable MySQLCatalog schema,
        @Nullable MySQLTableBase table,
        @Nullable List<MySQLTableColumn> columns,
        @NotNull MySQLPrivilege privilege
    ) {
        super(user, grant
            ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege
            : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.table = table;
        this.columns = columns;
        addPrivilege(privilege);
    }

    @Override
    public void updateModel() {
        getObject().clearGrantsCache();
    }

    @Nullable
    @Override
    public DBEPersistAction[] getPersistActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Map<String, Object> options
    ) {
        StringJoiner privList = new StringJoiner(", "); //$NON-NLS-1$
        for (MySQLPrivilege privilege : privileges) {
            if (grant && privilege.isGrantOption()) {
                // Rendered as the WITH GRANT OPTION suffix in GRANT statements
                continue;
            }
            String privName = privilege.getFixedPrivilegeName().toUpperCase(Locale.ROOT);
            if (!CommonUtils.isEmpty(columns) && !privilege.isGrantOption()) {
                StringJoiner columnList = new StringJoiner(", ", " (", ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                for (MySQLTableColumn column : columns) {
                    columnList.add(DBUtils.getQuotedIdentifier(column));
                }
                privName += columnList.toString();
            }
            privList.add(privName);
        }
        String privString = privList.length() > 0 ? privList.toString()
            : "USAGE"; // grant option alone: GRANT USAGE ... WITH GRANT OPTION //$NON-NLS-1$
        String grantScript = "GRANT " + privString + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " TO " + getObject().getFullName() + (withGrantOption ? " WITH GRANT OPTION" : ""); //$NON-NLS-1$ //$NON-NLS-2$
        String revokeScript = "REVOKE " + privString + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " FROM " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege,
                grant ? grantScript : revokeScript)
        };
    }

    @Nullable
    @Override
    public DBECommand<?> merge(@Nullable DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams) {
        // Consolidation of pending commands happens in the Privileges page at toggle time
        // (see MySQLUserEditorPrivileges); here only exact duplicates are merged, and an opposite
        // command on the same privileges cancels out - the General page relies on that, it adds
        // a command per click. The merge must stay free of side effects: the queue may be rebuilt many times.
        if (prevCommand instanceof MySQLCommandGrantPrivilege prevGrant
            && prevGrant.hasSameTarget(schema, schemaPattern, table, procedure, columns)
            && new HashSet<>(prevGrant.privileges).equals(new HashSet<>(privileges))
        ) {
            if (prevGrant.grant != grant) {
                return null;
            }
            if (prevGrant.withGrantOption == withGrantOption) {
                return prevCommand;
            }
        }
        return super.merge(prevCommand, userParams);
    }

    public boolean isGrant() {
        return grant;
    }

    public boolean isWithGrantOption() {
        return withGrantOption;
    }

    public boolean hasSameTarget(
        @Nullable MySQLCatalog schema,
        @Nullable String schemaPattern,
        @Nullable MySQLTableBase table,
        @Nullable MySQLProcedure procedure,
        @Nullable List<MySQLTableColumn> columns
    ) {
        return this.schema == schema && CommonUtils.equalObjects(this.schemaPattern, schemaPattern)
            && this.table == table && this.procedure == procedure
            && CommonUtils.equalObjects(this.columns, columns);
    }

    /**
     * Makes the statement address a wildcard schema grant by its stored pattern instead of the concrete schema.
     */
    public void setSchemaPattern(@Nullable String schemaPattern) {
        this.schemaPattern = schemaPattern;
    }

    /**
     * Adds a privilege to this statement. For a GRANT the grant-option pseudo-privilege also turns
     * on the {@code WITH GRANT OPTION} suffix, so both always change together.
     */
    public void addPrivilege(@NotNull MySQLPrivilege privilege) {
        if (!privileges.contains(privilege)) {
            privileges.add(privilege);
        }
        if (grant && privilege.isGrantOption()) {
            withGrantOption = true;
        }
    }

    /**
     * Removes a privilege from this statement. For a GRANT removing the grant-option
     * pseudo-privilege also drops the {@code WITH GRANT OPTION} suffix.
     */
    public boolean removePrivilege(@NotNull MySQLPrivilege privilege) {
        boolean removed = privileges.remove(privilege);
        if (grant && privilege.isGrantOption() && withGrantOption) {
            withGrantOption = false;
            removed = true;
        }
        return removed;
    }

    @NotNull
    public List<MySQLPrivilege> getPrivileges() {
        return List.copyOf(privileges);
    }

    @Nullable
    public MySQLCatalog getSchema() {
        return schema;
    }

    @Nullable
    public MySQLTableBase getTable() {
        return table;
    }

    @Nullable
    public MySQLProcedure getProcedure() {
        return procedure;
    }

    @Nullable
    public List<MySQLTableColumn> getColumns() {
        return columns;
    }

    public boolean isEmptyCommand() {
        return privileges.isEmpty() && !withGrantOption;
    }

    private @NotNull String getObjectName() {
        if (procedure != null) {
            return procedure.getProcedureType().name() + " " + //$NON-NLS-1$
                (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." + //$NON-NLS-1$ //$NON-NLS-2$
                DBUtils.getQuotedIdentifier(procedure);
        }
        if (schemaPattern != null) {
            return "`" + schemaPattern.replace("`", "``") + "`." + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                (table == null ? "*" : DBUtils.getQuotedIdentifier(table)); //$NON-NLS-1$
        }
        return
            (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." + //$NON-NLS-1$ //$NON-NLS-2$
            (table == null ? "*" : DBUtils.getQuotedIdentifier(table)); //$NON-NLS-1$
    }

}
