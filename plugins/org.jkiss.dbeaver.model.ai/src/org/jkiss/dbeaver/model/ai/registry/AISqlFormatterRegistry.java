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
import org.jkiss.dbeaver.model.ai.AISqlFormatter;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * Registry for SQL post-processors that may replace each other.
 */
public class AISqlFormatterRegistry
    extends AbstractReplaceableRegistry<AISqlFormatter,
    AISqlFormatterRegistry.SqlFormatterDescriptorAbstract> {

    private static final AISqlFormatterRegistry INSTANCE =
        new AISqlFormatterRegistry();

    public static AISqlFormatterRegistry getInstance() {
        return INSTANCE;
    }

    private AISqlFormatterRegistry() {
        super(Platform.getExtensionRegistry(), "com.dbeaver.ai.sql.formatter", "formatter");
    }

    public AISqlFormatter getSqlPostProcessor() throws DBException {
        return get("core");
    }

    public static class SqlFormatterDescriptorAbstract
        extends AbstractReplaceableDescriptor<AISqlFormatter> {

        private final IConfigurationElement cfg;

        SqlFormatterDescriptorAbstract(IConfigurationElement cfg) {
            super(cfg);
            this.cfg = cfg;
        }

        @Override
        public AISqlFormatter createInstance() throws DBException {
            return new ObjectType(cfg, RegistryConstants.ATTR_CLASS)
                .createInstance(AISqlFormatter.class);
        }
    }

    @Override
    protected SqlFormatterDescriptorAbstract createDescriptor(IConfigurationElement cfg) {
        return new SqlFormatterDescriptorAbstract(cfg);
    }
}
