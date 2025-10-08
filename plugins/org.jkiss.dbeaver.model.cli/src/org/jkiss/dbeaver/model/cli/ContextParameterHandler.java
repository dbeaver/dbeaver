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
import org.jkiss.utils.CommonUtils;

public class ContextParameterHandler implements ICommandLineParameterHandler {
    @Override
    public void handleParameter(
        @NotNull CommandLine commandLine,
        @NotNull String name,
        @Nullable String value,
        @NotNull CommandLineContext context
    ) throws CLIException {
        if (CommonUtils.isEmpty(value)) {
            throw new CLIException("--" + name + " parameter is empty", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }

        context.setContextParameter(name, value);
    }
}
