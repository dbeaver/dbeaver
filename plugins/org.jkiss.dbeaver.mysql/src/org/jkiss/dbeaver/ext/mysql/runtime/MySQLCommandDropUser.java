/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.prop.DBOCommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandDropUser extends DBOCommandComposite<MySQLUser, UserPropertyHandler> {

    protected MySQLCommandDropUser()
    {
        super("Drop user");
    }

    public IDatabasePersistAction[] getPersistActions(final MySQLUser object)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction("Drop user", "DROP USER " + object.getFullName()) {
                @Override
                public void handleExecute(Throwable error)
                {
                    if (error == null) {
                        object.setPersisted(false);
                    }
                }
            }};
    }

}