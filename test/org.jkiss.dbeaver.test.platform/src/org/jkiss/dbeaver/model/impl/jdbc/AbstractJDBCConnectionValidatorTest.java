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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class AbstractJDBCConnectionValidatorTest extends DBeaverUnitTest {
    private final JDBCConnectionValidator validator = new AbstractJDBCConnectionValidator() {
        @NotNull
        @Override
        protected List<String> getRestrictedProperties() {
            return List.of("loadExtension", "remoteScript");
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {
        "loadExtension", "remoteScript", "%6coadExtension", "remote%53cript",
        "loadExtension%", "%ggremoteScript", "loadExtension%2"
    })
    public void rejectsConfiguredNamesInUrlsKeysAndValues(@NotNull String name) {
        String text = "prefix-" + name.toUpperCase(Locale.ROOT) + "-suffix";
        Properties urlProperties = new Properties();
        urlProperties.setProperty("user", "test-user");
        DBCException error = Assertions.assertThrows(DBCException.class,
            () -> validator.validateConnectionProperties("jdbc:custom://user:secret@localhost/test?" + text + "=false", urlProperties));
        Assertions.assertFalse(error.getMessage().contains("secret"));
        Assertions.assertEquals(Map.of("user", "test-user"), urlProperties);

        for (Map<String, String> input : List.of(Map.of(text, "false"), Map.of("HOST", text))) {
            Properties properties = new Properties();
            properties.putAll(input);
            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
            Assertions.assertEquals(input, properties);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"example%password", "%", "%2", "%gg"})
    public void ignoresMalformedPercentEscapesWithoutMutation(@NotNull String value) throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("password", value);
        properties.setProperty("comment" + value, "text");
        Properties expected = (Properties) properties.clone();

        validator.validateConnectionProperties("jdbc:custom:test?comment=" + value, properties);

        Assertions.assertEquals(expected, properties);
    }

    @Test
    public void usesOnlyConfiguredRestrictionsAndAddsNoDefaults() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("autoDeserialize", "true");
        properties.setProperty("comment", "allowLocalInfile");
        properties.setProperty("password", "hello%20world+test");
        Properties expected = (Properties) properties.clone();

        validator.validateConnectionProperties("jdbc:mysql://localhost/test?useConfigs=custom", properties);

        Assertions.assertEquals(expected, properties);
    }

    @Test
    public void preservesInheritedAndNonStringProperties() throws DBCException {
        Properties defaults = new Properties();
        defaults.setProperty("password", "inherited%password+");
        Properties properties = new Properties(defaults);
        properties.put("enabled", Boolean.FALSE);
        properties.put(42, "answer");
        Properties expected = (Properties) properties.clone();

        validator.validateConnectionProperties("jdbc:mysql://localhost/test", properties);

        Assertions.assertEquals(expected, properties);
        Assertions.assertEquals("inherited%password+", properties.getProperty("password"));
        Assertions.assertFalse(properties.containsKey("password"));
        Assertions.assertEquals(Map.of("password", "inherited%password+"), defaults);
    }

    @Test
    public void rejectsRestrictedInheritedNamesAndValues() {
        for (Map<String, String> input : List.of(Map.of("LOADEXTENSION", "false"), Map.of("HOST", "%72emoteScript"))) {
            Properties defaults = new Properties();
            defaults.putAll(input);
            Properties properties = new Properties(defaults);

            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test", properties));

            Assertions.assertTrue(properties.isEmpty());
            Assertions.assertEquals(input, defaults);
        }
    }

    @Test
    public void checksNonStringPropertyEntries() {
        for (Map<?, ?> input : List.of(
            Map.of("loadExtension", Boolean.FALSE),
            Map.of(42, "remoteScript")
        )) {
            Properties properties = new Properties();
            properties.putAll(input);

            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test", properties));

            Assertions.assertEquals(input, properties);
        }
    }

    @Test
    public void rejectsMissingUrlWithoutMutation() {
        Properties properties = new Properties();
        properties.setProperty("user", "test-user");

        Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(null, properties));

        Assertions.assertEquals(Map.of("user", "test-user"), properties);
    }
}
