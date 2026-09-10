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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;

public class CDataAuthModelTest extends DBeaverUnitTest {
    @Test
    public void userCredentialsAreApplicable() {
        CDataAuthModel authModel = new CDataAuthModel();

        Assertions.assertTrue(authModel.isUserNameApplicable());
        Assertions.assertTrue(authModel.isUserPasswordApplicable());
    }

    @Test
    public void storedUserCredentialsRemainCompatible() {
        CDataAuthModel authModel = new CDataAuthModel();
        var credentials = authModel.createCredentials();
        credentials.setUserName("test-user");
        credentials.setUserPassword("test-password");
        Properties properties = new Properties();

        authModel.collectConnectionProperties(
            Mockito.mock(DBPDataSourceContainer.class),
            credentials,
            new DBPConnectionConfiguration(),
            properties,
            true
        );

        Assertions.assertEquals("test-user", properties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_USER));
        Assertions.assertEquals("test-password", properties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD));
    }

    @Test
    public void storeCredentialsSeparatelyFromUrl() {
        CDataAuthModel authModel = new CDataAuthModel();
        var credentials = authModel.createCredentials();
        credentials.setUserName("test-user");
        credentials.setUserPassword("test-password");
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        String url = "jdbc:postgresql:Server=localhost;Database=test;";
        configuration.setUrl(url);

        authModel.saveCredentials(Mockito.mock(DBPDataSourceContainer.class), configuration, credentials);

        Assertions.assertEquals("test-user", configuration.getUserName());
        Assertions.assertEquals("test-password", configuration.getUserPassword());
        Assertions.assertEquals(url, configuration.getUrl());
    }

    @Test
    public void excludePasswordFromUnsecuredProperties() {
        CDataAuthModel authModel = new CDataAuthModel();
        var credentials = authModel.createCredentials();
        credentials.setUserName("test-user");
        credentials.setUserPassword("test-password");
        Properties properties = new Properties();

        authModel.collectConnectionProperties(
            Mockito.mock(DBPDataSourceContainer.class), credentials, new DBPConnectionConfiguration(), properties, false);

        Assertions.assertEquals("test-user", properties.getProperty(DBConstants.DATA_SOURCE_PROPERTY_USER));
        Assertions.assertFalse(properties.containsKey(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD));
    }
}
