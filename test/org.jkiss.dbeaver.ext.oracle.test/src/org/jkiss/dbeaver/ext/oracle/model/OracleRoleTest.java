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
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OracleRoleTest extends DBeaverUnitTest {

    @Test
    public void unauthenticatedRoleCanBeDefault() throws SQLException {
        Assertions.assertTrue(role("NONE", "NO").isDefaultRoleAllowed());
    }

    @Test
    public void authenticatedRolesCannotBeDefault() throws SQLException {
        for (String authentication : List.of("PASSWORD", "APPLICATION", "EXTERNAL", "GLOBAL")) {
            Assertions.assertFalse(role(authentication, "NO").isDefaultRoleAllowed(), authentication);
        }
    }

    @Test
    public void legacyUnauthenticatedRoleCanBeDefault() throws SQLException {
        Assertions.assertTrue(role(null, "NO").isDefaultRoleAllowed());
    }

    @Test
    public void legacyProtectedRolesCannotBeDefault() throws SQLException {
        for (String authentication : List.of("YES", "APPLICATION", "EXTERNAL", "GLOBAL")) {
            Assertions.assertFalse(role(null, authentication).isDefaultRoleAllowed(), authentication);
        }
    }

    @Test
    public void missingAuthenticationDoesNotAllowDefaultRole() throws SQLException {
        Assertions.assertFalse(role(null, null).isDefaultRoleAllowed());
    }

    @NotNull
    private OracleRole role(@Nullable String authenticationType, @Nullable String passwordRequired) throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("ROLE")).thenReturn("APP_ROLE");
        Mockito.when(resultSet.getString("AUTHENTICATION_TYPE")).thenReturn(authenticationType);
        Mockito.when(resultSet.getString("PASSWORD_REQUIRED")).thenReturn(passwordRequired);
        return new OracleRole(Mockito.mock(OracleDataSource.class), resultSet);
    }
}
