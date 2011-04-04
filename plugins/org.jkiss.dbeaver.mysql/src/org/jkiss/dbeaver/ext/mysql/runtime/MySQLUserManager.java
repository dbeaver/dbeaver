/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandFilter;
import org.jkiss.dbeaver.model.edit.DBECommandQueue;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectScriptCommand;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;

import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends JDBCObjectManager<MySQLUser> implements DBEObjectMaker<MySQLUser>, DBECommandFilter<MySQLUser> {

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public MySQLUser createNewObject(IWorkbenchWindow workbenchWindow, DBECommandContext commander, Object parent, Object copyFrom)
    {
        MySQLUser newUser = new MySQLUser((MySQLDataSource) parent, null);
        if (copyFrom instanceof MySQLUser) {
            MySQLUser tplUser = (MySQLUser)copyFrom;
            newUser.setUserName(tplUser.getUserName());
            newUser.setHost(tplUser.getHost());
            newUser.setMaxQuestions(tplUser.getMaxQuestions());
            newUser.setMaxUpdates(tplUser.getMaxUpdates());
            newUser.setMaxConnections(tplUser.getMaxConnections());
            newUser.setMaxUserConnections(tplUser.getMaxUserConnections());
        }
        commander.addCommand(new NewUserPropertyCommand(newUser, UserPropertyHandler.NAME, newUser.getUserName()), null);
        commander.addCommand(new NewUserPropertyCommand(newUser, UserPropertyHandler.HOST, newUser.getHost()), null);

        return newUser;
    }

    public void deleteObject(DBECommandContext commander, MySQLUser user, Map<String, Object> options)
    {
        commander.addCommand(new MySQLCommandDropUser(user), null);
    }

    public void filterCommands(DBECommandQueue<MySQLUser> queue)
    {
        if (!queue.isEmpty()) {
            // Add privileges flush to the tail
            queue.add(
                new DatabaseObjectScriptCommand<MySQLUser>(
                    queue.getObject(),
                    "Flush privileges",
                    "FLUSH PRIVILEGES"));
        }
    }

    private static class NewUserPropertyCommand extends DBECommandProperty<MySQLUser> {
        public NewUserPropertyCommand(MySQLUser user, UserPropertyHandler property, Object value)
        {
            super(user, property, value);
        }

        @Override
        public boolean isUndoable()
        {
            return false;
        }
    }

}

