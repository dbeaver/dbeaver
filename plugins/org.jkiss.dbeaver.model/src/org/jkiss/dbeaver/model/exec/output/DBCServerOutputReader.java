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
package org.jkiss.dbeaver.model.exec.output;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.dpi.DPIObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Provides ability to read server logs for certain session
 */
@DPIObject
public interface DBCServerOutputReader extends DBPObject
{
    @DPIElement
    boolean isServerOutputEnabled();

    /**
     * If async output reading is supported then SQL job will read output during statement execution.
     */
    @DPIElement
    boolean isAsyncOutputReadSupported();

    /**
     * Reads server output messages.
     * Only @queryResult or @statement can be specified. Non-null statement means async output reading.
     * Output for statement can be requested only if @isAsyncOutputReadSupported returns true.
     */
    void readServerOutput(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext context,
        @Nullable DBCExecutionResult executionResult,
        @Nullable DBCStatement statement,
        @NotNull DBCOutputWriter output)
        throws DBCException;
}
