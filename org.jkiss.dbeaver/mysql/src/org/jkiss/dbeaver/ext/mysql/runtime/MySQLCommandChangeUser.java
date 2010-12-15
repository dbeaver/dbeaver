/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectCompositeCommand;

import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandChangeUser extends DatabaseObjectCompositeCommand<MySQLUser, UserPropertyHandler> {

    protected MySQLCommandChangeUser()
    {
        super("Update user");
    }

    public void updateModel(MySQLUser object)
    {
        for (Map.Entry<UserPropertyHandler, Object> entry : getProperties().entrySet()) {
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
        String passValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD));
        String confirmValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD_CONFIRM));
        if (!CommonUtils.isEmpty(passValue) && !CommonUtils.equalObjects(passValue, confirmValue)) {
            throw new DBException("Password confirmation value is invalid");
        }
    }

    public IDatabasePersistAction[] getPersistActions(MySQLUser object)
    {
        StringBuilder script = new StringBuilder();
        if (object.isPersisted()) {
            script.append("UPDATE mysql.user SET ");
            boolean hasSet = false;
            for (Map.Entry<UserPropertyHandler, Object> entry : getProperties().entrySet()) {
                if (entry.getKey() == UserPropertyHandler.PASSWORD_CONFIRM) {
                    continue;
                }
                if (hasSet) {
                    script.append(",");
                }
                switch (entry.getKey()) {
                    case PASSWORD: script.append("Password=PASSWORD('").append(SQLUtils.escapeString(CommonUtils.toString(entry.getValue()))).append("')"); break;
                    case MAX_QUERIES: script.append("Max_Questions=").append(CommonUtils.toInt(entry.getValue())); break;
                    case MAX_UPDATES: script.append("Max_Updates=").append(CommonUtils.toInt(entry.getValue())); break;
                    case MAX_CONNECTIONS: script.append("Max_Connections=").append(CommonUtils.toInt(entry.getValue())); break;
                    case MAX_USER_CONNECTIONS: script.append("Max_User_Connections=").append(CommonUtils.toInt(entry.getValue())); break;
                    default: script.append(entry.getKey()).append("='").append(SQLUtils.escapeString(CommonUtils.toString(entry.getValue()))).append("'"); break;
                }
                hasSet = true;
            }
            script.append(" WHERE User='").append(object.getUserName()).append("' AND Host='").append(object.getHost()).append("'");
        } else {
            script.append("CREATE USER '").append(object.getUserName()).append("' IDENTIFIED BY PASSWORD('").append("')");
        }

        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Update user record",
                script.toString())
        };
    }
}