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
package com.dbeaver.db.cdata.registry;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

public class CDataDriverCatalogTest extends DBeaverUnitTest {
    @Test
    public void loadCatalog() {
        var drivers = CDataDriverCatalog.load();

        Assertions.assertEquals(226, drivers.size());
        Assertions.assertEquals(182, drivers.stream()
            .filter(driver -> driver.tier() == CDataDriverTier.PROFESSIONAL)
            .count());
        Assertions.assertEquals(44, drivers.stream()
            .filter(driver -> driver.tier() == CDataDriverTier.PREMIUM)
            .count());
        Assertions.assertEquals(226, drivers.stream()
            .map(CDataDriverInfo::dataSource)
            .collect(Collectors.toSet())
            .size());
        Assertions.assertEquals(226, drivers.stream()
            .map(CDataDriverInfo::artifactId)
            .collect(Collectors.toSet())
            .size());
        Assertions.assertTrue(drivers.stream()
            .allMatch(driver -> driver.dataSource().matches("[a-z0-9]+")));
        Assertions.assertEquals("hubspot-jdbc", drivers.stream()
            .filter(driver -> driver.dataSource().equals("hubspot"))
            .findFirst()
            .orElseThrow()
            .artifactId());
        Assertions.assertEquals("aas-jdbc", drivers.stream()
            .filter(driver -> driver.dataSource().equals("azureanalysisservices"))
            .findFirst()
            .orElseThrow()
            .artifactId());
        Assertions.assertTrue(drivers.stream()
            .allMatch(driver -> driver.mavenVersionPattern().equals("{" + (driver.versionYear() - 2000) + "\\..*}")));
    }
}
