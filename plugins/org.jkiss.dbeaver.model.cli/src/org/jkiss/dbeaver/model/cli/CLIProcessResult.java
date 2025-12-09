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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

public class CLIProcessResult {
    public enum PostAction {
        START_INSTANCE(CLIConstants.EXIT_CODE_CONTINUE),
        SHUTDOWN(CLIConstants.EXIT_CODE_OK),
        ERROR(CLIConstants.EXIT_CODE_ERROR),
        UNKNOWN_COMMAND(CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        private final short defaultExitCode;

        PostAction(short exitCode) {
            this.defaultExitCode = exitCode;
        }
    }

    @NotNull
    private final PostAction postAction;
    private final short exitCode;
    @Nullable
    private final List<String> output;

    public CLIProcessResult(@NotNull PostAction postAction) {
        this(postAction, null, postAction.defaultExitCode);
    }

    public CLIProcessResult(@NotNull PostAction postAction, short exitCode) {
        this(postAction, null, exitCode);
    }

    public CLIProcessResult(@NotNull PostAction postAction, @Nullable String output) {
        this(postAction, output == null ? null : List.of(output), postAction.defaultExitCode);
    }

    public CLIProcessResult(@NotNull PostAction postAction, @Nullable List<String> output) {
        this(postAction, output, postAction.defaultExitCode);
    }

    public CLIProcessResult(@NotNull PostAction postAction, @Nullable List<String> output, short exitCode) {
        this.postAction = postAction;
        this.output = output;
        this.exitCode = exitCode;
    }

    @NotNull
    public PostAction getPostAction() {
        return postAction;
    }

    @Nullable
    public List<String> getOutput() {
        return output;
    }

    public short getExitCode() {
        return exitCode;
    }
}

