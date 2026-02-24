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
package org.jkiss.dbeaver.model.ai;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * AI function metadata.
 */
public interface AIFunctionDescriptor {

    @NotNull
    String getId();

    @NotNull
    String getName();
    
    @Nullable
    DBPImage getIcon();

    @NotNull
    AIFunctionResult.FunctionType getType();

    @NotNull
    AIFunctionPurpose getPurpose();

    @Nullable
    String getCategoryId();

    @Nullable
    String getDescription();

    /**
     * Global functions are passed in ALL requests
     */
    boolean isGlobal();

    boolean isHidden();

    @NotNull
    Parameter[] getParameters();

    @NotNull
    String[] getDependsOn();

    boolean isApplicable(@NotNull AIEngineDescriptor engine, @NotNull AIPromptGenerator prompt);

    @NotNull
    AIFunction createInstance() throws DBException;

    class Parameter {
        private final IConfigurationElement config;

        public Parameter(@NotNull IConfigurationElement config) {
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

        public String getDefaultValue() {
            return config.getAttribute("defaultValue");
        }

        @Nullable
        public String[] getValidValues() {
            String validValues = config.getAttribute("validValues");
            return CommonUtils.isEmpty(validValues) ? null : validValues.split(",");
        }
    }


}
