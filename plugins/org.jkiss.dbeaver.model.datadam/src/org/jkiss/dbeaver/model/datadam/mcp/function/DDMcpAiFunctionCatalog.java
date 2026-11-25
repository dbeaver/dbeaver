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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionDescriptor;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionParameter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;

public class DDMcpAiFunctionCatalog {
    public static final String PROJECT_ID_PARAMETER = "project_id";
    public static final String DATASOURCE_ID_PARAMETER = "datasource_id";

    private static final List<DDMcpFunctionParameter> CONNECTION_PARAMETERS = List.of(
        new DDMcpFunctionParameter(PROJECT_ID_PARAMETER, DDMcpFunctionParameter.Type.STRING,
            "Project ID from listDatasources", true, null, null),
        new DDMcpFunctionParameter(DATASOURCE_ID_PARAMETER, DDMcpFunctionParameter.Type.STRING,
            "Datasource ID from the same listDatasources entry as project_id", true, null, null)
    );
    private final Map<String, AIFunctionDescriptor> functions = new LinkedHashMap<>();

    public DDMcpAiFunctionCatalog() {
        AIToolbox toolbox = AIAssistantRegistry.getInstance()
            .getAssistant(DBWorkbench.getPlatform().getWorkspace())
            .getToolboxManager()
            .getToolbox(AIConstants.INTERNAL_TOOLBOX_ID);
        if (toolbox != null) {
            for (AIFunctionDescriptor function : toolbox.getSupportedFunctions(AIFunctionPurpose.DAM)) {
                String id = getFunctionId(function);
                if (functions.putIfAbsent(id, function) != null) {
                    throw new IllegalStateException("Duplicate DBeaver MCP function: " + id);
                }
            }
        }
    }

    @NotNull
    public List<DDMcpFunctionDescriptor> getFunctions() throws DDMcpFunctionException {
        return functions.values().stream()
            .map(function -> describe(function, function.getPurpose() == AIFunctionPurpose.DAM ? List.of() : CONNECTION_PARAMETERS))
            .toList();
    }

    @Nullable
    public AIFunctionDescriptor getFunction(@NotNull String id) {
        return functions.get(id);
    }

    @NotNull
    private DDMcpFunctionDescriptor describe(
        @NotNull AIFunctionDescriptor function,
        @NotNull List<DDMcpFunctionParameter> connectionParameters
    ) {
        List<DDMcpFunctionParameter> parameters = new ArrayList<>(connectionParameters);
        Arrays.stream(function.getParameters()).map(this::describeParameter).forEach(parameters::add);
        return new DDMcpFunctionDescriptor(
            getFunctionId(function), function.getName(), function.getAiDescription(), parameters,
            function.isReadOnlyHint(), function.isDestructiveHint(), function.isIdempotentHint()
        );
    }

    @NotNull
    private String getFunctionId(@NotNull AIFunctionDescriptor function) {
        // Keep the existing DataDam discovery IDs on the wire.
        return function.getPurpose() == AIFunctionPurpose.DAM ? function.getId() : function.getFullId();
    }

    @NotNull
    private DDMcpFunctionParameter describeParameter(@NotNull AIFunctionParameter parameter) {
        String[] validValues = parameter.getValidValues();
        DDMcpFunctionParameter.Type type;
        try {
            type = DDMcpFunctionParameter.Type.valueOf(parameter.getType().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = DDMcpFunctionParameter.Type.STRING;
        }
        return new DDMcpFunctionParameter(
            parameter.getName(),
            type,
            parameter.getDescription(),
            parameter.isRequired(),
            parameter.getDefaultValue(),
            validValues == null ? null : Arrays.asList(validValues)
        );
    }
}
