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

import java.util.LinkedHashMap;
import java.util.Map;

public class SMUser {
    private final String userId;
    private final Map<String, String> metaParameters = new LinkedHashMap<>();
    private boolean active = true;

    public SMUser(@NotNull String userId) {
        this.userId = userId;
    }

    public SMUser(@NotNull String userId, @NotNull Map<String, String> metaParameters) {
        this.userId = userId;
        this.metaParameters.putAll(metaParameters);
    }

    public SMUser(@NotNull String userId, boolean active) {
        this.userId = userId;
        this.active = active;
    }
    public SMUser(@NotNull String userId, @NotNull Map<String, String> metaParameters, boolean active) {
        this.userId = userId;
        this.metaParameters.putAll(metaParameters);
        this.active = active;
    }

    @NotNull
    public String getUserId() {
        return userId;
    }

    @NotNull
    public Map<String, String> getMetaParameters() {
        return metaParameters;
    }

    public boolean getStatus() {
        return active;
    }

    public void setStatus(boolean status) {
        this.active = status;
    }

    public void setMetaParameter(String name, String value) {
        metaParameters.put(name, value);
    }
}
