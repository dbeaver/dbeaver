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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Configuration files manager
 */
public interface DataSourceConfigurationManager {

    boolean isReadOnly();

    boolean isSecure();

    List<DBPDataSourceConfigurationStorage> getConfigurationStorages();

    /**
     * Reads datasource configuration.
     * If dataSourceIds is specified then reads only configuration linked with specified datasources.
     */
    InputStream readConfiguration(
        @NotNull String name,
        @Nullable Collection<String> dataSourceIds) throws DBException, IOException;

    void writeConfiguration(@NotNull String name, @Nullable byte[] data) throws DBException, IOException;

}
