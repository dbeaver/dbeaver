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
package org.jkiss.dbeaver.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;

import java.util.List;

/**
 * External datasource provider origin.
 */
public interface DBPDataSourceOriginExternal extends DBPDataSourceOrigin {

    /**
     * Returns external configuration ID.
     */
    @Nullable
    DBPExternalConfiguration getExternalConfiguration();

    /**
     * Returns network profiles available in this origin
     */
    List<DBWNetworkProfile> getAvailableNetworkProfiles();

}
