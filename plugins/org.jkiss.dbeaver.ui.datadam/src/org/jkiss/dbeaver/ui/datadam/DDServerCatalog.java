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

import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class DDServerCatalog {
    @Nullable
    private static DDServerCatalog instance;

    private final DDRemoteDamDesktopSessionService session;
    private final ConcurrentMap<ServiceKey, ServiceEntry> services = new ConcurrentHashMap<>();

    private DDServerCatalog() throws DBException {
        session = DDRemoteDamDesktopSessionService.getInstance();
    }

    @NotNull
    public static synchronized DDServerCatalog getInstance() throws DBException {
        if (instance == null) {
            instance = new DDServerCatalog();
        }
        return instance;
    }

    @FunctionalInterface
    public interface ServiceCall<S, R> {
        @Nullable
        R run(@NotNull S service) throws Exception;
    }

    private record ServiceKey(@NotNull String id, @NotNull Class<?> type) {
    }

    private record ServiceEntry(@NotNull MessagingRpcEndpoint endpoint, @NotNull Object service) {
    }

    @NotNull
    public <S, R> AbstractJob call(
        @NotNull String jobName,
        @NotNull String serviceId,
        @NotNull Class<S> serviceType,
        @NotNull ServiceCall<S, R> operation,
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
                    R result = operation.run(getService(serviceId, serviceType));
                    UIUtils.asyncExec(() -> {
                        if (!monitor.isCanceled() && !isCanceled()) {
                            onSuccess.accept(result);
                        }
                    });
                } catch (Exception e) {
                    DBException failure = e instanceof DBException dbException ? dbException : new DBException(jobName + " failed", e);
                    UIUtils.asyncExec(() -> {
                        if (!monitor.isCanceled() && !isCanceled()) {
                            onFailure.accept(failure);
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
        return job;
    }

    @NotNull
    private <S> S getService(@NotNull String serviceId, @NotNull Class<S> serviceType) throws DBException {
        MessagingRpcEndpoint endpoint = session.getRpc();
        ServiceKey key = new ServiceKey(serviceId, serviceType);
        ServiceEntry entry = services.compute(key, (ignored, cached) -> {
            if (cached != null && cached.endpoint() == endpoint) {
                return cached;
            }
            return new ServiceEntry(endpoint, endpoint.proxy(serviceId, serviceType));
        });
        return serviceType.cast(entry.service());
    }
}
