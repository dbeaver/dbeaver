/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli.command;

import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import picocli.AutoComplete;
import picocli.CommandLine;

@CommandLine.Command(name = "completion", description = "Generate bash/zsh completion script for ${ROOT-COMMAND-NAME}")
public class CLIAutoComplete extends CLIAbstractSubcommand {
    //todo powershellsupport
    public enum TERMINAL {
        bash,
        zsh
    }

    @CommandLine.Parameters(
        index = "0",
        description = "Terminal type",
        defaultValue = "bash",
        arity = "0..1"
    )
    private TERMINAL terminal = TERMINAL.bash;

    @Override
    public void run() throws CLIException {
        String script;
        switch (terminal) {
            case bash, zsh -> script = AutoComplete.bash(this.spec.root().name(), this.spec.root().commandLine());
            default -> throw new CLIException("Unsupported terminal type: " + terminal, CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        context().addResult(script);
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
    }
}
