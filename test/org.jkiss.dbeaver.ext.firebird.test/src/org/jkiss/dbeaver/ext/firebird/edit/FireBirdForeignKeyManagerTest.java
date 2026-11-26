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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdMetaModel;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class FireBirdForeignKeyManagerTest extends DBeaverUnitTest {

    private GenericTableBase table;

    @BeforeEach
    public void setUp() {
        table = Mockito.mock(GenericTableBase.class);
        Mockito.when(table.getDataSource()).thenReturn(Mockito.mock(GenericDataSource.class));
    }

    @Test
    public void writesSupportedRules() {
        Assertions.assertEquals(
            " ON DELETE CASCADE ON UPDATE SET NULL",
            getRuleClauses(DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.SET_NULL));
        Assertions.assertEquals(
            " ON DELETE SET DEFAULT",
            getRuleClauses(DBSForeignKeyModifyRule.SET_DEFAULT, DBSForeignKeyModifyRule.NO_ACTION));
    }

    @Test
    public void writesRestrictAsNoAction() {
        // Firebird has no RESTRICT; NO ACTION is the default and needs no clause
        Assertions.assertEquals("", getRuleClauses(DBSForeignKeyModifyRule.RESTRICT, DBSForeignKeyModifyRule.RESTRICT));
        Assertions.assertEquals(
            " ON UPDATE CASCADE",
            getRuleClauses(DBSForeignKeyModifyRule.RESTRICT, DBSForeignKeyModifyRule.CASCADE));
    }

    @Test
    public void createsFireBirdForeignKey() {
        GenericTableForeignKey foreignKey = new FireBirdMetaModel().createTableForeignKeyImpl(
            table, "FK_TEST", null, null, DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyDeferability.NOT_DEFERRABLE, true);

        Assertions.assertInstanceOf(FireBirdTableForeignKey.class, foreignKey);
        Assertions.assertEquals(DBSForeignKeyModifyRule.CASCADE, foreignKey.getDeleteRule());
    }

    @Test
    public void tableManagerUsesFireBirdForeignKeys() {
        // Table DDL picks the foreign key manager by the child types of the table manager
        Assertions.assertTrue(
            Arrays.asList(new FireBirdTableManager().getChildTypes()).contains(FireBirdTableForeignKey.class));
        Assertions.assertFalse(
            Arrays.asList(new FireBirdTableManager().getChildTypes()).contains(GenericTableForeignKey.class));
    }

    @NotNull
    private String getRuleClauses(@NotNull DBSForeignKeyModifyRule deleteRule, @NotNull DBSForeignKeyModifyRule updateRule) {
        FireBirdTableForeignKey foreignKey = new FireBirdTableForeignKey(
            table, "FK_TEST", null, null, deleteRule, updateRule, DBSForeignKeyDeferability.NOT_DEFERRABLE, false);
        StringBuilder decl = new StringBuilder();
        new TestForeignKeyManager().appendRules(foreignKey, decl);
        return decl.toString();
    }

    /**
     * Exposes the protected rule writer of the manager.
     */
    private static class TestForeignKeyManager extends FireBirdForeignKeyManager {
        void appendRules(@NotNull GenericTableForeignKey foreignKey, @NotNull StringBuilder decl) {
            appendUpdateDeleteRule(foreignKey, decl);
        }
    }
}
