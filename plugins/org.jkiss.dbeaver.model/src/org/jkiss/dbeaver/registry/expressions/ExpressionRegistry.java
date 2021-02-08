/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

public class ExpressionRegistry {

    static final String TAG_FUNCTION = "function"; //$NON-NLS-1$
    static final String TAG_NAMESPACE = "namespace"; //$NON-NLS-1$

    private static ExpressionRegistry instance = null;

    public synchronized static ExpressionRegistry getInstance() {
        if (instance == null) {
            instance = new ExpressionRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ExpressionNamespaceDescriptor> expressionNamespaces = new ArrayList<>();

    private ExpressionRegistry() {
    }

    private void loadExtensions(IExtensionRegistry registry) {
        {
            IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(ExpressionNamespaceDescriptor.EXP_EXTENSION_ID);
            for (IConfigurationElement ext : extConfigs) {
                // Load expression functions
                if (TAG_NAMESPACE.equals(ext.getName())) {
                    this.expressionNamespaces.add(new ExpressionNamespaceDescriptor(ext));
                }
            }
        }
    }

    public void dispose() {
        expressionNamespaces.clear();
    }

    public List<ExpressionNamespaceDescriptor> getExpressionNamespaces() {
        return expressionNamespaces;
    }

}
