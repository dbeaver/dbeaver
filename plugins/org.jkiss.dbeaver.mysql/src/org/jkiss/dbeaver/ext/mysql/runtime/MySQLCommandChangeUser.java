/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.edit.prop.DBOCommandComposite;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandChangeUser extends DBOCommandComposite<MySQLUser, UserPropertyHandler> {

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

    public IDatabasePersistAction[] getPersistActions(final MySQLUser object)
    {
        List<IDatabasePersistAction> actions = new ArrayList<IDatabasePersistAction>();
        boolean newUser = !object.isPersisted();
        if (newUser) {
            actions.add(
                new AbstractDatabasePersistAction("Create new user", "CREATE USER " + object.getFullName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            object.setPersisted(true);
                        }
                    }
                });
        }
        StringBuilder script = new StringBuilder();
        script.append("UPDATE mysql.user SET ");
        boolean hasSet = false;
        for (Map.Entry<UserPropertyHandler, Object> entry : getProperties().entrySet()) {
            if (entry.getKey() == UserPropertyHandler.PASSWORD_CONFIRM) {
                continue;
            }
            String delim = hasSet ? "," : "";
            switch (entry.getKey()) {
                case PASSWORD: script.append(delim).append("Password=PASSWORD('").append(SQLUtils.escapeString(CommonUtils.toString(entry.getValue()))).append("')"); hasSet = true; break;
                case MAX_QUERIES: script.append(delim).append("Max_Questions=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break;
                case MAX_UPDATES: script.append(delim).append("Max_Updates=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break;
                case MAX_CONNECTIONS: script.append(delim).append("Max_Connections=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break;
                case MAX_USER_CONNECTIONS: script.append(delim).append("Max_User_Connections=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break;
                default: break;
            }
        }
        script.append(" WHERE User='").append(object.getUserName()).append("' AND Host='").append(object.getHost()).append("'");
        if (hasSet) {
            actions.add(new AbstractDatabasePersistAction("Update user record", script.toString()));
        }
        return actions.toArray(new IDatabasePersistAction[actions.size()]);
    }
}