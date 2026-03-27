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
import org.jkiss.dbeaver.model.ai.engine.AICredentialsProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.stream.Stream;

public class DBACredentialsProviderDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "com.dbeaver.ai.credentialsProvider";
    private final ObjectType objectType;
    private final String id;
    private final List<String> supportedEngines;
    private final boolean isDefault;
    private final String authTypeName;

    DBACredentialsProviderDescriptor(@NotNull IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.id = contributorConfig.getAttribute(RegistryConstants.ATTR_ID);
        this.supportedEngines = mapEngines(contributorConfig);
        this.authTypeName = contributorConfig.getAttribute("label");
        this.objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        this.isDefault = CommonUtils.toBoolean(contributorConfig.getAttribute("default"), false);
    }

    @NotNull
    private static List<String> mapEngines(@NotNull IConfigurationElement contributorConfig) {
        String engineIDString = contributorConfig.getAttribute("supportedEngines");
        //split by comma and trim
        if (engineIDString == null || engineIDString.isEmpty()) {
            return List.of();
        }
        String[] engineIDs = engineIDString.split(",");
        return Stream.of(engineIDs).map(String::trim).toList();
    }

    @NotNull
    public List<String> getSupportedEngines() {
        return supportedEngines;
    }

    @NotNull
    public String getAuthTypeName() {
        return authTypeName;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @NotNull
    public ObjectType getEngineObjectType() {
        return objectType;
    }

    @NotNull
    public AICredentialsProvider<?> createProviderInstance() throws Exception {
        return objectType.createInstance(AICredentialsProvider.class);
    }

    @Nullable
    public Class<?> getProviderClass() {
        return objectType.getObjectClass();
    }
}
