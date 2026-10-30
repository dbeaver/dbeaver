/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthSubjectCredentials;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLoader;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.security.auth.Subject;

public class JDBCConnectionOpenerTest {
    private static final String URL = "jdbc:connection-hook-test:test";

    @Test
    public void callsTheHookInsideTheDriverLoaderAndAuthenticationSubject() throws Exception {
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        DBPDriverLoader loader = Mockito.mock(DBPDriverLoader.class);
        final ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader driverLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(driver.getDriverLoader(container)).thenReturn(loader);
        Mockito.when(loader.getClassLoader()).thenReturn(driverLoader);
        Properties properties = new Properties();
        Subject subject = new Subject();
        DBAAuthSubjectCredentials credentials = Mockito.mock(DBAAuthSubjectCredentials.class);
        Mockito.when(credentials.getAuthSubject()).thenReturn(subject);
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(dataSource.openJdbcConnection(null, URL, properties)).thenAnswer(invocation -> {
            Assertions.assertSame(driverLoader, Thread.currentThread().getContextClassLoader());
            Assertions.assertSame(subject, Subject.current());
            return connection;
        });
        JDBCConnectionOpener opener = new JDBCConnectionOpener(dataSource, driver, null, URL, properties, credentials);

        opener.run(new VoidProgressMonitor());

        Assertions.assertNull(opener.getError());
        Assertions.assertSame(connection, opener.getConnection());
        Assertions.assertSame(originalLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void defaultHookCallsTheProvidedDriver() throws Exception {
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.CALLS_REAL_METHODS);
        Driver driver = Mockito.mock(Driver.class);
        Properties properties = new Properties();
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(driver.connect(URL, properties)).thenReturn(connection);

        Assertions.assertSame(connection, dataSource.openJdbcConnection(driver, URL, properties));
    }

    @Test
    public void defaultHookFallsBackToDriverManager() throws Exception {
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.CALLS_REAL_METHODS);
        Driver driver = Mockito.mock(Driver.class);
        Properties properties = new Properties();
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(driver.connect(URL, properties)).thenReturn(connection);
        DriverManager.registerDriver(driver);
        try {
            Assertions.assertSame(connection, dataSource.openJdbcConnection(null, URL, properties));
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    @Test
    public void restoresDriverLoaderAfterConnectionFailure() throws Exception {
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        DBPDriverLoader loader = Mockito.mock(DBPDriverLoader.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(driver.getDriverLoader(container)).thenReturn(loader);
        Mockito.when(loader.getClassLoader()).thenReturn(Mockito.mock(ClassLoader.class));
        Properties properties = new Properties();
        SQLException failure = new SQLException("Connection failed", "28000");
        Mockito.when(dataSource.openJdbcConnection(null, URL, properties)).thenThrow(failure);
        final ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        JDBCConnectionOpener opener = new JDBCConnectionOpener(dataSource, driver, null, URL, properties, null);

        opener.run(new VoidProgressMonitor());

        Assertions.assertSame(failure, opener.getError());
        Assertions.assertSame(originalLoader, Thread.currentThread().getContextClassLoader());
    }
}
