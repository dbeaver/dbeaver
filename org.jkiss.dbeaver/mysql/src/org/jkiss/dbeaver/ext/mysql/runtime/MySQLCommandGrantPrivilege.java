/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabaseObjectCommand;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandGrantPrivilege extends AbstractDatabaseObjectCommand<MySQLUser> {

    private boolean grant;
    private MySQLUser user;
    private String schema;
    private String privilege;

    public MySQLCommandGrantPrivilege(boolean grant, MySQLUser user, String schema, String privilege)
    {
        super(grant ? "Grant privilege" : "Revoke privilege");
        this.grant = grant;
        this.user = user;
        this.schema = schema;
        this.privilege = privilege;
    }

    public void updateModel(MySQLUser object, boolean undo)
    {
        //this.user.updatePrivilege(schema, privilege, undo ? !grant : grant);
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        String privName = privilege.replace("_", " ");
        String grantScript = "GRANT " + privName +
            " ON " + (CommonUtils.isEmpty(schema) ? "*" : schema) + ".*" +
            " TO " + user.getFullName() + "";
        String revokeScript = "REVOKE " + privName +
            " ON " + (CommonUtils.isEmpty(schema) ? "*" : schema) + ".*" +
            " FROM " + user.getFullName() + "";
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Grant privilege",
                grant ? grantScript : revokeScript,
                grant ? revokeScript : grantScript)
        };
    }

}
