/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DBECommandContextImpl
 */
public abstract class AbstractCommandContext implements DBECommandContext {

    private static final Log log = Log.getLog(AbstractCommandContext.class);

    private final DBCExecutionContext executionContext;
    private final List<CommandInfo> commands = new ArrayList<>();
    private final List<CommandInfo> undidCommands = new ArrayList<>();
    private List<CommandQueue> commandQueues;

    private final Map<Object, Object> userParams = new HashMap<>();
    private final List<DBECommandListener> listeners = new ArrayList<>();

    private final boolean atomic;

    /**
     * Creates new context
     * @param executionContext Execution context
     * @param atomic atomic context reflect commands in UI only after all commands were executed. Non-atomic
     *               reflects each command at the moment it executed
     */
    public AbstractCommandContext(DBCExecutionContext executionContext, boolean atomic)
    {
        this.executionContext = executionContext;
        this.atomic = atomic;
    }

    @Override
    public DBCExecutionContext getExecutionContext()
    {
        return executionContext;
    }

    @Override
    public boolean isDirty()
    {
        synchronized (commands) {
            return !getCommandQueues().isEmpty();
        }
    }

    @Override
    public void saveChanges(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (!executionContext.isConnected()) {
            throw new DBException("Context [" + executionContext.getContextName() + "] isn't connected to the database");
        }

        // Execute commands in transaction
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
        boolean oldAutoCommit = false;
        if (txnManager != null) {
            oldAutoCommit = txnManager.isAutoCommit();
            if (oldAutoCommit) {
                try {
                    txnManager.setAutoCommit(monitor, false);
                } catch (DBCException e) {
                    log.warn("Can't switch to transaction mode", e);
                }
            }
        }
        try {
            executeCommands(monitor, options);

            // Commit changes
            if (txnManager != null) {
                try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Commit script transaction")) {
                    txnManager.commit(session);
                } catch (DBCException e1) {
                    log.warn("Can't commit script transaction", e1);
                }
            }
        } catch (Throwable e) {
            // Rollback changes
            if (txnManager != null) {
                try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Rollback script transaction")) {
                    txnManager.rollback(session, null);
                } catch (DBCException e1) {
                    log.warn("Can't rollback transaction after error", e);
                }
            }
            throw e;
        } finally {
            if (txnManager != null && oldAutoCommit) {
                try {
                    txnManager.setAutoCommit(monitor, true);
                } catch (DBCException e) {
                    log.warn("Can't switch back to auto-commit mode", e);
                }
            }
        }
    }

    private void executeCommands(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        List<CommandQueue> commandQueues = getCommandQueues();

        // Validate commands
        for (CommandQueue queue : commandQueues) {
            for (CommandInfo cmd : queue.commands) {
                cmd.command.validateCommand();
            }
        }

        // Execute commands
        List<CommandInfo> executedCommands = new ArrayList<>();
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
                            DBEPersistAction[] persistActions = cmd.command.getPersistActions(options);
                            if (!ArrayUtils.isEmpty(persistActions)) {
                                cmd.persistActions = new ArrayList<>(persistActions.length);
                                for (DBEPersistAction action : persistActions) {
                                    cmd.persistActions.add(new PersistInfo(action));
                                }
                            }
                        //}
                        if (!CommonUtils.isEmpty(cmd.persistActions)) {
                            try (DBCSession session = openCommandPersistContext(monitor, cmd.command)) {
                                DBException error = null;
                                for (PersistInfo persistInfo : cmd.persistActions) {
                                    DBEPersistAction.ActionType actionType = persistInfo.action.getType();
                                    if (persistInfo.executed && actionType == DBEPersistAction.ActionType.NORMAL) {
                                        continue;
                                    }
                                    if (monitor.isCanceled()) {
                                        break;
                                    }
                                    try {
                                        if (error == null || actionType == DBEPersistAction.ActionType.FINALIZER) {
                                            queue.objectManager.executePersistAction(session, cmd.command, persistInfo.action);
                                        }
                                        persistInfo.executed = true;
                                    } catch (DBException e) {
                                        persistInfo.error = e;
                                        persistInfo.executed = false;
                                        if (actionType != DBEPersistAction.ActionType.OPTIONAL) {
                                            error = e;
                                        }
                                    }
                                }
                                if (error != null) {
                                    throw error;
                                }
                            }
                            cmd.executed = true;
                        }
                    }
                    if (cmd.executed) {
                        // Remove only executed commands
                        // Commands which do not perform any persist actions
                        // should remain - they constructs queue by merging with other commands
                        synchronized (commands) {
                            // Remove original command from stack
                            //final CommandInfo thisCommand = queue.commands.get(i);
                            commands.remove(cmd);
                        }
                    }
                    if (!executedCommands.contains(cmd)) {
                        executedCommands.add(cmd);
                    }
                }
            }

            // Let's clear commands
            // If everything went well then there should be nothing to do else.
            // But some commands may still remain in queue if they merged each other
            // (e.g. create + delete of the same entity produce 2 commands and zero actions).
            // There were no exceptions during save so we assume that everything went well
            commands.clear();
            userParams.clear();

