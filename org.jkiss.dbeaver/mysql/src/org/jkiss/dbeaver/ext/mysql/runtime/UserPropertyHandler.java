/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyHandler;

/**
* User property handler
*/
public enum UserPropertyHandler implements DatabaseObjectPropertyHandler<MySQLUser> {
    PASSWORD {public void modify(MySQLUser object, Object value)
        {
            // do not change anything for password
        }},
    PASSWORD_CONFIRM {public void modify(MySQLUser object, Object value)
        {
            // do not change anything for password
        }},
    MAX_QUERIES{public void modify(MySQLUser object, Object value)
        {
            object.setMaxQuestions(CommonUtils.toInt(value));
        }},
    MAX_UPDATES{public void modify(MySQLUser object, Object value)
        {
            object.setMaxUpdates(CommonUtils.toInt(value));
        }},
    MAX_CONNECTIONS{public void modify(MySQLUser object, Object value)
        {
            object.setMaxConnections(CommonUtils.toInt(value));
        }},
    MAX_USER_CONNECTIONS{public void modify(MySQLUser object, Object value)
        {
            object.setMaxUserConnections(CommonUtils.toInt(value));
        }}

}
