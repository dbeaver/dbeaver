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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.vertica.VerticaConstants;
import org.jkiss.dbeaver.ext.vertica.VerticaSSLUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class VerticaSSLUtilsTest {

    private static final String URL = "jdbc:vertica://localhost:5433/test";

    @Test
    public void requireWithoutKeyStoreNeedsWorkaround() {
        Properties properties = requireProperties();
        assertTrue(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
        assertTrue(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, ""));
        assertEquals(1, properties.size());
        properties.setProperty("KeystorePassword", "");
        assertTrue(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
    }

    @Test
    public void otherTLSModesAreUnchanged() {
        Properties properties = new Properties();
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
        for (String mode : new String[] {"disable", "verify-ca", "verify-full"}) {
            properties.setProperty(VerticaConstants.PROP_TLS_MODE, mode);
            assertFalse(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
        }
    }

    @Test
    public void configuredKeyStoresAndFactoriesAreUnchanged() {
        for (String name : new String[] {"KeystorePath", "KeystorePassword", "SSLSocketFactoryName"}) {
            Properties properties = requireProperties();
            properties.setProperty(name, "custom");
            assertFalse(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
        }
        Properties properties = requireProperties();
        properties.setProperty("KeystorePath", "");
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(requireProperties(), URL, "/custom/client.jks"));
    }

    @Test
    public void urlParametersAreRespected() {
        assertTrue(VerticaSSLUtils.needsEmptyKeyStore(new Properties(), URL + "?TLSMode=ReQuIrE", null));
        assertTrue(VerticaSSLUtils.needsEmptyKeyStore(new Properties(), URL + "?TLSMode=require#fragment", null));
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(requireProperties(), URL + "?TLSMode=verify-full", null));
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(requireProperties(), URL + "?KeystorePath=%2Fcustom.jks", null));
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(requireProperties(), URL + "?KeystorePath=", null));
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(requireProperties(), URL + "?SSLSocketFactoryName=custom", null));
        assertFalse(VerticaSSLUtils.needsEmptyKeyStore(requireProperties(), URL + "?KeystorePath=%invalid", null));
    }

    @Test
    public void trustStoreSettingsAreUnchanged() {
        Properties properties = requireProperties();
        properties.setProperty("truststorepath", "/custom/trust.jks");
        properties.setProperty("truststorepassword", "custom");
        Properties original = new Properties();
        original.putAll(properties);
        assertTrue(VerticaSSLUtils.needsEmptyKeyStore(properties, URL, null));
        assertEquals(original, properties);
    }

    @Test
    public void generatedKeyStoreIsEmptyAndReused(@TempDir @NotNull Path tempFolder) throws Exception {
        Path path = VerticaSSLUtils.getEmptyKeyStore(tempFolder);
        assertTrue(Files.isRegularFile(path));
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream input = Files.newInputStream(path)) {
            keyStore.load(input, VerticaSSLUtils.EMPTY_KEYSTORE_PASSWORD.toCharArray());
        }
        assertEquals(0, keyStore.size());
        assertEquals(path, VerticaSSLUtils.getEmptyKeyStore(tempFolder));
        Files.delete(path);
        assertTrue(Files.isRegularFile(VerticaSSLUtils.getEmptyKeyStore(tempFolder)));
    }

    @NotNull
    private Properties requireProperties() {
        Properties properties = new Properties();
        properties.setProperty(VerticaConstants.PROP_TLS_MODE, VerticaConstants.TLS_MODE_REQUIRE);
        return properties;
    }
}
