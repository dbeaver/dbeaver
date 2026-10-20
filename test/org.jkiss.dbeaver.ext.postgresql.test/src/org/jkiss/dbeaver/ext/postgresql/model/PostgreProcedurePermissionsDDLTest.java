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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostgreProcedurePermissionsDDLTest extends DBeaverUnitTest {

    private PostgreSchema testSchema;
    private PostgreRole testUser;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");

        PostgreDataSource testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return true;
            }
        };
        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(
            monitor, "testdb", new PostgreRole(null, "tester", "test", true), null, null, null);
        testUser = new PostgreRole(testDatabase, "tester", "test", true);
        testSchema = new PostgreSchema(testDatabase, "public", testUser);
    }

    @Test
    public void revokesPublicExecuteWhenAclHasNoPublicEntry() throws DBException {
        // proacl after REVOKE ALL ON FUNCTION acltest() FROM public (#42343)
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, "tester=X/tester");

        Assertions.assertTrue(ddl.endsWith(
            "ALTER FUNCTION public.acltest() OWNER TO tester\n"
                + "REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC\n"
                + "GRANT ALL ON FUNCTION public.acltest() TO tester"
        ), ddl);
    }

    @Test
    public void keepsPublicGrantWhenAclHasPublicEntry() throws DBException {
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, "=X/tester", "tester=X/tester");

        Assertions.assertFalse(ddl.contains("REVOKE"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO PUBLIC"), ddl);
    }

    @Test
    public void revokesPublicExecuteFromProcedure() throws DBException {
        String ddl = generatePermissionsDDL(PostgreProcedureKind.p, "tester=X/tester");

        Assertions.assertTrue(ddl.contains("REVOKE ALL ON PROCEDURE public.acltest() FROM PUBLIC"), ddl);
    }

    @Test
    public void usesFunctionKeywordForAggregates() throws DBException {
        // GRANT/REVOKE ... ON AGGREGATE is a syntax error, pg_dump writes ON FUNCTION
        String ddl = generatePermissionsDDL(PostgreProcedureKind.a, "tester=X/tester");

        Assertions.assertTrue(ddl.contains("REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO tester"), ddl);
        Assertions.assertFalse(ddl.contains("ON AGGREGATE"), ddl);
    }

    @Test
    public void realRoleNamedUppercasePublicIsNotThePseudoRole() throws DBException {
        // proacl as PostgreSQL 18 stores it after GRANT EXECUTE ... TO "PUBLIC"
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, "tester=X/tester", "PUBLIC=X/tester");

        Assertions.assertTrue(ddl.contains("REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC"), ddl);
    }

    @NotNull
    private String generatePermissionsDDL(@NotNull PostgreProcedureKind kind, @NotNull String... aclItems) throws DBException {
        PostgreProcedure procedure = new PostgreProcedure(testSchema) {
            @Override
            public PostgreRole getOwner(DBRProgressMonitor monitor) {
                return testUser;
            }

            @Override
            public Collection<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) {
                return PostgreUtils.extractPermissionsFromACL(this, aclItems, false);
            }
        };
        procedure.setName("acltest");
        procedure.setKind(kind);
        procedure.setPersisted(true);

        List<DBEPersistAction> actions = new ArrayList<>();
        PostgreUtils.getObjectGrantPermissionActions(
            monitor, procedure, actions, Map.of(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, true)
        );
        return actions.stream().map(DBEPersistAction::getScript).collect(Collectors.joining("\n"));
    }
}
