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

public class PostgreTablePermissionsDDLTest extends DBeaverUnitTest {

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
    public void splitsGrantWhenGrantOptionsAreMixed() throws DBException {
        // relacl after GRANT ALL ... WITH GRANT OPTION and REVOKE GRANT OPTION FOR SELECT (#42913)
        String ddl = generatePermissionsDDL("tester=arwdDxtm/tester", "qa_acl_user=a*rw*d*D*x*t*m*/tester");

        Assertions.assertTrue(ddl.endsWith(
            "GRANT ALL ON TABLE public.acltest TO tester\n"
                + "GRANT SELECT ON TABLE public.acltest TO qa_acl_user\n"
                + "GRANT INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, MAINTAIN ON TABLE public.acltest TO qa_acl_user WITH GRANT OPTION"
        ), ddl);
    }

    @Test
    public void splitsGrantWhenSomePrivilegesHaveMixedGrantOptions() throws DBException {
        // A single statement would either drop or over-grant the grant option
        String ddl = generatePermissionsDDL("tester=arwdDxtm/tester", "qa_acl_user=a*r/tester");

        Assertions.assertTrue(ddl.endsWith(
            "GRANT SELECT ON TABLE public.acltest TO qa_acl_user\n"
                + "GRANT INSERT ON TABLE public.acltest TO qa_acl_user WITH GRANT OPTION"
        ), ddl);
    }

    @Test
    public void keepsGrantAllWithGrantOptionWhenAllPrivilegesAreGrantable() throws DBException {
        String ddl = generatePermissionsDDL("tester=arwdDxtm/tester", "qa_acl_user=a*r*w*d*D*x*t*m*/tester");

        Assertions.assertTrue(ddl.endsWith(
            "GRANT ALL ON TABLE public.acltest TO qa_acl_user WITH GRANT OPTION"
        ), ddl);
    }

    @Test
    public void listsPrivilegesInDeclarationOrder() throws DBException {
        String ddl = generatePermissionsDDL("tester=arwdDxtm/tester", "qa_acl_user=xwar/tester");

        Assertions.assertTrue(ddl.endsWith(
            "GRANT SELECT, INSERT, UPDATE, REFERENCES ON TABLE public.acltest TO qa_acl_user"
        ), ddl);
    }

    @NotNull
    private String generatePermissionsDDL(@NotNull String... aclItems) throws DBException {
        PostgreTableRegular table = new PostgreTableRegular(testSchema) {
            @Override
            public PostgreRole getOwner(DBRProgressMonitor monitor) {
                return testUser;
            }

            @Override
            public Collection<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) {
                return PostgreUtils.extractPermissionsFromACL(this, aclItems, false);
            }
        };
        table.setName("acltest");
        table.setPersisted(true);

        List<DBEPersistAction> actions = new ArrayList<>();
        PostgreUtils.getObjectGrantPermissionActions(
            monitor, table, actions, Map.of(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, true)
        );
        return actions.stream().map(DBEPersistAction::getScript).collect(Collectors.joining("\n"));
    }
}
