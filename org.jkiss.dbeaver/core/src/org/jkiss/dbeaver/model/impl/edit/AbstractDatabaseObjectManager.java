/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.window.IShellProvider;
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
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

/**
 * AbstractDatabaseObjectManager
 */
public abstract class AbstractDatabaseObjectManager<OBJECT_TYPE extends DBSObject> implements IDatabaseObjectManager<OBJECT_TYPE> {

    private static class PersistInfo {
        final IDatabasePersistAction action;
        boolean executed = false;
        Throwable error;

        public PersistInfo(IDatabasePersistAction action)
        {
            this.action = action;
        }
    }

    protected class CommandInfo {
        final IDatabaseObjectCommand<OBJECT_TYPE> command;
        final IDatabaseObjectCommandReflector reflector;
        List<PersistInfo> persistActions;
        CommandInfo mergedBy = null;
        boolean executed = false;

        public CommandInfo(IDatabaseObjectCommand<OBJECT_TYPE> command, IDatabaseObjectCommandReflector reflector)
        {
            this.command = command;
            this.reflector = reflector;
        }

        public IDatabaseObjectCommand<OBJECT_TYPE> getCommand()
        {
            return command;
        }

        public IDatabaseObjectCommandReflector getReflector()
        {
            return reflector;
        }
    }

    private IShellProvider shellProvider;
    private OBJECT_TYPE object;
    private final List<CommandInfo> commands = new ArrayList<CommandInfo>();
    private final List<CommandInfo> undidCommands = new ArrayList<CommandInfo>();
    private List<CommandInfo> mergedCommands = null;

    public DBPDataSource getDataSource() {
        return object.getDataSource();
    }

    public OBJECT_TYPE getObject() {
        return object;
    }

    @SuppressWarnings("unchecked")
    public void init(IShellProvider shellProvider, OBJECT_TYPE object) {
        if (object == null) {
            throw new IllegalArgumentException("Object can't be NULL");
        }
        this.shellProvider = shellProvider;
        this.object = object;

        // Clear all commands
        this.commands.clear();
        clearUndidCommands();
        clearMergedCommands();
    }

    public boolean supportsEdit() {
        return false;
    }

    public boolean isDirty()
    {
        synchronized (commands) {
            return !getMergedCommands().isEmpty();
        }
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        synchronized (commands) {
            List<CommandInfo> mergedCommands = getMergedCommands();

            // Validate commands
            for (CommandInfo cmd : mergedCommands) {
                try {
                    cmd.command.validateCommand(object);
                } catch (DBException e) {
                    UIUtils.showErrorDialog(shellProvider.getShell(), "Validation failed", e.getMessage());
                    return;
                }
            }
            try {
                // Make list of not-executed commands
                for (int i = 0; i < mergedCommands.size(); i++) {
                    CommandInfo cmd = mergedCommands.get(i);
                    if (cmd.mergedBy != null) {
                        cmd = cmd.mergedBy;
                    }
                    if (cmd.executed) {
                        continue;
                    }
                    if (monitor.isCanceled()) {
                        break;
                    }
                    // Persist changes
                    if (CommonUtils.isEmpty(cmd.persistActions)) {
                        IDatabasePersistAction[] persistActions = cmd.command.getPersistActions(object);
                        if (!CommonUtils.isEmpty(persistActions)) {
                            cmd.persistActions = new ArrayList<PersistInfo>(persistActions.length);
                            for (IDatabasePersistAction action : persistActions) {
                                cmd.persistActions.add(new PersistInfo(action));
                            }
                        }
                    }
                    if (!CommonUtils.isEmpty(cmd.persistActions)) {
                        DBCExecutionContext context = openCommandPersistContext(monitor, cmd.command);
                        try {
                            for (PersistInfo persistInfo : cmd.persistActions) {
                                if (persistInfo.executed) {
                                    continue;
                                }
                                if (monitor.isCanceled()) {
                                    break;
                                }
                                try {
                                    executePersistAction(context, persistInfo.action);
                                    persistInfo.executed = true;
                                } catch (DBException e) {
                                    persistInfo.error = e;
                                    persistInfo.executed = false;
                                    throw e;
                                }
                            }
                        } finally {
                            closePersistContext(context);
                        }
                    }
                    // Update model
                    cmd.command.updateModel(getObject());
                    cmd.executed = true;

                    // Remove executed command from stack
                    commands.remove(cmd);
                }
            }
            finally {
                clearMergedCommands();
                clearUndidCommands();
            }
        }
    }

    public void resetChanges(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (commands) {
            while (!commands.isEmpty()) {
                undoCommand(monitor);
            }
            clearUndidCommands();
            clearMergedCommands();
        }
    }

