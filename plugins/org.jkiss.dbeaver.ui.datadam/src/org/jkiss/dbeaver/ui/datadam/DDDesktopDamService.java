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
package org.jkiss.dbeaver.ui.datadam;

import com.dbeaver.datadam.dam.remote.api.model.DDConnectedRemoteDam;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.net.URI;
import java.util.function.Consumer;

public class DDDesktopDamService {
    @Nullable
    private static DDDesktopDamService instance;

    private final DDAccountClient account = new DDAccountClient(URI.create(DDEndpoints.getAccountBaseUrl()));

    private DDDesktopDamService() {
    }

    @NotNull
    public static synchronized DDDesktopDamService getInstance() {
        if (instance == null) {
            instance = new DDDesktopDamService();
        }
        return instance;
    }

    @Nullable
    public DDConnectedRemoteDam getDam() throws DBException {
        return account.getDam(DDRemoteDamDesktopSessionService.getInstance().getClientDamId());
    }

    @NotNull
    public DDConnectedRemoteDam renameDam(@NotNull String name) throws DBException {
        return account.renameDam(DDRemoteDamDesktopSessionService.getInstance().getClientDamId(), name);
    }

    /** An Account request executed outside the UI thread. */
    @FunctionalInterface
    public interface ServiceCall<S, R> {
        @Nullable
        R run(@NotNull S service) throws Exception;
    }

    @NotNull
    public <R> AbstractJob createJob(
        @NotNull String jobName,
        @NotNull ServiceCall<DDAccountClient, R> operation,
        @NotNull Consumer<R> onSuccess,
        @NotNull Consumer<DBException> onFailure
    ) {
        AbstractJob job = new AbstractJob(jobName) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    R result = operation.run(account);
                    UIUtils.asyncExec(() -> {
                        if (!monitor.isCanceled() && !isCanceled()) {
                            onSuccess.accept(result);
                        }
                    });
                } catch (DBException e) {
                    notifyFailure(e, monitor);
                } catch (Exception e) {
                    notifyFailure(new DBException(jobName + " failed", e), monitor);
                }
                return Status.OK_STATUS;
            }

            private void notifyFailure(@NotNull DBException failure, @NotNull DBRProgressMonitor monitor) {
                UIUtils.asyncExec(() -> {
                    if (!monitor.isCanceled() && !isCanceled()) {
                        onFailure.accept(failure);
                    }
                });
            }
        };
        job.setSystem(true);
        job.schedule();
        return job;
    }

}
