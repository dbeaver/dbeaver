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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks that an auth model may open the connection itself by returning
 * {@link JDBCAuthConnectionOpener} from {@code initAuthentication}.
 */
public class JDBCAuthConnectionOpenerTest {

    private static final String URL = "jdbc:h2:mem:test";

    @Test
    public void authOpenerReplacesDriverConnect() throws Exception {
        Fixture fixture = new Fixture();
        Connection expected = Mockito.mock(Connection.class);
        AtomicReference<String> openedUrl = new AtomicReference<>();
        AtomicReference<Properties> openedProps = new AtomicReference<>();
        AtomicReference<Driver> openedDriverInstance = new AtomicReference<>();
        AtomicReference<ClassLoader> openerClassLoader = new AtomicReference<>();

        JDBCAuthConnectionOpener opener = (driver, driverInstance, url, connectProps) -> {
            openedUrl.set(url);
            openedProps.set(connectProps);
            openedDriverInstance.set(driverInstance);
            openerClassLoader.set(Thread.currentThread().getContextClassLoader());
            return expected;
        };
        fixture.authenticateWith(opener);

        Connection actual = fixture.dataSource.openConnection(fixture.monitor, null, fixture.configuration, "test");

        Assertions.assertSame(expected, actual);
        Assertions.assertEquals(URL, openedUrl.get());
        Assertions.assertEquals("delegated", openedProps.get().getProperty("auth.marker"));
        Assertions.assertSame(fixture.jdbcDriver, openedDriverInstance.get());
        Assertions.assertSame(fixture.driverClassLoader, openerClassLoader.get(),
            "Opener must run with the driver class loader as context class loader");
        Mockito.verify(fixture.jdbcDriver, Mockito.never()).connect(Mockito.anyString(), Mockito.any());
        Mockito.verify(fixture.authModel).endAuthentication(Mockito.eq(fixture.container), Mockito.eq(fixture.configuration), Mockito.any());
    }

    @Test
    public void authOpenerFailureIsReportedAsConnectError() throws Exception {
        Fixture fixture = new Fixture();
        JDBCAuthConnectionOpener failingOpener = (driver, driverInstance, url, connectProps) -> {
            throw new SQLException("GSS handshake failed");
        };
        fixture.authenticateWith(failingOpener);

        DBCException exception = Assertions.assertThrows(DBCException.class,
            () -> fixture.dataSource.openConnection(fixture.monitor, null, fixture.configuration, "test"));

        Assertions.assertEquals("GSS handshake failed", exception.getMessage());
        Mockito.verify(fixture.jdbcDriver, Mockito.never()).connect(Mockito.anyString(), Mockito.any());
        Mockito.verify(fixture.authModel).endAuthentication(Mockito.eq(fixture.container), Mockito.eq(fixture.configuration), Mockito.any());
    }

    @Test
    public void plainAuthResultStillUsesDriverConnect() throws Exception {
        Fixture fixture = new Fixture();
        Connection expected = Mockito.mock(Connection.class);
        fixture.authenticateWith(new Object());
        Mockito.when(fixture.jdbcDriver.connect(Mockito.eq(URL), Mockito.any())).thenReturn(expected);

        Connection actual = fixture.dataSource.openConnection(fixture.monitor, null, fixture.configuration, "test");

        Assertions.assertSame(expected, actual);
        Mockito.verify(fixture.jdbcDriver).connect(Mockito.eq(URL), Mockito.any());
    }

    private static final class Fixture {
        final DBPDataSourceContainer container;
        final DBPDriver driver;
        final DBPConnectionConfiguration configuration;
        final DBAAuthModel<DBAAuthCredentials> authModel;
        final TestDataSource dataSource;
        final VoidProgressMonitor monitor;
        final Driver jdbcDriver;
        final ClassLoader driverClassLoader;

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(container.getDataSource()).thenReturn(null);
            Mockito.when(container.getDriverSubstitution()).thenReturn(null);
            Mockito.when(container.isConnectionReadOnly()).thenReturn(false);
            driver = container.getDriver();
            driverClassLoader = new ClassLoader(getClass().getClassLoader()) {
            };
            Mockito.when(driver.getDriverLoader(container).getClassLoader()).thenReturn(driverClassLoader);

            configuration = new DBPConnectionConfiguration();
            configuration.setUrl(URL);

            DBPAuthModelDescriptor authDescriptor = Mockito.mock(DBPAuthModelDescriptor.class);
            Mockito.when(driver.getDataSourceProvider().detectConnectionAuthModel(driver, configuration)).thenReturn(authDescriptor);
            Mockito.when(authDescriptor.isApplicableTo(driver)).thenReturn(true);
            authModel = Mockito.mock(DBAAuthModel.class);
            Mockito.when(authDescriptor.getInstance()).thenReturn(authModel);

            dataSource = Mockito.mock(TestDataSource.class, Mockito.withSettings()
                .useConstructor(container, Mockito.mock(SQLDialect.class)).defaultAnswer(Mockito.CALLS_REAL_METHODS));
            monitor = new VoidProgressMonitor();
            jdbcDriver = Mockito.mock(Driver.class);
            Mockito.when(jdbcDriver.acceptsURL(URL)).thenReturn(true);
            Mockito.doReturn(jdbcDriver).when(dataSource).createDriverInstance(monitor, driver);
        }

        void authenticateWith(Object authResult) throws Exception {
            Mockito.doAnswer(invocation -> {
                Properties properties = invocation.getArgument(4);
                properties.setProperty("auth.marker", "delegated");
                return authResult;
            }).when(authModel).initAuthentication(Mockito.eq(monitor), Mockito.eq(dataSource), Mockito.any(),
                Mockito.eq(configuration), Mockito.any());
        }
    }

    public abstract static class TestDataSource extends JDBCDataSource {
        public TestDataSource(@NotNull DBPDataSourceContainer container, @NotNull SQLDialect dialect) {
            super(container, dialect);
        }

        @Nullable
        @Override
        public Driver createDriverInstance(@NotNull DBRProgressMonitor monitor, @NotNull DBPDriver driver) throws DBCException {
            return super.createDriverInstance(monitor, driver);
        }

        @NotNull
        @Override
        public Connection openConnection(
            @NotNull DBRProgressMonitor monitor,
            @Nullable JDBCExecutionContext context,
            @NotNull DBPConnectionConfiguration connectionInfo,
            @NotNull String purpose
        ) throws DBCException {
            return super.openConnection(monitor, context, connectionInfo, purpose);
        }
    }
}
