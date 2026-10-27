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

import java.util.List;
import java.util.Properties;

public class MySQLConnectionSecurityTest extends DBeaverUnitTest {
    private static final String URL = "jdbc:mysql://localhost/test";

    @Test
    public void rejectsUnsafeQueryOptions() {
        for (String option : List.of(
            "autoDeserialize=true", "autoDeserialize=yes", "allowLocalInfile=true", "ALLOWLOCALINFILE=true",
            "allowLocalInfile=1", "allowLocalInfile", "allowLocalInfile=", "allowLoadLocalInfile=true",
            "allowUrlInLocalInfile=true", "allowLoadLocalInfileInPath", "allowLoadLocalInfileInPath=",
            "allowLoadLocalInfileInPath=%2Ftest-path", "%61llowLoadLocalInfile=%74rue",
            "allowLocalInfile=false&allowLocalInfile=true", "allowLocalInfile=true&allowLocalInfile=false",
            "allowLocalInfile=false&ALLOWLOCALINFILE=true", "allowLocalInfile=true%ZZ",
            "propertiesTransform=example.Transformer", "useConfigs=maxPerformance"
        )) {
            assertRejected(URL + "?" + option);
            assertRejected("jdbc:mariadb://localhost/test?" + option);
        }
    }

    @Test
    public void rejectsUnsafeHostOptions() {
        for (String host : List.of(
            "address=(host=localhost)(autoDeserialize=true)",
            "ADDRESS=(host=localhost)(allowLoadLocalInfile=yes)",
            "address=(host=localhost)(%61llowLoadLocalInfile=%74rue)",
            "(host=localhost,allowLoadLocalInfile=true)",
            "address=(host=localhost)(allowLoadLocalInfileInPath=/test-path)",
            "localhost,address=(host=other)(autoDeserialize=true)",
            "user:password@[(host=localhost,allowLoadLocalInfile=true),other]",
            "[(host=localhost),(host=other,propertiesTransform=example.Transformer)]",
            "address=(host=localhost)(useConfigs=maxPerformance)"
        )) {
            assertRejected("jdbc:mysql://" + host + "/test");
            assertRejected("jdbc:mysql:loadbalance://" + host + "/test");
            assertRejected("jdbc:mysql:replication://" + host + "/test");
        }
    }

    @Test
    public void rejectsAmbiguousLegacyHostSyntax() {
        for (String host : List.of(
            "address=(host=localhost)(autoDeserialize=\"true\")",
            "address=(host=localhost)autoDeserialize=true)",
            "address=XautoDeserialize=true)",
            "address= autoDeserialize=true)",
            "address=(host=localhost)(autoDeserialize=true",
            "address=(host=localhost)(autoDeserialize=true)garbage",
            "address=", "(host=localhost", "[localhost", "localhost]", "localhost)"
        )) {
            assertRejected("jdbc:mysql://" + host + "/test");
        }
    }

    @Test
    public void doesNotDiscardOptionsAfterHash() {
        assertRejected("jdbc:mariadb://localhost/test?x=y#fragment&allowLocalInfile=true");
        assertRejected("jdbc:mariadb://localhost/test#fragment?allowLocalInfile=true");
    }

    @Test
    public void rejectsDifferentialLegacyAuthorityParsing() {
        for (String host : List.of(
            "address=XautoDeserialize=true=foo@address=(host=localhost)(host=localhost)",
            "address=(host=localhost)(x=foo,address=XautoDeserialize=true)(host=localhost)",
            "address=(host=localhost)/*comment*/,address=(host=localhost)(autoDeserialize=true)",
            "address=(host=localhost)*/ignored,address=(host=localhost)(autoDeserialize=true)",
            "u'@localhost/hidden',address=(host=localhost)(autoDeserialize=true)",
            "u\"@localhost/hidden\",address=(host=localhost)(autoDeserialize=true)",
            "localhost\\/hidden,address=(host=localhost)(autoDeserialize=true)",
            "localhost-- /hidden\n,address=(host=localhost)(autoDeserialize=true)",
            "localhost--\u2003/hidden\n,address=(host=localhost)(autoDeserialize=true)",
            "localhost#/hidden\n,address=(host=localhost)(autoDeserialize=true)"
        )) {
            assertRejected("jdbc:mysql:loadbalance://" + host + "/test");
        }
    }