    public Collection<? extends IDatabaseObjectCommand<OBJECT_TYPE>> getCommands()
    {
        synchronized (commands) {
            List<IDatabaseObjectCommand<OBJECT_TYPE>> cmdCopy = new ArrayList<IDatabaseObjectCommand<OBJECT_TYPE>>(commands.size());
            for (CommandInfo cmdInfo : getMergedCommands()) {
                if (cmdInfo.mergedBy != null) {
                    cmdInfo = cmdInfo.mergedBy;
                }
                if (!cmdCopy.contains(cmdInfo.command)) {
                    cmdCopy.add(cmdInfo.command);
                }
            }
            return cmdCopy;
        }
    }

    public <COMMAND extends IDatabaseObjectCommand<OBJECT_TYPE>> void addCommand(
        COMMAND command,
        IDatabaseObjectCommandReflector<OBJECT_TYPE, COMMAND> reflector)
    {
        synchronized (commands) {
            commands.add(new CommandInfo(command, reflector));

            clearUndidCommands();
            clearMergedCommands();
        }
    }

    public <COMMAND extends IDatabaseObjectCommand<OBJECT_TYPE>> void removeCommand(COMMAND command)
    {
        synchronized (commands) {
            for (CommandInfo cmd : commands) {
                if (cmd.command == command) {
                    commands.remove(cmd);
                    break;
                }
            }
            clearUndidCommands();
            clearMergedCommands();
        }
    }

    public <COMMAND extends IDatabaseObjectCommand<OBJECT_TYPE>> void updateCommand(COMMAND command)
    {
        synchronized (commands) {
            clearUndidCommands();
            clearMergedCommands();
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
            clearMergedCommands();
        }
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
            clearMergedCommands();
        }
    }

    private void clearUndidCommands()
    {
        undidCommands.clear();
    }

    private List<CommandInfo> getMergedCommands()
    {
        if (mergedCommands != null) {
            return mergedCommands;
        }
        mergedCommands = new ArrayList<CommandInfo>();

        final Map<IDatabaseObjectCommand, CommandInfo> mergedByMap = new IdentityHashMap<IDatabaseObjectCommand, CommandInfo>();
        final Map<String, Object> userParams = new HashMap<String, Object>();
        for (int i = 0; i < commands.size(); i++) {
            CommandInfo lastCommand = commands.get(i);
            lastCommand.mergedBy = null;
            CommandInfo firstCommand = null;
            IDatabaseObjectCommand<OBJECT_TYPE> result = lastCommand.command;
            if (mergedCommands.isEmpty()) {
                result = lastCommand.command.merge(null, userParams);
            } else {
                for (int k = mergedCommands.size(); k > 0; k--) {
                    firstCommand = mergedCommands.get(k - 1);
                    result = lastCommand.command.merge(firstCommand.command, userParams);
                    if (result != lastCommand.command) {
                        break;
                    }
                }
            }
            if (result == null) {
                // Remove first and skip last command
                mergedCommands.remove(firstCommand);
                continue;
            }

            mergedCommands.add(lastCommand);
            if (result == lastCommand.command) {
                // No changes
                //firstCommand.mergedBy = lastCommand;
            } else if (firstCommand != null && result == firstCommand.command) {
                // Remove last command from queue
                lastCommand.mergedBy = firstCommand;
            } else {
                // Some other command
                // May be it is some earlier command from queue or some new command (e.g. composite)
                CommandInfo mergedBy = mergedByMap.get(result);
                if (mergedBy == null) {
                    // Try to find in command stack
                    for (int k = i; k >= 0; k--) {
                        if (commands.get(k).command == result) {
                            mergedBy = commands.get(k);
                            break;
                        }
                    }
                    if (mergedBy == null) {
                        // Create new command info
                        mergedBy = new CommandInfo(result, null);
                    }
                    mergedByMap.put(result, mergedBy);
                }
                lastCommand.mergedBy = mergedBy;
                if (!mergedCommands.contains(mergedBy)) {
                    mergedCommands.add(mergedBy);
                }
            }
        }
        filterCommands(mergedCommands);
        return mergedCommands;
    }

    private void clearMergedCommands()
    {
        mergedCommands = null;
    }

    protected DBCExecutionContext openCommandPersistContext(
        DBRProgressMonitor monitor,
        IDatabaseObjectCommand<OBJECT_TYPE> command)
        throws DBException
    {
        return object.getDataSource().openContext(
            monitor,
            DBCExecutionPurpose.USER_SCRIPT,
            "Execute " + command.getTitle());
    }

    protected void closePersistContext(DBCExecutionContext context)
    {
        context.close();
    }

    protected void filterCommands(List<CommandInfo> commands)
    {
        // do nothing by default
    }


    protected abstract void executePersistAction(
        DBCExecutionContext context,
        IDatabasePersistAction action)
        throws DBException;

}
