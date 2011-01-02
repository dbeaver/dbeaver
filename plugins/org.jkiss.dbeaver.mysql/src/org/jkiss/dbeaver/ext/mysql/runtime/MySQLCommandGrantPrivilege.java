/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.model.edit.DBOCommand;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.DBOCommandImpl;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends DBOCommandImpl<MySQLUser> {

    private boolean grant;
    private MySQLCatalog schema;
    private MySQLTable table;
    private MySQLPrivilege privilege;

    public MySQLCommandGrantPrivilege(boolean grant, MySQLCatalog schema, MySQLTable table, MySQLPrivilege privilege)
    {
        super(grant ? "Grant privilege" : "Revoke privilege");
        this.grant = grant;
        this.schema = schema;
        this.table = table;
        this.privilege = privilege;
    }

    public void updateModel(MySQLUser object)
    {
        object.clearGrantsCache();
    }

    public IDatabasePersistAction[] getPersistActions(MySQLUser object)
    {
        String privName = privilege.getName();
        String grantScript = "GRANT " + privName +
            " ON " + getObjectName() +
            " TO " + object.getFullName() + "";
        String revokeScript = "REVOKE " + privName +
            " ON " + getObjectName() +
            " FROM " + object.getFullName() + "";
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Grant privilege",
                grant ? grantScript : revokeScript)
        };
    }

    @Override
    public DBOCommand<MySQLUser> merge(DBOCommand<MySQLUser> prevCommand, Map<String, Object> userParams)
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
