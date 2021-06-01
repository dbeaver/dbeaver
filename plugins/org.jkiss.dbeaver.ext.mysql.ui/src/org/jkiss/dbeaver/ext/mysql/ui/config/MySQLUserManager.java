/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends AbstractObjectManager<MySQLUser> implements DBEObjectMaker<MySQLUser, MySQLDataSource>,
    DBECommandFilter<MySQLUser>, DBEObjectRenamer<MySQLUser> {

    // Perhaps we should set it in UI? For now it is always disabled
    private static final String OPTION_SUPPRESS_FLUSH_PRIVILEGES = "suppress.flushPrivileges";

    private static final boolean USE_DIRECT_UPDATE = false;

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
        if (USE_DIRECT_UPDATE && !queue.isEmpty() && !MySQLUtils.isAlterUSerSupported(queue.getObject().getDataSource())) {
            // Add privileges flush to the tail
            queue.add(
                new DBECommandAbstract<MySQLUser>(
                    queue.getObject(),
                    MySQLUIMessages.edit_user_manager_command_flush_privileges) {
                    @Override
                    public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, Map<String, Object> options) throws DBException {
                        if (CommonUtils.getOption(options, OPTION_SUPPRESS_FLUSH_PRIVILEGES)) {
                            return new DBEPersistAction[0];
                        }
                        return new DBEPersistAction[] {
                            new SQLDatabasePersistAction(
                                getTitle(),
                                "FLUSH PRIVILEGES")
                        };
                    }
                });
        }
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull MySQLUser object, @NotNull Map<String, Object> options,
                             @NotNull String newName) {
        DBECommand<MySQLUser> command = new CommandRenameUser(object, MySQLUIMessages.edit_user_manager_command_rename_user, options, newName);
        commandContext.addCommand(command, new ReflectorRenameUser(), true);
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

    //---- Rename command and rename reflector. For the most part, it is copy-pasted from SQLObjectEditor.

    public static final class CommandRenameUser extends DBECommandAbstract<MySQLUser> implements DBECommandRename {
        private final Map<String, Object> options;
        private final String oldName;
        private final String oldUserName;
        private final String oldHost;
        private String newName;
        private String newUserName;
        private String newHost;

        private CommandRenameUser(MySQLUser user, String title, Map<String, Object> options, String newName) {
            super(user, title);
            this.options = options;
            oldName = user.getName();
            oldUserName = user.getUserName();
            oldHost = user.getHost();
            setNewName(newName);
        }

        Map<String, Object> getOptions() {
            return Collections.unmodifiableMap(options);
        }

        String getOldName() {
            return oldName;
        }

        String getNewName() {
            return newName;
        }

        String getOldUserName() {
            return oldUserName;
        }

        String getOldHost() {
            return oldHost;
        }

        public String getNewUserName() {
            return newUserName;
        }

        public String getNewHost() {
            return newHost;
        }

        private void setNewName(String newName) {
            this.newName = newName;
            int atPosition = newName.indexOf('@');
            if (atPosition == -1 || atPosition == newName.length() - 1) {
                newUserName = newName;
                newHost = "";
                return;
            }
            newUserName = newName.substring(0, atPosition);
            newHost = newName.substring(atPosition + 1);
        }

        @Override
        public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, Map<String, Object> options) {
            if (CommonUtils.equalObjects(oldName, newName)) {
                return new DBEPersistAction[0];
            }
            List<DBEPersistAction> actions = new ArrayList<>();
            DBPDataSource dataSource = executionContext.getDataSource();
            actions.add(new SQLDatabasePersistAction(
               "Rename user", //$NON-NLS-1$
               "RENAME USER " + getQuotedName(oldUserName, oldHost, dataSource) + " TO " + getQuotedName(newUserName, newHost, dataSource) //$NON-NLS-1$ //$NON-NLS-2$
            ));
            return actions.toArray(new DBEPersistAction[0]);
        }

        @NotNull
        private static String getQuotedName(@NotNull String userName, @NotNull String host, @NotNull DBPDataSource dataSource) {
            if (host.isEmpty()) {
                return DBUtils.getQuotedIdentifier(dataSource, userName);
            }
            return DBUtils.getQuotedIdentifier(dataSource, userName) + "@" + DBUtils.getQuotedIdentifier(dataSource, host);
        }

        @Override
        public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams) {
            // We need very first and very last rename commands. They produce final rename
            final String mergeId = "rename" + getObject().hashCode();
            CommandRenameUser renameCmd = (CommandRenameUser) userParams.get(mergeId);
            if (renameCmd == null) {
                renameCmd = new CommandRenameUser(getObject(), getTitle(), options, newName);
                userParams.put(mergeId, renameCmd);
            } else {
                renameCmd.setNewName(newName);
                return renameCmd;
            }
            return super.merge(prevCommand, userParams);
        }

        @Override
        public String toString() {
            return "CMD:RenameObject:" + getObject();
        }
    }

    private class ReflectorRenameUser implements DBECommandReflector<MySQLUser, CommandRenameUser> {
        @Override
        public void redoCommand(CommandRenameUser command) {
            MySQLUser user = command.getObject();
            user.setUserName(command.getNewUserName());
            setHost(user, command.getNewHost());

            // Update cache
            DBSObjectCache<? extends DBSObject, MySQLUser> cache = getObjectsCache(command.getObject());
            if (cache != null) {
                cache.renameObject(command.getObject(), command.getOldName(), command.getNewName());
            }

            Map<String, Object> options = new LinkedHashMap<>(command.getOptions());
            options.put(DBEObjectRenamer.PROP_OLD_NAME, command.getOldName());
            options.put(DBEObjectRenamer.PROP_NEW_NAME, command.getNewName());

            DBUtils.fireObjectUpdate(command.getObject(), options, DBPEvent.RENAME);
        }

        @Override
        public void undoCommand(CommandRenameUser command) {
            MySQLUser user = command.getObject();
            user.setUserName(command.getOldUserName());
            setHost(user, command.getOldHost());

            // Update cache
            DBSObjectCache<? extends DBSObject, MySQLUser> cache = getObjectsCache(command.getObject());
            if (cache != null) {
                cache.renameObject(command.getObject(), command.getNewName(), command.getOldName());
            }

            Map<String, Object> options = new LinkedHashMap<>(command.getOptions());
            DBUtils.fireObjectUpdate(command.getObject(), options, DBPEvent.RENAME);
        }

        private void setHost(@NotNull MySQLUser user, @NotNull String host) {
            if (host.isEmpty()) {
                host =  "%";
            }
            user.setHost(host);
        }
    }
}
