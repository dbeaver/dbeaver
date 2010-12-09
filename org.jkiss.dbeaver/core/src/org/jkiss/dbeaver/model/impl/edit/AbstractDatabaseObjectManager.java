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
            // Make list of not-executed commands
            while (!mergedCommands.isEmpty()) {
                CommandInfo cmd = mergedCommands.get(0);
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
                mergedCommands.remove(0);
            }
            clearMergedCommands();
            clearUndidCommands();
            commands.clear();
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
            commands.add(new CommandInfo(command, reflector));

            clearUndidCommands();
            clearMergedCommands();
        }
    }

    public void removeCommand(DatabaseObjectPropertyCommand<OBJECT_TYPE> command)
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

    public void updateCommand(DatabaseObjectPropertyCommand<OBJECT_TYPE> object_typeDatabaseObjectPropertyCommand)
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

        for (int i = 0; i < commands.size(); i++) {
            CommandInfo lastCommand = commands.get(i);
            CommandInfo firstCommand = null;
            IDatabaseObjectCommand.MergeResult result = IDatabaseObjectCommand.MergeResult.NONE;
            for (int k = mergedCommands.size(); k > 0; k--) {
                firstCommand = mergedCommands.get(k - 1);
                result = lastCommand.command.merge(firstCommand.command);
                if (result != IDatabaseObjectCommand.MergeResult.NONE) {
                    break;
                }
            }
            switch (result) {
            case NONE:
                // Add command to list
                mergedCommands.add(lastCommand);
                break;
            case ABSORBED:
                // Command absorbed by previous one
                continue;
            case CANCEL_PREVIOUS:
                mergedCommands.remove(firstCommand);
                mergedCommands.add(lastCommand);
                break;
            case CANCEL_BOTH:
                mergedCommands.remove(firstCommand);
                break;
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

    protected <HANDLER_TYPE extends DatabaseObjectPropertyHandler> Map<HANDLER_TYPE, Object> filterPropertyCommands(
        List<CommandInfo> commands,
        Class<HANDLER_TYPE> handlerClass,
        boolean removeCommands)
    {
        Map<HANDLER_TYPE, Object> userProps = new HashMap<HANDLER_TYPE, Object>();
        boolean hasPermissionChanges = false;
        for (Iterator<CommandInfo> cmdIter = commands.iterator(); cmdIter.hasNext(); ) {
            CommandInfo cmd = cmdIter.next();
            if (cmd.getCommand() instanceof DatabaseObjectPropertyCommand) {
                DatabaseObjectPropertyCommand propCommand = (DatabaseObjectPropertyCommand)cmd.getCommand();
                if (handlerClass.isAssignableFrom(propCommand.getHandler().getClass())) {
                    userProps.put(handlerClass.cast(propCommand.getHandler()), propCommand.getNewValue());
                    if (removeCommands) {
                        cmdIter.remove();
                    }
                }
            }
        }
        return userProps;
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
