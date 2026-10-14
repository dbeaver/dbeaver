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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.auth.OracleAuthModelDatabaseNative;
import org.jkiss.dbeaver.ext.oracle.model.auth.OracleAuthOS;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBChangeUserPasswordManager;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSQLDialect;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

public class YashanDBChangeUserPasswordManagerTest extends DBeaverUnitTest {

    private PasswordValidationDataSource dataSource;
    private DBPConnectionConfiguration configuration;
    private Connection validationConnection;
    private JDBCSession session;
    private JDBCPreparedStatement statement;
    private YashanDBChangeUserPasswordManager passwordManager;

    @BeforeEach
    public void setUp() throws Exception {
        configuration = new DBPConnectionConfiguration();
        configuration.setAuthModelId(OracleAuthModelDatabaseNative.ID);
        configuration.setUserName("sales");
        configuration.setUserPassword("Old%Password1");
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(container.getDataSource()).thenReturn(null);
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(configuration);
        validationConnection = Mockito.mock(Connection.class);
        dataSource = Mockito.spy(new PasswordValidationDataSource(container, validationConnection));
        monitor = Mockito.mock(DBRProgressMonitor.class);
        session = Mockito.mock(JDBCSession.class);
        statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.doReturn(new YashanDBSQLDialect()).when(dataSource).getSQLDialect();
        JDBCRemoteInstance instance = Mockito.mock(JDBCRemoteInstance.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn(instance).when(dataSource).getDefaultInstance();
        Mockito.when(instance.getDefaultContext(monitor, true)
            .openSession(monitor, DBCExecutionPurpose.META, "Change user password")).thenReturn(session);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
        passwordManager = new YashanDBChangeUserPasswordManager(dataSource);
    }

    @Test
    public void changesPasswordWithoutReplaceClause() throws Exception {
        passwordManager.changeUserPassword(monitor, "sales", "23%ad1", "Old%Password1");

        InOrder order = Mockito.inOrder(dataSource, validationConnection, session, statement);
        order.verify(dataSource).validateUserPassword(monitor, "sales", "Old%Password1");
        order.verify(validationConnection).close();
        order.verify(session).enableLogging(false);
        order.verify(session).prepareStatement("ALTER USER SALES IDENTIFIED BY \"23%ad1\"");
        order.verify(statement).execute();
        order.verify(statement).close();
        order.verify(session).close();

        Assertions.assertNotSame(configuration, dataSource.validationConfiguration);
        Assertions.assertEquals("sales", dataSource.validationConfiguration.getUserName());
        Assertions.assertEquals("Old%Password1", dataSource.validationConfiguration.getUserPassword());
        Assertions.assertEquals(OracleAuthModelDatabaseNative.ID, configuration.getAuthModelId());
        Assertions.assertEquals("Old%Password1", configuration.getUserPassword());
    }

    @Test
    public void preservesQuotedUserNameAndPasswordCase() throws Exception {
        passwordManager.changeUserPassword(monitor, "\"Sales User\"", "MixedCase%Password1", "Old%Password1");

        Mockito.verify(session).prepareStatement("ALTER USER \"Sales User\" IDENTIFIED BY \"MixedCase%Password1\"");
    }

    @Test
    public void preservesAllowedSpecialCharactersInPassword() throws Exception {
        passwordManager.changeUserPassword(monitor, "sales", "New'Pass/1@", "Old%Password1");

        Mockito.verify(session).prepareStatement("ALTER USER SALES IDENTIFIED BY \"New'Pass/1@\"");
    }

    @Test
    public void rejectsDoubleQuotesBeforeOpeningConnections() throws Exception {
        for (String newPassword : new String[] {"\"New%Password1\"", "New\"Password1", "\"New%Password1", "New%Password1\""}) {
            DBCException error = Assertions.assertThrows(DBCException.class,
                () -> passwordManager.changeUserPassword(monitor, "sales", newPassword, "Old%Password1"));

            Assertions.assertFalse(error.getMessage().contains(newPassword));
        }

        Assertions.assertNull(dataSource.validationConfiguration);
        Mockito.verify(dataSource, Mockito.never()).validateUserPassword(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verifyNoInteractions(validationConnection, session, statement);
    }

    @Test
    public void rejectsInvalidCurrentPasswordBeforeChangingPassword() throws Exception {
        DBCException failure = new DBCException("Invalid current password");
        dataSource.validationFailure = failure;

        Assertions.assertSame(failure, Assertions.assertThrows(DBCException.class,
            () -> passwordManager.changeUserPassword(monitor, "sales", "23%ad1", "Wrong%Password1")));
        Assertions.assertEquals("Wrong%Password1", dataSource.validationConfiguration.getUserPassword());
        Mockito.verifyNoInteractions(session, statement);
    }

    @Test
    public void acceptsNativeAndDefaultAuthentication() throws Exception {
        for (String authModelId : new String[] {AuthModelDatabaseNative.ID, ""}) {
            configuration.setAuthModelId(authModelId);
            dataSource.validateUserPassword(monitor, "sales", "Old%Password1");
        }

        Mockito.verify(validationConnection, Mockito.times(2)).close();
    }

    @Test
    public void rejectsOsAuthenticationBeforeOpeningConnections() {
        configuration.setAuthModelId(OracleAuthOS.ID);

        Assertions.assertThrows(DBCException.class,
            () -> passwordManager.changeUserPassword(monitor, "sales", "23%ad1", "Old%Password1"));
        Assertions.assertNull(dataSource.validationConfiguration);
        Mockito.verifyNoInteractions(validationConnection, session, statement);
    }

    @Test
    public void wrapsSqlErrorAndClosesSession() throws Exception {
        SQLException failure = new SQLException("Password policy violation");
        Mockito.when(statement.execute()).thenThrow(failure);

        DBCException error = Assertions.assertThrows(DBCException.class,
            () -> passwordManager.changeUserPassword(monitor, "sales", "23%ad1", "Old%Password1"));

        Assertions.assertSame(failure, error.getCause());
        Mockito.verify(statement).close();
        Mockito.verify(session).close();
    }

    @Test
    public void usesYashanDBPasswordManager() {
        Assertions.assertInstanceOf(YashanDBChangeUserPasswordManager.class, dataSource.getAdapter(DBAUserPasswordManager.class));
    }

    public static class PasswordValidationDataSource extends YashanDBDataSource {

        private final Connection validationConnection;
        private DBPConnectionConfiguration validationConfiguration;
        private DBCException validationFailure;

        public PasswordValidationDataSource(@NotNull DBPDataSourceContainer container, @NotNull Connection validationConnection) {
            super(container);
            this.validationConnection = validationConnection;
        }

        @NotNull
        @Override
        protected Connection openConnectionForPasswordValidation(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPConnectionConfiguration connectionInfo
        ) throws DBCException {
            validationConfiguration = connectionInfo;
            if (validationFailure != null) {
                throw validationFailure;
            }
            return validationConnection;
        }
    }
}
