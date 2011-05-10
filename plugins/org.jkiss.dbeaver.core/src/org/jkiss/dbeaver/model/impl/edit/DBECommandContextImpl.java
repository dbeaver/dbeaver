/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.commands.ICommandService;
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

    static final Log log = LogFactory.getLog(DBECommandContextImpl.class);

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
        List<CommandQueue> commandQueues = getCommandQueues();

        // Validate commands
        for (CommandQueue queue : commandQueues) {
            for (CommandInfo cmd : queue.commands) {
                cmd.command.validateCommand();
            }
        }

        // Execute commands
        List<DBECommand> executedCommands = new ArrayList<DBECommand>();
        try {
            for (CommandQueue queue : commandQueues) {
                // Make list of not-executed commands
                for (int i = 0; i < queue.commands.size(); i++) {
                    if (monitor.isCanceled()) {
                        break;
                    }

                    CommandInfo cmd = queue.commands.get(i);
                    while (cmd.mergedBy != null) {
                        cmd = cmd.mergedBy;
                    }
                    if (!cmd.executed) {
                        // Persist changes
                        //if (CommonUtils.isEmpty(cmd.persistActions)) {
                            IDatabasePersistAction[] persistActions = cmd.command.getPersistActions();
                            if (!CommonUtils.isEmpty(persistActions)) {
                                cmd.persistActions = new ArrayList<PersistInfo>(persistActions.length);
                                for (IDatabasePersistAction action : persistActions) {
                                    cmd.persistActions.add(new PersistInfo(action));
                                }
                            }
                        //}
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

                        cmd.executed = true;
                    }
                    synchronized (commands) {
                        // Remove original command from stack
                        final CommandInfo thisCommand = queue.commands.get(i);
                        executedCommands.add(thisCommand.command);
                        commands.remove(thisCommand);
                    }
                }
            }

            // Let's clear commands
            // If everything went well then there should be nothing to do else.
            // But some commands may still remain in queue if they merged each other
            // (e.g. create + delete of the same entity produce 2 commands and zero actions).
            // There were no exceptions during save so we assume that everything went well
            commands.clear();
        }
        finally {
            try {
                // Update model
                for (DBECommand cmd : executedCommands) {
                    cmd.updateModel();
                }
            } catch (Exception e) {
                log.warn("Error updating model", e);
            }

            clearCommandQueues();
            clearUndidCommands();

            // Notify listeners
            for (DBECommandListener listener : getListeners()) {
                listener.onSave();
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

    public Collection<? extends DBECommand<?>> getFinalCommands()
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

    public Collection<? extends DBECommand<?>> getCommands(DBPObject object)
    {
        for (CommandQueue queue : getCommandQueues()) {
            if (queue.getObject() == object) {
                //return queue.commands;
            }
        }
        return null;
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
        DBECommand command,
        DBECommandReflector reflector)
    {
        addCommand(command, reflector, false);
    }

    public void addCommand(DBECommand command, DBECommandReflector reflector, boolean execute)
    {
        synchronized (commands) {
            commands.add(new CommandInfo(command, reflector));

            clearUndidCommands();
            clearCommandQueues();
        }
        fireCommandChange(command);
        if (execute && reflector != null) {
            reflector.redoCommand(command);
        }
        refreshCommandState();
    }

    public void addCommandBatch(List<DBECommand> commandBatch, DBECommandReflector reflector, boolean execute)
    {
        if (commandBatch.isEmpty()) {
            return;
        }

        synchronized (commands) {
            CommandInfo prevInfo = null;
            for (int i = 0, commandBatchSize = commandBatch.size(); i < commandBatchSize; i++) {
                DBECommand command = commandBatch.get(i);
                final CommandInfo info = new CommandInfo(command, i == 0 ? reflector : null);
                info.prevInBatch = prevInfo;
                commands.add(info);
                prevInfo = info;
            }
            clearUndidCommands();
            clearCommandQueues();
        }

        // Fire only single event
        fireCommandChange(commandBatch.get(0));
        if (execute && reflector != null) {
            reflector.redoCommand(commandBatch.get(0));
        }
        refreshCommandState();
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

    public DBECommand getUndoCommand()
    {
        synchronized (commands) {
            if (!commands.isEmpty()) {
                CommandInfo cmd = commands.get(commands.size() - 1);
                while (cmd.prevInBatch != null) {
                    cmd = cmd.prevInBatch;
                }
                if (cmd.command.isUndoable()) {
                    return cmd.command;
                }
            }
            return null;
        }
    }

    public DBECommand getRedoCommand()
    {
        synchronized (commands) {
            if (!undidCommands.isEmpty()) {
                CommandInfo cmd = undidCommands.get(undidCommands.size() - 1);
                while (cmd.prevInBatch != null) {
                    cmd = cmd.prevInBatch;
                }
                return cmd.command;
            }
            return null;
        }
    }

    public void undoCommand()
    {
        if (getUndoCommand() == null) {
            throw new IllegalStateException("Can't undo command");
        }
        List<CommandInfo> processedCommands = new ArrayList<CommandInfo>();
        synchronized (commands) {
            CommandInfo lastCommand = commands.get(commands.size() - 1);
            if (!lastCommand.command.isUndoable()) {
                throw new IllegalStateException("Last executed command is not undoable");
            }
            // Undo command batch
            while (lastCommand != null) {
                commands.remove(lastCommand);
                undidCommands.add(lastCommand);
                processedCommands.add(lastCommand);
                lastCommand = lastCommand.prevInBatch;
            }
            clearCommandQueues();
            getCommandQueues();
        }
        refreshCommandState();

        // Undo UI changes
        for (CommandInfo cmd : processedCommands) {
            if (cmd.reflector != null) {
                cmd.reflector.undoCommand(cmd.command);
            }
        }
    }

    public void redoCommand()
    {
        if (getRedoCommand() == null) {
            throw new IllegalStateException("Can't redo command");
        }
        List<CommandInfo> processedCommands = new ArrayList<CommandInfo>();
        synchronized (commands) {
            // Just redo UI changes and put command on the top of stack
            CommandInfo commandInfo = null;
            // Redo batch
            while (!undidCommands.isEmpty() &&
                (commandInfo == null || undidCommands.get(undidCommands.size() - 1).prevInBatch == commandInfo))
            {
                commandInfo = undidCommands.remove(undidCommands.size() - 1);
                commands.add(commandInfo);
                processedCommands.add(commandInfo);
            }
            clearCommandQueues();
            getCommandQueues();
        }
        refreshCommandState();

        // Redo UI changes
        for (CommandInfo cmd : processedCommands) {
            if (cmd.reflector != null) {
                cmd.reflector.redoCommand(cmd.command);
            }
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
                    boolean skipCommand = false;
                    for (int k = mergedCommands.size(); k > 0; k--) {
                        firstCommand = mergedCommands.get(k - 1);
                        result = lastCommand.command.merge(firstCommand.command, userParams);
                        if (result == null) {
                            // Remove first and skip last command
                            mergedCommands.remove(firstCommand);
                            skipCommand = true;
                        } else if (result != lastCommand.command) {
                            break;
                        }
                    }
                    if (skipCommand) {
                        continue;
                    }
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

    private void refreshCommandState()
    {
        ICommandService commandService = (ICommandService) DBeaverCore.getActiveWorkbenchWindow().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(IWorkbenchCommandConstants.EDIT_UNDO, null);
            commandService.refreshElements(IWorkbenchCommandConstants.EDIT_REDO, null);
        }
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
        CommandInfo prevInBatch = null;
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