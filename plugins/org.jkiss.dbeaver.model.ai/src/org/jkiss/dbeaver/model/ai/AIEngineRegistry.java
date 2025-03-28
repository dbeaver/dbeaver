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
package org.jkiss.dbeaver.model.ai;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI engine settings
 */
public class AIEngineRegistry {

    private static final Log log = Log.getLog(AIEngineRegistry.class);

    private static AIEngineRegistry instance = null;

    public synchronized static AIEngineRegistry getInstance() {
        if (instance == null) {
            instance = new AIEngineRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, AIEngineDescriptor> descriptorMap = new LinkedHashMap<>();
    private final Map<String, String> replaceMap = new LinkedHashMap<>();

    public AIEngineRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor("com.dbeaver.ai.engine");
        for (IConfigurationElement ext : extElements) {
            if ("completionEngine".equals(ext.getName())) {
                AIEngineDescriptor descriptor = new AIEngineDescriptor(ext);
                descriptorMap.put(descriptor.getId(), descriptor);

                String replaces = descriptor.getReplaces();
                if (!CommonUtils.isEmpty(replaces)) {
                    for (String rl : replaces.split(",")) {
                        replaceMap.put(rl, descriptor.getId());
                    }
                }
            }
        }
    }

    public List<AIEngineDescriptor> getCompletionEngines() {
        List<AIEngineDescriptor> list = new ArrayList<>();
        for (Map.Entry<String, AIEngineDescriptor> entry : descriptorMap.entrySet()) {
            if (replaceMap.containsKey(entry.getKey())) {
                continue;
            }
            list.add(entry.getValue());
        }
        return list;
    }

    public AIEngineDescriptor getDefaultCompletionEngineDescriptor() {
        return getCompletionEngines().stream().filter(AIEngineDescriptor::isDefault).findFirst().orElse(null);
    }

    public DAICompletionEngine getCompletionEngine(String id) throws DBException {
        AIEngineDescriptor descriptor = getEngineDescriptor(id);
        if (descriptor == null) {
            log.warn("Active engine is not present in the configuration, switching to default active engine");
            AIEngineDescriptor defaultCompletionEngineDescriptor = getDefaultCompletionEngineDescriptor();
            if (defaultCompletionEngineDescriptor == null) {
                throw new DBException("AI engine '" + id + "' not found");
            }
            descriptor = defaultCompletionEngineDescriptor;
        }
        return descriptor.createInstance();
    }

    public AIEngineDescriptor getEngineDescriptor(String id) {
        while (true) {
            String replace = replaceMap.get(id);
            if (replace == null) {
                break;
            }
            id = replace;
        }
        return descriptorMap.get(id);
    }

}
