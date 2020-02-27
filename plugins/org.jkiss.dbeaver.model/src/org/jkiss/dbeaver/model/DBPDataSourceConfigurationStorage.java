/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;

import java.util.List;
import java.util.Map;

/**
 * Provided data source configuration storage
 */
public interface DBPDataSourceConfigurationStorage {

    String getStorageId();

    boolean isValid();

    boolean isDefault();

    String getStatus();

    List<? extends DBPDataSourceContainer> loadDataSources(DBPDataSourceRegistry registry, Map<String, Object> options)
        throws DBException;

    // Used for secure credentials save/load (it is a prt of credentials file name)
    String getConfigurationFileSuffix();
}
