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
        this.user.updatePrivilege(schema, privilege, undo ? !grant : grant);
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        String grantScript = "GRANT" + privilege +
            " ON " + (CommonUtils.isEmpty(schema) ? "*.*" : schema) +
            " TO '" + user.getName() + "'";
        String revokeScript = "REVOKE" + privilege +
            " ON " + (CommonUtils.isEmpty(schema) ? "*.*" : schema) +
            " FROM '" + user.getName() + "'";
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Grant privilege",
                grant ? grantScript : revokeScript,
                grant ? revokeScript : grantScript)
        };
    }

}
