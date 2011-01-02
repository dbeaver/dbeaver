/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.prop.DBOCommandProperty;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectScriptCommand;
import org.jkiss.dbeaver.model.impl.jdbc.edit.DBOEditorJDBC;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends DBOEditorJDBC<MySQLUser> implements DBOCreator<MySQLUser> {

    protected void filterCommands(List<CommandInfo> commands)
    {
        if (!commands.isEmpty()) {
            // Add privileges flush to the tail
            commands.add(new CommandInfo(new DatabaseObjectScriptCommand<MySQLUser>("Flush privileges", "FLUSH PRIVILEGES"), null));
        }
    }

    public CreateResult createNewObject(IWorkbenchWindow workbenchWindow, DBSObject parent, MySQLUser copyFrom)
    {
        MySQLUser newUser = new MySQLUser((MySQLDataSource) parent, null);
        if (copyFrom != null) {
            newUser.setUserName(copyFrom.getUserName());
            newUser.setHost(copyFrom.getHost());
            newUser.setMaxQuestions(copyFrom.getMaxQuestions());
            newUser.setMaxUpdates(copyFrom.getMaxUpdates());
            newUser.setMaxConnections(copyFrom.getMaxConnections());
            newUser.setMaxUserConnections(copyFrom.getMaxUserConnections());
        }
        setObject(newUser);
        addCommand(new NewUserPropertyCommand(UserPropertyHandler.NAME, newUser.getUserName()), null);
        addCommand(new NewUserPropertyCommand(UserPropertyHandler.HOST, newUser.getHost()), null);

        return CreateResult.OPEN_EDITOR;
    }

    public void deleteObject(Map<String, Object> options)
    {
        addCommand(new MySQLCommandDropUser(), null);
    }

    private static class NewUserPropertyCommand extends DBOCommandProperty<MySQLUser> {
        public NewUserPropertyCommand(UserPropertyHandler property, Object value)
        {
            super(property, value);
        }

        @Override
        public boolean isUndoable()
        {
            return false;
        }
    }

}

