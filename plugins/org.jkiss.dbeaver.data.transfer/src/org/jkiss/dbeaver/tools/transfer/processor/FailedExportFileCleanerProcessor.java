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
package org.jkiss.dbeaver.tools.transfer.processor;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.IDataTransferEventProcessor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FailedExportFileCleanerProcessor implements IDataTransferEventProcessor<StreamTransferConsumer> {

    public static final String ID = "failedExportFileCleaner";
    private static final Log log = Log.getLog(FailedExportFileCleanerProcessor.class);

    @Override
    public void processEvent(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Event event,
        @NotNull StreamTransferConsumer consumer,
        @Nullable DBTTask task,
        @NotNull Map<String, Object> settings
    ) {
        // do nothing
    }

    @Override
    public void processError(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Throwable error,
        @NotNull StreamTransferConsumer consumer,
        @Nullable DBTTask task,
        @NotNull Map<String, Object> settings
    ) {
        if (consumer.getSettings().isUseSingleFile()) {
            // We don't want to remove existing file
            return;
        }
        for (Path outputFile : consumer.getOutputFiles()) {
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException e) {
                log.warn("Unable to delete failed export file '" + outputFile.toAbsolutePath() + "'", e);
            }
        }
    }
}
