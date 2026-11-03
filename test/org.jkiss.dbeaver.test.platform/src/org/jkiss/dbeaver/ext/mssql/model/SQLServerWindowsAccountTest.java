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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;

public class SQLServerWindowsAccountTest extends DBeaverUnitTest {
    private final DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
    private final SQLServerDataSource dataSource = Mockito.mock(SQLServerDataSource.class);
    private final JDBCSession session = Mockito.mock(JDBCSession.class);
    private final JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
    private final JDBCResultSet result = Mockito.mock(JDBCResultSet.class);

    @BeforeEach
    public void setUpSession() {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        JDBCRemoteInstance instance = Mockito.mock(JDBCRemoteInstance.class);
        JDBCExecutionContext context = Mockito.mock(JDBCExecutionContext.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(dataSource.getDefaultInstance()).thenReturn(instance);
        Mockito.when(instance.getDataSource()).thenReturn(dataSource);
        Mockito.when(instance.getDefaultContext(Mockito.any(), Mockito.anyBoolean())).thenReturn(context);
        Mockito.when(context.isConnected()).thenReturn(true);
        Mockito.when(context.openSession(Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(session);
    }

    @Test
    public void readsDomainSuggestionsFromVisibleLogins() throws Exception {
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(result);
        Mockito.when(result.next()).thenReturn(true, true, false);
        Mockito.when(result.getString(1)).thenReturn("DOMAIN", "COMPUTER");
        Assertions.assertEquals(List.of("DOMAIN", "COMPUTER"), SQLServerWindowsAccount.readKnownDomains(monitor, dataSource));
    }

    @Test
    public void detectsUnknownAccount() throws Exception {
        checkAccount(null, false, SQLServerWindowsAccount.CheckResult.NOT_FOUND);
    }

    @Test
    public void acceptsResolvedAccountWithoutLogin() throws Exception {
        checkAccount(new byte[] {1, 2, 3}, false, SQLServerWindowsAccount.CheckResult.AVAILABLE);
    }

    @Test
    public void detectsExistingLoginBySid() throws Exception {
        checkAccount(new byte[] {1, 2, 3}, true, SQLServerWindowsAccount.CheckResult.LOGIN_EXISTS);
    }

    @Test
    public void preservesLookupFailure() throws Exception {
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenThrow(new SQLException("Domain unavailable"));
        DBException error = Assertions.assertThrows(DBException.class,
            () -> SQLServerWindowsAccount.checkName(monitor, dataSource, "DOMAIN\\account"));
        Assertions.assertEquals("Domain unavailable", error.getCause().getMessage());
    }

    private void checkAccount(
        @Nullable byte[] sid,
        boolean exists,
        @NotNull SQLServerWindowsAccount.CheckResult expected
    ) throws Exception {
        Mockito.when(session.prepareStatement("SELECT SUSER_SID(?)")).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(result);
        Mockito.when(result.next()).thenReturn(true);
        Mockito.when(result.getBytes(1)).thenReturn(sid);
        JDBCPreparedStatement existing = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet existingResult = Mockito.mock(JDBCResultSet.class);
        Mockito.when(session.prepareStatement("SELECT name FROM sys.server_principals WHERE sid = ?")).thenReturn(existing);
        Mockito.when(existing.executeQuery()).thenReturn(existingResult);
        Mockito.when(existingResult.next()).thenReturn(exists);
        Assertions.assertEquals(expected, SQLServerWindowsAccount.checkName(monitor, dataSource, "DOMAIN\\account"));
        Mockito.verify(statement).setString(1, "DOMAIN\\account");
        if (sid != null) {
            Mockito.verify(existing).setBytes(1, sid);
        }
    }
}
