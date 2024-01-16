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
package org.jkiss.dbeaver.model.dpi;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Map;

public class DPIDataSourceParameters {
    @NotNull
    private final String session;
    private final @NotNull String containerConfiguration;
    private final @NotNull String[] driverLibraries;
    private final @Nullable Map<String, String> credentials;

    public DPIDataSourceParameters(
        @NotNull String session,
        @NotNull String containerConfiguration,
        @NotNull String[] driverLibraries,
        @Nullable Map<String, String> credentials
    ) {
        this.session = session;
        this.containerConfiguration = containerConfiguration;
        this.driverLibraries = driverLibraries;
        this.credentials = credentials;
    }

    @NotNull
    public String getSession() {
        return session;
    }

    @NotNull
    public String getContainerConfiguration() {
        return containerConfiguration;
    }

    @NotNull
    public String[] getDriverLibraries() {
        return driverLibraries;
    }

    @Nullable
    public Map<String, String> getCredentials() {
        return credentials;
    }
}
