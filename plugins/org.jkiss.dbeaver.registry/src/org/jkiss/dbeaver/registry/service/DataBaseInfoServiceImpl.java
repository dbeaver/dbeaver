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
package org.jkiss.dbeaver.registry.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.registry.DataBaseInfo;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataBaseInfoServiceImpl implements DataBaseInfo {
    private static final String DATABASES_LABEL = "databases"; //$NON-NLS-1$
    private static final String DB_NAME_LABEL = "name"; //$NON-NLS-1$
    private static final String DB_CATEGORY_LABEL = "category"; //$NON-NLS-1$
    private static final String PRODUCT_LABEL = "product"; //$NON-NLS-1$
    private static final String DESCRIPTION_LABEL = "description"; //$NON-NLS-1$
    private static final Log log = Log.getLog(DataBaseInfoServiceImpl.class);
    private static final Gson DB_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create();

    @Override
    public void publishDataBaseInfo(@NotNull Path path) {
        List<DriverDescriptor> drivers = getSupportedDBInstances();
        if (drivers.isEmpty()) {
            return;
        }
        try (Writer mdWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = DB_GSON.newJsonWriter(mdWriter)) {
                jsonWriter.setIndent(JSONUtils.DEFAULT_INDENT);
                jsonWriter.beginObject();
                JSONUtils.field(jsonWriter, PRODUCT_LABEL, Platform.getProduct().getName());
                JSONUtils.field(jsonWriter, DESCRIPTION_LABEL, Platform.getProduct().getDescription());
                jsonWriter.name(DATABASES_LABEL);
                jsonWriter.beginArray();
                for (DriverDescriptor driver : drivers) {
                    jsonWriter.beginObject();
                    JSONUtils.field(jsonWriter, DB_NAME_LABEL, driver.getName());
                    JSONUtils.serializeObjectList(jsonWriter, DB_CATEGORY_LABEL, driver.getCategories());
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
                jsonWriter.endObject();
                jsonWriter.flush();
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    private List<DriverDescriptor> getSupportedDBInstances() {
        List<DriverDescriptor> supportedDataBases = new ArrayList<>();
        DataSourceProviderRegistry dataSourceRegistry = DataSourceProviderRegistry.getInstance();
        List<DataSourceProviderDescriptor> dataSourceProviders = dataSourceRegistry.getDataSourceProviders();
        for (DataSourceProviderDescriptor providerDescriptor : dataSourceProviders) {
            for (DriverDescriptor driverDescriptor : providerDescriptor.getEnabledDrivers()) {
                supportedDataBases.add(driverDescriptor);
            }
        }
        Collections.sort(supportedDataBases, (DriverDescriptor o1, DriverDescriptor o2) -> o1.getName().compareTo(o2.getName()));
        return supportedDataBases;
    }
}
