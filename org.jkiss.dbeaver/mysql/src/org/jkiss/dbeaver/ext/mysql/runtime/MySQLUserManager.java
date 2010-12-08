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
        Map<UserPropertyHandler, Object> userProps = new HashMap<UserPropertyHandler, Object>();
        boolean hasPermissionChanges = false;
        for (Iterator<CommandInfo> cmdIter = commands.iterator(); cmdIter.hasNext(); ) {
            CommandInfo cmd = cmdIter.next();
            if (cmd.getCommand() instanceof MySQLCommandGrantPrivilege) {
                hasPermissionChanges = true;
            } else if (cmd.getCommand() instanceof DatabaseObjectPropertyCommand) {
                DatabaseObjectPropertyCommand propCommand = (DatabaseObjectPropertyCommand)cmd.getCommand();
                if (propCommand.getHandler() instanceof UserPropertyHandler) {
                    userProps.put((UserPropertyHandler) propCommand.getHandler(), propCommand.getNewValue());
                    cmdIter.remove();
                }
            }
        }
        
        if (!userProps.isEmpty()) {
            // Add user change in the beginning
            commands.add(0, new CommandInfo(new MySQLCommandChangeUser(userProps), null));
        }
        if (hasPermissionChanges) {
            // Add privileges flush to the tail
            commands.add(new CommandInfo(new DatabaseObjectScriptCommand<MySQLUser>("Flush privileges", "FLUSH PRIVILEGES"), null));
        }

    }

}
