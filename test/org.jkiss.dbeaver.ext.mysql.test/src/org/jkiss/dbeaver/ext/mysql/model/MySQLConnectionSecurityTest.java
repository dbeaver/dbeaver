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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MySQLConnectionSecurityTest extends DBeaverUnitTest {
    private static final String URL = "jdbc:mysql://localhost/test";
    private Driver driver;
    private LegacyDriver legacyDriver;

    @BeforeEach
    public void setUp() throws SQLException {
        driver = Mockito.mock(Driver.class);
        Mockito.when(driver.getPropertyInfo(Mockito.anyString(), Mockito.any(Properties.class))).thenReturn(new DriverPropertyInfo[0]);
        legacyDriver = Mockito.mock(LegacyDriver.class, Mockito.CALLS_REAL_METHODS);
        MySQLParser.properties = new HashMap<>();
        MySQLParser.hosts = List.of();
        MySQLParser.hostsRead = false;
        LegacyDriver.parsed = new Properties();
        LegacyDriver.hostOptions = new HashMap<>();
        LegacyDriver.parseCalls = 0;
        MariaDBConfiguration.allowLocalInfile = false;
        MariaDBConfiguration.parseCalls = 0;
    }

    @Test
    public void checksDriverParsedQueryOptions() {
        for (String name : List.of("autoDeserialize", "allowLocalInfile", "ALLOWLOCALINFILE", "allowLoadLocalInfile", "allowUrlInLocalInfile")) {
            MySQLParser.properties.clear();
            MySQLParser.properties.put(name, "true");
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMySQLUrl(URL, MySQLParser.class));
            MySQLParser.properties.put(name, null);
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMySQLUrl(URL, MySQLParser.class));
        }
    }

    @Test
    public void readsLazyHostPropertiesForEveryHost() {
        MySQLParser.hosts = List.of(new HostInfo(Map.of("host", "first")), new HostInfo(Map.of("allowLoadLocalInfile", "true")));
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMySQLUrl(URL, MySQLParser.class));
        Assertions.assertTrue(MySQLParser.hostsRead);
    }

    @Test
    public void rejectsParsedPathAndIndirectOptions() {
        for (String name : List.of("allowLoadLocalInfileInPath", "propertiesTransform", "useConfigs", "credentialType")) {
            MySQLParser.hosts = List.of();
            MySQLParser.properties = Map.of(name, "");
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMySQLUrl(URL, MySQLParser.class));
            MySQLParser.properties = Map.of();
            MySQLParser.hosts = List.of(new HostInfo(Map.of(name, "test")));
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMySQLUrl(URL, MySQLParser.class));
        }
    }

    @Test
    public void doesNotDecodeOrTokenizeDriverOutputAgain() throws Exception {
        MySQLParser.properties = Map.of("%61llowLocalInfile", "true", "comment", "allowLocalInfile=true&useConfigs=test");
        MySQLParser.hosts = List.of(new HostInfo(Map.of("password", "allowLoadLocalInfile=true")));
        MySQLConnectionSecurity.validateMySQLUrl(URL, MySQLParser.class);
        Assertions.assertTrue(MySQLParser.hostsRead);
    }

    @Test
    public void usesDriverResolutionOfDuplicateQueryOptions() throws Exception {
        MySQLParser.properties = Map.of("allowLoadLocalInfile", "false");
        MySQLConnectionSecurity.validateMySQLUrl(URL + "?allowLoadLocalInfile=true&allowLoadLocalInfile=false", MySQLParser.class);
    }

    @Test
    public void preventsConfigurationExecutionBeforeCallingLegacyOrMariaDbParsers() {
        for (String name : List.of("propertiesTransform", "USECONFIGS", "credentialType")) {
            String url = URL + "?" + name + "=test";
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateLegacyUrl(legacyDriver, url, new Properties()));
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMariaDBUrl(url, new Properties(), MariaDBConfiguration.class));
        }
        Assertions.assertEquals(0, LegacyDriver.parseCalls);
        Assertions.assertEquals(0, MariaDBConfiguration.parseCalls);
    }

    @Test
    public void checksLegacyHostsUsingDriverExpansion() {
        for (String key : List.of("HOST", "HOST.1", "HOST.2")) {
            LegacyDriver.parsed.clear();
            LegacyDriver.parsed.setProperty(key, "driver-specific-host");
            Properties hostOptions = new Properties();
            hostOptions.setProperty("autoDeserialize", "true");
            LegacyDriver.hostOptions.put("driver-specific-host", hostOptions);
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateLegacyUrl(legacyDriver, URL, new Properties()));
        }
    }

    @Test
    public void legacyParsingDoesNotMutateConnectionProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("password", "test-password");
        MySQLConnectionSecurity.validateLegacyUrl(legacyDriver, URL, properties);
        Assertions.assertEquals(Map.of("password", "test-password"), properties);
        Assertions.assertEquals(1, LegacyDriver.parseCalls);
    }

    @Test
    public void checksEffectiveMariaDbSettingWithoutMutatingConnectionProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("allowLocalInfile", "false");
        MySQLConnectionSecurity.validateMariaDBUrl(URL, properties, MariaDBConfiguration.class);
        MariaDBConfiguration.allowLocalInfile = true;
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validateMariaDBUrl(URL, properties, MariaDBConfiguration.class));
        Assertions.assertEquals(Map.of("allowLocalInfile", "false"), properties);
    }

    @Test
    public void otherDriversUseTheirOwnPropertyMetadataInsteadOfBeingRejected() throws Exception {
        MySQLConnectionSecurity.validate(driver, "jdbc:other:driver-specific-syntax", new Properties());
        Mockito.verify(driver).getPropertyInfo(Mockito.eq("jdbc:other:driver-specific-syntax"), Mockito.any(Properties.class));
        Mockito.when(driver.getPropertyInfo(Mockito.anyString(), Mockito.any(Properties.class)))
            .thenReturn(new DriverPropertyInfo[] {new DriverPropertyInfo("allowLocalInfile", "true")});
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(driver, URL, new Properties()));
    }

    @Test
    public void guardsEncodedIndirectOptionsBeforeCallingUnknownDriver() {
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(driver, URL + "?%70ropertiesTransform=test", new Properties()));
        Mockito.verifyNoInteractions(driver);
    }

    @Test
    public void removesCaseVariantsBeforeInsertingCanonicalSafeValues() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("ALLOWLOCALINFILE", "true");
        properties.setProperty("AllowLocalInfile", "true");
        properties.setProperty("ALLOWLOADLOCALINFILEINPATH", "/test-path");
        properties.setProperty("connectTimeout", "1000");
        MySQLConnectionSecurity.sanitizeProperties(properties);
        MySQLConnectionSecurity.validate(driver, URL, properties);
        Assertions.assertEquals("false", properties.getProperty("allowLocalInfile"));
        Assertions.assertFalse(properties.containsKey("ALLOWLOCALINFILE"));
        Assertions.assertFalse(properties.containsKey("AllowLocalInfile"));
        Assertions.assertFalse(properties.containsKey("ALLOWLOADLOCALINFILEINPATH"));
        Assertions.assertEquals("1000", properties.getProperty("connectTimeout"));
    }

    @Test
    public void rejectsIndirectAndInheritedUnsafePropertiesBeforeDriverInvocation() {
        for (String name : List.of("ALLOWLOCALINFILE", "propertiesTransform", "useConfigs", "credentialType")) {
            Properties defaults = new Properties();
            defaults.setProperty(name, "true");
            Properties properties = new Properties(defaults);
            MySQLConnectionSecurity.sanitizeProperties(properties);
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(driver, URL, properties));
        }
        Mockito.verifyNoInteractions(driver);
    }

    @Test
    public void parserErrorsDoNotDiscloseConnectionSecrets() throws SQLException {
        String url = "jdbc:mysql://user:secret-password@localhost/test";
        Mockito.when(driver.getPropertyInfo(Mockito.anyString(), Mockito.any(Properties.class))).thenThrow(new SQLException(url));
        DBCException exception = Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(driver, url, new Properties()));
        Assertions.assertFalse(exception.getMessage().contains("secret-password"));
        Assertions.assertFalse(exception.getMessage().contains("jdbc:mysql"));
        Assertions.assertNull(exception.getCause());
    }

    // API doubles provide already-parsed values. They intentionally do not implement any URL grammar.
    public static class MySQLParser {
        static Map<String, String> properties;
        static List<HostInfo> hosts;
        static boolean hostsRead;

        @NotNull
        public static MySQLParser parseConnectionString(@NotNull String url) {
            return new MySQLParser();
        }

        @NotNull
        public Map<String, String> getProperties() {
            return properties;
        }

        @NotNull
        public List<HostInfo> getHosts() {
            hostsRead = true;
            return hosts;
        }
    }

    public record HostInfo(@NotNull Map<String, String> properties) {
        @NotNull
        public Map<String, String> getHostProperties() {
            return properties;
        }
    }

    public abstract static class LegacyDriver implements Driver {
        static Properties parsed;
        static Map<String, Properties> hostOptions;
        static int parseCalls;

        @NotNull
        public Properties parseURL(@NotNull String url, @NotNull Properties properties) {
            parseCalls++;
            properties.setProperty("driverMutation", "test");
            return parsed;
        }

        public static boolean isHostPropertiesList(@NotNull String host) {
            return hostOptions.containsKey(host);
        }

        @NotNull
        public static Properties expandHostKeyValues(@NotNull String host) {
            return hostOptions.getOrDefault(host, new Properties());
        }
    }

    public static class MariaDBConfiguration {
        static boolean allowLocalInfile;
        static int parseCalls;

        @NotNull
        public static MariaDBConfiguration parse(@NotNull String url, @NotNull Properties properties) {
            parseCalls++;
            properties.setProperty("driverMutation", "test");
            return new MariaDBConfiguration();
        }

        public boolean allowLocalInfile() {
            return allowLocalInfile;
        }
    }
}
