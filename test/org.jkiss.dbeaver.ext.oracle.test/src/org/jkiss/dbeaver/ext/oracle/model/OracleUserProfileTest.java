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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class OracleUserProfileTest extends DBeaverUnitTest {

    private OracleDataSource dataSource;
    private VoidProgressMonitor monitor;
    private JDBCSession session;
    private JDBCPreparedStatement statement;
    private JDBCResultSet resultSet;

    @BeforeEach
    public void setUp() throws SQLException {
        dataSource = Mockito.mock(OracleDataSource.class);
        monitor = new VoidProgressMonitor();
        session = Mockito.mock(JDBCSession.class);
        statement = Mockito.mock(JDBCPreparedStatement.class);
        resultSet = Mockito.mock(JDBCResultSet.class);
        JDBCRemoteInstance instance = Mockito.mock(JDBCRemoteInstance.class);
        Mockito.when(dataSource.getDefaultInstance()).thenReturn(instance);
        JDBCExecutionContext context = Mockito.mock(JDBCExecutionContext.class);
        Mockito.when(instance.getDefaultContext(monitor, true)).thenReturn(context);
        Mockito.when(context.openSession(monitor, DBCExecutionPurpose.META, "Load profile users")).thenReturn(session);
        Mockito.when(session.prepareStatement("SELECT * FROM DBA_USERS WHERE PROFILE=? ORDER BY USERNAME")).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    public void loadsAssignmentsWithoutDependingOnDefaultUserView() throws Exception {
        OracleUser cached = Mockito.mock(OracleUser.class);
        Mockito.when(cached.getName()).thenReturn("ALICE");
        Mockito.when(dataSource.getUsers(monitor)).thenReturn(List.of(cached));
        Mockito.when(resultSet.next()).thenReturn(true, true, false);
        Mockito.when(resultSet.getString("USERNAME")).thenReturn("ALICE", "BOB");
        Mockito.when(resultSet.getString("PROFILE")).thenReturn("APP_PROFILE");

        Collection<OracleUser> users = profile("APP_PROFILE").getUsers(monitor);

        Assertions.assertEquals(List.of("ALICE", "BOB"), users.stream().map(OracleUser::getName).toList());
        Assertions.assertTrue(users.stream().allMatch(user -> "APP_PROFILE".equals(user.getLazyReference("profile"))));
        Mockito.verify(statement).setString(1, "APP_PROFILE");
        Mockito.verify(dataSource, Mockito.never()).getUsers(Mockito.any());
        Mockito.verify(resultSet).close();
        Mockito.verify(statement).close();
        Mockito.verify(session).close();
    }

    @Test
    public void bindsQuotedProfileNameExactly() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.getString("USERNAME")).thenReturn("ALICE");
        Mockito.when(resultSet.getString("PROFILE")).thenReturn("App 'Profile");

        Assertions.assertEquals(List.of("ALICE"),
            profile("App 'Profile").getUsers(monitor).stream().map(OracleUser::getName).toList());
        Mockito.verify(statement).setString(1, "App 'Profile");
    }

    @Test
    public void listsUsersOfDefaultProfile() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.getString("USERNAME")).thenReturn("ALICE");
        Mockito.when(resultSet.getString("PROFILE")).thenReturn(OracleUserProfile.DEFAULT_PROFILE_NAME);

        Assertions.assertEquals(List.of("ALICE"),
            profile(OracleUserProfile.DEFAULT_PROFILE_NAME).getUsers(monitor).stream().map(OracleUser::getName).toList());
        Mockito.verify(statement).setString(1, OracleUserProfile.DEFAULT_PROFILE_NAME);
    }

    @Test
    public void profileWithoutUsersReturnsEmptyCollection() throws Exception {
        Assertions.assertTrue(profile("APP_PROFILE").getUsers(monitor).isEmpty());
        Mockito.verify(statement).setString(1, "APP_PROFILE");
    }

    @Test
    public void newProfileDoesNotLoadUsers() throws DBException {
        OracleUserProfile profile = new OracleUserProfile(dataSource, "APP_PROFILE");

        Assertions.assertTrue(profile.getUsers(monitor).isEmpty());
        Mockito.verify(dataSource, Mockito.never()).getDefaultInstance();
        Mockito.verifyNoInteractions(session);
    }

    @Test
    public void userLoadingFailureIsPropagated() throws Exception {
        SQLException failure = new SQLException("Cannot read users");
        Mockito.when(statement.executeQuery()).thenThrow(failure);
        OracleUserProfile profile = profile("APP_PROFILE");

        Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> profile.getUsers(monitor)).getCause());
        Mockito.verify(statement).close();
        Mockito.verify(session).close();
    }

    @Test
    public void reloadsAssignmentsAfterTheyChange() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false, false);
        Mockito.when(resultSet.getString("USERNAME")).thenReturn("ALICE");
        Mockito.when(resultSet.getString("PROFILE")).thenReturn("APP_PROFILE");
        OracleUserProfile profile = profile("APP_PROFILE");

        Assertions.assertEquals(1, profile.getUsers(monitor).size());
        Assertions.assertTrue(profile.getUsers(monitor).isEmpty());
        Mockito.verify(statement, Mockito.times(2)).executeQuery();
    }

    @NotNull
    private OracleUserProfile profile(@NotNull String name) throws SQLException {
        ResultSet profileResult = Mockito.mock(ResultSet.class);
        Mockito.when(profileResult.getString("PROFILE")).thenReturn(name);
        return new OracleUserProfile(dataSource, profileResult);
    }
}
