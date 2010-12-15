/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyHandler;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyReflector;

/**
* User property handler
*/
public enum UserPropertyHandler implements DatabaseObjectPropertyHandler<MySQLUser>, DatabaseObjectPropertyReflector<MySQLUser> {
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

    public void reflectValueChange(MySQLUser object, Object oldValue, Object newValue)
    {
        if (this == NAME || this == HOST) {
            if (this == NAME) {
                object.setUserName(CommonUtils.toString(newValue));
            } else {
                object.setHost(CommonUtils.toString(newValue));
            }
            object.getDataSource().getContainer().fireEvent(
                new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
        }
    }
}
