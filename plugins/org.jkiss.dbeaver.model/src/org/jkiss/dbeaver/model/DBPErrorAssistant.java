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

package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Error assistant
 */
public interface DBPErrorAssistant
{
    enum ErrorType {
        NORMAL,
        CONNECTION_LOST,
        DRIVER_CLASS_MISSING,
        PERMISSION_DENIED,
        FEATURE_UNSUPPORTED
    }

    class ErrorPosition
    {
        // Line number (starts from zero)
        public int line = -1;
        // Position in line. If line < 0 then position from start of query (starts from zero)
        public int position = -1;
        // Position information
        public String info = null;

        @Override
        public String toString() {
            return line + ":" + position + (info == null ? "" : " (" + info + ")");
        }
    }

    ErrorType discoverErrorType(@NotNull Throwable error);

    @Nullable
    ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error);

}