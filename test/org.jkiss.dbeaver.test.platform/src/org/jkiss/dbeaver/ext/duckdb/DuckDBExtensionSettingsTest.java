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
package org.jkiss.dbeaver.ext.duckdb;

import org.jkiss.dbeaver.ext.duckdb.model.DuckDBExtensionSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

public class DuckDBExtensionSettingsTest {
    @Test
    public void permitsDefaultsAndDisabledSettings() {
        Assertions.assertTrue(DuckDBExtensionSettings.getUnsafeSettings(null, new Properties()).isEmpty());
        Properties properties = new Properties();
        properties.setProperty("allow_unsigned_extensions", "false");
        Assertions.assertTrue(DuckDBExtensionSettings.getUnsafeSettings(
            "jdbc:duckdb:;allow_extensions_metadata_mismatch=0", properties).isEmpty());
    }

    @Test
    public void capturesBothSettingsAndAlternativeBooleanValues() {
        for (String name : List.of("allow_unsigned_extensions", "allow_extensions_metadata_mismatch")) {
            for (String value : List.of("true", "TRUE", "1", "yes", "on", "t")) {
                Properties properties = new Properties();
                properties.setProperty(name, value);
                Assertions.assertEquals(List.of(name + "=" + value),
                    DuckDBExtensionSettings.getUnsafeSettings("jdbc:duckdb:", properties));
            }
        }
    }

    @Test
    public void capturesUrlSettingsWithWhitespaceAndMixedCase() {
        Assertions.assertEquals(List.of("Allow_Unsigned_Extensions=true", "allow_extensions_metadata_mismatch=1"),
            DuckDBExtensionSettings.getUnsafeSettings(
                "jdbc:duckdb:; Allow_Unsigned_Extensions = true ;allow_extensions_metadata_mismatch=1", new Properties()));
    }

    @Test
    public void doesNotHideUnsafeValuesBehindDuplicateOrOverridingSettings() {
        Properties properties = new Properties();
        properties.setProperty("allow_unsigned_extensions", "true");
        Assertions.assertFalse(DuckDBExtensionSettings.getUnsafeSettings(
            "jdbc:duckdb:;allow_unsigned_extensions=false", properties).isEmpty());
        Assertions.assertFalse(DuckDBExtensionSettings.getUnsafeSettings(
            "jdbc:duckdb:;allow_unsigned_extensions=true;allow_unsigned_extensions=false", new Properties()).isEmpty());
    }

    @Test
    public void includesPropertyDefaultsAndNonStringValues() {
        Properties defaults = new Properties();
        defaults.setProperty("allow_unsigned_extensions", "true");
        Properties properties = new Properties(defaults);
        properties.put("allow_extensions_metadata_mismatch", Boolean.TRUE);
        Assertions.assertEquals(List.of("allow_extensions_metadata_mismatch=true", "allow_unsigned_extensions=true"),
            DuckDBExtensionSettings.getUnsafeSettings("jdbc:duckdb:", properties));
    }

    @Test
    public void snapshotsSettingsWithoutExposingUnrelatedProperties() {
        Properties properties = new Properties();
        properties.setProperty("password", "secret");
        properties.setProperty("allow_unsigned_extensions", "true");
        List<String> settings = DuckDBExtensionSettings.getUnsafeSettings("jdbc:duckdb:", properties);
        properties.setProperty("allow_unsigned_extensions", "false");
        Assertions.assertEquals(List.of("allow_unsigned_extensions=true"), settings);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> settings.add("other"));
    }
}
