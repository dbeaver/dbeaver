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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionParameterTransformer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class AIFunctionImplementationDescriptor extends AbstractDescriptor {
    static final String EXTENSION_ID = "com.dbeaver.ai.functionImplementation";
    private static final Log log = Log.getLog(AIFunctionImplementationDescriptor.class);

    private final String functionId;
    private final boolean headless;
    private final ObjectType objectType;
    private final Map<String, ParameterTransformerDescriptor> parameterTransformers = new LinkedHashMap<>();

    AIFunctionImplementationDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.functionId = config.getAttribute("functionId");
        this.headless = CommonUtils.toBoolean(config.getAttribute("headless"));
        this.objectType = new ObjectType(config, RegistryConstants.ATTR_CLASS);
        for (IConfigurationElement child : config.getChildren("parameterTransformer")) {
            String parameterName = child.getAttribute("parameter");
            if (parameterTransformers.putIfAbsent(parameterName, new ParameterTransformerDescriptor(child)) != null) {
                log.error("Duplicate parameter transformer for AI function '" + functionId + "': " + parameterName);
            }
        }
    }

    @NotNull
    String getFunctionId() {
        return functionId;
    }

    boolean isHeadless() {
        return headless;
    }

    boolean hasClass() {
        return CommonUtils.isNotEmpty(objectType.getImplName());
    }

    @NotNull
    AIFunction createInstance() throws DBException {
        return objectType.createInstance(AIFunction.class);
    }

    @NotNull
    Set<String> getTransformedParameters() {
        return parameterTransformers.keySet();
    }

    @Nullable
    AIFunctionParameterTransformer createParameterTransformer(@NotNull String parameterName) throws DBException {
        ParameterTransformerDescriptor descriptor = parameterTransformers.get(parameterName);
        return descriptor == null ? null : descriptor.objectType.createInstance(AIFunctionParameterTransformer.class);
    }

    @Nullable
    String getParameterTransformerSuffix(@NotNull String parameterName) {
        ParameterTransformerDescriptor descriptor = parameterTransformers.get(parameterName);
        return descriptor == null ? null : descriptor.targetSuffix;
    }

    private final class ParameterTransformerDescriptor {
        private final ObjectType objectType;
        private final String targetSuffix;

        private ParameterTransformerDescriptor(@NotNull IConfigurationElement config) {
            this.objectType = new ObjectType(config, RegistryConstants.ATTR_CLASS);
            this.targetSuffix = config.getAttribute("targetSuffix");
        }
    }
}
