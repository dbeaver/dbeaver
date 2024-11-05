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
package org.jkiss.dbeaver.ext.config.migration.dbvis;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.config.migration.wizards.ImportData;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DbvisConfigurationImporter {
    private static final Log log = Log.getLog(DbvisConfigurationImporter.class);
    private static final Map<String, DbvisConfigurationCreator> version2creator = new LinkedHashMap<>();

    static {
        version2creator.put(DbvisConfigurationCreatorv7.VERSION, new DbvisConfigurationCreatorv7());
        version2creator.put(DbvisConfigurationCreatorv233.VERSION, new DbvisConfigurationCreatorv233());
    }

    public ImportData importConfiguration(
        @NotNull ImportData data,
        @NotNull File folder
    ) {
        for (Entry<String, DbvisConfigurationCreator> configuration : version2creator.entrySet()) {
            DbvisConfigurationCreator dbvisConfigurationCreator = configuration.getValue();
            File configurationAsset = dbvisConfigurationCreator.getConfigurationAsset(folder);
            if (configurationAsset != null && configurationAsset.exists()) {
                try {
                    data = dbvisConfigurationCreator.create(data, configurationAsset);
                } catch (DBException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return data;
    }
}
