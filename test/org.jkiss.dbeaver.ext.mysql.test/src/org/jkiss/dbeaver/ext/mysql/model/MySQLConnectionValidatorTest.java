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

public class MySQLConnectionValidatorTest extends DBeaverUnitTest {
    private final MySQLConnectionValidator validator = new MySQLConnectionValidator();
    private static final List<String> BOOLEAN_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"
    );

    @ParameterizedTest
    @ValueSource(strings = {
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile",
        "allowLoadLocalInfileInPath", "propertiesTransform", "useConfigs", "credentialType"
    })
    public void rejectsRestrictedProperties(@NotNull String name) {
        Properties properties = new Properties();
        properties.setProperty(name, "true");
        Assertions.assertThrows(DBCException.class,
            () -> validator.validateConnectionProperties("jdbc:mysql://localhost/test", properties));
    }

    @Test
    public void doesNotInjectDefaults() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("user", "test-user");
        Properties expected = (Properties) properties.clone();

        validator.validateConnectionProperties("jdbc:mysql://localhost/test", properties);

        Assertions.assertEquals(expected, properties);
    }

    @Test
    public void rejectsNonStringBooleanValues() {
        Properties properties = new Properties();
        properties.put("allowLocalInfile", Boolean.FALSE);
        Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "False", "no", "NO", "No"})
    public void acceptsExplicitlyDisabledBooleanPropertiesIncludingInheritedDefaults(@NotNull String value) throws DBCException {
        Properties defaults = new Properties();
        BOOLEAN_PROPERTIES.forEach(name -> defaults.setProperty(name, value));
        Properties expectedDefaults = (Properties) defaults.clone();
        Properties properties = new Properties(defaults);
        BOOLEAN_PROPERTIES.forEach(name -> properties.setProperty(name.toUpperCase(Locale.ROOT), value));
        Properties expected = (Properties) properties.clone();

        validator.validateConnectionProperties("jdbc:custom:test", properties);

        Assertions.assertEquals(expected, properties);
        Assertions.assertEquals(expectedDefaults, defaults);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "yes", "YES", "1", "", " false", "NO ", "%66alse"})
    public void rejectsBooleanValuesOtherThanFalseOrNo(@NotNull String value) {
        for (String name : BOOLEAN_PROPERTIES) {
            Properties properties = new Properties();
            properties.setProperty(name, value);
            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "allowLoadLocalInfileInPath", "propertiesTransform", "useConfigs", "credentialType",
        "%61utoDeserialize", "prefix-allowLocalInfile"
    })
    public void disabledValuesDoNotExemptOtherRestrictedProperties(@NotNull String name) {
        for (String value : List.of("false", "NO")) {
            Properties properties = new Properties();
            properties.setProperty(name, value);
            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "NO"})
    public void rejectsBooleanNamesInUrlEvenWhenDisabled(@NotNull String value) {
        for (String name : BOOLEAN_PROPERTIES) {
            Assertions.assertThrows(DBCException.class,
                () -> validator.validateConnectionProperties("jdbc:custom:test?" + name + "=" + value, new Properties()));
        }
    }
}
