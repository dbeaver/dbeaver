/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
