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
package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Objects;

public class SMAuthConfigurationReference {
    @NotNull
    private final String authProviderId;
    @Nullable
    private final String authProviderConfigurationId;

    public SMAuthConfigurationReference(
        @NotNull String authProviderId,
        @Nullable String authProviderConfigurationId
    ) {
        this.authProviderId = authProviderId;
        this.authProviderConfigurationId = authProviderConfigurationId;
    }

    @NotNull
    public String getAuthProviderId() {
        return authProviderId;
    }

    @Nullable
    public String getAuthProviderConfigurationId() {
        return authProviderConfigurationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMAuthConfigurationReference that = (SMAuthConfigurationReference) o;
        return Objects.equals(authProviderId, that.authProviderId) && Objects.equals(authProviderConfigurationId,
            that.authProviderConfigurationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authProviderId, authProviderConfigurationId);
    }
}
