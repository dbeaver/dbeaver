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
import org.jkiss.code.Nullable;
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

import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
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
    public void noRevokeWhenAclIsNull() throws DBException {
        // proacl is NULL, the function has default privileges
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, (String[]) null);

        Assertions.assertFalse(ddl.contains("REVOKE"), ddl);
    }

    @Test
    public void noRevokeWhenAclCannotBeDecoded() throws DBException {
        // proacl is an array, but Array.getArray() fails, so its privileges are unknown
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, testUser, createAclArray(null));

        Assertions.assertFalse(ddl.contains("REVOKE"), ddl);
        Assertions.assertFalse(ddl.contains("GRANT"), ddl);
    }

    @Test
    public void realRoleNamedUppercasePublicIsNotThePseudoRole() throws DBException {
        // proacl as PostgreSQL 18 stores it after GRANT EXECUTE ... TO "PUBLIC"
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, "tester=X/tester", "PUBLIC=X/tester");

        Assertions.assertTrue(ddl.contains("REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO \"PUBLIC\""), ddl);
    }

    @Test
    public void revokesFromOwnerNamedUppercasePublic() throws DBException {
        // A real role "PUBLIC" owns the function and proacl is {}
        PostgreRole owner = new PostgreRole(testSchema.getDatabase(), "PUBLIC", "test", true);
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, owner, createAclArray(new String[0]));

        Assertions.assertTrue(ddl.endsWith(
            "ALTER FUNCTION public.acltest() OWNER TO \"PUBLIC\"\n"
                + "REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC\n"
                + "REVOKE ALL ON FUNCTION public.acltest() FROM \"PUBLIC\""
        ), ddl);
    }

    @Test
    public void ownerNamedUppercasePublicKeepsItsGrant() throws DBException {
        PostgreRole owner = new PostgreRole(testSchema.getDatabase(), "PUBLIC", "test", true);
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, owner, createAclArray(new String[]{"PUBLIC=X/PUBLIC"}));

        Assertions.assertTrue(ddl.endsWith(
            "ALTER FUNCTION public.acltest() OWNER TO \"PUBLIC\"\n"
                + "REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC\n"
                + "GRANT ALL ON FUNCTION public.acltest() TO \"PUBLIC\""
        ), ddl);
    }

    @Test
    public void matchesOwnerNameCaseSensitively() throws DBException {
        // AclOwner and aclowner are different roles in the catalog
        PostgreRole owner = new PostgreRole(testSchema.getDatabase(), "AclOwner", "test", true);
        String ddl = generatePermissionsDDL(
            PostgreProcedureKind.f, owner, createAclArray(new String[]{"=X/AclOwner", "aclowner=X/AclOwner"}));

        Assertions.assertTrue(ddl.contains("REVOKE ALL ON FUNCTION public.acltest() FROM \"AclOwner\""), ddl);
        Assertions.assertFalse(ddl.contains("FROM PUBLIC"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO aclowner"), ddl);

        ddl = generatePermissionsDDL(
            PostgreProcedureKind.f, owner, createAclArray(new String[]{"=X/AclOwner", "AclOwner=X/AclOwner"}));

        Assertions.assertFalse(ddl.contains("REVOKE"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO \"AclOwner\""), ddl);
    }

    @Test
    public void revokesPublicAndOwnerWhenAclIsEmpty() throws DBException {
        // proacl is {} after REVOKE ALL ... FROM public and REVOKE ALL ... FROM tester
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f);

        Assertions.assertTrue(ddl.endsWith(
            "ALTER FUNCTION public.acltest() OWNER TO tester\n"
                + "REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC\n"
                + "REVOKE ALL ON FUNCTION public.acltest() FROM tester"
        ), ddl);
    }

    @Test
    public void revokesOwnerWhenAclHasNoOwnerEntry() throws DBException {
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, "=X/tester");

        Assertions.assertTrue(ddl.contains("REVOKE ALL ON FUNCTION public.acltest() FROM tester"), ddl);
        Assertions.assertFalse(ddl.contains("FROM PUBLIC"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO PUBLIC"), ddl);
    }

    @Test
    public void parsesOwnerNameWithEqualsSign() throws DBException {
        // proacl for a function owned by role "a=b", as PostgreSQL prints it
        PostgreRole owner = new PostgreRole(testSchema.getDatabase(), "a=b", "test", true);
        String ddl = generatePermissionsDDL(
            PostgreProcedureKind.f, owner, createAclArray(new String[]{"=X/\"a=b\"", "\"a=b\"=X/\"a=b\""}));

        Assertions.assertFalse(ddl.contains("REVOKE"), ddl);
        Assertions.assertTrue(ddl.contains("GRANT ALL ON FUNCTION public.acltest() TO \"a=b\""), ddl);
    }

    @Test
    public void parsesOwnerNameWithSlashAndQuote() throws DBException {
        // role x"/y is printed as "x""/y" in aclitem text
        PostgreRole owner = new PostgreRole(testSchema.getDatabase(), "x\"/y", "test", true);
        String ddl = generatePermissionsDDL(
            PostgreProcedureKind.f, owner, createAclArray(new String[]{"\"x\"\"/y\"=X/\"x\"\"/y\""}));

        Assertions.assertFalse(ddl.contains("FROM \"x"), ddl);
        Assertions.assertTrue(ddl.contains("REVOKE ALL ON FUNCTION public.acltest() FROM PUBLIC"), ddl);
    }

    @Test
    public void keepsGrantOption() throws DBException {
        // proacl after GRANT EXECUTE ... TO qa_acl_user WITH GRANT OPTION (#42798)
        String ddl = generatePermissionsDDL(PostgreProcedureKind.f, "tester=X/tester", "qa_acl_user=X*/tester");

        Assertions.assertTrue(ddl.endsWith(
            "GRANT ALL ON FUNCTION public.acltest() TO tester\n"
                + "GRANT ALL ON FUNCTION public.acltest() TO qa_acl_user WITH GRANT OPTION"
        ), ddl);
    }

    @NotNull
    private String generatePermissionsDDL(@NotNull PostgreProcedureKind kind, @Nullable String... aclItems) throws DBException {
        return generatePermissionsDDL(kind, testUser, aclItems == null ? null : createAclArray(aclItems));
    }

    @NotNull
    private String generatePermissionsDDL(
        @NotNull PostgreProcedureKind kind,
        @NotNull PostgreRole owner,
        @Nullable Array proacl
    ) throws DBException {
        PostgreProcedure procedure = new PostgreProcedure(testSchema) {
            {
                acl = proacl;
            }

            @Override
            public PostgreRole getOwner(DBRProgressMonitor monitor) {
                return owner;
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

    /**
     * proacl as the JDBC driver returns it. Null items make getArray() fail.
     */
    @NotNull
    private static Array createAclArray(@Nullable String[] aclItems) {
        return (Array) Proxy.newProxyInstance(
            Array.class.getClassLoader(),
            new Class<?>[]{Array.class},
            (proxy, method, args) -> {
                if ("getArray".equals(method.getName()) && method.getParameterCount() == 0) {
                    if (aclItems == null) {
                        throw new SQLException("Can't decode proacl");
                    }
                    return aclItems;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }
}
