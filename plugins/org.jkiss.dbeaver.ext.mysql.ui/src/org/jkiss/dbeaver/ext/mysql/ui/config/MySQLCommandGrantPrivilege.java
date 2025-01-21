/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Locale;
import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends DBECommandAbstract<MySQLUser> {

    private boolean grant;
    private boolean withGrantOption;
    private MySQLCatalog schema;
    private MySQLTableBase table;
    private MySQLPrivilege privilege;

    public MySQLCommandGrantPrivilege(MySQLUser user, boolean grant, boolean withGrantOption, MySQLCatalog schema, MySQLTableBase table, MySQLPrivilege privilege)
    {
        super(user, grant ? MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege : MySQLUIMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.withGrantOption = withGrantOption;
        this.schema = schema;
        this.table = table;
        this.privilege = privilege;
    }

    @Override
    public void updateModel()
    {
        getObject().clearGrantsCache();
    }

    @NotNull
    @Override
    public DBEPersistAction[] getPersistActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull Map<String, Object> options)
    {
        String privName = privilege.getFixedPrivilegeName().toUpperCase(Locale.ROOT);
        String grantScript = "GRANT " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " TO " + getObject().getFullName() + (withGrantOption ? " WITH GRANT OPTION" : ""); //$NON-NLS-1$ //$NON-NLS-2$
        String revokeScript = "REVOKE " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " FROM " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                MySQLUIMessages.edit_command_grant_privilege_action_grant_privilege,
                grant ? grantScript : revokeScript)
        };
    }

    @NotNull
    @Override
    public DBECommand<?> merge(@NotNull DBECommand<?> prevCommand, @NotNull Map<Object, Object> userParams)
    {
        if (prevCommand instanceof MySQLCommandGrantPrivilege) {
            MySQLCommandGrantPrivilege prevGrant = (MySQLCommandGrantPrivilege) prevCommand;
            if (prevGrant.schema == schema && prevGrant.table == table) {
                if (prevGrant.privilege == privilege) {
                    if (prevGrant.grant == grant) {
                        return prevCommand;
                    } else {
                        return null;
                    }
                } else if (MySQLConstants.PRIVILEGE_GRANT_OPTION_NAME.equals(privilege.getName()) && withGrantOption) {
                    // To add WITH GRANT OPTION part to the GRANT statement
                    // Just one such addition option is enough to expand this option on the entire table
                    prevGrant.withGrantOption = true;
                    return prevCommand;
                }
            }
        }
        return super.merge(prevCommand, userParams);
    }

    private String getObjectName()
    {
        return
            (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." + //$NON-NLS-1$ //$NON-NLS-2$
            (table == null ? "*" : DBUtils.getQuotedIdentifier(table)); //$NON-NLS-1$
    }

}
