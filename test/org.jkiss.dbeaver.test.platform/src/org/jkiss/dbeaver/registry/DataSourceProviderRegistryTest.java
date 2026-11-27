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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeDescriptor;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.MissingDataSourceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DataSourceProviderRegistryTest {
    @Test
    public void testEveryDriverHasDataSourceType() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();

        for (DBPDataSourceProviderDescriptor provider : registry.getDataSourceProviders()) {
            for (DBPDriver driver : provider.getDrivers()) {
                DBPDataSourceType type = driver.getDataSourceType();
                Assertions.assertNotNull(type, provider.getId() + ":" + driver.getId());
                Assertions.assertFalse(type.getDataSourceInformation().isBlank(), type.getId());
            }
        }
    }

    @Test
    public void testCustomDriverUsesCustomDataSourceType() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        DriverDescriptor driver = new DriverDescriptor(registry.getDataSourceProvider("generic"), "test-custom-driver");

        Assertions.assertSame(registry.getDataSourceType(DBPDataSourceType.CUSTOM_ID), driver.getDataSourceType());
    }

    @Test
    public void testBigQueryDriversShareDataSourceType() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        DBPDataSourceType type = registry.getDataSourceType("bigquery");
        DBPDriver simbaDriver = registry.getDataSourceProvider("bigquery").getDriver("google_bigquery_jdbc_simba");
        DBPDriver standardDriver = registry.getDataSourceProvider("bigquery").getDriver("google_bigquery_jdbc");

        Assertions.assertNotNull(type);
        Assertions.assertEquals(type, standardDriver.getDataSourceType());
        Assertions.assertEquals(type, simbaDriver.getDataSourceType());
        Assertions.assertSame(type.getIcon(), standardDriver.getPlainIcon());
        Assertions.assertSame(type.getIconBig(), standardDriver.getIconBig());
    }

    @Test
    public void testConnectionTypesCollectionIsStableDuringRegistryUpdate() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        Collection<DBPConnectionType> connectionTypesSnapshot = registry.getConnectionTypes();
        DBPConnectionType customType = new DBPConnectionType(
            "snapshot-test",
            "Snapshot test",
            "1,2,3",
            "4,5,6",
            null,
            true,
            false,
            false,
            false,
            false,
            false,
            0,
            false,
            0
        );

        try {
            registry.addConnectionType(customType);

            Assertions.assertSame(customType, registry.getConnectionType(customType.getId(), null));
            Assertions.assertFalse(connectionTypesSnapshot.contains(customType));
        } finally {
            registry.removeConnectionType(customType);
        }
    }

    @Test
    public void testBuiltInDriverReplacement() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        DBPDriver original = registry.getDataSourceProvider("generic").getOriginalDriver("h2_embedded_v2");
        DBPDriver replacement = registry.getDataSourceProvider("h2").getOriginalDriver("h2_embedded_v2");

        Assertions.assertNotNull(original);
        Assertions.assertNotNull(replacement);
        Assertions.assertSame(replacement, original.getReplacedBy());
    }

    @Test
    public void testBuiltInDriverLibrariesAreAvailable() {
        DBPDriver driver = DataSourceProviderRegistry.getInstance()
            .getDataSourceProvider("h2")
            .getOriginalDriver("h2_embedded_v2");

        Assertions.assertNotNull(driver);
        Assertions.assertFalse(driver.getDriverLibraries().isEmpty());
    }

    @Test
    public void testInheritedTreeRemainsProviderSpecific() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        DataSourceProviderDescriptor generic = registry.getDataSourceProvider("generic");
        DataSourceProviderDescriptor h2 = registry.getDataSourceProvider("h2");

        DBXTreeDescriptor genericTree = generic.getTreeDescriptor();
        DBXTreeDescriptor h2Tree = h2.getTreeDescriptor();
        Assertions.assertNotNull(genericTree);
        Assertions.assertNotNull(h2Tree);
        Assertions.assertNotSame(genericTree, h2Tree);
        Assertions.assertEquals(genericTree.getPath(), h2Tree.getPath());
    }

    @Test
    public void testTreePropertyChangeInvalidatesCachedGetterAndPreservesChildren() {
        var provider = DataSourceProviderRegistry.getInstance().getDataSourceProvider("generic");
        var item = new DBXTreeItem(provider, null, null, "item", "original", false, true, false, false, false, null, null);
        var child = new DBXTreeItem(provider, item, null, "child", "original", false, true, false, false, false, null, null);
        Assertions.assertEquals(String.class, item.getPropertyOrCollectionItemType(PropertyContainer.class));
        item.setPropertyName("replacement");

        Assertions.assertEquals("replacement", item.getPropertyName());
        Assertions.assertEquals(Integer.class, item.getPropertyOrCollectionItemType(PropertyContainer.class));
        Assertions.assertSame(child, item.findChildItemByPath("child"));
    }

    @Test
    public void testProviderInheritDriversOptInIncludesBehavioralAttributes() {
        var registry = Mockito.mock(DataSourceProviderRegistry.class);
        var parent = new DataSourceProviderDescriptor(registry, InheritedConfigurationTest.element(
            "datasource", "parent.bundle", Map.of(
                "id", "attribute-parent", "label", "Parent", "description", "Description",
                "supports-migration", "true", "migration-completed-property", "migration.done",
                "dataSourceType", "inherited-type", "icon", "icons/parent.svg"
            )));
        Mockito.when(registry.getDataSourceProvider(parent.getId())).thenReturn(parent);
        var child = new DataSourceProviderDescriptor(registry, InheritedConfigurationTest.element(
            "datasource", "child.bundle", Map.of(
                "id", "attribute-child", "parent", parent.getId(), "inheritDrivers", "true"
            )));
        var overridden = new DataSourceProviderDescriptor(registry, InheritedConfigurationTest.element(
            "datasource", "child.bundle", Map.of(
                "id", "attribute-override", "parent", parent.getId(), "inheritDrivers", "true", "icon", "icons/local.svg"
            )));

        Assertions.assertEquals(parent.getName(), child.getName());
        Assertions.assertEquals(parent.getDescription(), child.getDescription());
        Assertions.assertTrue(child.supportsDriverMigration());
        Assertions.assertEquals("migration.done", child.getMigrationCompletedProperty());
        Assertions.assertEquals("inherited-type", child.getDataSourceTypeId());
        Assertions.assertEquals("platform:/plugin/parent.bundle/icons/parent.svg", child.getIcon().getLocation());
        Assertions.assertEquals("platform:/plugin/child.bundle/icons/local.svg", overridden.getIcon().getLocation());
    }

    @Test
    public void testProviderInheritanceRequiresNewRawOptIn() {
        var registry = Mockito.mock(DataSourceProviderRegistry.class);
        var parent = provider(registry, Map.of(
            "id", "opt-in-parent", "label", "Parent", "inheritDrivers", "true", "supports-migration", "true"
        ));
        Mockito.when(registry.getDataSourceProvider(parent.getId())).thenReturn(parent);
        var absent = provider(registry, Map.of("id", "opt-in-absent", "parent", parent.getId()));
        var disabled = provider(registry, Map.of(
            "id", "opt-in-disabled", "parent", parent.getId(), "inheritDrivers", "false"
        ));
        var legacy = provider(registry, Map.of(
            "id", "opt-in-legacy", "parent", parent.getId(), "inheritMetadata", "true"
        ));

        for (var child : List.of(absent, disabled, legacy)) {
            Assertions.assertEquals(child.getId(), child.getName());
            Assertions.assertFalse(child.supportsDriverMigration());
            Assertions.assertSame(DBIcon.DATABASE_DEFAULT, child.getIcon());
        }
    }

    @Test
    public void testProviderExplicitEmptyAndFalseOverrideParent() {
        var registry = Mockito.mock(DataSourceProviderRegistry.class);
        var parent = provider(registry, Map.of(
            "id", "override-parent", "description", "Parent", "supports-migration", "true",
            "migration-completed-property", "migration.done", "dataSourceType", "parent-type",
            "icon", "icons/parent.svg"
        ));
        Mockito.when(registry.getDataSourceProvider(parent.getId())).thenReturn(parent);
        var child = provider(registry, Map.of(
            "id", "override-child", "parent", parent.getId(), "inheritDrivers", "true",
            "description", "", "supports-migration", "false", "migration-completed-property", "",
            "dataSourceType", "", "icon", ""
        ));

        Assertions.assertEquals("", child.getDescription());
        Assertions.assertFalse(child.supportsDriverMigration());
        Assertions.assertEquals("", child.getMigrationCompletedProperty());
        Assertions.assertEquals("", child.getDataSourceTypeId());
        Assertions.assertSame(DBIcon.DATABASE_DEFAULT, child.getIcon());
    }

    @Test
    public void testProviderClassIsNotInherited() {
        var registry = Mockito.mock(DataSourceProviderRegistry.class);
        var parent = provider(registry, Map.of(
            "id", "class-parent", "class", MissingDataSourceProvider.class.getName()
        ));
        Mockito.when(registry.getDataSourceProvider(parent.getId())).thenReturn(parent);
        var child = provider(registry, Map.of(
            "id", "class-child", "parent", parent.getId(), "inheritDrivers", "true"
        ));
        var explicit = provider(registry, Map.of(
            "id", "class-explicit", "parent", parent.getId(), "inheritDrivers", "true",
            "class", MissingDataSourceProvider.class.getName()
        ));

        Assertions.assertThrows(IllegalStateException.class, child::getInstance);
        Assertions.assertInstanceOf(MissingDataSourceProvider.class, explicit.getInstance());
    }

    @Test
    public void testDriverParentInheritsAttributesAndParameters() {
        var registry = Mockito.mock(DataSourceProviderRegistry.class);
        var parentProvider = provider(registry, Map.of("id", "driver-parent-provider"));
        var childProvider = provider(registry, Map.of("id", "driver-child-provider"));
        var original = new DriverDescriptor(parentProvider,
            InheritedConfigurationTest.element("driver", "parent.bundle", Map.of(
                "id", "selected-parent", "label", "Selected", "description", "Inherited",
                "class", "example.ParentDriver", "defaultPort", "1234",
                "embedded", "true", "promoted", "9", "icon", "icons/driver.svg",
                "iconBig", "icons/driver-big.svg", "logoImage", "icons/logo.svg"
            ),
                element("parameter", Map.of("name", "parent.parameter", "value", "parent")),
                element("property", Map.of("name", "parent.property", "value", "parent")),
                element("file", Map.of("type", "jar", "path", "parent.jar")),
                element("fileSource", Map.of("name", "Parent source", "url", "https://example.com/driver")),
                element("replace", Map.of("provider", "generic", "driver", "old-driver")),
                element("main-properties", Map.of(), element("propertyGroup", Map.of(),
                    element("property", Map.of("id", "parent.main")))),
                element("provider-properties", Map.of(), element("propertyGroup", Map.of(),
                    element("property", Map.of("id", "parent.provider"))))
            ));
        var intermediate = new DriverDescriptor(childProvider, element("driver", Map.of("id", "intermediate")), original);
        var inherited = new DriverDescriptor(childProvider,
            InheritedConfigurationTest.element("driver", "child.bundle", Map.of("id", "different-id")), intermediate);
        var overridden = new DriverDescriptor(childProvider,
            InheritedConfigurationTest.element("driver", "child.bundle", Map.of(
                "id", "overridden", "description", "", "embedded", "false", "promoted", "0", "icon", "icons/local.svg"
            ), element("parameter", Map.of("name", "child.parameter", "value", "child")),
                element("property", Map.of("name", "child.property", "value", "child"))),
            original);

        Assertions.assertEquals("different-id", inherited.getId());
        Assertions.assertEquals("Selected", inherited.getName());
        Assertions.assertEquals("Inherited", inherited.getDescription());
        Assertions.assertEquals("example.ParentDriver", inherited.getDriverClassName());
        Assertions.assertEquals("1234", inherited.getDefaultPort());
        Assertions.assertTrue(inherited.isEmbedded());
        Assertions.assertEquals(9, inherited.getPromotedScore());
        Assertions.assertEquals("platform:/plugin/parent.bundle/icons/driver.svg", inherited.getPlainIcon().getLocation());
        Assertions.assertEquals("platform:/plugin/parent.bundle/icons/driver-big.svg", inherited.getIconBig().getLocation());
        Assertions.assertNotNull(inherited.getLogoImage());
        Assertions.assertEquals("platform:/plugin/parent.bundle/icons/logo.svg", inherited.getLogoImage().getLocation());
        Assertions.assertFalse(original.getDriverParameters().isEmpty());
        Assertions.assertFalse(original.getDefaultConnectionProperties().isEmpty());
        Assertions.assertFalse(original.getDriverLibraries().isEmpty());
        Assertions.assertFalse(original.getDriverFileSources().isEmpty());
        Assertions.assertFalse(original.getDriverReplacementsInfo().isEmpty());
        Assertions.assertEquals(1, original.getMainPropertyDescriptors().length);
        Assertions.assertEquals(1, original.getProviderPropertyDescriptors().length);
        Assertions.assertEquals(original.getDefaultDriverParameters(), inherited.getDriverParameters());
        Assertions.assertTrue(inherited.getDefaultConnectionProperties().isEmpty());
        Assertions.assertTrue(inherited.getDriverLibraries().isEmpty());
        Assertions.assertTrue(inherited.getDriverFileSources().isEmpty());
        Assertions.assertTrue(inherited.getDriverReplacementsInfo().isEmpty());
        Assertions.assertEquals(0, inherited.getMainPropertyDescriptors().length);
        Assertions.assertEquals(0, inherited.getProviderPropertyDescriptors().length);
        Assertions.assertEquals("", overridden.getDescription());
        Assertions.assertFalse(overridden.isEmbedded());
        Assertions.assertEquals(0, overridden.getPromotedScore());
        Assertions.assertEquals("platform:/plugin/child.bundle/icons/local.svg", overridden.getPlainIcon().getLocation());
        Assertions.assertEquals(Map.of("parent.parameter", "parent", "child.parameter", "child"), overridden.getDriverParameters());
        Assertions.assertEquals(Map.of("child.property", "child"), overridden.getDefaultConnectionProperties());
    }

    @Test
    public void testDriverEmptyLogoDoesNotCopyParentLogo() {
        var registry = Mockito.mock(DataSourceProviderRegistry.class);
        var provider = provider(registry, Map.of("id", "logo-provider"));
        var parent = new DriverDescriptor(provider, element("driver", Map.of(
            "id", "logo-parent", "label", "Parent", "logoImage", "icons/logo.svg",
            "icon", "#database_icon_default", "iconBig", "platform:/plugin/other.bundle/icons/large.svg"
        )));
        var inherited = new DriverDescriptor(provider, element("driver", Map.of("id", "logo-inherited")), parent);
        var empty = new DriverDescriptor(provider, element("driver", Map.of("id", "logo-empty", "logoImage", "")), parent);

        Assertions.assertNotNull(parent.getLogoImage());
        Assertions.assertEquals(parent.getLogoImage().getLocation(), inherited.getLogoImage().getLocation());
        Assertions.assertSame(DBIcon.DATABASE_DEFAULT, inherited.getPlainIcon());
        Assertions.assertEquals("platform:/plugin/other.bundle/icons/large.svg", inherited.getIconBig().getLocation());
        Assertions.assertNull(empty.getLogoImage());
    }

    @NotNull
    private static DataSourceProviderDescriptor provider(
        @NotNull DataSourceProviderRegistry registry,
        @NotNull Map<String, String> attributes
    ) {
        return new DataSourceProviderDescriptor(registry, element("datasource", attributes));
    }

    @NotNull
    private static IConfigurationElement element(
        @NotNull String name,
        @NotNull Map<String, String> attributes,
        @NotNull IConfigurationElement... children
    ) {
        return InheritedConfigurationTest.element(name, "org.jkiss.dbeaver.test.platform", attributes, children);
    }

    public static class PropertyContainer {
        public List<String> getOriginal() {
            return List.of();
        }

        public List<Integer> getReplacement() {
            return List.of();
        }
    }
}
