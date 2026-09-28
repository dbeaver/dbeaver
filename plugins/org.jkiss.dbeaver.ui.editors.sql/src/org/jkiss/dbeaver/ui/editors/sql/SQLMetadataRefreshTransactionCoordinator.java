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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.sql.SQLMetadataRefreshTargetResolver.RefreshTarget;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

final class SQLMetadataRefreshTransactionCoordinator {
    private static final String STATE_ATTRIBUTE = SQLMetadataRefreshTransactionCoordinator.class.getName();

    private SQLMetadataRefreshTransactionCoordinator() {
    }

    @NotNull
    static Handling begin(
        @NotNull SQLEditor editor,
        @NotNull DBCExecutionContext context,
        @NotNull DBCSession session
    ) {
        DBCTransactionManager transactionManager = DBUtils.getTransactionManager(context);
        if (transactionManager == null || isAutoCommit(transactionManager)) {
            return new Handling(context, DBCDDLTransactionBehavior.IMMEDIATE, null, false);
        }

        DBCDDLTransactionBehavior behavior = DBCDDLTransactionBehavior.parse(
            editor.getActivePreferenceStore().getString(ModelPreferences.TRANSACTIONS_DDL_BEHAVIOR)
        );
        if (behavior == DBCDDLTransactionBehavior.AUTO) {
            behavior = transactionManager.getDDLTransactionBehavior();
            if (behavior == DBCDDLTransactionBehavior.AUTO) {
                behavior = DBCDDLTransactionBehavior.TRANSACTIONAL;
            }
        }
        if (behavior == DBCDDLTransactionBehavior.TRANSACTIONAL && !transactionManager.supportsTransactionListeners()) {
            behavior = DBCDDLTransactionBehavior.IMMEDIATE;
        }
        if (behavior != DBCDDLTransactionBehavior.TRANSACTIONAL) {
            return new Handling(context, behavior, null, false);
        }

        boolean sameNavigatorContext = context.getOwnerInstance().getDefaultContext(session.getProgressMonitor(), true) == context;
        State state = getOrCreateState(context, transactionManager, sameNavigatorContext);
        return new Handling(context, behavior, state, sameNavigatorContext);
    }

    private static boolean isAutoCommit(@NotNull DBCTransactionManager transactionManager) {
        try {
            return transactionManager.isAutoCommit();
        } catch (DBCException e) {
            return false;
        }
    }

    @NotNull
    private static State getOrCreateState(
        @NotNull DBCExecutionContext context,
        @NotNull DBCTransactionManager transactionManager,
        boolean sameNavigatorContext
    ) {
        synchronized (context) {
            Object attribute = context.getContextAttribute(STATE_ATTRIBUTE);
            if (attribute instanceof State state) {
                return state;
            }
            State state = new State(context, transactionManager, sameNavigatorContext);
            context.setContextAttribute(STATE_ATTRIBUTE, state);
            transactionManager.addTransactionListener(state);
            return state;
        }
    }

    static final class Handling {
        private final DBCExecutionContext context;
        private final DBCDDLTransactionBehavior behavior;
        private final State state;
        private final boolean sameNavigatorContext;
        private final Set<RefreshTarget> targets = new LinkedHashSet<>();
        private final Set<RefreshTarget> lateTargets = new LinkedHashSet<>();

        private Handling(
            @NotNull DBCExecutionContext context,
            @NotNull DBCDDLTransactionBehavior behavior,
            @Nullable State state,
            boolean sameNavigatorContext
        ) {
            this.context = context;
            this.behavior = behavior;
            this.state = state;
            this.sameNavigatorContext = sameNavigatorContext;
        }

        void add(@NotNull RefreshTarget target) {
            targets.add(target);
            if (state != null && !state.add(target)) {
                lateTargets.add(target);
            }
        }

        boolean acceptsTargets() {
            return state == null || state.isActive();
        }

