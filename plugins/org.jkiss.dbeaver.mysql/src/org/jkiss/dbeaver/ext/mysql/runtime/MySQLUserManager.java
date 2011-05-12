/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.runtime;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.impl.edit.DatabaseObjectScriptCommand;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MySQLUserManager
 */
public class MySQLUserManager extends JDBCObjectManager<MySQLUser> implements DBEObjectMaker<MySQLUser, MySQLDataSource>, DBECommandFilter<MySQLUser> {

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public MySQLUser createNewObject(IWorkbenchWindow workbenchWindow, IEditorPart activeEditor, DBECommandContext commandContext, MySQLDataSource parent, Object copyFrom)
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
        List<DBECommand> commands = new ArrayList<DBECommand>();
        commands.add(new MySQLCommandCreateUser(newUser));
        commands.add(new DBECommandProperty<MySQLUser>(newUser, UserPropertyHandler.NAME, newUser.getUserName()));
        commands.add(new DBECommandProperty<MySQLUser>(newUser, UserPropertyHandler.HOST, newUser.getHost()));
        commandContext.addCommandBatch(commands, new CreateObjectReflector(), true);

        return newUser;
    }

    public void deleteObject(DBECommandContext commandContext, MySQLUser user, Map<String, Object> options)
    {
        commandContext.addCommand(new MySQLCommandDropUser(user), new DeleteObjectReflector(), true);
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

}

