/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectCompositeCommand;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyHandler;

/**
* User property handler
*/
public enum UserPropertyHandler implements DatabaseObjectPropertyHandler<MySQLUser> {
    NAME,
    HOST,
    PASSWORD,
    PASSWORD_CONFIRM,
    MAX_QUERIES,
    MAX_UPDATES,
    MAX_CONNECTIONS,
    MAX_USER_CONNECTIONS;


    public MySQLCommandChangeUser createCompositeCommand()
    {
        return new MySQLCommandChangeUser();
    }
}
