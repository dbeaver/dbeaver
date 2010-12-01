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
        boolean executed = false;
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
            if (commands.isEmpty()) {
                return false;
            }
            // If we have at least one not executed command then we are dirty
            for (CommandInfo commandInfo : commands) {
                if (!commandInfo.executed) {
                    return true;
                }
            }
            // Nope
            return false;
        }
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        synchronized (commands) {
            // Make list of not-executed commands
            List<CommandInfo> runCommands = new ArrayList<CommandInfo>();
            for (CommandInfo cmd : commands) {
                if (!cmd.executed) {
                    runCommands.add(cmd);
                }
            }

            for (int i = 0, runCommandsSize = runCommands.size(); i < runCommandsSize; i++) {
                CommandInfo cmd = runCommands.get(i);
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
                                    executePersistAction(context, persistInfo.action, false);
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
                cmd.command.updateModel(getObject(), false);

                // done
                cmd.executed = true;
            }
        }
    }

    public void resetChanges(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (commands) {
            while (!commands.isEmpty()) {
                CommandInfo lastCommand = commands.get(commands.size() - 1);
                if (!lastCommand.executed) {
                    undoCommand(monitor);
                } else {
                    break;
                }
            }
            commands.clear();
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
            if (commands.isEmpty()) {
                return false;
            }
            CommandInfo lastCommand = commands.get(commands.size() - 1);
            // We can't undo permanent commands, check this flag in last command
            return
                !lastCommand.executed ||
                (lastCommand.command.getFlags() & IDatabaseObjectCommand.FLAG_PERMANENT) != IDatabaseObjectCommand.FLAG_PERMANENT;
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
            if (lastCommand.executed && hasPersistedActions(lastCommand)) {
                // This command was executed and persisted
                DBCExecutionContext context = openCommandPersistContext(monitor, lastCommand.command);
                try {
                    // Undo all persisted actions in reverse order
                    for (int i = lastCommand.persistActions.size(); i > 0; i++) {
                        PersistInfo persistInfo = lastCommand.persistActions.get(i - 1);
                        if (persistInfo.executed) {
                            executePersistAction(context, persistInfo.action, true);
                            persistInfo.executed = false;
                        }
                    }
                } finally {
                    closePersistContext(context);
                }
                // Undo model changes
                lastCommand.command.updateModel(getObject(), true);
            } else {
                // Command wasn't really executed
                // Just undo UI changes
            }
            lastCommand.executed = false;

            // Undo UI changes and put command in undid command stack
            if (lastCommand.reflector != null) {
                lastCommand.reflector.undoCommand(lastCommand.command);
            }
            undidCommands.add(lastCommand);
        }
    }

    private boolean hasPersistedActions(CommandInfo commandInfo)
    {
        if (CommonUtils.isEmpty(commandInfo.persistActions)) {
            return false;
        }
        for (PersistInfo persistInfo : commandInfo.persistActions) {
            if (persistInfo.executed) {
                return true;
            }
        }
        return false;
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
            commandInfo.executed = true;
            commands.add(commandInfo);
        }
    }

    protected abstract void executePersistAction(
        DBCExecutionContext context,
        IDatabasePersistAction action,
        boolean undo)
        throws DBException;

}
