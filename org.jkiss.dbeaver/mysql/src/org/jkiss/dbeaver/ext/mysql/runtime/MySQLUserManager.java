/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectPropertyCommand;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectScriptCommand;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCDatabaseObjectManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends JDBCDatabaseObjectManager<MySQLUser> {

    @Override
    public boolean supportsEdit() {
        return true;
    }

    protected void filterCommands(List<CommandInfo> commands)
    {
        Map<UserPropertyHandler, Object> userProps = filterPropertyCommands(commands, UserPropertyHandler.class, true);
        boolean hasPermissionChanges = false;
        for (CommandInfo cmd : commands) {
            if (cmd.getCommand() instanceof MySQLCommandGrantPrivilege) {
                hasPermissionChanges = true;
            }
        }
        
        if (!userProps.isEmpty()) {
            // Add user change in the beginning
            commands.add(0, new CommandInfo(new MySQLCommandChangeUser(userProps), null));
        }
        if (!userProps.isEmpty() || hasPermissionChanges) {
            // Add privileges flush to the tail
            commands.add(new CommandInfo(new DatabaseObjectScriptCommand<MySQLUser>("Flush privileges", "FLUSH PRIVILEGES"), null));
        }

    }

}
