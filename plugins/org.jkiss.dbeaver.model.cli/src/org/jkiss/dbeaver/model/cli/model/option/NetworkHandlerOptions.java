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
package org.jkiss.dbeaver.model.cli.model.option;

import org.jkiss.code.Nullable;
import picocli.CommandLine;

import java.util.List;

public class NetworkHandlerOptions {
    @Nullable
    @CommandLine.Option(
        names = {"-net", "--network-handler-param"},
        arity = "1",
        description = "Network handler parameter in the form 'name=value'. May be specified multiple times.")
    private List<String> handlerParams;

    @CommandLine.Option(
        names = {"-net-save-pwd", "--network-handler-save-password"},
        arity = "1",
        description = "Save network handler secure parameters (like passwords). Default true.",
        defaultValue = "true"
    )

    private boolean savePassword;

    @Nullable
    public List<String> getHandlerParams() {
        return handlerParams;
    }
    
    public boolean isSavePassword() {
        return savePassword;
    }
}
