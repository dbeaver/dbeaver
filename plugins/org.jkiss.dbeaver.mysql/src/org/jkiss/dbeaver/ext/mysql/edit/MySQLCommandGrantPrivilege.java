/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends DBECommandAbstract<MySQLUser> {

    private boolean grant;
    private MySQLCatalog schema;
    private MySQLTable table;
    private MySQLPrivilege privilege;

    public MySQLCommandGrantPrivilege(MySQLUser user, boolean grant, MySQLCatalog schema, MySQLTable table, MySQLPrivilege privilege)
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
    public IDatabasePersistAction[] getPersistActions()
    {
        String privName = privilege.getName();
        String grantScript = "GRANT " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " TO " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        String revokeScript = "REVOKE " + privName + //$NON-NLS-1$
            " ON " + getObjectName() + //$NON-NLS-1$
            " FROM " + getObject().getFullName() + ""; //$NON-NLS-1$ //$NON-NLS-2$
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
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
