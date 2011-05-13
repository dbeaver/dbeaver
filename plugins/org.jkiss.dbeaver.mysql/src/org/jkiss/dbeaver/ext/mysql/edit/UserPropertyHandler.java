/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;

/**
* User property handler
*/
public enum UserPropertyHandler implements DBEPropertyHandler<MySQLUser>, DBEPropertyReflector<MySQLUser> {
    NAME,
    HOST,
    PASSWORD,
    PASSWORD_CONFIRM,
    MAX_QUERIES,
    MAX_UPDATES,
    MAX_CONNECTIONS,
    MAX_USER_CONNECTIONS;


    public Object getId()
    {
        return name();
    }

    public MySQLCommandChangeUser createCompositeCommand(MySQLUser object)
    {
        return new MySQLCommandChangeUser(object);
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
