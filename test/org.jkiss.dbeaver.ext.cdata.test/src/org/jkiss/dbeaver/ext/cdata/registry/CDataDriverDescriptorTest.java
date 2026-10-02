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

import org.jkiss.dbeaver.ext.cdata.model.CDataIcons;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

public class CDataDriverDescriptorTest extends DBeaverUnitTest {
    @Test
    public void notifyLicenseChangesAndRemoveListener() {
        var registry = DataSourceProviderRegistry.getInstance();
        var driver = new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-license-listeners",
            new CDataDriverInfo("postgresql", "postgresql", "PostgreSQL JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "postgresql", "APRN-VSDBVR"));
        var notifications = new AtomicInteger();
        Runnable failingListener = () -> {
            throw new IllegalStateException("test listener failure");
        };
        Runnable listener = () -> {
            Assertions.assertEquals(CDataLicenseStatus.TRIAL_ACTIVE, driver.getLicenseStatus());
            notifications.incrementAndGet();
        };
        driver.addLicenseChangeListener(failingListener);
        driver.addLicenseChangeListener(listener);

        driver.setCurrentLicense(new CDataDriverLicense(CDataLicenseStatus.TRIAL_ACTIVE, null, null));

        Assertions.assertEquals(1, notifications.get());
        driver.removeLicenseChangeListener(failingListener);
        driver.removeLicenseChangeListener(listener);
        driver.setCurrentLicense(new CDataDriverLicense(CDataLicenseStatus.PURCHASED_ACTIVE, null, null));
        Assertions.assertEquals(1, notifications.get());
    }

    @Test
    public void connectedIconPreservesOriginalDriverIcon() {
        var registry = DataSourceProviderRegistry.getInstance();
        var driver = new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-connected-icon",
            new CDataDriverInfo("postgresql", "postgresql", "PostgreSQL JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "postgresql", "APRN-VSDBVR"));
        var icon = (DBIconComposite) driver.getIcon();

        var connectedIcon = (DBIconComposite) DBNModel.getStateOverlayImage(icon, DBSObjectState.ACTIVE);

        Assertions.assertNull(connectedIcon.getTopLeft());
        Assertions.assertSame(DBIcon.OVER_SUCCESS, connectedIcon.getBottomRight());
        Assertions.assertSame(CDataIcons.CDATA_OVERLAY, icon.getBottomRight());
    }

    @Test
    public void aggregateWithExistingDataSourceType() {
        var registry = DataSourceProviderRegistry.getInstance();
        var type = registry.getDataSourceType("postgresql");
        Assertions.assertNotNull(type);
        var icon = type.getIconBig();
        var driver = new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-postgresql",
            new CDataDriverInfo("postgresql", "postgresql", "PostgreSQL JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "postgresql", "APRN-VSDBVR"));

        Assertions.assertSame(type, driver.getDataSourceType());
        Assertions.assertEquals("https://cdn.cdata.com/help/APN/jdbc/", driver.getDatabaseDocumentationSuffixURL());
        Assertions.assertEquals("https://cdn.cdata.com/help/APN/jdbc/", driver.getPropertiesWebURL());
        Assertions.assertEquals("https://www.cdata.com/", driver.getDriverPurchaseURL());
        Assertions.assertTrue(type.getDrivers().contains(driver));
        driver.setIconBig(DBIcon.DATABASE_BIG_DEFAULT);
        Assertions.assertSame(icon, type.getIconBig());
    }

    @Test
    public void dynamicDataSourceTypeTracksLoadedDriverIcons() {
        var registry = DataSourceProviderRegistry.getInstance();
        var driver = new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-new-source",
            new CDataDriverInfo("newsource", "newsource", "New source JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "test-cdata-new-type", null));
        driver.setName("CData new source");
        var type = driver.getDataSourceType();
        Assertions.assertEquals("test-cdata-new-type", type.getId());
        Assertions.assertEquals("CData new source", type.getName());
        Assertions.assertSame(driver.getIconBig(), type.getIconBig());
        var smallIcon = new DBIcon("file:/test-small.png");
        var bigIcon = new DBIcon("file:/test-big.png");

        driver.setIconPlain(smallIcon);
        driver.setIconBig(bigIcon);

        Assertions.assertSame(smallIcon, type.getIcon());
        Assertions.assertSame(bigIcon, type.getIconBig());
    }

    @Test
    public void loadCachedIconsInBackground() throws Exception {
        var registry = DataSourceProviderRegistry.getInstance();
        var driver = new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-icons",
            new CDataDriverInfo("testcdataicons", "testcdataicons", "Test icons JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "test-cdata-icons-type", null));
        var type = driver.getDataSourceType();
        var placeholder = type.getIconBig();
        var iconDirectory = CDataDriverLoaderDescriptor.getStoragePath().resolve("icons");
        Files.createDirectories(iconDirectory);
        var sizes = Map.of(".png", 16, "@2x.png", 32, "_big.png", 64, "_big@2x.png", 128);
        try {
            for (var entry : sizes.entrySet()) {
                var image = new BufferedImage(entry.getValue(), entry.getValue(), BufferedImage.TYPE_INT_ARGB);
                ImageIO.write(image, "png", iconDirectory.resolve("testcdataicons" + entry.getKey()).toFile());
            }
            var updated = new CompletableFuture<Void>();

            driver.loadIcon(() -> updated.complete(null));
            updated.get(10, TimeUnit.SECONDS);

            Assertions.assertNotSame(placeholder, type.getIconBig());
            Assertions.assertSame(driver.getPlainIcon(), type.getIcon());
            Assertions.assertSame(driver.getIconBig(), type.getIconBig());
        } finally {
            for (String suffix : sizes.keySet()) {
                Files.deleteIfExists(iconDirectory.resolve("testcdataicons" + suffix));
            }
        }
    }

}
