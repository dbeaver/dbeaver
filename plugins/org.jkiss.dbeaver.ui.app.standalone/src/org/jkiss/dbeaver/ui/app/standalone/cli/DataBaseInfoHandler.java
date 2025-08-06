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
package org.jkiss.dbeaver.ui.app.standalone.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.cli.CommandLine;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.app.standalone.ICommandLineParameterHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DataBaseInfoHandler implements ICommandLineParameterHandler {
    private static final String OUTPUT_DATABASES_JSON = "database.drivers.json"; //$NON-NLS-1$
    private static final String PRODUCT_ID_LABEL = "id"; //$NON-NLS-1$
    private static final String PRODUCT_NAME_LABEL = "name"; //$NON-NLS-1$
    private static final String PRODUCT_VERSION_LABEL = "version"; //$NON-NLS-1$
    private static final String PRODUCT_DESCRIPTION_LABEL = "description"; //$NON-NLS-1$
    private static final String PRODUCT_EDITION_LABEL = "edition"; //$NON-NLS-1$
    private static final String DATABASES_LABEL = "databases"; //$NON-NLS-1$
    private static final String DB_NAME_LABEL = "name"; //$NON-NLS-1$
    private static final String DB_CATEGORY_LABEL = "category"; //$NON-NLS-1$
    private static final String DB_EMBEDDED_LABEL = "embedded"; //$NON-NLS-1$
    private static final String DB_REQUIRE_DOWNLOAD_LABEL = "download"; //$NON-NLS-1$
    private static final String DB_ADITIONAL_FEATURE_LABEL = "pro"; //$NON-NLS-1$
    private static final Log log = Log.getLog(DataBaseInfoHandler.class);
    private static final Gson DB_GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .create();

    @Override
    public void handleParameter(CommandLine commandLine, String name, String directory) {
        Path path = Path.of(directory);
        if (!path.toFile().exists()) {
            log.error("Directory by path '" + directory + "' does not exists"); //$NON-NLS-1$
            return;
        }
        if (!path.toFile().isDirectory()) {
            log.error("Target location is not a directory '" + directory + "'"); //$NON-NLS-1$
            return;
        }
        publishDataBaseInfo(path.resolve(OUTPUT_DATABASES_JSON));
    }

    private void publishDataBaseInfo(@NotNull Path path) {
        List<DriverDescriptor> drivers = getSupportedDBInstances();
        if (drivers.isEmpty()) {
            return;
        }
        try (Writer mdWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = DB_GSON.newJsonWriter(mdWriter)) {
                jsonWriter.setIndent(JSONUtils.DEFAULT_INDENT);
                jsonWriter.beginObject();
                JSONUtils.field(jsonWriter, PRODUCT_ID_LABEL, Platform.getProduct().getId());
                JSONUtils.field(jsonWriter, PRODUCT_EDITION_LABEL, Platform.getProduct().getProperty("appEdition")); //$NON-NLS-1$
                JSONUtils.field(jsonWriter, PRODUCT_NAME_LABEL, GeneralUtils.getProductName());
                JSONUtils.field(jsonWriter, PRODUCT_VERSION_LABEL, GeneralUtils.getPlainVersion());
                JSONUtils.field(jsonWriter, PRODUCT_DESCRIPTION_LABEL, Platform.getProduct().getDescription());
                jsonWriter.name(DATABASES_LABEL);
                jsonWriter.beginArray();
                for (DriverDescriptor driver : drivers) {
                    jsonWriter.beginObject();
                    JSONUtils.field(jsonWriter, DB_NAME_LABEL, driver.getName());
                    JSONUtils.serializeObjectList(jsonWriter, DB_CATEGORY_LABEL, driver.getCategories());
                    if (driver.isEmbedded()) {
                        JSONUtils.field(jsonWriter, DB_EMBEDDED_LABEL, Boolean.TRUE);
                    }
                    if (isRequireToDownload(driver)) {
                        JSONUtils.field(jsonWriter, DB_REQUIRE_DOWNLOAD_LABEL, Boolean.TRUE);
                    }
                    if (isExtendedInPro(driver)) {
                        JSONUtils.field(jsonWriter, DB_ADITIONAL_FEATURE_LABEL, Boolean.TRUE);
                    }
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

    private boolean isRequireToDownload(DriverDescriptor driver) {
        return driver.getDriverLibraries()
            .stream()
            .map(DBPDriverLibrary::getLocalFile)
            .filter(Objects::nonNull)
            .map(path -> path.toAbsolutePath().toString())
            .findAny().isEmpty();
    }

    private boolean isExtendedInPro(DriverDescriptor driver) {
        return !driver.getDriverReplacementsInfo().isEmpty();
    }
}
