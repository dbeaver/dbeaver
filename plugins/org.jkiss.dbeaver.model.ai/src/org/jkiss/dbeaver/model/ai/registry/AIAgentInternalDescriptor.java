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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionPurpose;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

import java.util.List;
import java.util.Map;

public class AIAgentInternalDescriptor extends AbstractDescriptor implements AIAgentDescriptor {

    public static final String EXTENSION_ID = "com.dbeaver.ai.agent";

    private final String id;
    private final String name;
    private final String description;

    public AIAgentInternalDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
    }

    @Override
    @NotNull
    public String getAgentId() {
        return id;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isAccessible() {
        return true;
    }

    @NotNull
    @Override
    public List<AIFunctionDescriptor> getSupportedFunctions() {
        return AIFunctionRegistry.getInstance().getAllFunctions(AIFunctionPurpose.ALL);
    }

    @Nullable
    @Override
    public AIFunctionDescriptor getFunctionById(@NotNull String id) {
        return AIFunctionRegistry.getInstance().getFunction(id);
    }

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        return AIFunctionRegistry.getInstance().callFunction(context, descriptor, arguments);
    }

}
