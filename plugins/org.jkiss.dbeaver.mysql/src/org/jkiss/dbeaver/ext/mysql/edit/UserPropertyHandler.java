/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;
import org.jkiss.utils.CommonUtils;

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


    @Override
    public Object getId()
    {
        return name();
    }

    @Override
    public MySQLCommandChangeUser createCompositeCommand(MySQLUser object)
    {
        return new MySQLCommandChangeUser(object);
    }

    @Override
    public void reflectValueChange(MySQLUser object, Object oldValue, Object newValue)
    {
        if (this == NAME || this == HOST) {
            if (this == NAME) {
                object.setUserName(CommonUtils.toString(newValue));
            } else {
                object.setHost(CommonUtils.toString(newValue));
            }
            DBUtils.fireObjectUpdate(object);
        }
    }
}
