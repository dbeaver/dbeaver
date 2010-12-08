/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabaseObjectCommand;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyCommand;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandChangeUser extends AbstractDatabaseObjectCommand<MySQLUser> {

    private Map<UserPropertyHandler, Object> userProps;

    protected MySQLCommandChangeUser(Map<UserPropertyHandler, Object> userProps)
    {
        super("Update user");
        this.userProps = userProps;
    }

    public void updateModel(MySQLUser object)
    {

    }

    public IDatabasePersistAction[] getPersistActions(MySQLUser object)
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Update user record",
                "BLAH-BLAH")
        };
    }
}