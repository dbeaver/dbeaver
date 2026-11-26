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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class FireBirdRoleTest extends DBeaverUnitTest {

    private FireBirdDataSource dataSource;
    private DBRProgressMonitor monitor;

    @BeforeEach
    public void setUp() {
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dialect.getQuotedIdentifier(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(dialect.getQuotedString(Mockito.anyString()))
            .thenAnswer(invocation -> "'" + invocation.<String>getArgument(0).replace("'", "''") + "'");
        dataSource = Mockito.mock(FireBirdDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        monitor = Mockito.mock(DBRProgressMonitor.class);
    }

    @Test
    public void generatesRoleWithCommentAndGrants() throws Exception {
        FireBirdRole role = Mockito.spy(new FireBirdRole(dataSource, "R_SALES", "SYSDBA", "Sales staff", false, List.of()));
        Mockito.doReturn(List.of(
            new FireBirdRoleMember(role, "ALICE", false, "SYSDBA", false, false),
            new FireBirdRoleMember(role, "ALICE", false, "R_ADMIN_USER", false, false),
            new FireBirdRoleMember(role, "BOB", false, "SYSDBA", false, true),
            new FireBirdRoleMember(role, "R_MANAGER", true, "SYSDBA", true, false),
            new FireBirdRoleMember(role, "PUBLIC", false, "SYSDBA", false, false)
        )).when(role).getMembers(monitor);

        Assertions.assertEquals(
            "CREATE ROLE R_SALES;\n\n"
                + "COMMENT ON ROLE R_SALES IS 'Sales staff';\n\n"
                + "GRANT R_SALES TO USER ALICE GRANTED BY SYSDBA;\n"
                + "GRANT R_SALES TO USER ALICE GRANTED BY R_ADMIN_USER;\n"
                + "GRANT DEFAULT R_SALES TO USER BOB GRANTED BY SYSDBA;\n"
                + "GRANT R_SALES TO ROLE R_MANAGER WITH ADMIN OPTION GRANTED BY SYSDBA;\n"
                + "GRANT R_SALES TO PUBLIC GRANTED BY SYSDBA;",
            role.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void omitsCreateForSystemRole() throws Exception {
        FireBirdRole role = Mockito.spy(new FireBirdRole(dataSource, "RDB$ADMIN", "SYSDBA", null, true, List.of()));
        Mockito.doReturn(List.of(new FireBirdRoleMember(role, "SYSDBA", false, null, true, true)))
            .when(role).getMembers(monitor);

        Assertions.assertEquals(
            "GRANT DEFAULT RDB$ADMIN TO USER SYSDBA WITH ADMIN OPTION;",
            role.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void readsRoleFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$ROLE_NAME")).thenReturn("R_SALES   ");
        Mockito.when(resultSet.getString("RDB$OWNER_NAME")).thenReturn("SYSDBA   ");
        Mockito.when(resultSet.getString("RDB$DESCRIPTION")).thenReturn("Sales staff");
        Mockito.when(resultSet.getInt("RDB$SYSTEM_FLAG")).thenReturn(0);
        // USER_MANAGEMENT (1) and CREATE_DATABASE (9)
        Mockito.when(resultSet.getBytes("RDB$SYSTEM_PRIVILEGES")).thenReturn(new byte[] {0x02, 0x02, 0, 0, 0, 0, 0, 0});

        FireBirdRole role = new FireBirdRoleCache().fetchObject(Mockito.mock(JDBCSession.class), dataSource, resultSet);

        Assertions.assertNotNull(role);
        Assertions.assertEquals("R_SALES", role.getName());
        Assertions.assertEquals("SYSDBA", role.getOwnerName());
        Assertions.assertEquals("Sales staff", role.getDescription());
        Assertions.assertFalse(role.isSystem());
        Assertions.assertEquals(List.of("USER_MANAGEMENT", "CREATE_DATABASE"), role.getSystemPrivileges());
    }

    @Test
    public void readsMemberFromResultSet() throws Exception {
        FireBirdRole role = new FireBirdRole(dataSource, "R_SALES", "SYSDBA", null, false, List.of());
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$USER")).thenReturn("R_MANAGER   ");
        Mockito.when(resultSet.getInt("RDB$USER_TYPE")).thenReturn(FireBirdConstants.OBJECT_TYPE_ROLE);
        Mockito.when(resultSet.getString("RDB$GRANTOR")).thenReturn("SYSDBA   ");
        Mockito.when(resultSet.getInt("RDB$GRANT_OPTION")).thenReturn(2);
        Mockito.when(resultSet.getString("RDB$FIELD_NAME")).thenReturn("D   ");

        FireBirdRoleMember member = new FireBirdRole.MemberCache()
            .fetchObject(Mockito.mock(JDBCSession.class), role, resultSet);

        Assertions.assertNotNull(member);
        Assertions.assertEquals("R_MANAGER", member.getName());
        Assertions.assertTrue(member.isRole());
        Assertions.assertEquals("SYSDBA", member.getGrantor());
        Assertions.assertTrue(member.isAdminOption());
        Assertions.assertTrue(member.isDefaultRole());
        Assertions.assertEquals("ROLE R_MANAGER (SYSDBA)", member.getUniqueName());
    }

    @Test
    public void generatesRoleWithSystemPrivileges() throws Exception {
        FireBirdRole role = Mockito.spy(new FireBirdRole(
            dataSource, "R_USER_ADMIN", "SYSDBA", null, false, List.of("USER_MANAGEMENT", "IGNORE_DB_TRIGGERS")));
        Mockito.doReturn(List.of()).when(role).getMembers(monitor);

        Assertions.assertEquals(
            "CREATE ROLE R_USER_ADMIN\n"
                + "    SET SYSTEM PRIVILEGES TO USER_MANAGEMENT, IGNORE_DB_TRIGGERS;",
            role.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void decodesSystemPrivileges() {
        Assertions.assertEquals(List.of(), FireBirdRole.decodeSystemPrivileges(null));
        Assertions.assertEquals(List.of(), FireBirdRole.decodeSystemPrivileges(new byte[8]));
        // Bit 0 is the unused NULL_PRIVILEGE, ids beyond the known list are ignored
        byte[] all = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        Assertions.assertEquals(FireBirdConstants.SYSTEM_PRIVILEGES.length - 1, FireBirdRole.decodeSystemPrivileges(all).size());
        Assertions.assertEquals(List.of("PROFILE_ANY_ATTACHMENT"), FireBirdRole.decodeSystemPrivileges(new byte[] {0, 0, 0, 0x08}));
    }
}
