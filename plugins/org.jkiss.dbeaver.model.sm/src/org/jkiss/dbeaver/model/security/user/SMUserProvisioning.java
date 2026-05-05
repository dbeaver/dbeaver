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
package org.jkiss.dbeaver.model.security.user;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Map;

public class SMUserProvisioning {
    @NotNull
    private final String userId;

    @NotNull
    private final Map<String, String> metaParameters;

    @Nullable
    private final String authRole;

    public SMUserProvisioning(@NotNull String userId, @NotNull Map<String, String> metaParameters, @Nullable String authRole) {
        this.userId = userId;
        this.metaParameters = metaParameters;
        this.authRole = authRole;
    }

    @NotNull
    public String getUserId() {
        return userId;
    }

    @NotNull
    public Map<String, String> getMetaParameters() {
        return metaParameters;
    }

    @Nullable
    public String getAuthRole() {
        return authRole;
    }

    public static SMUserProvisioningBuilder builder() {
        return new SMUserProvisioningBuilder();
    }

    public static final class SMUserProvisioningBuilder {
        private String authRole;
        private String userId;
        private Map<String, String> metaParameters;

        private SMUserProvisioningBuilder() {
        }

        public static SMUserProvisioningBuilder aSMUserProvisioning() {
            return new SMUserProvisioningBuilder();
        }

        public SMUserProvisioningBuilder authRole(String authRole) {
            this.authRole = authRole;
            return this;
        }

        public SMUserProvisioningBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public SMUserProvisioningBuilder metaParameters(Map<String, String> metaParameters) {
            this.metaParameters = metaParameters;
            return this;
        }

        public SMUserProvisioning build() {
            return new SMUserProvisioning(userId, metaParameters, authRole);
        }
    }
}
