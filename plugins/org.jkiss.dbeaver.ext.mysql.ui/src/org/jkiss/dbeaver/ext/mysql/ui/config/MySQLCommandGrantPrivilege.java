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
    // Commands on the same object merge into one GRANT/REVOKE statement with several privileges
    private final List<MySQLPrivilege> privileges = new ArrayList<>();

    public MySQLCommandGrantPrivilege(MySQLUser user, boolean grant, boolean withGrantOption, MySQLCatalog schema, MySQLTableBase table, MySQLPrivilege privilege)
    {
        super(user, grant ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.table = table;
        this.privileges.add(privilege);
    }

    public MySQLCommandGrantPrivilege(MySQLUser user, boolean grant, boolean withGrantOption, MySQLCatalog schema, MySQLProcedure procedure, MySQLPrivilege privilege)
    {
        super(user, grant ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.procedure = procedure;
        this.privileges.add(privilege);
    }

    public MySQLCommandGrantPrivilege(MySQLUser user, boolean grant, boolean withGrantOption, MySQLCatalog schema, MySQLTableBase table, List<MySQLTableColumn> columns, MySQLPrivilege privilege)
    {
        super(user, grant ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.table = table;
        this.columns = columns;
        this.privileges.add(privilege);
    }

    @Override
    public void updateModel()
    {
        getObject().clearGrantsCache();
    }

    @Nullable
    @Override
    public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options)
    {
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
    public DBECommand<?> merge(@Nullable DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams)
    {
        // Consolidation of pending commands happens in the editor at toggle time
        // (see MySQLUserEditorPrivileges); here only exact duplicates are merged.
        // The merge must stay free of side effects: the command queue may be rebuilt many times.
        if (prevCommand instanceof MySQLCommandGrantPrivilege prevGrant
            && prevGrant.schema == schema && prevGrant.table == table && prevGrant.procedure == procedure
            && CommonUtils.equalObjects(prevGrant.columns, columns)
            && prevGrant.grant == grant
            && prevGrant.withGrantOption == withGrantOption
            && prevGrant.privileges.equals(privileges)
        ) {
            return prevCommand;
        }
        return super.merge(prevCommand, userParams);
    }

    public boolean isGrant()
    {
        return grant;
    }

    public boolean isWithGrantOption()
    {
        return withGrantOption;
    }

    public void setWithGrantOption(boolean withGrantOption)
    {
        this.withGrantOption = withGrantOption;
    }

    public boolean hasSameTarget(MySQLCatalog schema, MySQLTableBase table, MySQLProcedure procedure, List<MySQLTableColumn> columns)
    {
        return this.schema == schema && this.table == table && this.procedure == procedure
            && CommonUtils.equalObjects(this.columns, columns);
    }

    public void addPrivilege(MySQLPrivilege privilege)
    {
        if (!privileges.contains(privilege)) {
            privileges.add(privilege);
        }
    }

    public boolean removePrivilege(MySQLPrivilege privilege)
    {
        return privileges.remove(privilege);
    }

    public boolean isEmptyCommand()
    {
        return privileges.isEmpty() && !withGrantOption;
    }

    private String getObjectName()
    {
        if (procedure != null) {
            return procedure.getProcedureType().name() + " " + //$NON-NLS-1$
                (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." + //$NON-NLS-1$ //$NON-NLS-2$
                DBUtils.getQuotedIdentifier(procedure);
        }
        return
            (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." + //$NON-NLS-1$ //$NON-NLS-2$
            (table == null ? "*" : DBUtils.getQuotedIdentifier(table)); //$NON-NLS-1$
    }

}
