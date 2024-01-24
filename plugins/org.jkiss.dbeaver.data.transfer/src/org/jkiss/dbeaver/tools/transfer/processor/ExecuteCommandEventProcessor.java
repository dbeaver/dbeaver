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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.IDataTransferEventProcessor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.Map;

public class ExecuteCommandEventProcessor implements IDataTransferEventProcessor<StreamTransferConsumer> {
    public static final String ID = "executeCommand";
    public static final String PROP_COMMAND = "command";
    public static final String PROP_WORKING_DIRECTORY = "workingDirectory";

    @Override
    public void processEvent(@NotNull DBRProgressMonitor monitor, @NotNull Event event, @NotNull StreamTransferConsumer consumer, @Nullable DBTTask task, @NotNull Map<String, Object> processorSettings) throws DBException {
        Path folderPath = DBFUtils.resolvePathFromString(
            monitor, task == null ? null : task.getProject(), consumer.getOutputFolder());
        final String commandLine = consumer.translatePattern(
            CommonUtils.toString(processorSettings.get(PROP_COMMAND)),
            folderPath.resolve(consumer.getOutputFileName()));
        final String workingDirectory = CommonUtils.toString(processorSettings.get(PROP_WORKING_DIRECTORY));

        final DBRShellCommand command = new DBRShellCommand(commandLine);
        command.setWorkingDirectory(workingDirectory);

        final DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command);
        processDescriptor.execute();
    }
}
