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
package org.jkiss.dbeaver.ui.datadam.project.job;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;

/** Runs a DataDam project operation off the UI thread. Callers handle completion and presentation. */
public abstract class DDProjectOperationJob<T> extends AbstractJob {
    @NotNull
    protected final DDProjectUIManager uiManager;
    @NotNull
    private final String errorMessage;
    @Nullable
    private T operationResult;

    protected DDProjectOperationJob(@NotNull String name, @NotNull String errorMessage) {
        super(name);
        this.uiManager = DDProjectUIManager.getInstance();
        this.errorMessage = errorMessage;
    }

    @Nullable
    public T getOperationResult() {
        return operationResult;
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        operationResult = null;
        try {
            if (!canRun(monitor)) {
                return Status.CANCEL_STATUS;
            }
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            T result = perform(monitor);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            operationResult = result;
        } catch (DBException e) {
            return Status.error(errorMessage, e);
        }
        return Status.OK_STATUS;
    }

    protected boolean canRun(@NotNull DBRProgressMonitor monitor) throws DBException {
        return DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor);
    }

    @Nullable
    protected abstract T perform(@NotNull DBRProgressMonitor monitor) throws DBException;
}
