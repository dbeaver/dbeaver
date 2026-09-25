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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link MimerObjectPrivilege}'s {@code GRANT}/{@code REVOKE ON TABLE} DDL - including the
 * {@code ALL PRIVILEGES} shorthand and {@code WITH GRANT OPTION}.
 *
 * @author Mimer Information Technology
 */
public class MimerObjectPrivilegeTest extends DBeaverUnitTest {

    @Mock
    private GenericTableBase table;
    @Mock
    private GenericSchema schema;

    @BeforeEach
    public void setUp() {
        lenient().when(table.getSchema()).thenReturn(schema);
        lenient().when(schema.getName()).thenReturn("mimer_store");
        lenient().when(table.getName()).thenReturn("orders");
    }

    @Test
    public void allPrivilegesIsOffered() {
        Assertions.assertTrue(Arrays.asList(MimerObjectPrivilege.PRIVILEGE_TYPES).contains("ALL PRIVILEGES"));
        // ...but SELECT stays the default (index 0), not ALL.
        Assertions.assertEquals("SELECT", MimerObjectPrivilege.PRIVILEGE_TYPES[0]);
    }

    @Test
    public void grantSinglePrivilege() {
        MimerObjectPrivilege p = new MimerObjectPrivilege(table, "alice", "SELECT");
        Assertions.assertEquals(
            "GRANT SELECT ON TABLE \"mimer_store\".\"orders\" TO \"alice\"", p.buildGrantDDL());
    }

    @Test
    public void grantAllPrivileges() {
        MimerObjectPrivilege p = new MimerObjectPrivilege(table, "alice", "ALL PRIVILEGES");
        Assertions.assertEquals(
            "GRANT ALL PRIVILEGES ON TABLE \"mimer_store\".\"orders\" TO \"alice\"", p.buildGrantDDL());
    }

    @Test
    public void grantAllPrivilegesWithGrantOption() {
        MimerObjectPrivilege p = new MimerObjectPrivilege(table, "alice", "ALL PRIVILEGES");
        p.setGrantable(true);
        Assertions.assertEquals(
            "GRANT ALL PRIVILEGES ON TABLE \"mimer_store\".\"orders\" TO \"alice\" WITH GRANT OPTION",
            p.buildGrantDDL());
    }

    @Test
    public void revokeAllPrivileges() {
        MimerObjectPrivilege p = new MimerObjectPrivilege(table, "bob", "ALL PRIVILEGES");
        p.setGrantable(true);
        Assertions.assertEquals(
            "REVOKE ALL PRIVILEGES ON TABLE \"mimer_store\".\"orders\" FROM \"bob\"", p.buildRevokeDDL());
    }
}
