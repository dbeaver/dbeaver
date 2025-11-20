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
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

public class AIEngineDescriptor extends AbstractDescriptor {

    public static final String EXTENSION_ID = "com.dbeaver.ai.engine";

    private final IConfigurationElement contributorConfig;
    private final String id;
    private final ObjectType objectType;
    private final ObjectType propertiesType;
    private final boolean supportsFunctions;

    protected AIEngineDescriptor(@NotNull IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.contributorConfig = contributorConfig;
        this.id = contributorConfig.getAttribute("id");
        this.objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        this.supportsFunctions = CommonUtils.toBoolean(contributorConfig.getAttribute("supportsFunctions"));
        this.propertiesType = new ObjectType(contributorConfig, "properties");
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getLabel() {
        return contributorConfig.getAttribute("label");
    }

    @Nullable
    public String getReplaces() {
        return contributorConfig.getAttribute("replaces");
    }

    @Nullable
    public String getFallbacks() {
        return contributorConfig.getAttribute("fallbacks");
    }

    public boolean isDefault() {
        return CommonUtils.toBoolean(contributorConfig.getAttribute("default"));
    }

    public boolean isSupportsFunctions() {
        return supportsFunctions;
    }

    @NotNull
    public Class<? extends AIEngineProperties> getPropertiesType() {
        return propertiesType.getImplClass(AIEngineProperties.class);
    }

    @NotNull
    public <T extends AIEngineProperties> T createPropertiesInstance() throws DBException {
        return (T) propertiesType.createInstance(AIEngineProperties.class);
    }

    @NotNull
    public ObjectType getEngineObjectType() {
        return objectType;
    }

    @NotNull
    public AIEngine createEngineInstance() throws DBException {
        return createEngineInstance(AISettingsManager.getInstance().getSettings().getEngineConfiguration(getId()));
    }

    @NotNull
    public AIEngine createEngineInstance(@NotNull AIEngineProperties properties) throws DBException {
        return objectType.createInstance(AIEngine.class, properties);
    }
}
