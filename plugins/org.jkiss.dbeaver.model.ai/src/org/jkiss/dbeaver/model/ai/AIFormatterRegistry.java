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
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class AIFormatterRegistry {
    public static class FormatterDescriptor extends AbstractDescriptor {
        private final IConfigurationElement contributorConfig;
        protected FormatterDescriptor(IConfigurationElement contributorConfig) {
            super(contributorConfig);
            this.contributorConfig = contributorConfig;
        }

        public String getId() {
            return contributorConfig.getAttribute("id");
        }

        String getReplaces() {
            return contributorConfig.getAttribute("replaces");
        }

        public IAIFormatter createInstance() throws DBException {
            ObjectType objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
            return objectType.createInstance(IAIFormatter.class);
        }
    }

    private static AIFormatterRegistry instance = null;

    public synchronized static AIFormatterRegistry getInstance() {
        if (instance == null) {
            instance = new AIFormatterRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, AIFormatterRegistry.FormatterDescriptor> descriptorMap = new LinkedHashMap<>();
    private final Map<String, String> replaceMap = new LinkedHashMap<>();

    public AIFormatterRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor("com.dbeaver.ai.formatter");
        for (IConfigurationElement ext : extElements) {
            if ("formatter".equals(ext.getName())) {
                FormatterDescriptor descriptor = new FormatterDescriptor(ext);
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

    public IAIFormatter getFormatter(String id) throws DBException {
        while (true) {
            String replace = replaceMap.get(id);
            if (replace == null) {
                break;
            }
            id = replace;
        }
        FormatterDescriptor descriptor = descriptorMap.get(id);
        if (descriptor == null) {
            throw new DBException("AI formatter '" + id + "' not found");
        }
        return descriptor.createInstance();
    }
}
