/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandChangeUser extends DBECommandComposite<MySQLUser, UserPropertyHandler> {

    protected MySQLCommandChangeUser(MySQLUser user)
    {
        super(user, MySQLMessages.edit_command_change_user_name);
    }

    @Override
    public void updateModel()
    {
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case MAX_QUERIES: getObject().setMaxQuestions(CommonUtils.toInt(entry.getValue())); break;
                case MAX_UPDATES: getObject().setMaxUpdates(CommonUtils.toInt(entry.getValue())); break;
                case MAX_CONNECTIONS: getObject().setMaxConnections(CommonUtils.toInt(entry.getValue())); break;
                case MAX_USER_CONNECTIONS: getObject().setMaxUserConnections(CommonUtils.toInt(entry.getValue())); break;
                default:
                    break;
            }
        }
    }

    @Override
    public void validateCommand() throws DBException
    {
        String passValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD));
        String confirmValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD_CONFIRM));
        if (!CommonUtils.isEmpty(passValue) && !CommonUtils.equalObjects(passValue, confirmValue)) {
            throw new DBException("Password confirmation value is invalid");
        }
    }

    @Override
    public DBEPersistAction[] getPersistActions(Map<String, Object> options)
    {
        List<DBEPersistAction> actions = new ArrayList<>();
        boolean newUser = !getObject().isPersisted();
        if (newUser) {
            actions.add(
                new SQLDatabasePersistAction(MySQLMessages.edit_command_change_user_action_create_new_user, "CREATE USER " + getObject().getFullName()) { //$NON-NLS-2$
                    @Override
                    public void afterExecute(DBCSession session, Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(true);
                        }
                    }
                });
        }
        StringBuilder script = new StringBuilder();
        boolean hasSet;
        final MySQLDataSource dataSource = getObject().getDataSource();
        if (!dataSource.isMariaDB() && dataSource.isServerVersionAtLeast(5, 7)) {
            hasSet = generateAlterScript(script);
        } else {
            hasSet = generateUpdateScript(script);
        }
        if (hasSet) {
            actions.add(new SQLDatabasePersistAction(MySQLMessages.edit_command_change_user_action_update_user_record, script.toString()));
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    private boolean generateUpdateScript(StringBuilder script) {
        script.append("UPDATE mysql.user SET "); //$NON-NLS-1$
        boolean hasSet = false;
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            if (entry.getKey() == UserPropertyHandler.PASSWORD_CONFIRM) {
                continue;
            }
            String delim = hasSet ? "," : ""; //$NON-NLS-1$ //$NON-NLS-2$
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case PASSWORD: script.append(delim).append("Password=PASSWORD(").append(SQLUtils.quoteString(getObject(), CommonUtils.toString(entry.getValue()))).append(")"); hasSet = true; break; //$NON-NLS-1$ //$NON-NLS-2$
                case MAX_QUERIES: script.append(delim).append("Max_Questions=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                case MAX_UPDATES: script.append(delim).append("Max_Updates=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                case MAX_CONNECTIONS: script.append(delim).append("Max_Connections=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                case MAX_USER_CONNECTIONS: script.append(delim).append("Max_User_Connections=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                default: break;
            }
        }
        script.append(" WHERE User='").append(getObject().getUserName()).append("' AND Host='").append(getObject().getHost()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return hasSet;
    }

    private boolean generateAlterScript(StringBuilder script) {
        boolean hasSet = false, hasResOptions = false;

        script.append("ALTER USER ").append(getObject().getFullName()); //$NON-NLS-1$
        if (getProperties().containsKey(UserPropertyHandler.PASSWORD.name())) {
            script.append("\nIDENTIFIED BY ").append(SQLUtils.quoteString(getObject(), CommonUtils.toString(getProperties().get(UserPropertyHandler.PASSWORD.name())))).append(" ");
            hasSet = true;
        }
        StringBuilder resOptions = new StringBuilder();
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case MAX_QUERIES: resOptions.append(" MAX_QUERIES_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
                case MAX_UPDATES: resOptions.append(" MAX_UPDATES_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
                case MAX_CONNECTIONS: resOptions.append(" MAX_CONNECTIONS_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
                case MAX_USER_CONNECTIONS: resOptions.append(" MAX_USER_CONNECTIONS ").append(CommonUtils.toInt(entry.getValue())); hasResOptions = true; break; //$NON-NLS-1$
            }
        }
        if (resOptions.length() > 0) {
            script.append("\nWITH ").append(resOptions);
        }
        return hasSet || hasResOptions;
    }

}