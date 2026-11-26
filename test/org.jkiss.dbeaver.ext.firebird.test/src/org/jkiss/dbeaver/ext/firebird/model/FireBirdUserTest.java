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

import java.util.LinkedHashMap;
import java.util.Map;

public class FireBirdUserTest extends DBeaverUnitTest {

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
    public void generatesUserWithAllOptions() throws Exception {
        FireBirdUser user = Mockito.spy(new FireBirdUser(
            dataSource, "ALICE", "Alice", null, "O'Neil", false, true, "Srp", "Sales lead"));
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("DEPT", "sales");
        tags.put("LEVEL", "2");
        Mockito.doReturn(tags).when(user).getTags(monitor);

        Assertions.assertEquals(
            "-- The password can't be read from the database, set it before running the script\n"
                + "CREATE USER ALICE PASSWORD '********'\n"
                + "    FIRSTNAME 'Alice'\n"
                + "    LASTNAME 'O''Neil'\n"
                + "    INACTIVE\n"
                + "    USING PLUGIN Srp\n"
                + "    TAGS (DEPT = 'sales', LEVEL = '2')\n"
                + "    GRANT ADMIN ROLE;\n\n"
                + "COMMENT ON USER ALICE USING PLUGIN Srp IS 'Sales lead';",
            user.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
        Assertions.assertEquals("DEPT=sales, LEVEL=2", user.getTagsText(monitor));
    }

    @Test
    public void generatesMinimalUser() throws Exception {
        FireBirdUser user = Mockito.spy(new FireBirdUser(dataSource, "BOB", null, null, null, true, false, null, null));
        Mockito.doReturn(Map.of()).when(user).getTags(monitor);

        Assertions.assertEquals(
            "-- The password can't be read from the database, set it before running the script\n"
                + "CREATE USER BOB PASSWORD '********';",
            user.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void readsUserFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("SEC$USER_NAME")).thenReturn("ALICE   ");
        Mockito.when(resultSet.getString("SEC$FIRST_NAME")).thenReturn("Alice");
        Mockito.when(resultSet.getString("SEC$LAST_NAME")).thenReturn("O'Neil");
        Mockito.when(resultSet.getBoolean("SEC$ACTIVE")).thenReturn(true);
        Mockito.when(resultSet.getBoolean("SEC$ADMIN")).thenReturn(false);
        Mockito.when(resultSet.getString("SEC$PLUGIN")).thenReturn("Srp   ");
        Mockito.when(resultSet.getString("SEC$DESCRIPTION")).thenReturn("Sales lead");

        FireBirdUser user = new FireBirdUserCache().fetchObject(Mockito.mock(JDBCSession.class), dataSource, resultSet);

        Assertions.assertNotNull(user);
        Assertions.assertEquals("ALICE", user.getName());
        Assertions.assertEquals("Alice", user.getFirstName());
        Assertions.assertNull(user.getMiddleName());
        Assertions.assertEquals("O'Neil", user.getLastName());
        Assertions.assertTrue(user.isActive());
        Assertions.assertFalse(user.isAdmin());
        Assertions.assertEquals("Srp", user.getPlugin());
        Assertions.assertEquals("Sales lead", user.getDescription());
    }

    @Test
    public void uniqueNameIncludesPlugin() {
        Assertions.assertEquals("ALICE (Srp)",
            new FireBirdUser(dataSource, "ALICE", null, null, null, true, false, "Srp", null).getUniqueName());
        Assertions.assertEquals("ALICE (Legacy_UserManager)",
            new FireBirdUser(dataSource, "ALICE", null, null, null, true, false, "Legacy_UserManager", null).getUniqueName());
        Assertions.assertEquals("BOB",
            new FireBirdUser(dataSource, "BOB", null, null, null, true, false, null, null).getUniqueName());
    }
}
