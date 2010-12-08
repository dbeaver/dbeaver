/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
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
        for (Map.Entry<UserPropertyHandler, Object> entry : userProps.entrySet()) {
            switch (entry.getKey()) {
                case MAX_QUERIES: object.setMaxQuestions(CommonUtils.toInt(entry.getValue())); break;
                case MAX_UPDATES: object.setMaxUpdates(CommonUtils.toInt(entry.getValue())); break;
                case MAX_CONNECTIONS: object.setMaxConnections(CommonUtils.toInt(entry.getValue())); break;
                case MAX_USER_CONNECTIONS: object.setMaxUserConnections(CommonUtils.toInt(entry.getValue())); break;
                default:
                    break;
            }
        }
    }

    @Override
    public void validateCommand(MySQLUser object) throws DBException
    {
        String passValue = CommonUtils.toString(userProps.get(UserPropertyHandler.PASSWORD));
        String confirmValue = CommonUtils.toString(userProps.get(UserPropertyHandler.PASSWORD_CONFIRM));
        if (!CommonUtils.isEmpty(passValue) && !CommonUtils.equalObjects(passValue, confirmValue)) {
            throw new DBException("Password confirmation value is invalid");
        }
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