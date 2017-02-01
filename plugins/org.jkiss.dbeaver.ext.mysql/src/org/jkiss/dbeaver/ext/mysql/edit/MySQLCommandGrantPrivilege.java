/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends DBECommandAbstract<MySQLUser> {

    private boolean grant;
    private MySQLCatalog schema;
    private MySQLTableBase table;
    private MySQLPrivilege privilege;

    public MySQLCommandGrantPrivilege(MySQLUser user, boolean grant, MySQLCatalog schema, MySQLTableBase table, MySQLPrivilege privilege)
    {
        super(user, grant ? MySQLMessages.edit_command_grant_privilege_action_grant_privilege : MySQLMessages.edit_command_grant_privilege_name_revoke_privilege);
        this.grant = grant;
        this.schema = schema;
        this.table = table;
        this.privilege = privilege;
    }

    @Override
    public void updateModel()
    {
        getObject().clearGrantsCache();
    }

    @Override
    public DBEPersistAction[] getPersistActions()
    {
        String privName = privilege.getName();
        String grantScript = "GRANT " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " TO " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        String revokeScript = "REVOKE " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " FROM " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        return new DBEPersistAction[] {
            new SQLDatabasePersistAction(
                MySQLMessages.edit_command_grant_privilege_action_grant_privilege,
                grant ? grantScript : revokeScript)
        };
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        if (prevCommand instanceof MySQLCommandGrantPrivilege) {
            MySQLCommandGrantPrivilege prevGrant = (MySQLCommandGrantPrivilege)prevCommand;
            if (prevGrant.schema == schema && prevGrant.table == table && prevGrant.privilege == privilege) {
                if (prevGrant.grant == grant) {
                    return prevCommand;
                } else {
                    return null;
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
