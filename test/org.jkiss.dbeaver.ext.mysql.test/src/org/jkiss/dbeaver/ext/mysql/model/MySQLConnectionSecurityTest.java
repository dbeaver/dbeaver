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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class MySQLConnectionSecurityTest extends DBeaverUnitTest {
    private final MySQLConnectionSecurity validator = new MySQLConnectionSecurity();

    @ParameterizedTest
    @ValueSource(strings = {
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile",
        "allowLoadLocalInfileInPath", "propertiesTransform", "useConfigs", "credentialType"
    })
    public void rejectsAnyMentionOfProhibitedNames(@NotNull String name) {
        String upperName = name.toUpperCase(Locale.ROOT);
        for (String url : List.of(
            "jdbc:mysql://localhost/test?" + name + "=true",
            "jdbc:mariadb://localhost/test?" + upperName + "=false",
            "jdbc:custom:prefix-" + name + "-suffix",
            "jdbc:custom://address=(host=localhost)(" + upperName + "=)/test",
            "jdbc:custom://localhost/test?comment=prefix-" + name + "-suffix",
            "jdbc:custom://localhost/test?" + name + "=true&" + name + "=false"
        )) {
            Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(url, new Properties()), url);
        }
        for (String value : List.of("true", "false", "", "0", "1")) {
            Properties properties = new Properties();
            properties.setProperty("prefix-" + upperName + "-suffix", value);
            assertPropertiesRejected(properties);
        }
        Properties properties = new Properties();
        properties.setProperty("ordinaryProperty", "prefix-" + upperName + "-suffix");
        assertPropertiesRejected(properties);
    }

    @Test
    public void rejectsEncodedNamesInUrlsAndProperties() {
        for (String text : List.of("%61utoDeserialize", "%41LLOWLOCALINFILE", "allowLoadLocalInfile%49nPath")) {
            Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(
                "jdbc:custom://localhost/test?" + text + "=false", new Properties()));
            Properties properties = new Properties();
            properties.setProperty(text, "false");
            assertPropertiesRejected(properties);
            properties.clear();
            properties.setProperty("HOST", "address=(host=localhost)(" + text + "=true)");
            assertPropertiesRejected(properties);
        }
    }

    @Test
    public void rejectsInheritedAndNonStringProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("ALLOWLOCALINFILE", "false");
        Properties properties = new Properties(defaults);
        assertPropertiesRejected(properties);
        Assertions.assertEquals("false", defaults.getProperty("ALLOWLOCALINFILE"));

        properties = new Properties();
        properties.put("allowLocalInfile", Boolean.FALSE);
        assertPropertiesRejected(properties);
        properties.clear();
        properties.put(123, "autoDeserialize");
        assertPropertiesRejected(properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:mysql://localhost/test", "jdbc:mariadb://localhost/test", "jdbc:mysql:loadbalance://localhost/test",
        "jdbc:mysql:replication://localhost/test", "jdbc:mysql+srv://localhost/test",
        "jdbc:mysql+srv:loadbalance://localhost/test", "jdbc:mariadb:sequential://localhost/test",
        "JDBC:MYSQL://localhost/test", "jdbc:%6dysql://localhost/test",
        "jdbc:mysql+srv://localhost/test?comment=hello%20world", "jdbc:%6dysql+srv://localhost/test"
    })
    public void safeInputsArePreservedAndReceiveSafeDefaults(@NotNull String url) throws DBCException {
        Properties defaults = new Properties();
        defaults.setProperty("connectTimeout", "1000");
        Properties properties = new Properties(defaults);
        properties.setProperty("user", "test-user");
        properties.setProperty("password", "test-password");
        properties.setProperty("HOST", "address=(host=localhost)(port=3306)");

        validator.validateConnectionProperties(url, properties);

        Assertions.assertEquals("1000", properties.getProperty("connectTimeout"));
        Assertions.assertEquals("test-user", properties.getProperty("user"));
        Assertions.assertEquals("test-password", properties.getProperty("password"));
        Assertions.assertEquals("address=(host=localhost)(port=3306)", properties.getProperty("HOST"));
        for (String name : List.of("autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile")) {
            Assertions.assertEquals("false", properties.getProperty(name));
            Assertions.assertFalse(defaults.containsKey(name));
        }
        Assertions.assertFalse(properties.containsKey("allowLoadLocalInfileInPath"));
    }

    @Test
    public void rejectsMissingUrlsAndProhibitedNamesWithoutDisclosingSecrets() {
        Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(null, new Properties()));
        for (String url : List.of(
            "jdbc:custom://user:secret@localhost/test?autoDeserialize=false",
            "jdbc:custom://user:secret%@localhost/test?%61utoDeserialize=false"
        )) {
            DBCException error = Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties(url, new Properties()));
            Assertions.assertFalse(error.getMessage().contains("secret"));
            Assertions.assertFalse(error.getMessage().contains(url));
            Assertions.assertNull(error.getCause());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:trino://localhost:8080/test", "jdbc:postgresql://localhost/test", "jdbc:custom:arbitrary-template",
        "jdbc:custom://localhost/jdbc:mysql:", "jdbc:mysql-other://localhost/test"
    })
    public void otherUrlFamiliesDoNotReceiveMysqlDefaults(@NotNull String url) throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("user", "test-user");
        properties.setProperty("password", "test-password");
        Properties original = (Properties) properties.clone();

        validator.validateConnectionProperties(url, properties);

        Assertions.assertEquals(original, properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {"example%password", "%secret", "100%", "%", "%2", "%gg", "hello%20world%", "%%41"})
    public void literalPercentValuesAreAcceptedWithoutMutation(@NotNull String value) throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("password", value);
        Properties original = (Properties) properties.clone();

        validator.validateConnectionProperties("jdbc:custom:test?comment=" + value, properties);

        Assertions.assertEquals(original, properties);
    }

    @Test
    public void literalPercentDoesNotHideEncodedProhibitedNames() {
        for (String text : List.of("100%&%61utoDeserialize=false", "%gg%61llowLocalInfile=true", "%2%75seConfigs")) {
            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test?" + text, new Properties()));
            Properties properties = new Properties();
            properties.setProperty("HOST", text);
            assertPropertiesRejected(properties);
            properties.clear();
            properties.setProperty(text, "false");
            assertPropertiesRejected(properties);
        }
    }

    @Test
    public void comparisonDoesNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(
                "jdbc:custom:test?ALLOWLOCALINFILE=false", new Properties()));
        } finally {
            Locale.setDefault(original);
        }
    }

    private void assertPropertiesRejected(@NotNull Properties properties) {
        Properties original = (Properties) properties.clone();
        Assertions.assertThrows(DBCException.class,
            () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
        Assertions.assertEquals(original, properties);
    }
}
