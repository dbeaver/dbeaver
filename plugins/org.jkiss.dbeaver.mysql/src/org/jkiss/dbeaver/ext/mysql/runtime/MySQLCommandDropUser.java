/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandDropUser extends DBECommandComposite<MySQLUser, UserPropertyHandler> {

    protected MySQLCommandDropUser(MySQLUser user)
    {
        super(user, "Drop user");
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop user", "DROP USER " + getObject().getFullName()) {
                @Override
                public void handleExecute(Throwable error)
                {
                    if (error == null) {
                        getObject().setPersisted(false);
                    }
                }
            }};
    }

}