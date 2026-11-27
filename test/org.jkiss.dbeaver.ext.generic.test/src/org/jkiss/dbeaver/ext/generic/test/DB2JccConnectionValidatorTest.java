/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2026 DBeaver Corp and others
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

import org.jkiss.dbeaver.ext.generic.model.DB2JccConnectionValidator;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

class DB2JccConnectionValidatorTest {
    private static final String URL = "jdbc:db2://localhost/SAMPLE";
    private final DB2JccConnectionValidator validator = new DB2JccConnectionValidator();

    @Test
    void rejectsTraceOptionsInJdbcUrlAndProperties() {
        for (String name : List.of("traceDirectory", "traceFile", "traceFileAppend", "traceLevel")) {
            Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(URL + ":" + name + "=unused;",
                new Properties()), name);

            Properties properties = new Properties();
            properties.setProperty(name, "unused");
            Assertions.assertThrows(DBCException.class, () -> validator.validateConnectionProperties(URL, properties), name);
        }
    }

    @Test
    void acceptsStandardConnectionOptions() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("loginTimeout", "20");

        validator.validateConnectionProperties(URL, properties);
    }
}
