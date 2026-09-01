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
package org.jkiss.dbeaver.model.tracking.sync.core;

import com.dbeaver.datadam.gateway.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.gateway.model.DDUpdateConfigurationRequest;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * Remote storage of opaque values. Knows nothing about their content or encryption.
 */
public interface DDSyncTransport {

    @NotNull
    List<com.dbeaver.datadam.gateway.model.DDConfigurationSummary> listConfigurations() throws DBException;

    @NotNull
    com.dbeaver.datadam.gateway.model.DDConfiguration getConfiguration(
        @NotNull String configurationId
    ) throws DBException;

    @NotNull
    com.dbeaver.datadam.gateway.model.DDConfiguration createConfiguration(
        @NotNull DDCreateConfigurationRequest request
    ) throws DBException;

    @NotNull
    com.dbeaver.datadam.gateway.model.DDUpdateConfigurationResult updateConfiguration(
        @NotNull String configurationId,
        @NotNull DDUpdateConfigurationRequest request
    ) throws DBException;
}
