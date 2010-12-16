/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyCommand;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectScriptCommand;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCDatabaseObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends JDBCDatabaseObjectManager<MySQLUser> implements IDatabaseObjectManagerEx<MySQLUser> {

    @Override
    public boolean supportsEdit() {
        return true;
    }

    protected void filterCommands(List<CommandInfo> commands)
    {
        if (!commands.isEmpty()) {
            // Add privileges flush to the tail
            commands.add(new CommandInfo(new DatabaseObjectScriptCommand<MySQLUser>("Flush privileges", "FLUSH PRIVILEGES"), null));
        }
    }

    public void createNewObject(DBSObject parent, MySQLUser copyFrom)
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
    }

    public void deleteObject(MySQLUser object, Map<String, Object> options)
    {
        setObject(object);
        addCommand(new MySQLCommandDropUser(), null);
    }

    private static class NewUserPropertyCommand extends DatabaseObjectPropertyCommand<MySQLUser> {
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

