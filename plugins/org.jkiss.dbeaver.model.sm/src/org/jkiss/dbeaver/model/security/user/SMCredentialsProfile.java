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
package org.jkiss.dbeaver.model.security.user;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Map;
import java.util.Objects;

public class SMCredentialsProfile extends SMSubject {
    private final @Nullable String profileName;
    private final @Nullable String profileDescription;
    private final @Nullable String parentProfileId;

    public SMCredentialsProfile(
        @NotNull String subjectId,
        @Nullable String profileName,
        @Nullable String profileDescription,
        @Nullable String parentProfileId
    ) {
        super(subjectId, Map.of());
        this.profileName = profileName;
        this.profileDescription = profileDescription;
        this.parentProfileId = parentProfileId;
    }

    @NotNull
    public String getCredentialsProfileId() {
        return subjectId;
    }

    @NotNull
    @Override
    public String getName() {
        return Objects.requireNonNullElse(profileName, subjectId);
    }

    @Nullable
    public String getProfileDescription() {
        return profileDescription;
    }

    @Nullable
    public String getParentProfileId() {
        return parentProfileId;
    }

}
