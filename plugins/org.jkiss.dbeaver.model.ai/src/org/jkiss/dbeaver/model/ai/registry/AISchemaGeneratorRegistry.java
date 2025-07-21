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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * Registry for AI-based DDL generators.
 */
public class AISchemaGeneratorRegistry
    extends AbstractReplaceableRegistry<AISchemaGenerator,
    AISchemaGeneratorRegistry.GeneratorDescriptorAbstract> {

    private static final AISchemaGeneratorRegistry INSTANCE =
        new AISchemaGeneratorRegistry();

    public static AISchemaGeneratorRegistry getInstance() {
        return INSTANCE;
    }

    private AISchemaGeneratorRegistry() {
        super(Platform.getExtensionRegistry(), "com.dbeaver.ai.schema.generator", "generator");
    }

    public AISchemaGenerator getDdlGenerator() throws DBException {
        return get("core");
    }

    public static class GeneratorDescriptorAbstract
        extends AbstractReplaceableDescriptor<AISchemaGenerator> {

        private final IConfigurationElement cfg;

        GeneratorDescriptorAbstract(IConfigurationElement cfg) {
            super(cfg);
            this.cfg = cfg;
        }

        @Override
        public AISchemaGenerator createInstance() throws DBException {
            return new ObjectType(cfg, RegistryConstants.ATTR_CLASS)
                .createInstance(AISchemaGenerator.class);
        }
    }

    @Override
    protected GeneratorDescriptorAbstract createDescriptor(IConfigurationElement cfg) {
        return new GeneratorDescriptorAbstract(cfg);
    }
}

