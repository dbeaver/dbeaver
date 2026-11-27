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

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.connection.DBPDriverConstants;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConnectionValidator;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;

class KyuubiConnectionValidatorTest {
    private static final String URL = "jdbc:hive2://localhost/default";

    private JDBCConnectionValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        var driver = DataSourceProviderRegistry.getInstance().getDataSourceProvider("generic").getDriver("kyuubi_hive");
        Object validatorId = driver.getDriverParameter(DBPDriverConstants.PARAM_CONNECTION_VALIDATOR);
        var declaration = Arrays.stream(Platform.getExtensionRegistry()
                .getConfigurationElementsFor("org.jkiss.dbeaver.connectionValidator"))
            .filter(element -> element.getAttribute("id").equals(validatorId))
            .findFirst()
            .orElseThrow();
        validator = (JDBCConnectionValidator) declaration.createExecutableExtension("class");
    }

    @Test
    void rejectsInitFileInJdbcUrl() {
        assertThrows(DBCException.class, () -> validator.validateConnectionProperties(URL + ";initFile=unused", new Properties()));
    }

    @Test
    void rejectsInitFileInJdbcProperties() {
        Properties properties = new Properties();
        properties.setProperty("initFile", "unused");

        assertThrows(DBCException.class, () -> validator.validateConnectionProperties(URL, properties));
    }

    @Test
    void allowsNormalConnection() throws DBCException {
        Properties properties = new Properties();
        properties.setProperty("user", "kyuubi");

        validator.validateConnectionProperties(URL, properties);
    }
}
