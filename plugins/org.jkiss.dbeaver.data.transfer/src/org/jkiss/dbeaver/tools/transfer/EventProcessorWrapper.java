/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;

import java.util.Map;

public record EventProcessorWrapper(
    @NotNull DataTransferEventProcessorDescriptor descriptor,
    @NotNull IDataTransferEventProcessor<IDataTransferConsumer<?, ?>> processor,
    @NotNull Map<String, Object> settings
) {
    private static final Log log = Log.getLog(EventProcessorWrapper.class);

    public void processEvent(
        @NotNull DBRProgressMonitor monitor,
        @NotNull IDataTransferEventProcessor.Event event,
        @NotNull IDataTransferConsumer<?, ?> consumer,
        @Nullable DBTTask task
    ) {
        try {
            processor.processEvent(monitor, event, consumer, task, settings);
        } catch (DBException e) {
            reportException(e);
        }
    }

    public void processError(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Exception exception,
        @NotNull IDataTransferConsumer<?, ?> consumer,
        @Nullable DBTTask task
    ) {
        try {
            processor.processError(monitor, exception, consumer, task, settings);
        } catch (DBException e) {
            reportException(e);
        }
    }

    private void reportException(@NotNull DBException e) {
        DBWorkbench.getPlatformUI().showError("Transfer event processor", "Error executing data transfer event processor '" + descriptor.getId() + "'", e);
        log.error("Error executing event processor '" + descriptor.getId() + "'", e);
    }
}
