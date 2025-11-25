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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class AIFunctionDescriptor extends AbstractDescriptor {

    public static final String EXTENSION_ID = "com.dbeaver.ai.function";

    public class Parameter {
        private static final Log log = Log.getLog(Parameter.class);
        private final IConfigurationElement config;

        Parameter(@NotNull IConfigurationElement config) {
            this.config = config;
        }

        @NotNull
        public String getName() {
            return config.getAttribute("name");
        }

        @NotNull
        public String getType() {
            return config.getAttribute("type");
        }

        @Nullable
        public String getDescription() {
            return config.getAttribute("description");
        }

        public boolean isRequired() {
            return CommonUtils.getBoolean(config.getAttribute("required"));
        }

        @Nullable
        public String[] getValidValues() {
            String validValues = config.getAttribute("validValues");
            if (CommonUtils.isEmpty(validValues) && CommonUtils.isNotEmpty(config.getAttribute("validValuesProvider"))) {
                ObjectType validValuesProvider = new ObjectType(config, "validValuesProvider");
                try {
                    var provider = validValuesProvider.createInstance(IPropertyValueListProvider.class);
                    Object[] validObjects = provider.getPossibleValues(this);
                    return (String[]) validObjects;
                } catch (DBException e) {
                    log.error("Error on getting valid values from provider", e);
                }
            }
            return CommonUtils.isEmpty(validValues) ? null : validValues.split(",");
        }
    }

    private final IConfigurationElement contributorConfig;
    private final ObjectType objectType;
    private final String id;
    private final String name;
    private final DBPImage icon;
    private final boolean global;
    private final boolean hidden;
    private final AIFunctionResult.FunctionType type;
    private final String[] dependsOn;
    private final String categoryId;
    private final Parameter[] parameters;

    public AIFunctionDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.contributorConfig = config;
        this.objectType = new ObjectType(config, RegistryConstants.ATTR_CLASS);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.id = config.getAttribute("id");
        this.name = config.getAttribute("name");
        this.global = CommonUtils.toBoolean(config.getAttribute("global"));
        this.hidden = CommonUtils.toBoolean(config.getAttribute("hidden"));
        this.categoryId = config.getAttribute("categoryId");
        this.dependsOn = CommonUtils.splitString(config.getAttribute("dependsOn"), ',').toArray(new String[0]);
        this.type = CommonUtils.valueOf(
            AIFunctionResult.FunctionType.class,
            config.getAttribute("type"),
            AIFunctionResult.FunctionType.INFORMATION
        );

        List<Parameter> params = new ArrayList<>();
        for (IConfigurationElement pe : config.getChildren("parameter")) {
            params.add(new Parameter(pe));
        }
        this.parameters = params.toArray(new Parameter[0]);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    public AIFunctionResult.FunctionType getType() {
        return type;
    }

    @Nullable
    public String getDescription() {
        return contributorConfig.getAttribute("description");
    }

    /**
     * Global functions are passed in ALL requests
     */
    public boolean isGlobal() {
        return global;
    }

    public boolean isHidden() {
        return hidden;
    }

    @NotNull
    public Parameter[] getParameters() {
        return parameters;
    }

    @NotNull
    public String[] getDependsOn() {
        return dependsOn;
    }

    @NotNull
    public AIFunction createInstance() throws DBException {
        try {
            return objectType.createInstance(AIFunction.class);
        } catch (Exception e) {
            throw new DBException("Error creating AI function " + getId(), e);
        }
    }

    public boolean isApplicable(@NotNull AIEngineDescriptor engine, @NotNull AIPromptGenerator prompt) {
        return false;
    }

    @Override
    public String toString() {
        return "AI function: " + getId();
    }

    @NotNull
    public String getSignature() {
        return getId();
    }

    @Nullable
    public String getCategoryId() {
        return categoryId;
    }
}
