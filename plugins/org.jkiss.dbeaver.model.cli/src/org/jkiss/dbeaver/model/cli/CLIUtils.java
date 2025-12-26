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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.cli.model.option.InputFileOption;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CLIUtils {
    private static final Log log = Log.getLog(CLIUtils.class);

    @Nullable
    public static String readValueFromFileOrSystemIn(@Nullable InputFileOption filesOptions) throws CLIException {
        String value = null;
        if (filesOptions == null || filesOptions.getInputFile() == null) {
            value = tryReadFromSystemIn();
        } else if (filesOptions.getInputFile() != null) {
            if (Files.notExists(filesOptions.getInputFile())) {
                throw new CLIException(
                    "Input file does not exist: " + filesOptions.getInputFile(),
                    CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
                );
            }
            try {
                value = Files.readString(filesOptions.getInputFile());
            } catch (IOException e) {
                throw new CLIException(
                    "Error reading GQL from input file: " + filesOptions.getInputFile(),
                    e,
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
        }
        return value;
    }

    @Nullable
    private static String tryReadFromSystemIn() {
        try {
            if (System.in.available() > 0) {
                return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Error reading from system in", e);
            return null;
        }
        return null;
    }


    @NotNull
    public static DBPProject findProject(@Nullable String projectIdOrName, @NotNull CommandLineContext context) throws CLIException {
        DBPProject project;
        DBPWorkspace workspace = context.getContextParameter(DBPWorkspace.class.getName());
        if (workspace == null) {
            workspace = DBWorkbench.getPlatform().getWorkspace();
        }
        if (CommonUtils.isEmpty(projectIdOrName)) {
            project = workspace.getActiveProject();
        } else {
            project = workspace.getProject(projectIdOrName);
            if (project == null) {
                project = workspace.getProjectById(projectIdOrName);
            }
        }
        if (project == null) {
            throw new CLIException("Can't find project '" + projectIdOrName + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        return project;
    }

    @Nullable
    public static DBPDataSourceContainer findDataSource(
        @NotNull DBPProject project,
        @NotNull String connectionSpec
    ) throws CLIException {
        ApplicationInstanceServer.InstanceConnectionParameters instanceConParameters
            = new ApplicationInstanceServer.InstanceConnectionParameters();
        return DataSourceUtils.getDataSourceBySpec(
            project,
            GeneralUtils.replaceVariables(connectionSpec, SystemVariablesResolver.INSTANCE),
            instanceConParameters,
            false,
            instanceConParameters.isCreateNewConnection()
        );
    }
}
