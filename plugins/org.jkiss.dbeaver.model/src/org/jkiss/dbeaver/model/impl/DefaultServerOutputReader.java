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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Default output reader.
 * Dumps SQL warnings
 */
public class DefaultServerOutputReader implements DBCServerOutputReader {
    @Override
    public boolean isServerOutputEnabled() {
        return true;
    }

    @Override
    public boolean isAsyncOutputReadSupported() {
        return false;
    }

    @Override
    public void readServerOutput(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext context,
        @Nullable DBCExecutionResult executionResult,
        @Nullable DBCStatement statement,
        @NotNull DBCOutputWriter output
    ) throws DBCException {
        if (executionResult != null) {
            dumpWarnings(output, executionResult.getWarnings());
        }
    }

    protected void dumpWarnings(@NotNull DBCOutputWriter output, List<Throwable> warnings) {
        if (warnings != null && warnings.size() > 0) {
            for (Throwable warning : warnings) {
                dumpWarning(output, warning);
            }
        }
    }

    protected void dumpWarning(@NotNull DBCOutputWriter output, @NotNull Throwable warning) {
        output.println(null, warning.getMessage());
    }
}
