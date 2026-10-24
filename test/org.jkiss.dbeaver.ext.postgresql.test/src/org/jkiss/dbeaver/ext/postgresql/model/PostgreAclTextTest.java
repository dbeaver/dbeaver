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
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Array;
import java.util.List;

public class PostgreAclTextTest extends DBeaverUnitTest {

    private static final String[] ACL = {"postgres=arwdDxt/postgres", "reader=r/postgres"};

    private PostgrePrivilegeOwner owner;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");
        PostgreDataSource dataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return false;
            }
        };
        PostgreRole role = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase database = dataSource.createDatabaseImpl(monitor, "testdb", role, null, null, null);
        PostgreSchema schema = Mockito.mock(PostgreSchema.class);
        Mockito.when(schema.getName()).thenReturn("public");
        owner = Mockito.mock(PostgrePrivilegeOwner.class);
        Mockito.when(owner.getDatabase()).thenReturn(database);
        Mockito.when(owner.getSchema()).thenReturn(schema);
        Mockito.when(owner.getName()).thenReturn("t1");
    }

    @Test
    public void textAclMatchesArrayAcl() throws Exception {
        Array array = Mockito.mock(Array.class);
        Mockito.when(array.getArray()).thenReturn(ACL);

        List<String> fromArray = describe(PostgreUtils.extractPermissionsFromACL(monitor, owner, array, false));
        List<String> fromText = describe(PostgreUtils.extractPermissionsFromACL(
            monitor, owner, "{" + String.join(",", ACL) + "}", false));

        Assertions.assertEquals(2, fromArray.size());
        Assertions.assertEquals(fromArray, fromText);
    }

    @NotNull
    private static List<String> describe(@NotNull List<PostgrePrivilege> privileges) {
        return privileges.stream()
            .map(p -> ((PostgreObjectPrivilege) p).getGrantee().getRoleName() + ":" + p.getPrivileges().length)
            .toList();
    }
}
