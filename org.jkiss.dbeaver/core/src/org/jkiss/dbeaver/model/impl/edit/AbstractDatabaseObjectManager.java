/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommandReflector;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AbstractDatabaseObjectManager
 */
public abstract class AbstractDatabaseObjectManager<OBJECT_TYPE extends DBSObject> implements IDatabaseObjectManager<OBJECT_TYPE> {

    private class PersistInfo {
        IDatabasePersistAction action;
        boolean executed = false;
        Throwable error;

        public PersistInfo(IDatabasePersistAction action)
        {
            this.action = action;
        }
    }

    private class CommandInfo {
        IDatabaseObjectCommand<OBJECT_TYPE> command;
        IDatabaseObjectCommandReflector reflector;
        List<PersistInfo> persistActions;
    }

    private OBJECT_TYPE object;
    private final List<CommandInfo> commands = new ArrayList<CommandInfo>();
    private final List<CommandInfo> undidCommands = new ArrayList<CommandInfo>();

    public DBPDataSource getDataSource() {
        return object.getDataSource();
    }

    public OBJECT_TYPE getObject() {
        return object;
    }

    @SuppressWarnings("unchecked")
    public void init(OBJECT_TYPE object) {
        if (object == null) {
            throw new IllegalArgumentException("Object can't be NULL");
        }
        this.object = object;
    }

    public boolean supportsEdit() {
        return false;
    }

    public boolean isDirty()
    {
        synchronized (commands) {
            return !commands.isEmpty();
        }
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        synchronized (commands) {
            // Make list of not-executed commands
            while (!commands.isEmpty()) {
                CommandInfo cmd = commands.get(0);
                // Persist changes
                IDatabasePersistAction[] persistActions = cmd.command.getPersistActions();
                if (CommonUtils.isEmpty(cmd.persistActions) && !CommonUtils.isEmpty(persistActions)) {
                    cmd.persistActions = new ArrayList<PersistInfo>(persistActions.length);
                    for (IDatabasePersistAction action : persistActions) {
                        cmd.persistActions.add(new PersistInfo(action));
                    }
                }
                if (!CommonUtils.isEmpty(cmd.persistActions)) {
                    DBCExecutionContext context = openCommandPersistContext(monitor, cmd.command);
                    try {
                        for (PersistInfo persistInfo : cmd.persistActions) {
                            if (!persistInfo.executed) {
                                try {
                                    executePersistAction(context, persistInfo.action);
                                } catch (DBException e) {
                                    persistInfo.error = e;
                                    persistInfo.executed = false;
                                    throw e;
                                }
                                persistInfo.executed = true;
                            }
                        }
                    } finally {
                        closePersistContext(context);
                    }
                }
                // Update model
                cmd.command.updateModel(getObject());

                // done
                commands.remove(0);
            }
        }
    }

    public void resetChanges(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (commands) {
            while (!commands.isEmpty()) {
                undoCommand(monitor);
            }
            undidCommands.clear();
        }
    }

    public Collection<? extends IDatabaseObjectCommand<OBJECT_TYPE>> getCommands()
    {
        synchronized (commands) {
            List<IDatabaseObjectCommand<OBJECT_TYPE>> cmdCopy = new ArrayList<IDatabaseObjectCommand<OBJECT_TYPE>>(commands.size());
            for (CommandInfo cmdInfo : commands) {
                cmdCopy.add(cmdInfo.command);
            }
            return cmdCopy;
        }
    }

    public <COMMAND extends IDatabaseObjectCommand<OBJECT_TYPE>> void addCommand(
        COMMAND command,
        IDatabaseObjectCommandReflector<COMMAND> reflector)
    {
        synchronized (commands) {
            CommandInfo commandInfo = new CommandInfo();
            commandInfo.command = command;
            commandInfo.reflector = reflector;
            commands.add(commandInfo);
            undidCommands.clear();
        }
    }

    public boolean canUndoCommand()
    {
        synchronized (commands) {
            return !commands.isEmpty();
        }
    }

    public boolean canRedoCommand()
    {
        synchronized (commands) {
            return !undidCommands.isEmpty();
        }
    }

    public void undoCommand(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!canUndoCommand()) {
            throw new IllegalStateException("Can't undo command");
        }
        synchronized (commands) {
            CommandInfo lastCommand = commands.remove(commands.size() - 1);
            // Undo UI changes and put command in undid command stack
            if (lastCommand.reflector != null) {
                lastCommand.reflector.undoCommand(lastCommand.command);
            }
            undidCommands.add(lastCommand);
        }
    }

    protected DBCExecutionContext openCommandPersistContext(
        DBRProgressMonitor monitor,
        IDatabaseObjectCommand<OBJECT_TYPE> command)
        throws DBException
    {
        return object.getDataSource().openContext(
            monitor,
            DBCExecutionPurpose.USER_SCRIPT,
            "Undo " + command.getTitle());
    }

    protected void closePersistContext(DBCExecutionContext context)
    {
        context.close();
    }

    public void redoCommand(DBRProgressMonitor monitor)
    {
        if (!canRedoCommand()) {
            throw new IllegalStateException("Can't redo command");
        }
        synchronized (commands) {
            // Just redo UI changes and put command on the top of stack
            CommandInfo commandInfo = undidCommands.remove(undidCommands.size() - 1);
            if (commandInfo.reflector != null) {
                commandInfo.reflector.redoCommand(commandInfo.command);
            }
            commands.add(commandInfo);
        }
    }

    protected abstract void executePersistAction(
        DBCExecutionContext context,
        IDatabasePersistAction action)
        throws DBException;

}
