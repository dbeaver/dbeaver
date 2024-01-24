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
package org.jkiss.dbeaver.model.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auth provider custom configuration
 */
public class SMAuthProviderCustomConfiguration {

    private String id;
    private String provider;
    private String displayName;
    private boolean disabled;
    private String iconURL;
    private String description;
    private Map<String, Object> parameters = new LinkedHashMap<>();

    private SMAuthProviderCustomConfiguration() {
    }

    public SMAuthProviderCustomConfiguration(SMAuthProviderCustomConfiguration src) {
        this.id = src.id;
        this.provider = src.provider;
        this.displayName = src.displayName;
        this.disabled = src.disabled;
        this.iconURL = src.iconURL;
        this.description = src.description;
        this.parameters = new LinkedHashMap<>(src.parameters);
    }

    public SMAuthProviderCustomConfiguration(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getIconURL() {
        return iconURL;
    }

    public void setIconURL(String iconURL) {
        this.iconURL = iconURL;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public <T> T getParameter(String name) {
        return (T) parameters.get(name);
    }

    public <T> T getParameterOrDefault(String name, T defaultValue) {
        return (T) parameters.getOrDefault(name, defaultValue);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SMAuthProviderCustomConfiguration &&
            id.equals(((SMAuthProviderCustomConfiguration) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
