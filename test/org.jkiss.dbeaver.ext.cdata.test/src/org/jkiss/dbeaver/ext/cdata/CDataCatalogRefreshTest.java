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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverInfo;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverTier;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class CDataCatalogRefreshTest extends DBeaverUnitTest {
    @Test
    public void updateCatalogWithoutReplacingExistingDrivers() {
        var provider = new CDataDataSourceProvider();
        var descriptor = DataSourceProviderRegistry.getInstance().getDataSourceProvider("generic");
        List<DBPDriver> drivers = new ArrayList<>();
        var initial = info("postgresql", 2025);
        provider.refreshProvidedDrivers(descriptor, drivers, List.of(initial));
        var existing = (CDataDriverDescriptor) drivers.getFirst();
        var updated = info("postgresql", 2026);

        var changed = provider.refreshProvidedDrivers(descriptor, drivers, List.of(updated, info("jira", 2026)));

        Assertions.assertEquals(2, changed.size());
        Assertions.assertEquals(2, drivers.size());
        Assertions.assertSame(existing, drivers.getFirst());
        Assertions.assertEquals(updated, existing.getDriverInfo());
        Assertions.assertTrue(existing.getDriverLibraries().stream()
            .filter(library -> !library.isDisabled()).allMatch(library -> library.getVersion().contains("26")));
        Assertions.assertTrue(provider.refreshProvidedDrivers(descriptor, drivers, List.of(updated, info("jira", 2026))).isEmpty());
    }

    @Test
    public void preserveUserSettingsAndDriversMissingFromUpdatedCatalog() {
        var provider = new CDataDataSourceProvider();
        var descriptor = DataSourceProviderRegistry.getInstance().getDataSourceProvider("generic");
        List<DBPDriver> drivers = new ArrayList<>();
        provider.refreshProvidedDrivers(descriptor, drivers, List.of(info("postgresql", 2025), info("jira", 2025)));
        var existing = (CDataDriverDescriptor) drivers.getFirst();
        existing.setName("Custom PostgreSQL");
        existing.setModified(true);
        final var libraries = List.copyOf(existing.getDriverLibraries());

        provider.refreshProvidedDrivers(descriptor, drivers, List.of(info("postgresql", 2026)));

        Assertions.assertEquals(2, drivers.size());
        Assertions.assertSame(existing, drivers.getFirst());
        Assertions.assertEquals("Custom PostgreSQL", existing.getName());
        Assertions.assertEquals(libraries, existing.getDriverLibraries());
        Assertions.assertEquals(2026, existing.getDriverInfo().versionYear());
    }

    @NotNull
    private static CDataDriverInfo info(@NotNull String source, int year) {
        return new CDataDriverInfo(source, source, source + " JDBC Driver", year,
            CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", source, null);
    }
}
