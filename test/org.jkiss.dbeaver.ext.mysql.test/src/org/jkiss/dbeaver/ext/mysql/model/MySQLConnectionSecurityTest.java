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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class MySQLConnectionSecurityTest extends DBeaverUnitTest {
    private final MySQLConnectionSecurity validator = new MySQLConnectionSecurity();
    private static final Map<String, String> SAFE_DEFAULTS = Map.of(
        "autoDeserialize", "false", "allowLocalInfile", "false",
        "allowLoadLocalInfile", "false", "allowUrlInLocalInfile", "false"
    );

    @ParameterizedTest
    @ValueSource(strings = {
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile",
        "allowLoadLocalInfileInPath", "propertiesTransform", "useConfigs", "credentialType",
        "%61utoDeserialize", "100%&%61utoDeserialize", "%gg%61llowLocalInfile", "%2%75seConfigs"
    })
    public void rejectsAnyMentionOfProhibitedNames(@NotNull String name) {
        String text = "prefix-" + name.toUpperCase(Locale.ROOT) + "-suffix";
        String url = "jdbc:custom://user:secret@localhost/test?" + text + "=false";
        DBCException error = Assertions.assertThrows(DBCException.class,
            () -> validator.validateConnectionProperties(url, new Properties()));
        Assertions.assertFalse(error.getMessage().contains("secret"));
        Assertions.assertNull(error.getCause());
        for (Map<String, String> input : List.of(Map.of(text, "false"), Map.of("HOST", text))) {
            Properties properties = new Properties();
            properties.putAll(input);
            Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
            Assertions.assertEquals(input, properties);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "jdbc:mysql://localhost/test,true", "jdbc:mariadb://localhost/test,true",
        "jdbc:mysql:replication://localhost/test,true", "jdbc:mysql+srv:loadbalance://localhost/test,true",
        "JDBC:MYSQL://localhost/test,true", "jdbc:%6dysql+srv://localhost/test,true",
        "jdbc:trino://localhost:8080/test,false", "jdbc:custom://localhost/jdbc:mysql:,false",
        "jdbc:mysql-other://localhost/test,false"
    })
    public void defaultsAreLimitedToMysqlUrlFamilies(@NotNull String url, boolean mysql) throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("password", "example%password+");
        Properties expected = (Properties) properties.clone();
        if (mysql) {
            expected.putAll(SAFE_DEFAULTS);
        }

        validator.validateConnectionProperties(url, properties);

        Assertions.assertEquals(expected, properties);
    }

    @ParameterizedTest
    @ValueSource(strings = {"example%password", "%", "%2", "%gg", "hello%20world%", "%%41"})
    public void literalPercentValuesAreAcceptedWithoutMutation(@NotNull String value) throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("password", value);

        validator.validateConnectionProperties("jdbc:custom:test?comment=" + value, properties);

        Assertions.assertEquals(Map.of("password", value), properties);
    }

    @Test
    public void rejectsMissingUrlsAndInheritedOrNonStringProperties() {
        Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(null, new Properties()));
        Properties defaults = new Properties();
        defaults.setProperty("ALLOWLOCALINFILE", "false");
        Assertions.assertThrows(DBCException.class,
            () -> validator.validateConnectionProperties("jdbc:custom:test", new Properties(defaults)));
        Properties properties = new Properties();
        properties.put("allowLocalInfile", Boolean.FALSE);
        Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties("jdbc:custom:test", properties));
    }
}
