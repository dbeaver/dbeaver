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
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSourceSupplier;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class AIPromptGeneratorDescriptor extends AbstractDescriptor {

    public static final String EXTENSION_ID = "com.dbeaver.ai.prompt";

    private final IConfigurationElement contributorConfig;
    private final ObjectType objectType;
    private final String label;
    private final DBPImage icon;
    private final String[] dependsOn;

    protected AIPromptGeneratorDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        this.contributorConfig = config;
        this.objectType = new ObjectType(config, RegistryConstants.ATTR_CLASS);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.dependsOn = CommonUtils.splitString(config.getAttribute("dependsOn"), ',').toArray(new String[0]);
    }

    @NotNull
    public String getId() {
        return contributorConfig.getAttribute("id");
    }

    @Nullable
    public DBPImage getIcon() {
        return icon;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @NotNull
    public String[] getDependsOn() {
        return dependsOn;
    }

    @NotNull
    public AIPromptGenerator createGenerator(@NotNull DBSLogicalDataSourceSupplier dataSource) throws DBException {
        Class<? extends AIPromptGenerator> objectClass = objectType.getObjectClass(AIPromptGenerator.class);
        if (objectClass == null) {
            throw new DBException("Object class " + objectType.getImplName() + " not found");
        }
        try {
            Method createMethod = objectClass.getMethod("create", DBSLogicalDataSourceSupplier.class);
            if (Modifier.isStatic(createMethod.getModifiers())) {
                return (AIPromptGenerator) createMethod.invoke(null, dataSource);
            } else {
                throw new DBException("Prompt method '" + createMethod + "' is not static");
            }
        } catch (Exception e) {
            throw new DBException("Error creating prompt generator " + getId(), e);
        }
    }
}
