/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.function.Supplier;

/**
 * Grant/Revoke privilege command
 */
public class MySQLCommandChangeUser extends DBECommandComposite<MySQLUser, UserPropertyHandler> {

    protected MySQLCommandChangeUser(MySQLUser user)
    {
        super(user, MySQLUIMessages.edit_command_change_user_name);
    }

    @Override
    public void updateModel() {
        MySQLUser mySQLUser = getObject();
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case NAME:
                    mySQLUser.setPersistedUserName(mySQLUser.getUserName());
                    break;
                case HOST:
                    mySQLUser.setPersistedHost(mySQLUser.getHost());
                    break;
                case MAX_QUERIES:
                    mySQLUser.setMaxQuestions(CommonUtils.toInt(entry.getValue()));
                    break;
                case MAX_UPDATES:
                    mySQLUser.setMaxUpdates(CommonUtils.toInt(entry.getValue()));
                    break;
                case MAX_CONNECTIONS:
                    mySQLUser.setMaxConnections(CommonUtils.toInt(entry.getValue()));
                    break;
                case MAX_USER_CONNECTIONS:
                    mySQLUser.setMaxUserConnections(CommonUtils.toInt(entry.getValue()));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void validateCommand(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        String passValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD));
        String confirmValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD_CONFIRM));
        if (!CommonUtils.isEmpty(passValue) && !CommonUtils.equalObjects(passValue, confirmValue)) {
            throw new DBException("Password confirmation value is invalid");
        }
    }

    @Override
    public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, Map<String, Object> options)
    {
        List<DBEPersistAction> actions = new ArrayList<>();
        boolean newUser = !getObject().isPersisted();
        if (newUser) {
            actions.add(
                new SQLDatabasePersistAction(MySQLUIMessages.edit_command_change_user_action_create_new_user, "CREATE USER " + getObject().getFullName()) { //$NON-NLS-2$
                    @Override
                    public void afterExecute(DBCSession session, Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(true);
                        }
                    }
                });
        } else {
            addAction(actions, this::generateRenameScript);
        }
        final MySQLDataSource dataSource = getObject().getDataSource();
        if (MySQLUtils.isAlterUSerSupported(dataSource)) {
            addAction(actions, this::generateAlterScript);
        } else {
            addAction(actions, this::generateUpdateScript);
            addAction(actions, this::generatePasswordSet);
        }
        return actions.toArray(new DBEPersistAction[0]);
    }

    private static void addAction(@NotNull Collection<? super DBEPersistAction> actions, @NotNull Supplier<String> sqlGenerator) {
        String sql = sqlGenerator.get();
        if (sql != null) {
            actions.add(new SQLDatabasePersistAction(MySQLUIMessages.edit_command_change_user_action_update_user_record, sql));
        }
    }

    @Nullable
    private String generateRenameScript() {
        MySQLUser mySQLUser = getObject();
        if (mySQLUser.isPersisted() && (!Objects.equals(mySQLUser.getUserName(), mySQLUser.getPersistedUserName()) || !Objects.equals(mySQLUser.getHost(), mySQLUser.getPersistedHost()))) {
            return "RENAME USER " + mySQLUser.getPersistedFullName() + " TO " + getObject().getFullName() + ";"; //$NON-NLS-1$ //$NON-NLS-3$
        }
        return null;
    }

    @Nullable
    private String generateUpdateScript() {
        StringBuilder script = new StringBuilder();
        script.append("UPDATE mysql.user SET "); //$NON-NLS-1$
        boolean hasSet = false;
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            String delim = hasSet ? "," : ""; //$NON-NLS-1$ //$NON-NLS-2$
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case MAX_QUERIES: script.append(delim).append("Max_Questions=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                case MAX_UPDATES: script.append(delim).append("Max_Updates=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                case MAX_CONNECTIONS: script.append(delim).append("Max_Connections=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                case MAX_USER_CONNECTIONS: script.append(delim).append("Max_User_Connections=").append(CommonUtils.toInt(entry.getValue())); hasSet = true; break; //$NON-NLS-1$
                default: break;
            }
        }
        if (!hasSet) {
            return null;
        }
        script.append(" WHERE User='").append(getObject().getUserName()).append("' AND Host='").append(getObject().getHost()).append("'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return script.toString();
    }

    @Nullable
    private String generatePasswordSet() {
        Object passwordValue = getProperties().get(UserPropertyHandler.PASSWORD.toString());
        if (passwordValue == null) {
            return null;
        }
        return "SET PASSWORD FOR '" + getObject().getUserName() + "'@'" + getObject().getHost() +
            "' = PASSWORD('" + passwordValue + "')";
    }

    @Nullable
    private String generateAlterScript() {
        StringBuilder script = new StringBuilder();
        if (getProperties().containsKey(UserPropertyHandler.PASSWORD.name())) {
            script.append("\nIDENTIFIED BY ").append(SQLUtils.quoteString(getObject(), CommonUtils.toString(getProperties().get(UserPropertyHandler.PASSWORD.name())))).append(" ");
        }
        StringBuilder resOptions = new StringBuilder();
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
                case MAX_QUERIES: resOptions.append("\n\tMAX_QUERIES_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); break; //$NON-NLS-1$
                case MAX_UPDATES: resOptions.append("\n\tMAX_UPDATES_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); break; //$NON-NLS-1$
                case MAX_CONNECTIONS: resOptions.append("\n\tMAX_CONNECTIONS_PER_HOUR ").append(CommonUtils.toInt(entry.getValue())); break; //$NON-NLS-1$
                case MAX_USER_CONNECTIONS: resOptions.append("\n\tMAX_USER_CONNECTIONS ").append(CommonUtils.toInt(entry.getValue())); break; //$NON-NLS-1$
            }
        }
        if (resOptions.length() > 0) {
            script.append("\nWITH").append(resOptions);
        }

        if (script.length() == 0) {
            return null;
        }
        return "ALTER USER " + getObject().getFullName() + script; //$NON-NLS-1$
    }
}
