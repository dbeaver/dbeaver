/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectScriptCommand;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCDatabaseObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

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

    public MySQLUser createNewObject(DBSObject parent, MySQLUser copyFrom)
    {
        return new MySQLUser((MySQLDataSource) parent, null);
    }

    public void deleteObject(MySQLUser object)
    {

    }
}
