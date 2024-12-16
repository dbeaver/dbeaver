/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.data.hints;

import org.jkiss.dbeaver.Log;

import java.util.Map;

/**
 * ValueHintProviderConfiguration
 */
public class ValueHintProviderConfiguration {
    private static final Log log = Log.getLog(ValueHintProviderConfiguration.class);

    private transient final String hintProviderDescriptor;
    private boolean enabled;
    private Map<String, Object> parameters;

    public ValueHintProviderConfiguration(String hintProviderDescriptor) {
        this.hintProviderDescriptor = hintProviderDescriptor;
    }

    public String getHintProviderDescriptor() {
        return hintProviderDescriptor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}