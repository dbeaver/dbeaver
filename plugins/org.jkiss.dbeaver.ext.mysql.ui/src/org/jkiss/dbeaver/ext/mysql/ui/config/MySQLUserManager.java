/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLScriptCommand;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends AbstractObjectManager<MySQLUser> implements DBEObjectMaker<MySQLUser, MySQLDataSource>, DBECommandFilter<MySQLUser> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MySQLUser> getObjectsCache(MySQLUser object)
    {
        return null;
    }

    @Override
    public boolean canCreateObject(Object container)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(MySQLUser object)
    {
        return true;
    }

    @Override
    public MySQLUser createNewObject(DBRProgressMonitor monitor, DBECommandContext commandContext, Object container, Object copyFrom, Map<String, Object> options)
    {
        MySQLUser newUser = new MySQLUser((MySQLDataSource) container, null);
        if (copyFrom instanceof MySQLUser) {
            MySQLUser tplUser = (MySQLUser)copyFrom;
            newUser.setUserName(tplUser.getUserName());
            newUser.setHost(tplUser.getHost());
            newUser.setMaxQuestions(tplUser.getMaxQuestions());
            newUser.setMaxUpdates(tplUser.getMaxUpdates());
            newUser.setMaxConnections(tplUser.getMaxConnections());
            newUser.setMaxUserConnections(tplUser.getMaxUserConnections());
        }
        commandContext.addCommand(new CommandCreateUser(newUser), new CreateObjectReflector<>(this), true);

        return newUser;
    }

    @Override
    public void deleteObject(DBECommandContext commandContext, MySQLUser user, Map<String, Object> options)
    {
        commandContext.addCommand(new CommandDropUser(user), new DeleteObjectReflector<>(this), true);
    }

    @Override
    public void filterCommands(DBECommandQueue<MySQLUser> queue)
    {
        if (!queue.isEmpty()) {
            // Add privileges flush to the tail
            queue.add(
                new SQLScriptCommand<>(
                    queue.getObject(),
                    MySQLUIMessages.edit_user_manager_command_flush_privileges,
                    "FLUSH PRIVILEGES")); //$NON-NLS-1$
        }
    }

    private static class CommandCreateUser extends DBECommandAbstract<MySQLUser> {
        protected CommandCreateUser(MySQLUser user)
        {
            super(user, MySQLUIMessages.edit_user_manager_command_create_user);
        }

        @Override
        public void validateCommand(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
            if (CommonUtils.isEmpty(getObject().getUserName())) {
                throw new DBException("Can't create user with empty name");
            }
            if (CommonUtils.isEmpty(getObject().getHost())) {
                throw new DBException("Can't create user with empty host name");
            }
            super.validateCommand(monitor, options);
        }
    }


    private static class CommandDropUser extends DBECommandComposite<MySQLUser, UserPropertyHandler> {
        protected CommandDropUser(MySQLUser user)
        {
            super(user, MySQLUIMessages.edit_user_manager_command_drop_user);
        }
        @Override
        public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, Map<String, Object> options)
        {
            return new DBEPersistAction[] {
                new SQLDatabasePersistAction(MySQLUIMessages.edit_user_manager_command_drop_user, "DROP USER " + getObject().getFullName()) { //$NON-NLS-2$
                    @Override
                    public void afterExecute(DBCSession session, Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(false);
                        }
                    }
                }};
        }
    }

}

