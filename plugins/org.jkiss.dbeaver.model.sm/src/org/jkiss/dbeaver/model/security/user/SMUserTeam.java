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
package org.jkiss.dbeaver.model.security.user;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.security.SMTeamGrantType;

public class SMUserTeam extends SMTeam {
    @Nullable
    private final String teamRole;
    @NotNull
    private final SMTeamGrantType grantType;
    @NotNull
    private final String grantedBy;

    public SMUserTeam(
        @NotNull SMTeam smTeam, @Nullable String teamRole
    ) {
        this(smTeam, teamRole, SMTeamGrantType.MANUAL, "");
    }

    public SMUserTeam(
        @NotNull SMTeam smTeam,
        @Nullable String teamRole,
        @NotNull SMTeamGrantType grantType,
        @NotNull String grantedBy
    ) {
        super(smTeam.getTeamId(), smTeam.getTeamName(), smTeam.getDescription(), smTeam.isSecretStorage());
        this.teamRole = teamRole;
        this.grantType = grantType;
        this.grantedBy = grantedBy;
    }

    @Nullable
    public String getTeamRole() {
        return teamRole;
    }

    @NotNull
    public SMTeamGrantType getGrantType() {
        return grantType;
    }

    @NotNull
    public String getGrantedBy() {
        return grantedBy;
    }
}