    @Test
    public void acceptsSafeConnectionSyntax() {
        for (String url : List.of(
            URL, "jdbc:mysql:///test", "jdbc:mysql://localhost:3306/test", "jdbc:mysql://xn--example/test",
            "jdbc:mysql://localhost/database'with\"quotes",
            "jdbc:mysql://[::1]:3306/test", "jdbc:mysql://[::1]:3306,other:3307/test",
            "jdbc:mysql:replication://localhost,other/test", "jdbc:mysql+srv://localhost/test",
            "jdbc:mysql://user:password@[localhost,other]/test",
            "jdbc:mysql://address=(host=localhost)(port=3306)/test",
            "jdbc:mysql://(host=localhost,port=3306)/test",
            "jdbc:mysql://[(host=localhost),(host=other)]/test",
            "jdbc:mysql://address=(host=localhost)(autoDeserialize=false)/test",
            "jdbc:mysql://(host=localhost,allowLoadLocalInfile=false)/test",
            "jdbc:mariadb://localhost/test?allowLocalInfile=false",
            URL + "?%61llowLoadLocalInfile=%66alse", URL + "?autoDeserialize=FALSE",
            URL + "?allowLocalInfile=false&allowLocalInfile=false",
            URL + "?notallowLocalInfile=true", URL + "?comment=allowLocalInfile=true",
            URL + "?comment=v%26allowLocalInfile%3Dtrue", URL + "?%2561llowLoadLocalInfile=true",
            URL + "?comment=literal%", "jdbc:mysql://localhost/allowLocalInfile"
        )) {
            Assertions.assertDoesNotThrow(() -> MySQLConnectionSecurity.validate(url, new Properties()), url);
        }
    }

    @Test
    public void removesCaseVariantsBeforeInsertingCanonicalSafeValues() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("allowLocalInfile", "false");
        properties.setProperty("ALLOWLOCALINFILE", "true");
        properties.setProperty("AllowLocalInfile", "true");
        properties.setProperty("ALLOWLOADLOCALINFILEINPATH", "/test-path");
        properties.setProperty("connectTimeout", "1000");

        MySQLConnectionSecurity.sanitizeProperties(properties);
        MySQLConnectionSecurity.validate(URL, properties);

        Assertions.assertEquals("false", properties.getProperty("allowLocalInfile"));
        Assertions.assertFalse(properties.containsKey("ALLOWLOCALINFILE"));
        Assertions.assertFalse(properties.containsKey("AllowLocalInfile"));
        Assertions.assertFalse(properties.containsKey("ALLOWLOADLOCALINFILEINPATH"));
        Assertions.assertEquals("1000", properties.getProperty("connectTimeout"));
    }

    @Test
    public void validatesHostSpecificationsInPropertiesAndQuery() {
        for (String key : List.of("HOST", "HOST.1", "HOST.2")) {
            Properties properties = new Properties();
            properties.setProperty(key, "address=(host=localhost)(autoDeserialize=true)");
            MySQLConnectionSecurity.sanitizeProperties(properties);
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(URL, properties));
            assertRejected(URL + "?" + key + "=address%3D(host%3Dlocalhost)(autoDeserialize%3Dtrue)");
            properties.setProperty(key, "address=XautoDeserialize=true=foo@address=(host=localhost)(host=localhost)");
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(URL, properties));
        }
    }

    @Test
    public void rejectsIndirectConfigurationProperties() {
        for (String name : List.of("propertiesTransform", "useConfigs")) {
            Properties properties = new Properties();
            properties.setProperty(name, "test");
            MySQLConnectionSecurity.sanitizeProperties(properties);
            Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(URL, properties));
        }
    }

    @Test
    public void doesNotIgnoreUnsafeInheritedProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("ALLOWLOCALINFILE", "true");
        Properties properties = new Properties(defaults);
        MySQLConnectionSecurity.sanitizeProperties(properties);
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(URL, properties));
    }

    @Test
    public void doesNotDiscloseConnectionSecretsInErrors() {
        DBCException exception = Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(
            "jdbc:mysql://user:secret-password@localhost/test?allowLoadLocalInfileInPath=/private-path", new Properties()));
        Assertions.assertTrue(exception.getMessage().contains("allowLoadLocalInfileInPath"));
        Assertions.assertFalse(exception.getMessage().contains("secret-password"));
        Assertions.assertFalse(exception.getMessage().contains("private-path"));
        Assertions.assertFalse(exception.getMessage().contains("jdbc:mysql"));
    }

    @Test
    public void rejectsExcessiveHostNestingAndMissingUrls() {
        assertRejected("jdbc:mysql://" + "[".repeat(20) + "localhost" + "]".repeat(20) + "/test");
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(null, new Properties()));
    }

    @Test
    public void doesNotImposeMysqlUrlSyntaxOnDsnDriverSubstitutions() {
        Assertions.assertDoesNotThrow(() -> MySQLConnectionSecurity.validate("jdbc:odbc:test", new Properties()));
    }

    private void assertRejected(@NotNull String url) {
        Assertions.assertThrows(DBCException.class, () -> MySQLConnectionSecurity.validate(url, new Properties()), url);
    }
}
