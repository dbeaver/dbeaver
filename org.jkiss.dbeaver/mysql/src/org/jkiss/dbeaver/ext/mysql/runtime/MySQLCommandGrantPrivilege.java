/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabaseObjectCommand;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends AbstractDatabaseObjectCommand<MySQLUser> {

    private boolean grant;
    private MySQLUser user;
    private MySQLCatalog schema;
    private MySQLTable table;
    private MySQLPrivilege privilege;

    public MySQLCommandGrantPrivilege(boolean grant, MySQLUser user, MySQLCatalog schema, MySQLTable table, MySQLPrivilege privilege)
    {
        super(grant ? "Grant privilege" : "Revoke privilege");
        this.grant = grant;
        this.user = user;
        this.schema = schema;
        this.table = table;
        this.privilege = privilege;
    }

    public void updateModel(MySQLUser object)
    {
        this.user.clearGrantsCache();
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        String privName = privilege.getName();
        String grantScript = "GRANT " + privName +
            " ON " + getObjectName() +
            " TO " + user.getFullName() + "";
        String revokeScript = "REVOKE " + privName +
            " ON " + getObjectName() +
            " FROM " + user.getFullName() + "";
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Grant privilege",
                grant ? grantScript : revokeScript)
        };
    }

    private String getObjectName()
    {
        return
            (schema == null ? "*" : DBUtils.getQuotedIdentifier(schema)) + "." +
            (table == null ? "*" : DBUtils.getQuotedIdentifier(table));
    }

}
