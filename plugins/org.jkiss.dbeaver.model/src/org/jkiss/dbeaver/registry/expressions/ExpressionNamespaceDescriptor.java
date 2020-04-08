/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry.expressions;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;

/**
 * ExpressionNamespaceDescriptor
 */
public class ExpressionNamespaceDescriptor extends AbstractContextDescriptor {

    public static final String EXP_EXTENSION_ID = "org.jkiss.dbeaver.expressions"; //$NON-NLS-1$

    private final String id;
    private final String description;
    private final ObjectType implClass;

    public ExpressionNamespaceDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getImplClass() {
        return implClass.getObjectClass();
    }

}
