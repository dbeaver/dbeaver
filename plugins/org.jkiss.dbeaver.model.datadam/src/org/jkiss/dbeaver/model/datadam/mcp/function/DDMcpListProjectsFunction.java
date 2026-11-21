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
package org.jkiss.dbeaver.model.datadam.mcp.function;

import com.dbeaver.datadam.mcp.api.exception.DDMcpFunctionException;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.dbeaver.datadam.mcp.api.model.DDMcpListProjectsResult;
import com.dbeaver.datadam.mcp.api.model.DDMcpProject;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpFunction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DDMcpListProjectsFunction implements DDMcpFunction {
    private final DDMcpConnectionCatalog connections = DDMcpConnectionCatalog.getInstance();

    @NotNull
    @Override
    public DDMcpFunctionResult call(@NotNull Map<String, Object> arguments) throws DDMcpFunctionException {
        List<DDMcpProject> projects = new ArrayList<>();
        Set<String> projectIds = new HashSet<>();
        for (var available : connections.getAllowedConnections()) {
            var project = available.project();
            if (projectIds.add(project.getId())) {
                projects.add(new DDMcpProject(project.getId(), project.getName(), project.getDescription()));
            }
        }
        return new DDMcpFunctionResult(new DDMcpListProjectsResult(projects), false);
    }
}
