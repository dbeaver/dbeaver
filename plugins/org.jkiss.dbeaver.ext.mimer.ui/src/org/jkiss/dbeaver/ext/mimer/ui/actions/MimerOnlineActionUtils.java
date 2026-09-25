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
package org.jkiss.dbeaver.ext.mimer.ui.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerRefresh;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for the "Set Online State" / "Restore From Log" / "Switch To Master" navigator
 * actions - selecting the target nodes and running one-off {@code SET}/{@code ALTER} statements
 * on a background job, with error surfacing and a follow-up navigator refresh.
 * <p>
 * Statements run on the datasource's default (user) execution context, NOT the metadata
 * connection - opening a meta session from a menu handler can deadlock against the navigator
 * that is still loading on it.
 *
 * @author Mimer Information Technology
 */
final class MimerOnlineActionUtils {

    private static final Log log = Log.getLog(MimerOnlineActionUtils.class);

    private MimerOnlineActionUtils() {
    }

    /**
     * The selected navigator nodes whose object is an instance of {@code type} - empty if the
     * selection contains anything else (so an action only ever runs on a homogeneous selection).
     */
    @NotNull
    static <T extends DBSObject> List<DBNDatabaseNode> collectNodes(@Nullable ISelection selection, @NotNull Class<T> type) {
        List<DBNDatabaseNode> result = new ArrayList<>();
        if (selection == null) {
            return result;
        }
        for (DBNNode node : NavigatorUtils.getSelectedNodes(selection)) {
            if (node instanceof DBNDatabaseNode dbNode && type.isInstance(dbNode.getObject())) {
                result.add(dbNode);
            } else {
                return new ArrayList<>();
            }
        }
        return result;
    }

    /**
     * Runs {@code statements} against {@code dataSource} on a background job. On success runs
     * {@code onSuccessUi} on the UI thread; on failure shows an error dialog (a friendly
     * "run Restore From Log first" message when {@code restoreHint} and the server complains
     * about an old databank version).
     */
    static void run(
        @NotNull MimerDataSource dataSource,
        @NotNull String jobName,
        @NotNull List<String> statements,
        boolean restoreHint,
        @NotNull Runnable onSuccessUi
    ) {
        AbstractJob job = new AbstractJob(jobName) {
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                monitor.beginTask(jobName, statements.size());
                try (DBCSession session = dataSource.getDefaultInstance()
                    .getDefaultContext(monitor, false)
                    .openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
                    for (String sql : statements) {
                        monitor.subTask(sql);
                        try (DBCStatement stmt = DBUtils.createStatement(session, sql, false)) {
                            stmt.executeStatement();
                        }
                        monitor.worked(1);
                    }
                } catch (Throwable e) {
                    showFailure(e, restoreHint, jobName);
                    return Status.CANCEL_STATUS;
                } finally {
                    monitor.done();
                }
                UIUtils.asyncExec(onSuccessUi);
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private static void showFailure(@NotNull Throwable e, boolean restoreHint, @NotNull String jobName) {
        String message = String.valueOf(e.getMessage());
        if (restoreHint && message.contains("RESTORE")) {
            DBWorkbench.getPlatformUI().showError(
                MimerUIMessages.action_shadow_behind_master_title,
                MimerUIMessages.action_shadow_behind_master_message,
                e);
        } else {
            DBWorkbench.getPlatformUI().showError(NLS.bind(MimerUIMessages.action_job_failed, jobName), null, e);
        }
    }

    /** Force a navigator refresh of the given nodes (rebuilds any open properties editor too). */
    static void refresh(@NotNull List<? extends DBNNode> nodes) {
        try {
            NavigatorHandlerRefresh.refreshNavigator(nodes);
        } catch (Exception e) {
            log.debug("Failed to refresh navigator after Mimer online-state action", e);
        }
    }

    /** The distinct parent nodes of {@code nodes}, for refreshing after an unpredictable state change. */
    @NotNull
    static List<DBNNode> parentsOf(@NotNull List<DBNDatabaseNode> nodes) {
        List<DBNNode> parents = new ArrayList<>();
        for (DBNDatabaseNode node : nodes) {
            DBNNode parent = node.getParentNode() != null ? node.getParentNode() : node;
            if (!parents.contains(parent)) {
                parents.add(parent);
            }
        }
        return parents;
    }
}
