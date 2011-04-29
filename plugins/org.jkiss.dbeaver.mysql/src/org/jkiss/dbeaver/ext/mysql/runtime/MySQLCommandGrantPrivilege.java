/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
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
        super(user, grant ? "Grant privilege" : "Revoke privilege");
        this.grant = grant;
        this.schema = schema;
        this.table = table;
        this.privilege = privilege;
    }

    public void updateModel()
    {
        getObject().clearGrantsCache();
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        String privName = privilege.getName();
        String grantScript = "GRANT " + privName +
            " ON " + getObjectName() +
            " TO " + getObject().getFullName() + "";
        String revokeScript = "REVOKE " + privName +
            " ON " + getObjectName() +
            " FROM " + getObject().getFullName() + "";
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Grant privilege",
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
            (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." +
            (table == null ? "*" : DBUtils.getQuotedIdentifier(table));
    }

}
