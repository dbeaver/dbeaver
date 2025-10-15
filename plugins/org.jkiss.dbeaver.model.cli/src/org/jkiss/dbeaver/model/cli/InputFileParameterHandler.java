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


import org.apache.commons.cli.CommandLine;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class InputFileParameterHandler implements ICommandLineParameterHandler {
    private final Log log = Log.getLog(InputFileParameterHandler.class);

    public static final String INPUT_FILE = "inputFile";

    @Override
    public void handleParameter(
        @NotNull CommandLine commandLine,
        @NotNull String name,
        @Nullable String value,
        @NotNull CommandLineContext context
    ) {
        if (CommonUtils.isEmpty(value)) {
            log.warn("--input-file parameter is empty");
            return;
        }
        Path inputFile = Path.of(value);
        if (Files.notExists(inputFile)) {
            log.warn("--input-file does not exist: " + inputFile);
            return;
        }

        context.setContextParameter(INPUT_FILE, inputFile);
    }
}
