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
package org.jkiss.dbeaver.model.datadam.mcp;

import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionDescriptor;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DDMcpFunctionRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.model.datadam.desktopMcpFunction";
    private static DDMcpFunctionRegistry instance;

    private final Map<String, DDMcpFunctionExtensionDescriptor> descriptors = new LinkedHashMap<>();

    private DDMcpFunctionRegistry() {
        for (var extension : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"function".equals(extension.getName())) {
                continue;
            }
            DDMcpFunctionExtensionDescriptor descriptor = new DDMcpFunctionExtensionDescriptor(extension);
            String id = descriptor.getDescriptor().id();
            if (descriptors.putIfAbsent(id, descriptor) != null) {
                throw new IllegalStateException("Duplicate DataDam MCP function: " + id);
            }
        }
    }

    @NotNull
    public static synchronized DDMcpFunctionRegistry getInstance() {
        if (instance == null) {
            instance = new DDMcpFunctionRegistry();
        }
        return instance;
    }

    @NotNull
    public List<DDMcpFunctionDescriptor> getDescriptors() {
        return descriptors.values().stream().map(DDMcpFunctionExtensionDescriptor::getDescriptor).toList();
    }

    @Nullable
    public DDMcpFunctionExtensionDescriptor getDescriptor(@NotNull String id) {
        return descriptors.get(id);
    }
}