        void finish() {
            if (targets.isEmpty()) {
                return;
            }
            if (!lateTargets.isEmpty()) {
                showRefreshNotification(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_notification,
                    lateTargets,
                    () -> true,
                    null
                );
            }
            switch (behavior) {
                case AUTO -> throw new IllegalStateException("DDL transaction behavior was not resolved");
                case IMMEDIATE -> showRefreshNotification(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_notification,
                    targets,
                    () -> true,
                    null
                );
                case IGNORED -> showInformation(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_ignored_notification
                );
                case TRANSACTIONAL -> {
                    if (state == null || !state.isActive()) {
                        return;
                    }
                    if (sameNavigatorContext) {
                        showRefreshNotification(
                            context,
                            SQLEditorMessages.sql_editor_metadata_refresh_pending_notification,
                            targets,
                            () -> state.claimPreCommitRefresh(targets),
                            state::recordPreCommitRefresh
                        );
                    } else {
                        showInformation(
                            context,
                            SQLEditorMessages.sql_editor_metadata_refresh_pending_notification
                        );
                    }
                }
            }
        }
    }

    private static final class State implements DBCTransactionListener {
        private final DBCExecutionContext context;
        private final DBCTransactionManager transactionManager;
        private final boolean sameNavigatorContext;
        private final Set<RefreshTarget> targets = new LinkedHashSet<>();
        private final Set<RefreshTarget> refreshedTargets = new LinkedHashSet<>();
        private Outcome outcome = Outcome.ACTIVE;

        private State(
            @NotNull DBCExecutionContext context,
            @NotNull DBCTransactionManager transactionManager,
            boolean sameNavigatorContext
        ) {
            this.context = context;
            this.transactionManager = transactionManager;
            this.sameNavigatorContext = sameNavigatorContext;
        }

        private synchronized boolean add(@NotNull RefreshTarget target) {
            if (outcome == Outcome.ACTIVE) {
                targets.add(target);
                return true;
            }
            return false;
        }

        private synchronized boolean isActive() {
            return outcome == Outcome.ACTIVE;
        }

        private synchronized boolean claimPreCommitRefresh(@NotNull Set<RefreshTarget> requestedTargets) {
            if (outcome == Outcome.ROLLED_BACK || outcome == Outcome.CLOSED) {
                return false;
            }
            Set<RefreshTarget> newTargets = new LinkedHashSet<>(requestedTargets);
            newTargets.retainAll(targets);
            if (newTargets.isEmpty()) {
                return false;
            }
            return true;
        }

        private void recordPreCommitRefresh(@NotNull Set<RefreshTarget> successfulTargets) {
            Set<RefreshTarget> compensationTargets = Set.of();
            synchronized (this) {
                Set<RefreshTarget> trackedTargets = new LinkedHashSet<>(successfulTargets);
                trackedTargets.retainAll(targets);
                if (outcome == Outcome.ACTIVE) {
                    refreshedTargets.addAll(trackedTargets);
                } else if (outcome == Outcome.ROLLED_BACK) {
                    compensationTargets = trackedTargets;
                }
            }
            if (!compensationTargets.isEmpty()) {
                Set<RefreshTarget> refreshTargets = compensationTargets;
                showRefreshNotification(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_rolled_back_notification,
                    refreshTargets,
                    () -> claimCompensation(refreshTargets),
                    null
                );
            }
        }

        private synchronized boolean claimCompensation(@NotNull Set<RefreshTarget> requestedTargets) {
            if (outcome != Outcome.ACTIVE && outcome != Outcome.ROLLED_BACK) {
                return false;
            }
            return !requestedTargets.isEmpty();
        }

        @Override
        public void transactionCommitted() {
            completeCommit();
        }

        @Override
        public void transactionRolledBack(@Nullable DBCSavepoint savepoint) {
            if (savepoint == null) {
                completeRollback();
            } else {
                compensateSavepointRollback();
            }
        }

