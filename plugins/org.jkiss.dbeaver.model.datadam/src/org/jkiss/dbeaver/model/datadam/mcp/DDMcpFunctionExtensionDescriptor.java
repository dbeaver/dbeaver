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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionParameter;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

import java.util.Arrays;
import java.util.Locale;

public class DDMcpFunctionExtensionDescriptor extends AbstractDescriptor {
    private final ObjectType objectType;
    private final DDMcpFunctionDescriptor descriptor;
    @Nullable
    private DDMcpFunction instance;

    public DDMcpFunctionExtensionDescriptor(@NotNull IConfigurationElement extension) {
        super(extension);
        this.objectType = new ObjectType(extension, "class");
        this.descriptor = new DDMcpFunctionDescriptor(
            extension.getAttribute("id"),
            extension.getAttribute("name"),
            extension.getAttribute("description"),
            Arrays.stream(extension.getChildren("parameter")).map(this::readParameter).toList()
        );
    }

    @NotNull
    public DDMcpFunctionDescriptor getDescriptor() {
        return descriptor;
    }

    @NotNull
    public synchronized DDMcpFunction getInstance(@NotNull Object... arguments) {
        if (instance == null) {
            try {
                instance = objectType.createInstance(DDMcpFunction.class, arguments);
            } catch (DBException e) {
                throw new IllegalStateException("Cannot create DataDam MCP function " + descriptor.id(), e);
            }
        }
        return instance;
    }

    @NotNull
    private DDMcpFunctionParameter readParameter(@NotNull IConfigurationElement parameter) {
        String validValues = parameter.getAttribute("validValues");
        return new DDMcpFunctionParameter(
            parameter.getAttribute("name"),
            DDMcpFunctionParameter.Type.valueOf(parameter.getAttribute("type").toUpperCase(Locale.ROOT)),
            parameter.getAttribute("description"),
            Boolean.parseBoolean(parameter.getAttribute("required")),
            parameter.getAttribute("defaultValue"),
            validValues == null ? null : Arrays.asList(validValues.split(","))
        );
    }
}
