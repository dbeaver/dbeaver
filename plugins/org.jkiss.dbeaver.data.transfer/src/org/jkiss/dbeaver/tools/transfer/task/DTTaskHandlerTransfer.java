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
package org.jkiss.dbeaver.tools.transfer.task;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskDescriptor;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DTTaskHandlerTransfer
 */
public class DTTaskHandlerTransfer implements DBTTaskHandler {

    @Override
    public void executeTask(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBTTaskDescriptor task,
        @NotNull List<Object> inputObjects,
        @NotNull Locale locale,
        @NotNull Map<String, Object> properties) throws DBException
    {
        throw new DBException("Not implemented yet");
    }
}
