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
package org.jkiss.dbeaver.model.cli;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.cli.model.option.HiddenOptionsForSubcommands;
import picocli.CommandLine;

public abstract class CLIAbstractSubcommand extends CLIAbstractCommand {
    @CommandLine.Mixin
    private HiddenOptionsForSubcommands eclipseHiddenOptions;
    /**
     * Root command uses as context
     */
    @CommandLine.ParentCommand
    private CLIAbstractCommand parent;

    @NotNull
    protected CLIContext context() {
        return parent.context();
    }
}