        @Override
        public void autoCommitChanged(boolean autoCommit) {
            if (autoCommit) {
                completeCommit();
            }
        }

        @Override
        public void transactionContextClosed() {
            synchronized (this) {
                outcome = Outcome.CLOSED;
                targets.clear();
                refreshedTargets.clear();
            }
            detach();
        }

        private void completeCommit() {
            Set<RefreshTarget> refreshTargets;
            synchronized (this) {
                if (outcome != Outcome.ACTIVE) {
                    return;
                }
                outcome = Outcome.COMMITTED;
                refreshTargets = new LinkedHashSet<>(targets);
                refreshTargets.removeAll(refreshedTargets);
            }
            detach();
            if (!refreshTargets.isEmpty()) {
                showRefreshNotification(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_committed_notification,
                    refreshTargets,
                    () -> claimPreCommitRefresh(refreshTargets),
                    null
                );
            }
        }

        private void completeRollback() {
            Set<RefreshTarget> refreshTargets;
            synchronized (this) {
                if (outcome != Outcome.ACTIVE) {
                    return;
                }
                outcome = Outcome.ROLLED_BACK;
                refreshTargets = sameNavigatorContext ? Set.copyOf(refreshedTargets) : Set.of();
            }
            detach();
            if (!refreshTargets.isEmpty()) {
                showRefreshNotification(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_rolled_back_notification,
                    refreshTargets,
                    () -> claimCompensation(refreshTargets),
                    null
                );
            }
        }

        private void compensateSavepointRollback() {
            Set<RefreshTarget> refreshTargets;
            synchronized (this) {
                if (outcome != Outcome.ACTIVE || !sameNavigatorContext) {
                    return;
                }
                refreshTargets = Set.copyOf(refreshedTargets);
            }
            if (!refreshTargets.isEmpty()) {
                showRefreshNotification(
                    context,
                    SQLEditorMessages.sql_editor_metadata_refresh_rolled_back_notification,
                    refreshTargets,
                    () -> claimCompensation(refreshTargets),
                    null
                );
            }
        }

        private void detach() {
            transactionManager.removeTransactionListener(this);
            synchronized (context) {
                if (context.getContextAttribute(STATE_ATTRIBUTE) == this) {
                    context.removeContextAttribute(STATE_ATTRIBUTE);
                }
            }
        }
    }

    private static void showInformation(@NotNull DBCExecutionContext context, @NotNull String message) {
        DBeaverNotifications.showNotification(
            SQLEditor.NOTIFICATION_SQL_METADATA_REFRESH,
            NLS.bind(
                SQLEditorMessages.sql_editor_metadata_refresh_information_title,
                context.getDataSource().getContainer().getName()
            ),
            message,
            DBPMessageType.INFORMATION,
            null
        );
    }

    private static void showRefreshNotification(
        @NotNull DBCExecutionContext context,
        @NotNull String message,
        @NotNull Set<RefreshTarget> targets,
        @NotNull RefreshClaim claim,
        @Nullable Consumer<Set<RefreshTarget>> completionHandler
    ) {
        Set<RefreshTarget> refreshTargets = Set.copyOf(targets);
        DBeaverNotifications.showNotification(
            SQLEditor.NOTIFICATION_SQL_METADATA_REFRESH,
            NLS.bind(
                SQLEditorMessages.sql_editor_metadata_refresh_notification_title,
                context.getDataSource().getContainer().getName()
            ),
            message,
            DBPMessageType.WARNING,
            () -> {
                if (claim.claim()) {
                    SQLMetadataRefreshCoordinator.refresh(context, refreshTargets, completionHandler);
                }
            }
        );
    }

    private enum Outcome {
        ACTIVE,
        COMMITTED,
        ROLLED_BACK,
        CLOSED
    }

    @FunctionalInterface
    private interface RefreshClaim {
        boolean claim();
    }
}
