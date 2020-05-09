/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.connection;

import graphql.execution.nextgen.Common;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration profile.
 */
public class DBPConfigurationProfile {

    private String profileId;
    private String profileName;
    private String profileDescription;

    // Properties. Basically JSON
    private Map<String, String> properties = new LinkedHashMap<>();

    public DBPConfigurationProfile() {
    }

    public DBPConfigurationProfile(DBPConfigurationProfile source) {
        this.profileId = source.profileId;
        this.profileName = source.profileName;
        this.profileDescription = source.profileDescription;
        if (!CommonUtils.isEmpty(source.properties)) {
            this.properties = new LinkedHashMap<>(source.properties);
        }
    }

    public String getProfileId() {
        if (CommonUtils.isEmpty(profileId)) {
            return profileName;
        }
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileDescription() {
        return profileDescription;
    }

    public void setProfileDescription(String profileDescription) {
        this.profileDescription = profileDescription;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return profileName;
    }

}
