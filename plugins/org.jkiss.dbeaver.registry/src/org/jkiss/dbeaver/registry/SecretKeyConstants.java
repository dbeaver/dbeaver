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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

/**
 * Data source descriptors constants
 */
public class SecretKeyConstants {

    public static final String DEFAULT_KEY_PREFIX = "db-";
    public static final String DATASOURCE_KEY_PREFIX = DEFAULT_KEY_PREFIX + "datasource-";

    public static final Gson SECRET_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    public static final String getSecretKeyId(DBPDataSourceContainer dataSource) {
        return DATASOURCE_KEY_PREFIX + dataSource.getProject().getName() + "-" + dataSource.getId();
    }
}
