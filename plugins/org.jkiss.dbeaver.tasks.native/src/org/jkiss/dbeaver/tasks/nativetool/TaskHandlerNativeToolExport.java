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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.utils.CommonUtils;

import java.io.File;

/**
 * TaskHandlerNativeToolExport
 */
public abstract class TaskHandlerNativeToolExport<BASE_OBJECT extends DBSObject, PROCESS_ARG> extends TaskHandlerNativeToolBase<BASE_OBJECT, PROCESS_ARG> {

    protected File outputFolder;
    protected String outputFilePattern;

    public File getOutputFolder() {
        return outputFolder;
    }

    public String getOutputFilePattern() {
        return outputFilePattern;
    }

    @Override
    protected boolean prepareTaskRun(DBRProgressMonitor monitor, DBTTask task, Log log) {
        if (outputFolder != null) {
            if (!outputFolder.exists()) {
                if (!outputFolder.mkdirs()) {
                    log.error("Can't create directory '" + outputFolder.getAbsolutePath() + "'");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void loadToolSettings(@NotNull DBRProgressMonitor monitor, @NotNull DBTTask task, Log log) {
        super.loadToolSettings(monitor, task, log);

        String outputFolderName = CommonUtils.toString(task.getProperties().get("outputFolder"));
        if (!CommonUtils.isEmpty(outputFolderName)) {
            outputFolder = new File(outputFolderName);
        }

        outputFilePattern = CommonUtils.toString(task.getProperties().get("outputFilePattern"));
    }


}
