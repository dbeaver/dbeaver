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

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class FireBirdCharacterSetTest extends DBeaverUnitTest {

    private FireBirdDataSource dataSource;
    private DBRProgressMonitor monitor;

    @BeforeEach
    public void setUp() {
        dataSource = Mockito.mock(FireBirdDataSource.class);
        monitor = Mockito.mock(DBRProgressMonitor.class);
    }

    @Test
    public void listsOnlyCollationsOfCharacterSet() throws Exception {
        FireBirdCharacterSet utf8 = new FireBirdCharacterSet(dataSource, "UTF8", 4, 4, "UTF8", null);
        Mockito.doReturn(List.of(
            collation("UTF8", 4),
            collation("UNICODE_CI", 4),
            collation("WIN1252", 53),
            collation("PXW_INTL", 53)
        )).when(dataSource).getCollations(monitor);

        Assertions.assertEquals(
            List.of("UTF8", "UNICODE_CI"),
            utf8.getCollations(monitor).stream().map(FireBirdCollation::getName).toList());
    }

    @Test
    public void resolvesCharacterSetOfCollationById() throws Exception {
        FireBirdCharacterSet win1252 = new FireBirdCharacterSet(dataSource, "WIN1252", 53, 1, "WIN1252", null);
        Mockito.doReturn(List.of(
            new FireBirdCharacterSet(dataSource, "UTF8", 4, 4, "UTF8", null),
            win1252
        )).when(dataSource).getCharacterSets(monitor);
        Mockito.when(dataSource.getCharacterSet(monitor, 53)).thenCallRealMethod();

        Assertions.assertSame(win1252, collation("PXW_INTL", 53).getCharacterSet(monitor));
    }

    @Test
    public void readsCharacterSetFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$CHARACTER_SET_NAME")).thenReturn("UTF8   ");
        Mockito.when(resultSet.getInt("RDB$CHARACTER_SET_ID")).thenReturn(4);
        Mockito.when(resultSet.getInt("RDB$BYTES_PER_CHARACTER")).thenReturn(4);
        Mockito.when(resultSet.getString("RDB$DEFAULT_COLLATE_NAME")).thenReturn("UTF8   ");

        FireBirdCharacterSet characterSet = new FireBirdCharacterSetCache()
            .fetchObject(Mockito.mock(JDBCSession.class), dataSource, resultSet);

        Assertions.assertNotNull(characterSet);
        Assertions.assertEquals("UTF8", characterSet.getName());
        Assertions.assertEquals(4, characterSet.getId());
        Assertions.assertEquals(4, characterSet.getBytesPerCharacter());
        Assertions.assertEquals("UTF8", characterSet.getDefaultCollationName());
    }

    @Test
    public void readsCollationFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$COLLATION_NAME")).thenReturn("UNICODE_CI   ");
        Mockito.when(resultSet.getInt("RDB$COLLATION_ID")).thenReturn(3);
        Mockito.when(resultSet.getInt("RDB$CHARACTER_SET_ID")).thenReturn(4);
        Mockito.when(resultSet.getString("RDB$BASE_COLLATION_NAME")).thenReturn("UNICODE   ");
        Mockito.when(resultSet.getString("RDB$SPECIFIC_ATTRIBUTES")).thenReturn("COLL-VERSION=153.88");
        Mockito.when(resultSet.getInt("RDB$SYSTEM_FLAG")).thenReturn(1);

        FireBirdCollation collation = new FireBirdCollationCache()
            .fetchObject(Mockito.mock(JDBCSession.class), dataSource, resultSet);

        Assertions.assertNotNull(collation);
        Assertions.assertEquals("UNICODE_CI", collation.getName());
        Assertions.assertEquals(3, collation.getId());
        Assertions.assertEquals(4, collation.getCharacterSetId());
        Assertions.assertEquals("UNICODE", collation.getBaseCollationName());
        Assertions.assertEquals("COLL-VERSION=153.88", collation.getSpecificAttributes());
        Assertions.assertTrue(collation.isSystem());
    }

    private FireBirdCollation collation(String name, int characterSetId) {
        return new FireBirdCollation(dataSource, name, 0, characterSetId, null, null, null, true);
    }
}
