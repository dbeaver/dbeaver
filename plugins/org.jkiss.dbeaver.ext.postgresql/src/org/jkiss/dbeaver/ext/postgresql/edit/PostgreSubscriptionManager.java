/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.internal.PostgreSQLMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostgreSubscriptionManager extends SQLObjectEditor<PostgreSubscription, PostgreDatabase> {
    private static final String PROP_CONNECTION_INFO = "connectionInfo";
    private static final String PROP_PUBLICATIONS = "publications";
    private static final String PROP_SLOT_NAME = "slotName";
    private static final String PROP_ENABLED = "enabled";
    private static final String PROP_BINARY = "binary";
    private static final String PROP_STREAMING = "streaming";
    private static final String PROP_SYNCHRONOUS_COMMIT = "synchronousCommit";

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @NotNull
    @Override
    public DBSObjectCache<PostgreDatabase, PostgreSubscription> getObjectsCache(@NotNull PostgreSubscription object) {
        return object.getDatabase().getSubscriptionCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return container instanceof PostgreDatabase database && database.getDataSource().getServerType().supportsLogicalReplication()
            && super.canCreateObject(container);
    }

    @Override
    public boolean canEditObject(@NotNull PostgreSubscription object) {
        return object.getDataSource().getServerType().supportsLogicalReplication() && super.canEditObject(object);
    }

    @NotNull
    @Override
    protected PostgreSubscription createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, @NotNull Object container,
        @Nullable Object copyFrom, @NotNull Map<String, Object> options
    ) {
        PostgreSubscription subscription = new PostgreSubscription((PostgreDatabase) container, "new_subscription");
        if (copyFrom instanceof PostgreSubscription configured) {
            // The modeless configurator submits its completed form through the normal create/save command lifecycle.
            subscription.setName(configured.getName());
            subscription.setConnectionInfo(configured.getConnectionInfo());
            subscription.setPublications(configured.getPublications());
            subscription.setSlotName(configured.getSlotName());
            subscription.setSynchronousCommit(configured.getSynchronousCommit());
            subscription.setEnabled(configured.isEnabled());
            subscription.setBinary(configured.isBinary());
            subscription.setStreaming(configured.isStreaming());
            subscription.setConnect(configured.isConnect());
            subscription.setCreateSlot(configured.isCreateSlot());
            subscription.setCopyData(configured.isCopyData());
        }
        return subscription;
    }

    @Override
    protected void validateObjectProperties(
        @NotNull DBRProgressMonitor monitor, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options
    ) throws DBException {
        if (command.getObject().getName().isBlank()) {
            throw new DBException("Subscription name cannot be empty");
        }
        PostgreSubscription subscription = command.getObject();
        if (!subscription.isPersisted()) {
            subscription.getCreateStatement();
            return;
        }
        if (command.hasProperty(PROP_CONNECTION_INFO) && subscription.getConnectionInfo().isBlank()) {
            throw new DBException("Enter a replacement publisher connection string, or undo the connection change");
        }
        if (command.hasProperty(PROP_PUBLICATIONS)) {
            subscription.getPublicationNamesSQL();
        }
        if (command.hasProperty(PROP_SYNCHRONOUS_COMMIT) && !PostgreSubscription.isValidSynchronousCommit(subscription.getSynchronousCommit())) {
            throw new DBException("Invalid synchronous commit mode");
        }
        if (command.hasProperty(PROP_SLOT_NAME) && CommonUtils.isEmpty(subscription.getSlotName()) && subscription.isEnabled()) {
            throw new DBException("Disable the subscription before removing its replication slot association");
        }
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options
    ) throws DBException {
        command.setDisableSessionLogging(true); // Publisher connection strings may contain passwords.
        actions.add(new SQLDatabasePersistActionAtomic(
            PostgreSQLMessages.action_create_subscription, command.getObject().getCreateStatement()
        ));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options
    ) throws DBException {
        PostgreSubscription subscription = command.getObject();
        String prefix = "ALTER SUBSCRIPTION " + DBUtils.getQuotedIdentifier(subscription);
        String title = PostgreSQLMessages.action_alter_subscription;
        // Stop replication before changing settings; start it only after all changes have been applied.
        if (command.hasProperty(PROP_ENABLED) && !subscription.isEnabled()) {
            actions.add(new SQLDatabasePersistAction(title, prefix + " DISABLE"));
        }
        if (command.hasProperty(PROP_CONNECTION_INFO)) {
            command.setDisableSessionLogging(true); // Hide replacement credentials from Query Manager.
            actions.add(new SQLDatabasePersistAction(title, prefix + " CONNECTION " + SQLUtils.quoteString(subscription, subscription.getConnectionInfo())));
        }
        List<String> parameters = new ArrayList<>();
        if (command.hasProperty(PROP_SLOT_NAME)) {
            parameters.add("slot_name = " + (CommonUtils.isEmpty(subscription.getSlotName())
                ? "NONE" : SQLUtils.quoteString(subscription, subscription.getSlotName())));
        }
        if (command.hasProperty(PROP_BINARY)) {
            parameters.add("binary = " + subscription.isBinary());
        }
        if (command.hasProperty(PROP_STREAMING)) {
            parameters.add("streaming = " + subscription.isStreaming());
        }
        if (command.hasProperty(PROP_SYNCHRONOUS_COMMIT)) {
            parameters.add("synchronous_commit = " + SQLUtils.quoteString(subscription, subscription.getSynchronousCommit()));
        }
        if (!parameters.isEmpty()) {
            actions.add(new SQLDatabasePersistAction(title, prefix + " SET (" + String.join(", ", parameters) + ")"));
        }
        if (command.hasProperty(PROP_PUBLICATIONS)) {
            // Do not implicitly copy new tables or contact the publisher while editing metadata.
            // REFRESH PUBLICATION is a separate operation, outside a transaction block, once tables are ready.
            actions.add(new SQLDatabasePersistAction(title,
                prefix + " SET PUBLICATION " + subscription.getPublicationNamesSQL() + " WITH (refresh = false)"));
        }
        if (command.hasProperty(PROP_ENABLED) && subscription.isEnabled()) {
            actions.add(new SQLDatabasePersistAction(title, prefix + " ENABLE"));
        }
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistActionAtomic(
            PostgreSQLMessages.action_drop_subscription, "DROP SUBSCRIPTION " + DBUtils.getQuotedIdentifier(command.getObject())
        ));
    }

    @Override
    public void deleteObject(
        @NotNull DBECommandContext commandContext, @NotNull PostgreSubscription subscription, @NotNull Map<String, Object> options
    ) {
        commandContext.addCommand(new ObjectDeleteCommand(subscription, ModelMessages.model_jdbc_delete_object) {
            @Override
            public void validateCommand(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
                super.validateCommand(monitor, options);
                options.put(DBECommandContext.OPTION_ISOLATED_EXECUTION, true);
            }
        }, new DeleteObjectReflector<>(this), true);
    }

    @Override
    public void executePersistAction(
        @NotNull DBCSession session, @NotNull DBECommand<PostgreSubscription> command, @NotNull DBEPersistAction action
    ) throws DBException {
        try {
            super.executePersistAction(session, command, action);
        } catch (DBException e) {
            if (!command.isDisableSessionLogging()) {
                throw e;
            }
            // libpq syntax errors can echo password tokens. Never propagate their message, SQL, or exception chain.
            String state = SQLState.getStateFromException(e);
            String diagnostic = state != null && state.matches("[0-9A-Z]{5}") ? " (SQL state " + state + ")" : "";
            throw new DBException("Subscription command failed" + diagnostic
                + ". Check the publisher connection string, subscription privileges, and matching subscriber tables");
        }
    }
}
