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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.dbeaver.ext.cloudflare.d1.CloudflareD1ConnectionSettings;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Element;

import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudflareD1RegistrationTest {
    @Test
    void initialMetadataConnectionBuildsItsUrlFromTheSavedDatabaseId() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        CloudflareD1ConnectionSettings.save(configuration, "initial-id", false, "");
        var driver = DataSourceProviderRegistry.getInstance().getDataSourceProvider("cloudflare_d1")
            .getDriver("cloudflare_d1_jdbc");
        assertNotNull(driver);
        assertEquals("jdbc:d1://initial-id", driver.getConnectionURL(configuration));
        var getConnectionURL = JDBCDataSource.class.getDeclaredMethod("getConnectionURL", DBPConnectionConfiguration.class);
        getConnectionURL.setAccessible(true);
        // Invoke JDBCDataSource's URL fallback rather than assuming a blank persisted URL reaches the driver unchanged.
        var container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getDriver()).thenReturn(driver);
        var fallback = Mockito.mock(CloudflareD1DataSource.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(container).when(fallback).getContainer();
        assertEquals("jdbc:d1://initial-id", getConnectionURL.invoke(fallback, configuration));
    }

    @Test
    void smallIconHasExplicitNavigatorDimensions() throws Exception {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        try (var stream = bundle.getEntry("icons/cloudflare-d1.svg").openStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var icon = factory.newDocumentBuilder().parse(stream).getDocumentElement();
            assertEquals("16", icon.getAttribute("width"));
            assertEquals("16", icon.getAttribute("height"));
            assertEquals("0 0 16 16", icon.getAttribute("viewBox"));
        }
    }

    @Test
    void driverKeepsConnectionIdsAndDeclaresMavenLibraries() throws Exception {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        assertNotNull(bundle);
        assertEquals("org.jkiss.dbeaver.ext.cloudflare.d1", bundle.getSymbolicName());
        try (var stream = bundle.getEntry("plugin.xml").openStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var plugin = factory.newDocumentBuilder().parse(stream);
            var provider = (Element) plugin.getElementsByTagName("datasource").item(0);
            assertEquals("cloudflare_d1", provider.getAttribute("id"));
            var driver = (Element) plugin.getElementsByTagName("driver").item(0);
            assertEquals("cloudflare_d1_jdbc", driver.getAttribute("id"));
            assertEquals("com.dbeaver.jdbc.d1.D1Driver", driver.getAttribute("class"));
            var files = driver.getElementsByTagName("file");
            int libraries = 0;
            for (int index = 0; index < files.getLength(); index++) {
                var file = (Element) files.item(index);
                if ("jar".equals(file.getAttribute("type"))) {
                    libraries++;
                    assertEquals("maven:/com.dbeaver.jdbc:com.dbeaver.jdbc.driver.d1:RELEASE", file.getAttribute("path"));
                    assertFalse(file.hasAttribute("bundle"));
                    assertNotEquals("true", file.getAttribute("ignore-dependencies"));
                }
            }
            assertEquals(1, libraries);
            var auth = (Element) plugin.getElementsByTagName("authModel").item(0);
            assertEquals("cloudflare_d1_native", auth.getAttribute("id"));
        }
    }

    @Test
    void communityModelDoesNotDependOnEnterpriseOrTheJdbcImplementation() {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        var dependencies = bundle.getHeaders().get("Require-Bundle");
        assertFalse(dependencies.contains("com.dbeaver.db."));
        assertFalse(dependencies.contains("com.dbeaver.jdbc.driver.d1"));
        for (var type : new Class<?>[] {CloudflareD1ConnectionAdapter.class, CloudflareD1AdminClient.class}) {
            assertTrue(Arrays.stream(type.getDeclaredMethods()).noneMatch(method ->
                method.getName().equals("restore") || method.getName().toLowerCase().contains("bookmark")));
        }
    }
}
