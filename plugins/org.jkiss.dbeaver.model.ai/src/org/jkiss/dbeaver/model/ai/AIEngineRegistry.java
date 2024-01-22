/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.ArrayUtils;
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

    public static class EngineDescriptor extends AbstractDescriptor {

        private final IConfigurationElement contributorConfig;
        private final List<DBPPropertyDescriptor> properties = new ArrayList<>();
        protected EngineDescriptor(IConfigurationElement contributorConfig) {
            super(contributorConfig);
            this.contributorConfig = contributorConfig;
            for (IConfigurationElement propGroup : ArrayUtils.safeArray(contributorConfig.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
                properties.addAll(PropertyDescriptor.extractProperties(propGroup));
            }
        }

        public String getId() {
            return contributorConfig.getAttribute("id");
        }

        public String getLabel() {
            return contributorConfig.getAttribute("label");
        }

        String getReplaces() {
            return contributorConfig.getAttribute("replaces");
        }

        public boolean isDefault() {
            return CommonUtils.toBoolean(contributorConfig.getAttribute("default"));
        }

        public List<DBPPropertyDescriptor> getProperties() {
            return properties;
        }

        public DAICompletionEngine<?> createInstance() throws DBException {
            ObjectType objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
            return objectType.createInstance(DAICompletionEngine.class);
        }

    }

    private static AIEngineRegistry instance = null;

    public synchronized static AIEngineRegistry getInstance() {
        if (instance == null) {
            instance = new AIEngineRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, EngineDescriptor> descriptorMap = new LinkedHashMap<>();
    private final Map<String, String> replaceMap = new LinkedHashMap<>();

    public AIEngineRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor("com.dbeaver.ai.engine");
        for (IConfigurationElement ext : extElements) {
            if ("completionEngine".equals(ext.getName())) {
                EngineDescriptor descriptor = new EngineDescriptor(ext);
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

    public List<EngineDescriptor> getCompletionEngines() {
        List<EngineDescriptor> list = new ArrayList<>();
        for (Map.Entry<String, EngineDescriptor> entry : descriptorMap.entrySet()) {
            if (replaceMap.containsKey(entry.getKey())) {
                continue;
            }
            list.add(entry.getValue());
        }
        return list;
    }

    public EngineDescriptor getDefaultCompletionEngineDescriptor() {
        return getCompletionEngines().stream().filter(EngineDescriptor::isDefault).findFirst().orElse(null);
    }

    public DAICompletionEngine<?> getCompletionEngine(String id) throws DBException {
        EngineDescriptor descriptor = getEngineDescriptor(id);
        if (descriptor == null) {
            log.warn("Active engine is not present in the configuration, switching to default active engine");
            EngineDescriptor defaultCompletionEngineDescriptor = getDefaultCompletionEngineDescriptor();
            if (defaultCompletionEngineDescriptor == null) {
                throw new DBException("AI engine '" + id + "' not found");
            }
            descriptor = defaultCompletionEngineDescriptor;
        }
        return descriptor.createInstance();
    }

    public EngineDescriptor getEngineDescriptor(String id) {
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
