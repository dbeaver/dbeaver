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
package org.jkiss.dbeaver.ext.cdata.registry;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class CDataRegistryRefreshTest extends DBeaverUnitTest {
    private DataSourceProviderRegistry registry;
    private DataSourceProviderDescriptor provider;
    private Field catalogField;
    private List<CDataDriverInfo> savedCatalog;

    @BeforeEach
    public void setUp() throws Exception {
        var jobField = CDataDriverCatalog.class.getDeclaredField("UPDATE_JOB");
        jobField.setAccessible(true);
        var job = (Job) jobField.get(null);
        job.cancel();
        job.join();
        savedCatalog = CDataDriverCatalog.load();
        catalogField = CDataDriverCatalog.class.getDeclaredField("drivers");
        catalogField.setAccessible(true);
        catalogField.set(null, List.of(info("postgresql", 2025)));

        var constructor = DataSourceProviderRegistry.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        registry = constructor.newInstance();
        var configuration = Arrays.stream(Platform.getExtensionRegistry()
                .getConfigurationElementsFor(DataSourceProviderDescriptor.EXTENSION_ID))
            .filter(element -> "datasource".equals(element.getName()) && "cdata".equals(element.getAttribute("id")))
            .findFirst().orElseThrow();
        provider = new DataSourceProviderDescriptor(registry, configuration);
        registry.getDataSourceProviders().add(provider);
        registry.refreshProvidedDrivers();
        registry.getDataSourceTypes();
    }

    @AfterEach
    public void tearDown() throws IllegalAccessException {
        catalogField.set(null, savedCatalog);
    }

    @Test
    public void readDriverTypesWithoutApplyingPendingCatalog() throws Exception {
        var driver = (CDataDriverDescriptor) provider.getDrivers().getFirst();
        var type = driver.getDataSourceType();
        var updated = info("postgresql", 2026);
        catalogField.set(null, List.of(updated));

        Assertions.assertEquals(List.of(driver), Assertions.assertDoesNotThrow(type::getEnabledDrivers));
        Assertions.assertEquals(2025, driver.getDriverInfo().versionYear());
        Assertions.assertDoesNotThrow(registry::getDataSourceTypes);

        registry.refreshProvidedDrivers();

        Assertions.assertSame(driver, provider.getDrivers().getFirst());
        Assertions.assertEquals(updated, driver.getDriverInfo());
        Assertions.assertEquals(List.of(driver), type.getEnabledDrivers());
    }

    @Test
    public void keepDriverSnapshotStableUntilNextExplicitRefresh() throws Exception {
        catalogField.set(null, List.of(info("postgresql", 2025), info("jira", 2025)));
        registry.refreshProvidedDrivers();
        final var snapshot = provider.getDrivers();
        var types = registry.getDataSourceTypes();
        catalogField.set(null, List.of(info("postgresql", 2025), info("jira", 2025), info("mysql", 2025)));

        Assertions.assertEquals(2, provider.getDrivers().size());
        Assertions.assertEquals(types, registry.getDataSourceTypes());
        Assertions.assertEquals(2, types.stream().mapToInt(type -> type.getEnabledDrivers().size()).sum());

        registry.refreshProvidedDrivers();

        Assertions.assertEquals(2, snapshot.size());
        Assertions.assertEquals(3, provider.getDrivers().size());
        Assertions.assertEquals(3, registry.getDataSourceTypes().size());
        Assertions.assertNotNull(provider.getDriver("cdata_mysql"));
    }

    @NotNull
    private static CDataDriverInfo info(@NotNull String source, int year) {
        return new CDataDriverInfo(source, source, source + " JDBC Driver", year,
            CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", source, null);
    }
}
