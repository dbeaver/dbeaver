/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPConfigurationProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Network configuration profile
 */
public class DBWNetworkProfile extends DBPConfigurationProfile {

    @NotNull
    private final List<DBWHandlerConfiguration> configurations = new ArrayList<>();

    @NotNull
    public List<DBWHandlerConfiguration> getConfigurations() {
        return configurations;
    }

    public DBWNetworkProfile() {
    }

    public void updateConfiguration(@NotNull DBWHandlerConfiguration cfg) {
        for (int i = 0; i < configurations.size(); i++) {
            DBWHandlerConfiguration c = configurations.get(i);
            if (Objects.equals(cfg.getId(), c.getId())) {
                configurations.set(i, cfg);
                return;
            }
        }
        configurations.add(cfg);
    }

    @Nullable
    public DBWHandlerConfiguration getConfiguration(DBWHandlerDescriptor handler) {
        for (DBWHandlerConfiguration  cfg : configurations) {
            if (Objects.equals(cfg.getHandlerDescriptor().getId(), handler.getId())) {
                return cfg;
            }
        }
        return null;
    }

}
