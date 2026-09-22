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

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.sql.SQLException;

import static org.mockito.Mockito.when;

/**
 * {@link MimerGroupMember}'s {@code GRANT}/{@code REVOKE MEMBER ON GROUP} DDL - the {@code GROUP}
 * keyword in the {@code ON} clause is required (confirmed live against a real server -
 * Mimer SQL also silently accepts a bare {@code ON "g"}, but that isn't the correct form),
 * and {@code WITH GRANT OPTION} only appears when the loaded row's own {@code IS_GRANTABLE} said so.
 *
 * @author Mimer Information Technology
 */
public class MimerGroupMemberTest extends DBeaverUnitTest {

    @Mock
    private MimerGroup group;

    @Mock
    private JDBCResultSet resultSet;

    @BeforeEach
    public void setUp() {
        when(group.getName()).thenReturn("developers");
    }

    @Test
    public void grantDDLWithoutGrantOption() {
        MimerGroupMember member = new MimerGroupMember(group, "fredrik");
        Assertions.assertEquals(
            "GRANT MEMBER ON GROUP \"developers\" TO \"fredrik\"",
            member.buildGrantDDL());
    }

    @Test
    public void grantDDLIncludesWithGrantOptionWhenTheLoadedRowIsGrantable() throws SQLException {
        when(resultSet.getString("GRANTEE")).thenReturn("fredrik");
        when(resultSet.getString("GRANTOR")).thenReturn("SYSADM");
        when(resultSet.getString("IS_GRANTABLE")).thenReturn("YES");
        MimerGroupMember member = new MimerGroupMember(group, resultSet);

        Assertions.assertEquals(
            "GRANT MEMBER ON GROUP \"developers\" TO \"fredrik\" WITH GRANT OPTION",
            member.buildGrantDDL());
    }

    @Test
    public void revokeDDLNeverCarriesCascadeOrGrantOption() {
        MimerGroupMember member = new MimerGroupMember(group, "fredrik");
        Assertions.assertEquals(
            "REVOKE MEMBER ON GROUP \"developers\" FROM \"fredrik\"",
            member.buildRevokeDDL());
    }
}
