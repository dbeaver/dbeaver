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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OracleUserProfileTest extends DBeaverUnitTest {

    private OracleDataSource dataSource;
    private VoidProgressMonitor monitor;

    @BeforeEach
    public void setUp() {
        dataSource = Mockito.mock(OracleDataSource.class);
        monitor = new VoidProgressMonitor();
    }

    @Test
    public void listsOnlyUsersAssignedToProfile() throws Exception {
        OracleUser first = user("ALICE", "APP_PROFILE");
        OracleUser second = user("BOB", "APP_PROFILE");
        List<OracleUser> users = List.of(first, user("OTHER", "OTHER_PROFILE"), second, user("UNKNOWN", null));
        Mockito.when(dataSource.getUsers(monitor)).thenReturn(users);

        Assertions.assertEquals(List.of(first, second), profile("APP_PROFILE").getUsers(monitor));
    }

    @Test
    public void matchesQuotedProfileNamesExactly() throws Exception {
        OracleUser assigned = user("ALICE", "App Profile");
        List<OracleUser> users = List.of(assigned, user("BOB", "APP PROFILE"));
        Mockito.when(dataSource.getUsers(monitor)).thenReturn(users);

        Assertions.assertEquals(List.of(assigned), profile("App Profile").getUsers(monitor));
    }

    @Test
    public void listsUsersOfDefaultProfile() throws Exception {
        OracleUser assigned = user("ALICE", OracleUserProfile.DEFAULT_PROFILE_NAME);
        List<OracleUser> users = List.of(assigned, user("BOB", "APP_PROFILE"));
        Mockito.when(dataSource.getUsers(monitor)).thenReturn(users);

        Assertions.assertEquals(List.of(assigned), profile(OracleUserProfile.DEFAULT_PROFILE_NAME).getUsers(monitor));
    }

    @Test
    public void profileWithoutUsersReturnsEmptyCollection() throws Exception {
        List<OracleUser> users = List.of(user("ALICE", "OTHER_PROFILE"));
        Mockito.when(dataSource.getUsers(monitor)).thenReturn(users);

        Assertions.assertTrue(profile("APP_PROFILE").getUsers(monitor).isEmpty());
    }

    @Test
    public void newProfileDoesNotLoadUsers() throws DBException {
        OracleUserProfile profile = new OracleUserProfile(dataSource, "APP_PROFILE");

        Assertions.assertTrue(profile.getUsers(monitor).isEmpty());
        Mockito.verify(dataSource, Mockito.never()).getUsers(Mockito.any());
    }

    @Test
    public void userLoadingFailureIsPropagated() throws Exception {
        DBException failure = new DBException("Cannot read users");
        Mockito.when(dataSource.getUsers(monitor)).thenThrow(failure);
        OracleUserProfile profile = profile("APP_PROFILE");

        Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> profile.getUsers(monitor)));
    }

    @NotNull
    private OracleUserProfile profile(@NotNull String name) throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("PROFILE")).thenReturn(name);
        return new OracleUserProfile(dataSource, resultSet);
    }

    @NotNull
    private OracleUser user(@NotNull String name, @Nullable String profileName) throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("USERNAME")).thenReturn(name);
        Mockito.when(resultSet.getString("PROFILE")).thenReturn(profileName);
        return new OracleUser(dataSource, resultSet);
    }
}
