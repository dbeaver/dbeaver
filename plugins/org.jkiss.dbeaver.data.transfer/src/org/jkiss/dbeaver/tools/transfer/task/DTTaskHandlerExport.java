/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.task;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.model.task.DBTTaskRunStatus;
import org.jkiss.dbeaver.registry.BasePolicyDataProvider;
import org.jkiss.dbeaver.tools.transfer.DTConstants;

import java.io.PrintStream;
import java.util.Locale;

/**
 * DTTaskHandlerExport
 */
public class DTTaskHandlerExport extends DTTaskHandlerTransfer {

    @NotNull
    @Override
    public DBTTaskRunStatus executeTask(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBTTask task,
        @NotNull Locale locale,
        @NotNull Log log,
        @NotNull PrintStream logStream,
        @NotNull DBTTaskExecutionListener listener
    ) throws DBException {
        if (BasePolicyDataProvider.getInstance().isPolicyEnabled(DTConstants.POLICY_DATA_EXPORT)) {
            throw new DBException("Error: Data export operation is restricted by policy"); //$NON-NLS-1$
        }
        return super.executeTask(runnableContext, task, locale, log, logStream, listener);
    }
}
