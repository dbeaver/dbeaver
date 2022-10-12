/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.user;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class SMUser {

    private final String userId;
    private final Map<String, String> metaParameters = new LinkedHashMap<>();
    private String[] userTeams;
    private boolean enabled;

    public SMUser(@NotNull String userId, boolean enabled) {
        this(userId, null, new String[0], enabled);
    }

    public SMUser(
        @NotNull String userId,
        @Nullable Map<String, String> metaParameters,
        @NotNull String[] teams,
        boolean enabled
    ) {
        this.userId = userId;
        if (metaParameters != null) {
            this.metaParameters.putAll(metaParameters);
        }
        this.userTeams = teams;
        this.enabled = enabled;
    }

    @NotNull
    public String getUserId() {
        return userId;
    }

    @NotNull
    public Map<String, String> getMetaParameters() {
        return metaParameters;
    }

    @NotNull
    public String[] getUserTeams() {
        return userTeams;
    }

    public void setUserTeams(@NotNull String[] userTeams) {
        this.userTeams = userTeams;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enableUser(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMetaParameter(String name, String value) {
        metaParameters.put(name, value);
    }

}
