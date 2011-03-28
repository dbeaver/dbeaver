/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.*;

/**
 * DBEObjectCommanderImpl
 */
public class DBEObjectCommanderImpl implements DBEObjectCommander {

    private static class PersistInfo {
        final IDatabasePersistAction action;
        boolean executed = false;
        Throwable error;

        public PersistInfo(IDatabasePersistAction action)
        {
            this.action = action;
        }
    }

    public static class CommandInfo {
        final DBECommand<?> command;
        final DBECommandReflector<?, DBECommand<?>> reflector;
        List<PersistInfo> persistActions;
        CommandInfo mergedBy = null;
        boolean executed = false;

        CommandInfo(DBECommand<?> command, DBECommandReflector<?, DBECommand<?>> reflector)
        {
            this.command = command;
            this.reflector = reflector;
        }
    }

    private final DBSDataSourceContainer dataSourceContainer;
    private final List<CommandInfo> commands = new ArrayList<CommandInfo>();
    private final List<CommandInfo> undidCommands = new ArrayList<CommandInfo>();
    private List<CommandInfo> mergedCommands = null;

    private final List<DBECommandListener> listeners = new ArrayList<DBECommandListener>();

    public DBEObjectCommanderImpl(DBSDataSourceContainer dataSourceContainer)
    {
        this.dataSourceContainer = dataSourceContainer;
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }

