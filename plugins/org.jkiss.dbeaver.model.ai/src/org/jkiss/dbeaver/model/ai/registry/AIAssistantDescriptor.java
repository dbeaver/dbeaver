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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.ai.AISqlFormatter;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

public class AIAssistantDescriptor extends AbstractDescriptor {

    public static final String EXTENSION_ID = "com.dbeaver.ai.assistant";
    private final ObjectType objectType;
    private final ObjectType formatterType;
    private final ObjectType schemaGeneratorType;
    private final int priority;

    protected AIAssistantDescriptor(IConfigurationElement contributorConfig) {
        super(contributorConfig);
        this.objectType = new ObjectType(contributorConfig, RegistryConstants.ATTR_CLASS);
        this.formatterType = new ObjectType(contributorConfig, "sqlFormatter");
        this.schemaGeneratorType = new ObjectType(contributorConfig, "schemaGenerator");
        this.priority = CommonUtils.toInt(contributorConfig.getAttribute("priority"));
    }

    @NotNull
    public AIAssistant createInstance(DBPWorkspace workspace) throws DBException {
        return objectType.createInstance(AIAssistant.class, workspace);
    }

    @NotNull
    public AISqlFormatter createSqlFormatter() throws DBException {
        return formatterType.createInstance(AISqlFormatter.class);
    }

    @NotNull
    public AISchemaGenerator createSchemaGenerator() throws DBException {
        return schemaGeneratorType.createInstance(AISchemaGenerator.class);
    }

    public int getPriority() {
        return priority;
    }
}