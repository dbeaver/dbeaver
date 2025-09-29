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

import org.jkiss.dbeaver.DBException;

public class CLIException extends DBException {
    private final short exitCode;

    public CLIException(String message, short exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public CLIException(String message, Throwable cause, short exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public short getExitCode() {
        return exitCode;
    }
}
