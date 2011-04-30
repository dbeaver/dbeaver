/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.*;

/**
 * DBECommandContextImpl
 */
public class DBECommandContextImpl implements DBECommandContext {

    private final DBSDataSourceContainer dataSourceContainer;
    private final List<CommandInfo> commands = new ArrayList<CommandInfo>();
    private final List<CommandInfo> undidCommands = new ArrayList<CommandInfo>();
    //private List<CommandInfo> mergedCommands = null;
    private List<CommandQueue> commandQueues;

    private final Map<Object, Object> userParams = new HashMap<Object, Object>();
    private final List<DBECommandListener> listeners = new ArrayList<DBECommandListener>();

    public DBECommandContextImpl(DBSDataSourceContainer dataSourceContainer)
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
            return !getCommandQueues().isEmpty();
        }
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        if (!dataSourceContainer.isConnected()) {
            throw new DBException("Not connected to database");
        }
        synchronized (commands) {
            List<CommandQueue> commandQueues = getCommandQueues();

            // Validate commands
            for (CommandQueue queue : commandQueues) {
                for (CommandInfo cmd : queue.commands) {
                    cmd.command.validateCommand();
                }
                try {
                    // Make list of not-executed commands
                    for (int i = 0; i < queue.commands.size(); i++) {
                        if (monitor.isCanceled()) {
                            break;
                        }

                        CommandInfo cmd = queue.commands.get(i);
                        if (cmd.mergedBy != null) {
                            cmd = cmd.mergedBy;
                        }
                        if (!cmd.executed) {
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
                                            queue.objectManager.executePersistAction(context, cmd.command, persistInfo.action);
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
                        }
                        // Remove original command from stack
                        commands.remove(queue.commands.get(i));
                    }
                }
                finally {
                    clearCommandQueues();
                    clearUndidCommands();

                    for (DBECommandListener listener : getListeners()) {
                        listener.onSave();
                    }
                }
            }
        }
    }

    public void resetChanges()
    {
        synchronized (commands) {
            try {
                while (!commands.isEmpty()) {
                    undoCommand();
                }
                clearUndidCommands();
                clearCommandQueues();
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
            for (CommandQueue queue : getCommandQueues()) {
                for (CommandInfo cmdInfo : queue.commands) {
                    while (cmdInfo.mergedBy != null) {
                        cmdInfo = cmdInfo.mergedBy;
                    }
                    if (!cmdCopy.contains(cmdInfo.command)) {
                        cmdCopy.add(cmdInfo.command);
                    }
                }
            }
            return cmdCopy;
        }
    }

    public Collection<DBPObject> getEditedObjects()
    {
        final List<CommandQueue> queues = getCommandQueues();
        List<DBPObject> result = new ArrayList<DBPObject>(queues.size());
        for (CommandQueue queue : queues) {
            result.add(queue.getObject());
        }
        return result;
    }

    public void addCommand(
        DBECommand<?> command,
        DBECommandReflector<?, DBECommand<?>> reflector)
    {
        synchronized (commands) {
            commands.add(new CommandInfo(command, reflector));

            clearUndidCommands();
            clearCommandQueues();
        }
        fireCommandChange(command);
    }

    public void addCommandBatch(List<DBECommand> commandBatch)
    {
        synchronized (commands) {
            for (DBECommand command : commandBatch) {
                commands.add(new CommandInfo(command, null));
            }
            clearUndidCommands();
            clearCommandQueues();
        }

        if (!commandBatch.isEmpty()) {
            // Fire only single event
            fireCommandChange(commandBatch.get(0));
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
            clearCommandQueues();
        }
        fireCommandChange(command);
    }

    public void updateCommand(DBECommand<?> command)
    {
        synchronized (commands) {
            clearUndidCommands();
            clearCommandQueues();
        }
        fireCommandChange(command);
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

    public Map<Object, Object> getUserParams()
    {
        return userParams;
    }

    private void fireCommandChange(DBECommand<?> command)
    {
        for (DBECommandListener listener : getListeners()) {
            listener.onCommandChange(command);
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
            clearCommandQueues();
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
            clearCommandQueues();
        }
    }

    private void clearUndidCommands()
    {
        undidCommands.clear();
    }

    private List<CommandQueue> getCommandQueues()
    {
        if (commandQueues != null) {
            return commandQueues;
        }
        commandQueues = new ArrayList<CommandQueue>();

        CommandInfo aggregator = null;
        // Create queues from commands
        for (CommandInfo commandInfo : commands) {
            if (commandInfo.command instanceof DBECommandAggregator) {
                aggregator = commandInfo;
            }
            DBPObject object = commandInfo.command.getObject();
            CommandQueue queue = null;
            if (!commandQueues.isEmpty()) {
                for (CommandQueue tmpQueue : commandQueues) {
                    if (tmpQueue.getObject() == object) {
                        queue = tmpQueue;
                        break;
                    }
                }
            }
            if (queue == null) {
                queue = new CommandQueue(null, object);
                commandQueues.add(queue);
            }
            queue.addCommand(commandInfo);
        }

        // Merge commands
        for (CommandQueue queue : commandQueues) {
            final Map<DBECommand, CommandInfo> mergedByMap = new IdentityHashMap<DBECommand, CommandInfo>();
            final List<CommandInfo> mergedCommands = new ArrayList<CommandInfo>();
            for (int i = 0; i < queue.commands.size(); i++) {
                CommandInfo lastCommand = queue.commands.get(i);
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
                    commands.remove(firstCommand);
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
                            if (queue.commands.get(k).command == result) {
                                mergedBy = queue.commands.get(k);
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
            queue.commands = mergedCommands;
        }

        // Filter commands
        for (CommandQueue queue : commandQueues) {
            if (queue.objectManager instanceof DBECommandFilter) {
                ((DBECommandFilter) queue.objectManager).filterCommands(queue);
            }
        }

        // Aggregate commands
        if (aggregator != null) {
            for (CommandQueue queue : commandQueues) {
                for (CommandInfo cmd : queue.commands) {
                    if (cmd.mergedBy == null && ((DBECommandAggregator)aggregator.command).aggregateCommand(cmd.command)) {
                        cmd.mergedBy = aggregator;
                    }
                }
            }
        }

        return commandQueues;
    }

    private void clearCommandQueues()
    {
        commandQueues = null;
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

    private static class CommandQueue extends AbstractCollection<DBECommand<DBPObject>> implements DBECommandQueue<DBPObject> {
        private final CommandQueue parent;
        private List<DBECommandQueue> subQueues;
        private final DBPObject object;
        private final DBEObjectManager objectManager;
        private List<CommandInfo> commands = new ArrayList<CommandInfo>();

        private CommandQueue(CommandQueue parent, DBPObject object)
        {
            this.parent = parent;
            this.object = object;
            this.objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(object.getClass());
            if (this.objectManager == null) {
                throw new IllegalStateException("Can't find object manager for '" + object.getClass().getName() + "'");
            }
            if (parent != null) {
                parent.addSubQueue(this);
            }
        }

        void addSubQueue(CommandQueue queue)
        {
            if (subQueues == null) {
                subQueues = new ArrayList<DBECommandQueue>();
            }
            subQueues.add(queue);
        }

        void addCommand(CommandInfo info)
        {
            commands.add(info);
        }

        public DBPObject getObject()
        {
            return object;
        }

        public DBECommandQueue getParentQueue()
        {
            return parent;
        }

        public Collection<DBECommandQueue> getSubQueues()
        {
            return subQueues;
        }

        public boolean add(DBECommand dbeCommand)
        {
            return commands.add(new CommandInfo(dbeCommand, null));
        }

        @Override
        public Iterator<DBECommand<DBPObject>> iterator()
        {
            return new Iterator<DBECommand<DBPObject>>() {
                private int index = -1;
                public boolean hasNext()
                {
                    return index < commands.size() - 1;
                }

                public DBECommand<DBPObject> next()
                {
                    index++;
                    return (DBECommand<DBPObject>) commands.get(index).command;
                }

                public void remove()
                {
                    commands.remove(index);
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