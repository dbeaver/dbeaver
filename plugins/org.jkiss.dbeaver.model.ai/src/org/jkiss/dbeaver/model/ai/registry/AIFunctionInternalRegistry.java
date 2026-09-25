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
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;

/**
 * AI function registry
 */
public class AIFunctionInternalRegistry {
    private static final Log log = Log.getLog(AIFunctionInternalRegistry.class);

    private final Map<String, AIFunctionDescriptor> functionsById = new LinkedHashMap<>();
    private final Map<String, AIFunctionDescriptor> functionsByLegacyId = new LinkedHashMap<>();

    public AIFunctionInternalRegistry(@NotNull AIToolboxInternalDescriptor toolbox) {
        boolean headless = DBWorkbench.getPlatform().getApplication().isHeadlessMode();
        Map<String, AIFunctionImplementationDescriptor> implementations = loadImplementations(headless);
        IConfigurationElement[] extElements = Platform.getExtensionRegistry()
            .getConfigurationElementsFor(AIFunctionInternalDescriptor.EXTENSION_ID);
        Set<String> definitionIds = new HashSet<>();
        List<AIFunctionInternalDescriptor> descriptors = new ArrayList<>();
        for (IConfigurationElement ext : extElements) {
            if ("function".equals(ext.getName())) {
                String functionId = ext.getAttribute("id");
                definitionIds.add(functionId);
                AIFunctionInternalDescriptor fd = new AIFunctionInternalDescriptor(
                    toolbox,
                    ext,
                    implementations.get(functionId)
                );
                if (!fd.hasImplementation()) {
                    continue;
                }
                descriptors.add(fd);
            }
        }
        registerValidDescriptors(descriptors);
        for (String functionId : implementations.keySet()) {
            if (!definitionIds.contains(functionId)) {
                log.error("AI function implementation references an unknown function: " + functionId);
            }
        }
    }

    private void registerValidDescriptors(@NotNull List<AIFunctionInternalDescriptor> descriptors) {
        Map<String, Integer> identityCounts = new HashMap<>();
        for (AIFunctionInternalDescriptor descriptor : descriptors) {
            identityCounts.merge(descriptor.getId(), 1, Integer::sum);
            String legacyId = descriptor.getLegacyId();
            if (legacyId != null) {
                identityCounts.merge(legacyId, 1, Integer::sum);
            }
        }
        for (AIFunctionInternalDescriptor descriptor : descriptors) {
            Set<String> identities = new LinkedHashSet<>();
            identities.add(descriptor.getId());
            String legacyId = descriptor.getLegacyId();
            if (legacyId != null) {
                identities.add(legacyId);
            }
            if (identities.stream().anyMatch(id -> identityCounts.get(id) > 1)) {
                log.error("Conflicting AI function identifiers: " + identities);
                continue;
            }
            functionsById.put(descriptor.getId(), descriptor);
            if (legacyId != null) {
                functionsByLegacyId.put(legacyId, descriptor);
            }
        }
    }

    @NotNull
    private static Map<String, AIFunctionImplementationDescriptor> loadImplementations(boolean headless) {
        Map<String, AIFunctionImplementationDescriptor> implementations = new LinkedHashMap<>();
        Set<String> conflictingImplementations = new HashSet<>();
        for (IConfigurationElement ext : Platform.getExtensionRegistry().getConfigurationElementsFor(
            AIFunctionImplementationDescriptor.EXTENSION_ID)
        ) {
            if (!"implementation".equals(ext.getName())) {
                continue;
            }
            AIFunctionImplementationDescriptor implementation = new AIFunctionImplementationDescriptor(ext);
            if (implementation.isHeadless() != headless) {
                continue;
            }
            String functionId = implementation.getFunctionId();
            if (conflictingImplementations.contains(functionId)) {
                continue;
            }
            if (implementations.putIfAbsent(functionId, implementation) != null) {
                log.error("Duplicate AI function implementation: " + implementation.getFunctionId());
                implementations.remove(functionId);
                conflictingImplementations.add(functionId);
            }
        }
        return implementations;
    }

    @Nullable
    public AIFunctionDescriptor getFunction(@NotNull String id) {
        AIFunctionDescriptor function = functionsById.get(id);
        return function != null ? function : functionsByLegacyId.get(id);
    }

    @NotNull
    public List<AIFunctionDescriptor> getAllFunctions(@NotNull AIFunctionPurpose purpose) {
        return functionsById.values().stream()
            .filter(f -> functionFilter(f, purpose))
            .toList();
    }

    private boolean functionFilter(@NotNull AIFunctionDescriptor descriptor, @NotNull AIFunctionPurpose purpose) {
        return descriptor.getPurpose() == purpose
            || descriptor.getPurpose() == AIFunctionPurpose.ALL
            || purpose == AIFunctionPurpose.ALL;
    }

    @NotNull
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor descriptor,
        @NotNull Map<String, Object> arguments
    ) throws DBException {
        AIFunction function = descriptor.getInstance();
        return function.callFunction(context, arguments);
    }
}
