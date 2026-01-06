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
package org.jkiss.dbeaver.model.cli.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.cli.AbstractRootCommandLineParameterHandler;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.model.option.AuthenticateOptions;
import picocli.CommandLine;

public abstract class CommandLineWithAuth extends AbstractRootCommandLineParameterHandler {

    @Nullable
    @CommandLine.Mixin
    private AuthenticateOptions authenticateOptions;

    @Override
    public void run() throws CLIException {
        if (context().getContextParameter(CLIConstants.CONTEXT_PARAM_AUTHENTICATOR) != null) {
            try {
                ((CommandLineAuthenticator) context().getContextParameter(CLIConstants.CONTEXT_PARAM_AUTHENTICATOR))
                    .authenticate(authenticateOptions, context());
            } catch (DBException e) {
                throw new CLIException("Authentication failed: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
            }
        }
    }


}
