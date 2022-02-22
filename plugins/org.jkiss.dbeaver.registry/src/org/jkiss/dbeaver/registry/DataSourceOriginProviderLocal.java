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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceOrigin;
import org.jkiss.dbeaver.model.DBPDataSourceOriginProvider;
import org.jkiss.dbeaver.model.DBPExternalConfiguration;

import java.util.Map;

/**
 * DataSourceOriginProviderLocal
 */
public class DataSourceOriginProviderLocal implements DBPDataSourceOriginProvider
{
    public static final String PROVIDER_ID = "local";

    @NotNull
    @Override
    public DBPDataSourceOrigin getOrigin(
        @NotNull Map<String, Object> dsConfiguration,
        @Nullable DBPExternalConfiguration externalConfiguration) {
        // There is only one origin
        return DataSourceOriginLocal.INSTANCE;
    }
}
