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

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cloudflare.d1.CloudflareD1ConnectionSettings;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.ObjectManagerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;

public class CloudflareD1RegistrationTest {
    @Test
    void metadataLabelsUseTheStandardBundleLocalization() {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        Assertions.assertNotNull(bundle.getEntry("OSGI-INF/l10n/bundle.properties"));
        var resources = Platform.getResourceBundle(bundle);
        Assertions.assertEquals("Database UUID", resources.getString(
            "meta.org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1Catalog.databaseId.name"));
        Assertions.assertEquals("Account ID", resources.getString(
            "meta.org.jkiss.dbeaver.ext.cloudflare.d1.auth.CloudflareD1AuthCredentials.userName.name"));
        Assertions.assertEquals("API token", resources.getString(
            "meta.org.jkiss.dbeaver.ext.cloudflare.d1.auth.CloudflareD1AuthCredentials.userPassword.name"));
    }

    @Test
    void navigatorUsesD1CatalogTypeWithoutChangingInheritedTrees() {
        var registry = DataSourceProviderRegistry.getInstance();
        var catalog = registry.getDataSourceProvider("cloudflare_d1").getTreeDescriptor().findChildItemByPath("catalog");
        Assertions.assertNotNull(catalog);
        Assertions.assertEquals(CloudflareD1Catalog.class, catalog.getPropertyOrCollectionItemType(CloudflareD1DataSource.class));
        Assertions.assertNotNull(catalog.findChildItemByPath("schema"));
        for (String provider : List.of("generic", "sqlite")) {
            var inheritedCatalog = registry.getDataSourceProvider(provider).getTreeDescriptor().findChildItemByPath("catalog");
            Assertions.assertNotNull(inheritedCatalog);
            Assertions.assertEquals("catalogList", inheritedCatalog.getPropertyName());
            Assertions.assertEquals(GenericCatalog.class, inheritedCatalog.getPropertyOrCollectionItemType(GenericDataSource.class));
        }
    }

