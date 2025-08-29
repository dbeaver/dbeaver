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
package org.jkiss.dbeaver.launcher;

public class CliData {
    private final boolean shutdown;
    private final short exitCode;

    public CliData(boolean shutdown) {
        this(shutdown, shutdown ? (short) 0 : (short) -1);
    }

    public CliData(boolean shutdown, short exitCode) {
        this.shutdown = shutdown;
        this.exitCode = exitCode;
    }

    public short getExitCode() {
        return exitCode;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
