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
package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreSubscriptionManager;
import org.jkiss.dbeaver.ext.postgresql.internal.PostgreSQLMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreExecutionContext;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.ext.postgresql.ui.PostgreCreateSubscriptionDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SimpleCommandContext;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.HashMap;
import java.util.Map;

public class PostgreSubscriptionConfigurator implements DBEObjectConfigurator<PostgreSubscription> {
    @Nullable
    @Override
    public PostgreSubscription configureObject(
        @NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object parent,
        @NotNull PostgreSubscription subscription, @NotNull Map<String, Object> options
    ) {
        // Leave the navigator's synchronous progress operation before opening the modeless dialog.
        // It must not retain or save the active editor's command context while the user works elsewhere.
        Map<String, Object> creationOptions = new HashMap<>(options);
        creationOptions.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        UIUtils.asyncExec(() -> new PostgreCreateSubscriptionDialog(UIUtils.getActiveWorkbenchShell(), subscription,
            () -> saveSubscription(subscription, creationOptions)).open());
        return null;
    }

    private static void saveSubscription(@NotNull PostgreSubscription subscription, @NotNull Map<String, Object> options) {
        AbstractJob job = new AbstractJob(PostgreSQLMessages.action_create_subscription) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                // CREATE SUBSCRIPTION needs autocommit; never switch or commit a connection being used elsewhere.
                try (PostgreExecutionContext executionContext = subscription.getDatabase().openIsolatedContext(
                    monitor, PostgreSQLMessages.action_create_subscription, null)) {
                    DBECommandContext context = new SimpleCommandContext(executionContext, true);
                    try {
                        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
                        PostgreSubscription created = manager.createNewObject(
                            monitor, context, subscription.getDatabase(), subscription, new HashMap<>(options));
                        if (created == null || monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        context.saveChanges(monitor, options);
                        DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().findNode(created);
                        if (node != null) {
                            node.refreshNode(monitor, this);
                        }
                        return Status.OK_STATUS;
                    } finally {
                        context.resetChanges(true);
                    }
                } catch (Exception e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
            }
        };
        job.setUser(true);
        job.schedule();
    }
}
