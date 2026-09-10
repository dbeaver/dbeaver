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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.code.DynamicCall;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.model.CDataDataSource;
import org.jkiss.dbeaver.ext.cdata.model.CDataMetaModel;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverCatalog;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverInfo;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverProvider;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CDataDataSourceProvider extends GenericDataSourceProvider<CDataDataSource> implements DriverProvider {
    private static final Pattern DRIVER_NAME_SUFFIX = Pattern.compile("\\s*(JDBC\\s+)?Driver$");

    @DynamicCall
    public CDataDataSourceProvider() {
        super(CDataDataSource.class);
    }

    @NotNull
    @Override
    public CDataDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new CDataDataSource(monitor, container, new CDataMetaModel());
    }

    @NotNull
    @Override
    public List<DriverDescriptor> getProvidedDrivers(@NotNull DataSourceProviderDescriptor dataSourceProvider) {
        return CDataDriverCatalog.load().stream()
            .<DriverDescriptor>map(driverInfo -> createDriver(dataSourceProvider, driverInfo))
            .toList();
    }

    @NotNull
    private static CDataDriverDescriptor createDriver(
        @NotNull DataSourceProviderDescriptor dataSourceProvider,
        @NotNull CDataDriverInfo driverInfo
    ) {
        CDataDriverDescriptor driver = new CDataDriverDescriptor(
            dataSourceProvider,
            "cdata_" + driverInfo.dataSource(),
            driverInfo
        );
        driver.setName(getDriverName(driverInfo));
        driver.setDescription("CData " + driverInfo.tier().name().toLowerCase(Locale.ENGLISH) + " JDBC driver");
        driver.setSampleURL("jdbc:" + driverInfo.jdbcName() + ":");
        driver.setSingleConnection(true);
        driver.setCategories(List.of("cdata"));
        driver.setWebURL(driverInfo.purchaseUrl());
        DriverLibraryMavenArtifact jarLib = new DriverLibraryMavenArtifact(
            driver,
            DBPDriverLibrary.FileType.jar,
            DriverLibraryMavenArtifact.PATH_PREFIX + "cdata:" + driverInfo.artifactId(),
            driverInfo.mavenVersionPattern()
        );
        jarLib.setIgnoreDependencies(true);
        driver.addDriverLibrary(jarLib, false);
        return driver;
    }

    @NotNull
    static String getDriverName(@NotNull CDataDriverInfo driverInfo) {
        return "CData " + DRIVER_NAME_SUFFIX.matcher(driverInfo.driverName()).replaceFirst("");
    }
}
