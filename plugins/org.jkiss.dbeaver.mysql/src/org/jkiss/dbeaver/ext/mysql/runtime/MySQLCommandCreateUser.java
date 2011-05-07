/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandCreateUser extends DBECommandAbstract<MySQLUser> {

    protected MySQLCommandCreateUser(MySQLUser user)
    {
        super(user, "Create user");
    }

}