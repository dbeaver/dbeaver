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
package org.jkiss.dbeaver.ui.app.standalone.cli;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import picocli.CommandLine;

@CommandLine.Command(name = "unlink-project", aliases = {"-unlink-project", "--unlink-project", "unlink"},
    description = "Unlink a project from the workspace (files are kept on disk)")
public class UnlinkProjectHandler extends CLIAbstractSubcommand {
    private static final Log log = Log.getLog(UnlinkProjectHandler.class);

    @CommandLine.Parameters(index = "0", arity = "1", description = "Name of the project to unlink")
    private String name;

    @Override
    public void run() {
        try {
            if (!(DBWorkbench.getPlatform().getWorkspace() instanceof DBPWorkspaceDesktop workspace)) {
                throw new IllegalStateException("Project unlinking is not supported in this workspace");
            }
            DBPProject project = workspace.getProject(name);
            if (project == null) {
                context().addResult("Project '" + name + "' is not linked");
            } else {
                workspace.deleteProject(project, false);
                context().addResult("Project '" + name + "' unlinked");
            }
        } catch (Exception e) {
            log.error("Error unlinking project", e);
            context().addResult("Error unlinking project: " + e.getMessage());
        }
    }
}
