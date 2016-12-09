/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLScriptCommand;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends AbstractObjectManager<MySQLUser> implements DBEObjectMaker<MySQLUser, MySQLDataSource>, DBECommandFilter<MySQLUser> {

    @Override
    public long getMakerOptions()
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
    public boolean canCreateObject(MySQLDataSource parent)
    {
        return true;
    }

    @Override
    public boolean canDeleteObject(MySQLUser object)
    {
        return true;
    }

    @Override
    public MySQLUser createNewObject(DBRProgressMonitor monitor, DBECommandContext commandContext, MySQLDataSource parent, Object copyFrom)
    {
        MySQLUser newUser = new MySQLUser(parent, null);
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
                    MySQLMessages.edit_user_manager_command_flush_privileges,
                    "FLUSH PRIVILEGES")); //$NON-NLS-1$
        }
    }

    private static class CommandCreateUser extends DBECommandAbstract<MySQLUser> {
        protected CommandCreateUser(MySQLUser user)
        {
            super(user, MySQLMessages.edit_user_manager_command_create_user);
        }
    }


    private static class CommandDropUser extends DBECommandComposite<MySQLUser, UserPropertyHandler> {
        protected CommandDropUser(MySQLUser user)
        {
            super(user, MySQLMessages.edit_user_manager_command_drop_user);
        }
        @Override
        public DBEPersistAction[] getPersistActions()
        {
            return new DBEPersistAction[] {
                new SQLDatabasePersistAction(MySQLMessages.edit_user_manager_command_drop_user, "DROP USER " + getObject().getFullName()) { //$NON-NLS-2$
                    @Override
                    public void handleExecute(DBCSession session, Throwable error)
                    {
                        if (error == null) {
                            getObject().setPersisted(false);
                        }
                    }
                }};
        }
    }

}