    @Test
    void typedCatalogListPreservesCatalogInstancesAndUninitializedState() {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.CALLS_REAL_METHODS);
        Assertions.assertNull(source.getD1CatalogList());
        var catalog = Mockito.mock(CloudflareD1Catalog.class);
        Mockito.doReturn(List.of(catalog)).when(source).getCatalogs();
        Assertions.assertEquals(List.of(catalog), source.getD1CatalogList());
    }

    @Test
    void initialMetadataConnectionBuildsItsUrlFromTheSavedDatabaseId() throws Exception {
        var configuration = new DBPConnectionConfiguration();
        CloudflareD1ConnectionSettings.save(configuration, "initial-id", false, "");
        var driver = DataSourceProviderRegistry.getInstance().getDataSourceProvider("cloudflare_d1")
            .getDriver("cloudflare_d1_jdbc");
        Assertions.assertNotNull(driver);
        Assertions.assertEquals("jdbc:d1://initial-id", driver.getConnectionURL(configuration));
        // Exercise datasource URL generation rather than assuming a blank persisted URL reaches the driver unchanged.
        var container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getDriver()).thenReturn(driver);
        var fallback = Mockito.mock(ConnectionUrlDataSource.class, Mockito.CALLS_REAL_METHODS);
        Mockito.doReturn(container).when(fallback).getContainer();
        Assertions.assertEquals("jdbc:d1://initial-id", fallback.connectionUrl(configuration));
    }

    @Test
    void smallIconHasExplicitNavigatorDimensions() throws Exception {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        try (var stream = bundle.getEntry("icons/cloudflare-d1.svg").openStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var icon = factory.newDocumentBuilder().parse(stream).getDocumentElement();
            Assertions.assertEquals("16", icon.getAttribute("width"));
            Assertions.assertEquals("16", icon.getAttribute("height"));
            Assertions.assertEquals("0 0 16 16", icon.getAttribute("viewBox"));
        }
    }

    @Test
    void driverKeepsConnectionIdsAndDeclaresMavenLibraries() throws Exception {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        Assertions.assertNotNull(bundle);
        Assertions.assertEquals("org.jkiss.dbeaver.ext.cloudflare.d1", bundle.getSymbolicName());
        try (var stream = bundle.getEntry("plugin.xml").openStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var plugin = factory.newDocumentBuilder().parse(stream);
            var provider = (Element) plugin.getElementsByTagName("datasource").item(0);
            Assertions.assertEquals("cloudflare_d1", provider.getAttribute("id"));
            var driver = (Element) plugin.getElementsByTagName("driver").item(0);
            Assertions.assertEquals("cloudflare_d1_jdbc", driver.getAttribute("id"));
            Assertions.assertEquals("com.dbeaver.jdbc.d1.D1Driver", driver.getAttribute("class"));
            var files = driver.getElementsByTagName("file");
            int libraries = 0;
            for (int index = 0; index < files.getLength(); index++) {
                var file = (Element) files.item(index);
                if ("jar".equals(file.getAttribute("type"))) {
                    libraries++;
                    Assertions.assertEquals("maven:/com.dbeaver.jdbc:com.dbeaver.jdbc.driver.d1:RELEASE", file.getAttribute("path"));
                    Assertions.assertFalse(file.hasAttribute("bundle"));
                    Assertions.assertNotEquals("true", file.getAttribute("ignore-dependencies"));
                }
            }
            Assertions.assertEquals(1, libraries);
            var auth = (Element) plugin.getElementsByTagName("authModel").item(0);
            Assertions.assertEquals("cloudflare_d1_native", auth.getAttribute("id"));
        }
    }

    @Test
    void communityModelDoesNotDependOnEnterpriseOrTheJdbcImplementation() {
        var bundle = FrameworkUtil.getBundle(CloudflareD1DataSource.class);
        var dependencies = bundle.getHeaders().get("Require-Bundle");
        Assertions.assertFalse(dependencies.contains("com.dbeaver.db."));
        Assertions.assertFalse(dependencies.contains("com.dbeaver.jdbc.driver.d1"));
        for (var type : new Class<?>[] {CloudflareD1ConnectionAdapter.class, CloudflareD1Client.class}) {
            Assertions.assertTrue(Arrays.stream(type.getDeclaredMethods()).noneMatch(method ->
                method.getName().equals("restore") || method.getName().toLowerCase().contains("bookmark")));
        }
    }

    @Test
    void communityCatalogsCannotBeCreatedDeletedOrEdited() throws Exception {
        Assertions.assertFalse(DBPSaveableObject.class.isAssignableFrom(CloudflareD1Catalog.class));
        Assertions.assertNull(ObjectManagerRegistry.getInstance().getObjectManager(CloudflareD1Catalog.class));
        var property = CloudflareD1Catalog.class.getMethod("getReadReplicationMode", DBRProgressMonitor.class)
            .getAnnotation(Property.class);
        Assertions.assertFalse(property.editable());
        Assertions.assertFalse(property.updatable());
        var mutations = Set.of("create", "createDatabase", "delete", "deleteDatabase", "setReplication", "setReadReplication");
        for (var type : new Class<?>[] {CloudflareD1Client.class, CloudflareD1ConnectionAdapter.class}) {
            Assertions.assertTrue(Arrays.stream(type.getDeclaredMethods()).noneMatch(method -> mutations.contains(method.getName())));
        }
    }

    // Public so Mockito can subclass this fixture from its OSGi class loader.
    public static class ConnectionUrlDataSource extends CloudflareD1DataSource {
        private ConnectionUrlDataSource(
            @NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull GenericMetaModel metaModel
        ) throws DBException {
            super(monitor, container, metaModel);
        }

        @Nullable
        private String connectionUrl(@NotNull DBPConnectionConfiguration configuration) throws DBException {
            return super.getConnectionURL(configuration);
        }
    }
}