/*
            // Refresh object states
            for (CommandQueue queue : commandQueues) {
                if (queue.getObject() instanceof DBPStatefulObject) {
                    try {
                        ((DBPStatefulObject) queue.getObject()).refreshObjectState(monitor);
                    } catch (DBCException e) {
                        // Just report an error
                        log.error(e);
                    }
                }
            }
*/
        }
        finally {
            try {
                // Update UI
                if (atomic) {
                    for (CommandInfo cmd : executedCommands) {
                        if (cmd.reflector != null) {
                            cmd.reflector.redoCommand(cmd.command);
                        }
                    }
                }

                // Update model
                for (CommandInfo cmd : executedCommands) {
                    cmd.command.updateModel();
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

    @Override
    public void resetChanges()
    {
        synchronized (commands) {
            try {
                while (!commands.isEmpty()) {
                    undoCommand();
                }
                clearUndidCommands();
                clearCommandQueues();

                commands.clear();
                userParams.clear();
            } finally {
                for (DBECommandListener listener : getListeners()) {
                    listener.onReset();
                }
            }
        }
    }

    @Override
    public Collection<? extends DBECommand<?>> getFinalCommands()
    {
        synchronized (commands) {
            List<DBECommand<?>> cmdCopy = new ArrayList<>(commands.size());
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

    @Override
    public Collection<? extends DBECommand<?>> getUndoCommands()
    {
        synchronized (commands) {
            List<DBECommand<?>> result = new ArrayList<>();
            for (int i = commands.size() - 1; i >= 0; i--) {
                CommandInfo cmd = commands.get(i);
                while (cmd.prevInBatch != null) {
                    cmd = cmd.prevInBatch;
                    i--;
                }
                if (!cmd.command.isUndoable()) {
                    break;
                }
                result.add(cmd.command);
            }
            return result;
        }
    }

    @Override
    public Collection<DBPObject> getEditedObjects()
    {
        final List<CommandQueue> queues = getCommandQueues();
        List<DBPObject> result = new ArrayList<>(queues.size());
        for (CommandQueue queue : queues) {
            result.add(queue.getObject());
        }
        return result;
    }

    @Override
    public void addCommand(
        DBECommand command,
        DBECommandReflector reflector)
    {
        addCommand(command, reflector, false);
    }

    @Override
    public void addCommand(DBECommand command, DBECommandReflector reflector, boolean execute)
    {
        synchronized (commands) {
            commands.add(new CommandInfo(command, reflector));

            clearUndidCommands();
            clearCommandQueues();
        }
        fireCommandChange(command);
        if (execute && reflector != null && !atomic) {
            reflector.redoCommand(command);
        }
        refreshCommandState();
    }

/*
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
*/

    @Override
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

    @Override
    public void updateCommand(DBECommand<?> command, DBECommandReflector commandReflector)
    {
        synchronized (commands) {
            boolean found = false;
            for (CommandInfo cmd : commands) {
                if (cmd.command == command) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Actually it is a new command
                addCommand(command, commandReflector);
            } else {
                clearUndidCommands();
                clearCommandQueues();
            }
        }
        fireCommandChange(command);
    }

    @Override
    public void addCommandListener(DBECommandListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeCommandListener(DBECommandListener listener)
    {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void undoCommand()
    {
        if (getUndoCommand() == null) {
            throw new IllegalStateException("Can't undo command");
        }
        List<CommandInfo> processedCommands = new ArrayList<>();
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

        // Undo UI changes (always because undo doesn't make sense in atomic contexts)
        for (CommandInfo cmd : processedCommands) {
            if (cmd.reflector != null && !atomic) {
                cmd.reflector.undoCommand(cmd.command);
            }
        }
    }

    @Override
    public void redoCommand()
    {
        if (getRedoCommand() == null) {
            throw new IllegalStateException("Can't redo command");
        }
        List<CommandInfo> processedCommands = new ArrayList<>();
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

        // Redo UI changes (always because redo doesn't make sense in atomic contexts)
        for (CommandInfo cmd : processedCommands) {
            if (cmd.reflector != null) {
                cmd.reflector.redoCommand(cmd.command);
            }
        }

        refreshCommandState();
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
        commandQueues = new ArrayList<>();

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
                DBEObjectManager<?> objectManager = executionContext.getDataSource().getContainer().getPlatform().getEditorsRegistry().getObjectManager(object.getClass());
                if (objectManager == null) {
                    throw new IllegalStateException("Can't find object manager for '" + object.getClass().getName() + "'");
                }
                queue = new CommandQueue(objectManager, null, object);
                commandQueues.add(queue);
            }
            queue.addCommand(commandInfo);
        }

        // Merge commands
        for (CommandQueue queue : commandQueues) {
            final Map<DBECommand, CommandInfo> mergedByMap = new IdentityHashMap<>();
            final List<CommandInfo> mergedCommands = new ArrayList<>();
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
            ((DBECommandAggregator)aggregator.command).resetAggregatedCommands();
            for (CommandQueue queue : commandQueues) {
                for (CommandInfo cmd : queue.commands) {
                    if (cmd.command != aggregator.command && cmd.mergedBy == null && ((DBECommandAggregator)aggregator.command).aggregateCommand(cmd.command)) {
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

    protected DBCSession openCommandPersistContext(
        DBRProgressMonitor monitor,
        DBECommand<?> command)
        throws DBException
    {
        return executionContext.openSession(
            monitor,
            DBCExecutionPurpose.META_DDL,
            ModelMessages.model_edit_execute_ + command.getTitle());
    }

    protected void refreshCommandState()
    {
    }

    private static class PersistInfo {
        final DBEPersistAction action;
        boolean executed = false;
        Throwable error;

        public PersistInfo(DBEPersistAction action)
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
        private List<CommandInfo> commands = new ArrayList<>();

        private CommandQueue(DBEObjectManager objectManager, CommandQueue parent, DBPObject object)
        {
            this.parent = parent;
            this.object = object;
            this.objectManager = objectManager;
            if (parent != null) {
                parent.addSubQueue(this);
            }
        }

        void addSubQueue(CommandQueue queue)
        {
            if (subQueues == null) {
                subQueues = new ArrayList<>();
            }
            subQueues.add(queue);
        }

        void addCommand(CommandInfo info)
        {
            commands.add(info);
        }

        @Override
        public DBPObject getObject()
        {
            return object;
        }

        @Override
        public DBECommandQueue getParentQueue()
        {
            return parent;
        }

        @Override
        public Collection<DBECommandQueue> getSubQueues()
        {
            return subQueues;
        }

        @Override
        public boolean add(DBECommand dbeCommand)
        {
            return commands.add(new CommandInfo(dbeCommand, null));
        }

        @Override
        public Iterator<DBECommand<DBPObject>> iterator()
        {
            return new Iterator<DBECommand<DBPObject>>() {
                private int index = -1;
                @Override
                public boolean hasNext()
                {
                    return index < commands.size() - 1;
                }

                @Override
                public DBECommand<DBPObject> next()
                {
                    index++;
                    return (DBECommand<DBPObject>) commands.get(index).command;
                }

                @Override
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