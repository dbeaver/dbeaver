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
import org.jkiss.dbeaver.DBRuntimeException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class AIFunctionInternalDescriptor extends AbstractDescriptor implements AIFunctionDescriptor {

    public static final String EXTENSION_ID = "com.dbeaver.ai.function";

    private final AIToolboxInternalDescriptor toolbox;
    private final ObjectType objectType;
    private final String id;
    private final String name;
    private final DBPImage icon;
    private final boolean global;
    private final boolean hidden;
    private final boolean ui;
    private boolean enabledByDefault;
    private final AIFunctionPurpose purpose;
    private final AIFunctionType type;
    private final String[] dependsOn;
    private final String description;
    private final String categoryId;
    private final AIFunctionInternalParameter[] parameters;
    private transient AIFunction instance;

    public AIFunctionInternalDescriptor(
        @NotNull AIToolboxInternalDescriptor toolbox,
        @NotNull IConfigurationElement config
    ) {
        super(config);
        this.toolbox = toolbox;
        this.objectType = new ObjectType(config, RegistryConstants.ATTR_CLASS);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.ui = CommonUtils.toBoolean(config.getAttribute("ui"));
        this.global = CommonUtils.toBoolean(config.getAttribute("global"));
        this.hidden = CommonUtils.toBoolean(config.getAttribute("hidden"));
        this.enabledByDefault = CommonUtils.toBoolean(config.getAttribute("enabledByDefault"));
        this.purpose = CommonUtils.valueOf(AIFunctionPurpose.class, config.getAttribute("purpose"), AIFunctionPurpose.TOOL);
        this.categoryId = config.getAttribute("categoryId");
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.dependsOn = CommonUtils.splitString(config.getAttribute("dependsOn"), ',').toArray(new String[0]);
        this.type = CommonUtils.valueOf(
            AIFunctionType.class,
            config.getAttribute("type"),
            AIFunctionType.INFORMATION
        );

        List<AIFunctionInternalParameter> params = new ArrayList<>();
        for (IConfigurationElement pe : config.getChildren("parameter")) {
            params.add(new AIFunctionInternalParameter(pe));
        }
        this.parameters = params.toArray(new AIFunctionInternalParameter[0]);
    }

    @NotNull
    @Override
    public AIToolbox getToolbox() {
        return toolbox;
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
    public AIFunctionType getType() {
        return type;
    }

    @NotNull
    public AIFunctionPurpose getPurpose() {
        return purpose;
    }

    @Nullable
    public String getCategoryId() {
        return categoryId;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isUI() {
        return ui;
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

    @Override
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    @NotNull
    public AIFunctionParameter[] getParameters() {
        return parameters;
    }

    @NotNull
    public String[] getDependsOn() {
        return dependsOn;
    }

    @NotNull
    public AIFunction getInstance() {
        if (instance == null) {
            try {
                instance = objectType.createInstance(AIFunction.class);
            } catch (Exception e) {
                throw new DBRuntimeException("Error creating AI function " + getId(), e);
            }
        }
        return instance;
    }

    public boolean isApplicable(@NotNull AIEngineDescriptor engine, @NotNull AIPromptGenerator prompt) {
        return true;
    }

    @Override
    public String toString() {
        return "AI function: " + getId();
    }

}
