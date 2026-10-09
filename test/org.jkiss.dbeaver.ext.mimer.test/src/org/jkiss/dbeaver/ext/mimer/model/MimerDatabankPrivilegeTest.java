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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * {@link MimerDatabankPrivilege}'s {@code GRANT}/{@code REVOKE TABLE|SEQUENCE ON DATABANK} DDL -
 * {@code WITH GRANT OPTION} only when set, and the chosen privilege type carried through.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankPrivilegeTest extends DBeaverUnitTest {

    @Mock
    private MimerDatabank databank;

    @BeforeEach
    public void setUp() {
        when(databank.getName()).thenReturn("SALESDB");
    }

    @Test
    public void grantDDLWithoutGrantOption() {
        MimerDatabankPrivilege p = new MimerDatabankPrivilege(databank, "alice", "TABLE");
        Assertions.assertEquals("GRANT TABLE ON DATABANK \"SALESDB\" TO \"alice\"", p.buildGrantDDL());
    }

    @Test
    public void grantDDLWithGrantOption() {
        MimerDatabankPrivilege p = new MimerDatabankPrivilege(databank, "alice", "SEQUENCE");
        p.setGrantable(true);
        Assertions.assertEquals(
            "GRANT SEQUENCE ON DATABANK \"SALESDB\" TO \"alice\" WITH GRANT OPTION", p.buildGrantDDL());
    }

    @Test
    public void revokeDDLNeverCarriesGrantOptionOrCascade() {
        MimerDatabankPrivilege p = new MimerDatabankPrivilege(databank, "bob", "TABLE");
        p.setGrantable(true);
        Assertions.assertEquals("REVOKE TABLE ON DATABANK \"SALESDB\" FROM \"bob\"", p.buildRevokeDDL());
    }

    @Test
    public void privilegeTypeCanBeChangedBeforeCreate() {
        MimerDatabankPrivilege p = new MimerDatabankPrivilege(databank, "alice", "TABLE");
        p.setPrivilegeType("SEQUENCE");
        p.setGrantee("carol");
        Assertions.assertEquals("GRANT SEQUENCE ON DATABANK \"SALESDB\" TO \"carol\"", p.buildGrantDDL());
        Assertions.assertEquals("carol (SEQUENCE)", p.getName());
    }
}
