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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

public class DriverUtilsTest extends DBeaverUnitTest {

    @Before
    public void clearTimeoutOverrides() {
        System.clearProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS);
        ModelPreferences.getPreferences().setValue(ModelPreferences.UI_DRIVERS_UPDATE_TIMEOUT, 0);
    }

    @After
    public void restoreTimeoutOverrides() {
        System.clearProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS);
        ModelPreferences.getPreferences().setValue(ModelPreferences.UI_DRIVERS_UPDATE_TIMEOUT, 0);
    }

    @Test
    public void testSystemPropertyTakesPrecedence() {
        System.setProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS, "60");
        Assert.assertEquals(Duration.ofSeconds(60), DriverUtils.getDownloadTimeout());
    }

    @Test
    public void testSystemPropertyNegativeValueIsIgnored() {
        System.setProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS, "-1");
        Assert.assertEquals(Duration.ofMillis(WebUtils.DEFAULT_HTTP_TIMEOUT_MS), DriverUtils.getDownloadTimeout());
    }

    @Test
    public void testSystemPropertyZeroValueIsIgnored() {
        System.setProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS, "0");
        Assert.assertEquals(Duration.ofMillis(WebUtils.DEFAULT_HTTP_TIMEOUT_MS), DriverUtils.getDownloadTimeout());
    }

    @Test
    public void testSystemPropertyInvalidValueIsIgnored() {
        System.setProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS, "not-a-number");
        Assert.assertEquals(Duration.ofMillis(WebUtils.DEFAULT_HTTP_TIMEOUT_MS), DriverUtils.getDownloadTimeout());
    }

    @Test
    public void testPreferenceValueUsedWhenNoSystemProperty() {
        ModelPreferences.getPreferences().setValue(ModelPreferences.UI_DRIVERS_UPDATE_TIMEOUT, 30_000);
        Assert.assertEquals(Duration.ofMillis(30_000), DriverUtils.getDownloadTimeout());
    }

    @Test
    public void testSystemPropertyOverridesPreference() {
        ModelPreferences.getPreferences().setValue(ModelPreferences.UI_DRIVERS_UPDATE_TIMEOUT, 30_000);
        System.setProperty(DriverUtils.PROP_DRIVER_DOWNLOAD_TIMEOUT_SECONDS, "5");
        Assert.assertEquals(Duration.ofSeconds(5), DriverUtils.getDownloadTimeout());
    }

    @Test
    public void testDefaultTimeoutWhenNoOverridesAreSet() {
        Assert.assertEquals(Duration.ofMillis(WebUtils.DEFAULT_HTTP_TIMEOUT_MS), DriverUtils.getDownloadTimeout());
    }
}
