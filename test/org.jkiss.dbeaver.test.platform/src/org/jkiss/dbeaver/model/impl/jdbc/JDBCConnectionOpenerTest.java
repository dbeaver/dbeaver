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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLoader;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class JDBCConnectionOpenerTest {
    @Test
    public void invokesDataSourceWithDriverClassLoaderAndAuthResult() throws Exception {
        TestDataSource dataSource = Mockito.mock(TestDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        DBPDriverLoader loader = Mockito.mock(DBPDriverLoader.class);
        Driver driverInstance = Mockito.mock(Driver.class);
        Connection connection = Mockito.mock(Connection.class);
        Properties properties = new Properties();
        Object authResult = new Object();
        final ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader driverLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(driver.getDriverLoader(container)).thenReturn(loader);
        Mockito.when(loader.getClassLoader()).thenReturn(driverLoader);
        Mockito.when(dataSource.openJdbcConnection(driverInstance, "jdbc:test", properties, authResult)).thenAnswer(invocation -> {
            Assertions.assertSame(driverLoader, Thread.currentThread().getContextClassLoader());
            return connection;
        });

        Class<?> openerClass = JDBCDataSource.class.getClassLoader().loadClass(
            "org.jkiss.dbeaver.model.impl.jdbc.JDBCConnectionOpener");
        var constructor = openerClass.getDeclaredConstructor(
            JDBCDataSource.class, DBPDriver.class, Driver.class, String.class, Properties.class, Object.class);
        constructor.setAccessible(true);
        var opener = (PrivilegedExceptionAction<?>) constructor.newInstance(
            dataSource, driver, driverInstance, "jdbc:test", properties, authResult);
        Assertions.assertSame(connection, opener.run());
        Assertions.assertSame(originalLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void standardConnectionsUseDriver() throws Exception {
        TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.CALLS_REAL_METHODS);
        Driver driver = Mockito.mock(Driver.class);
        Connection connection = Mockito.mock(Connection.class);
        Properties properties = new Properties();
        Mockito.when(driver.connect("jdbc:test", properties)).thenReturn(connection);

        Assertions.assertSame(connection, dataSource.openJdbcConnection(driver, "jdbc:test", properties, null));
    }

    @Test
    public void connectionsWithoutDriverInstanceReportMissingDriver() {
        TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.CALLS_REAL_METHODS);
        SQLException failure = Assertions.assertThrows(SQLException.class,
            () -> dataSource.openJdbcConnection(null, "jdbc:dbeaver-test-missing-driver", new Properties(), null));
        Assertions.assertEquals("08001", failure.getSQLState());
    }

    public abstract static class TestDataSource extends JDBCDataSource {
        protected TestDataSource(@NotNull DBPDataSourceContainer container, @NotNull SQLDialect dialect) {
            super(container, dialect);
        }

        @Nullable
        @Override
        public Connection openJdbcConnection(
            @Nullable Driver driver,
            @NotNull String url,
            @NotNull Properties properties,
            @Nullable Object authResult
        ) throws SQLException, DBException {
            return super.openJdbcConnection(driver, url, properties, authResult);
        }
    }
}
