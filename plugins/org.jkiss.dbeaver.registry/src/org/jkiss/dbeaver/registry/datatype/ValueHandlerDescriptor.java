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
package org.jkiss.dbeaver.registry.datatype;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * ValueHandlerDescriptor
 */
public class ValueHandlerDescriptor extends DataTypeAbstractDescriptor<DBDValueHandlerProvider> {
    @Nullable
    private final String parentProvider;

    public ValueHandlerDescriptor(IConfigurationElement config)
    {
        super(config, DBDValueHandlerProvider.class);
        this.parentProvider = config.getAttribute(RegistryConstants.ATTR_PARENT);
    }

    @Nullable
    public String getParentProvider() {
        return parentProvider;
    }

    public boolean isChildOf(ValueHandlerDescriptor descriptor) {
        return parentProvider != null && parentProvider.equals(descriptor.getId());
    }
}