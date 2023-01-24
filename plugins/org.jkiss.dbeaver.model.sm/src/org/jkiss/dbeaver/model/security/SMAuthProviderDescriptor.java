/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import java.util.ArrayList;
import java.util.List;

/**
 * Auth provider descriptor
 */
public class SMAuthProviderDescriptor {

    private String id;
    private String label;
    private String description;
    private String icon;
    private List<SMAuthCredentialsProfile> credentialProfiles;
    private List<SMAuthProviderCustomConfiguration> customConfigurations;

    public SMAuthProviderDescriptor() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<SMAuthCredentialsProfile> getCredentialProfiles() {
        return credentialProfiles;
    }

    public void setCredentialProfiles(List<SMAuthCredentialsProfile> credentialProfiles) {
        this.credentialProfiles = credentialProfiles;
    }

    public List<SMAuthProviderCustomConfiguration> getCustomConfigurations() {
        return customConfigurations;
    }

    public void setCustomConfigurations(List<SMAuthProviderCustomConfiguration> customConfigurations) {
        this.customConfigurations = customConfigurations;
    }

    public void addCustomConfiguration(SMAuthProviderCustomConfiguration customConfiguration) {
        if (this.customConfigurations == null) {
            this.customConfigurations = new ArrayList<>();
        }
        this.customConfigurations.add(customConfiguration);
    }

}
