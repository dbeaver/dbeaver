/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.task.DTTaskHandlerTransfer;
import org.jkiss.dbeaver.ui.task.TaskProcessorUI;

import java.util.Locale;

class DataTransferWizardExecutor extends TaskProcessorUI {
    private static final Log log = Log.getLog(DataTransferWizard.class);

    private DataTransferSettings settings;

    public DataTransferWizardExecutor(@NotNull DBRRunnableContext staticContext, @NotNull DBTTask task, @NotNull DataTransferSettings settings) {
        super(staticContext, task);
        this.settings = settings;
    }

    public DataTransferWizardExecutor(@NotNull DBRRunnableContext staticContext, @NotNull String taskName, @NotNull DataTransferSettings settings) {
        super(staticContext, taskName);
        this.settings = settings;
    }

    @Override
    protected void runTask() throws DBException {
        DTTaskHandlerTransfer handlerTransfer = new DTTaskHandlerTransfer();
        handlerTransfer.executeWithSettings(this, Locale.getDefault(), log, this, settings);
    }

}