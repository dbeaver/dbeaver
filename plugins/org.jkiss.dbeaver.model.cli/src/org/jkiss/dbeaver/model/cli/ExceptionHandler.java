/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.Nullable;
import picocli.CommandLine;

public class ExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Nullable
    private Exception exception;

    public ExceptionHandler() {
    }

    @Override
    public int handleExecutionException(Exception e, CommandLine commandLine, CommandLine.ParseResult fullParseResult) throws Exception {
        exception = e;
        return CLIConstants.EXIT_CODE_ERROR;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }
}
