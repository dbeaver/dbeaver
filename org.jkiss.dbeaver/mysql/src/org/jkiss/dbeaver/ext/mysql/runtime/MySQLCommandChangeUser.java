/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.ControlDatabaseObjectCommand;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandChangeUser extends ControlDatabaseObjectCommand<MySQLUser> {

    public enum UserProperty {
        PASSWORD,
        MAX_QUERIES,
        MAX_UPDATES,
        MAX_CONNECTIONS,
        MAX_USER_CONNECTIONS
    }

    private UserProperty property;

    public MySQLCommandChangeUser(UserProperty property)
    {
        super("Change " + property);
        this.property = property;
    }

    public void updateModel(MySQLUser object)
    {

    }

    public IDatabasePersistAction[] getPersistActions()
    {
        return new IDatabasePersistAction[0];
    }
}