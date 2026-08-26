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
package org.jkiss.dbeaver.model.data.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.data.messages.DataMessages;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataUpdaterJob extends DataSourceJob {
    private final DBDResultSetDataUpdater<?, ?, ?> resultSetPersister;
    private final boolean generateScript;
    private final ResultSetSaveSettings settings;
    private final DBDDataUpdateListener listener;
    private Throwable error;

    public DataUpdaterJob(
        @NotNull DBDResultSetDataUpdater<?, ?, ?> resultSetPersister,
        boolean generateScript,
        @NotNull ResultSetSaveSettings settings,
        @Nullable DBDDataUpdateListener listener,
        @NotNull DBCExecutionContext executionContext
    ) {
        super(DataMessages.controls_resultset_viewer_job_update, executionContext);
        this.resultSetPersister = resultSetPersister;
        this.generateScript = generateScript;
        this.settings = settings;
        this.listener = listener;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }

    @NotNull
    @Override
    public IStatus run(@NotNull DBRProgressMonitor monitor) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put(DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, settings.isUseFullyQualifiedNames());
        try {
            error = executeUpdate(monitor, options);
        } finally {
            resultSetPersister.releaseClonedValues();
        }

        if (!generateScript) {
            resultSetPersister.processReflectChanges(error);
            if (this.listener != null) {
                this.listener.onUpdate(error == null);
            }
        } else if (error != null) {
            resultSetPersister.showError(error);
        }

        return Status.OK_STATUS;
    }

    @Nullable
    protected Throwable executeUpdate(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) {
        return resultSetPersister.executeStatements(
            monitor,
            options,
            resultSetPersister.getSmartTransactionManager(),
            generateScript
        );
    }
}
