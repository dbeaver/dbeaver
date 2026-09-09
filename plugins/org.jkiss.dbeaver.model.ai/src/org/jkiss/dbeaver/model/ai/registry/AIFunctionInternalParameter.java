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
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.AIFunctionParameterTransformer;
import org.jkiss.dbeaver.model.ai.AIFunctionParameterValueProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

public class AIFunctionInternalParameter extends AbstractDescriptor implements AIFunctionParameter {
    private static final Log log = Log.getLog(AIFunctionInternalParameter.class);

    private final IConfigurationElement config;
    private String targetSuffix;
    private AIFunctionParameterTransformer transformer;
    private AIFunctionParameterValueProvider validValuesProvider;

    public AIFunctionInternalParameter(@NotNull IConfigurationElement config) {
        super(config);
        this.config = config;
        String transformerClass = this.config.getAttribute("transformer");
        if (!CommonUtils.isEmpty(transformerClass)) {
            try {
                transformer = new ObjectType(transformerClass).createInstance(AIFunctionParameterTransformer.class);
                targetSuffix = this.config.getAttribute("targetSuffix");
            } catch (DBException e) {
                log.error("Error creating transformer");
            }
        }
        String validValuesProviderClass = this.config.getAttribute("validValuesProvider");
        if (!CommonUtils.isEmpty(validValuesProviderClass)) {
            try {
                validValuesProvider = new ObjectType(validValuesProviderClass)
                    .createInstance(AIFunctionParameterValueProvider.class);
            } catch (DBException e) {
                log.error("Error creating valid values provider", e);
            }
        }
    }

    @Override
    @NotNull
    public String getName() {
        return config.getAttribute("name");
    }

    @Override
    @NotNull
    public String getType() {
        return config.getAttribute("type");
    }

    @Override
    @Nullable
    public String getDescription() {
        String description = config.getAttribute("description");
        if (validValuesProvider != null) {
            String suffix = validValuesProvider.getValidValuesDescription();
            if (!CommonUtils.isEmpty(suffix)) {
                return CommonUtils.isEmpty(description) ? suffix : description + ". " + suffix;
            }
        }
        return description;
    }

    @Override
    public boolean isRequired() {
        return CommonUtils.getBoolean(config.getAttribute("required"));
    }

    @Override
    @Nullable
    public String getDefaultValue() {
        return config.getAttribute("defaultValue");
    }

    @Override
    @Nullable
    public String[] getValidValues() {
        if (validValuesProvider != null) {
            return validValuesProvider.getValidValues();
        }
        String validValues = config.getAttribute("validValues");
        return CommonUtils.isEmpty(validValues) ? null : validValues.split(",");
    }

    @Nullable
    @Override
    public AIFunctionParameterTransformer getTransformer() {
        return transformer;
    }

    @Nullable
    @Override
    public String getTransformerSuffix() {
        return targetSuffix;
    }
}