    public boolean isDirty()
    {
        synchronized (commands) {
            return !getMergedCommands().isEmpty();
        }
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        if (!dataSourceContainer.isConnected()) {
            throw new DBException("Not connected to database");
        }
        synchronized (commands) {
            List<CommandInfo> mergedCommands = getMergedCommands();

            // Validate commands
            for (CommandInfo cmd : mergedCommands) {
                cmd.command.validateCommand();
            }
            try {
                // Make list of not-executed commands
                for (int i = 0; i < mergedCommands.size(); i++) {
                    CommandInfo cmd = mergedCommands.get(i);
                    if (cmd.mergedBy != null) {
                        cmd = cmd.mergedBy;
                    }
                    if (cmd.executed) {
                        commands.remove(mergedCommands.get(i));
                        continue;
                    }
                    if (monitor.isCanceled()) {
                        break;
                    }
                    // Persist changes
                    if (CommonUtils.isEmpty(cmd.persistActions)) {
                        IDatabasePersistAction[] persistActions = cmd.command.getPersistActions();
                        if (!CommonUtils.isEmpty(persistActions)) {
                            cmd.persistActions = new ArrayList<PersistInfo>(persistActions.length);
                            for (IDatabasePersistAction action : persistActions) {
                                cmd.persistActions.add(new PersistInfo(action));
                            }
                        }
                    }
                    if (!CommonUtils.isEmpty(cmd.persistActions)) {
                        DBCExecutionContext context = openCommandPersistContext(monitor, dataSourceContainer.getDataSource(), cmd.command);
                        try {
                            for (PersistInfo persistInfo : cmd.persistActions) {
                                if (persistInfo.executed) {
                                    continue;
                                }
                                if (monitor.isCanceled()) {
                                    break;
                                }
                                try {
                                    getObjectManager(cmd.command.getObject()).executePersistAction(context, cmd.command, persistInfo.action);
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
                    cmd.command.updateModel();
                    cmd.executed = true;

                    // Remove original command from stack
                    commands.remove(mergedCommands.get(i));
                }
            }
            finally {
                clearMergedCommands();
                clearUndidCommands();

                for (DBECommandListener listener : getListeners()) {
                    listener.onSave();
                }
            }
        }
    }

    private DBEObjectManager getObjectManager(DBSObject object)
    {
        return DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(object.getClass());
    }

    public void resetChanges()
    {
        synchronized (commands) {
            try {
                while (!commands.isEmpty()) {
                    undoCommand();
                }
                clearUndidCommands();
                clearMergedCommands();
            } finally {
                for (DBECommandListener listener : getListeners()) {
                    listener.onReset();
                }
            }
        }
    }

    public Collection<? extends DBECommand<?>> getCommands()
    {
        synchronized (commands) {
            List<DBECommand<?>> cmdCopy = new ArrayList<DBECommand<?>>(commands.size());
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

    public void addCommand(
        DBECommand<?> command,
        DBECommandReflector<?, DBECommand<?>> reflector)
    {
        synchronized (commands) {
            commands.add(new CommandInfo(command, reflector));

            clearUndidCommands();
            clearMergedCommands();
        }
    }

    public void removeCommand(DBECommand<?> command)
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

    public void updateCommand(DBECommand<?> command)
    {
        synchronized (commands) {
            clearUndidCommands();
            clearMergedCommands();
        }
    }

    public void addCommandListener(DBECommandListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeCommandListener(DBECommandListener listener)
    {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    DBECommandListener[] getListeners()
    {
        synchronized (listeners) {
            return listeners.toArray(new DBECommandListener[listeners.size()]);
        }
    }

    public boolean canUndoCommand()
    {
        synchronized (commands) {
            return !commands.isEmpty() && commands.get(commands.size() - 1).command.isUndoable();
        }
    }

    public boolean canRedoCommand()
    {
        synchronized (commands) {
            return !undidCommands.isEmpty();
        }
    }

    public void undoCommand()
    {
        if (!canUndoCommand()) {
            throw new IllegalStateException("Can't undo command");
        }
        synchronized (commands) {
            CommandInfo lastCommand = commands.remove(commands.size() - 1);
            if (!lastCommand.command.isUndoable()) {
                throw new IllegalStateException("Last executed command is not undoable");
            }
            // Undo UI changes and put command in undid command stack
            if (lastCommand.reflector != null) {
                lastCommand.reflector.undoCommand(lastCommand.command);
            }
            undidCommands.add(lastCommand);
            clearMergedCommands();
        }
    }

    public void redoCommand()
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

        final Map<DBECommand, CommandInfo> mergedByMap = new IdentityHashMap<DBECommand, CommandInfo>();
        final Map<String, Object> userParams = new HashMap<String, Object>();
        for (int i = 0; i < commands.size(); i++) {
            CommandInfo lastCommand = commands.get(i);
            lastCommand.mergedBy = null;
            CommandInfo firstCommand = null;
            DBECommand<?> result = lastCommand.command;
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
        if (!mergedCommands.isEmpty()) {
            // Add special commands
            filterCommands(mergedCommands);
        }
        return mergedCommands;
    }

    private void clearMergedCommands()
    {
        mergedCommands = null;
    }

    protected DBCExecutionContext openCommandPersistContext(
        DBRProgressMonitor monitor,
        DBPDataSource dataSource,
        DBECommand<?> command)
        throws DBException
    {
        return dataSource.openContext(
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
        Map<DBSObject, CommandQueue> queueMap = new LinkedHashMap<DBSObject, CommandQueue>();
        //new ArrayList<DBECommand<?>>(mergedCommands.size());
        for (CommandInfo commandInfo : mergedCommands) {
            DBSObject object = commandInfo.command.getObject();
            CommandQueue queue = queueMap.get(object);
            if (queue == null) {
                queue = new CommandQueue(object);
                queueMap.put(object, queue);
            }
            queue.addCommand(commandInfo);
        }
        // Filter queues
        boolean queuesFiltered = false;
        for (CommandQueue queue : queueMap.values()) {
            DBSObject object = queue.getObject();
            DBEObjectManager objectManager = getObjectManager(object);
            if (objectManager instanceof DBECommandFilter) {
                ((DBECommandFilter) objectManager).filterCommands(queue);
                if (queue.modified) {
                    queuesFiltered = true;
                }
            }
        }

        if (queuesFiltered) {
            mergedCommands.clear();
            for (CommandQueue queue : queueMap.values()) {
                mergedCommands.addAll(queue.commands);
            }
        }
    }

    private static class CommandQueue extends AbstractCollection<DBECommand<DBSObject>> implements DBECommandQueue<DBSObject> {
        private final DBSObject object;
        private final List<CommandInfo> commands = new ArrayList<CommandInfo>();
        private boolean modified = false;

        private CommandQueue(DBSObject object)
        {
            this.object = object;
        }

        void addCommand(CommandInfo info)
        {
            commands.add(info);
        }

        public DBSObject getObject()
        {
            return object;
        }

        public boolean add(DBECommand dbeCommand)
        {
            if (commands.add(new CommandInfo(dbeCommand, null))) {
                modified = true;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Iterator<DBECommand<DBSObject>> iterator()
        {
            return new Iterator<DBECommand<DBSObject>>() {
                private int index = -1;
                public boolean hasNext()
                {
                    return index <= commands.size() - 1;
                }

                public DBECommand<DBSObject> next()
                {
                    index++;
                    return (DBECommand<DBSObject>) commands.get(index).command;
                }

                public void remove()
                {
                    commands.remove(index);
                    modified = true;
                }
            };
        }

        @Override
        public int size()
        {
            return commands.size();
        }
    }

}