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

import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FireBirdTableConstraintTest extends DBeaverUnitTest {

    @Test
    public void extractsConditionFromCheckSource() {
        Assertions.assertEquals("PRICE >= 0", FireBirdTableConstraint.extractCheckCondition("CHECK (PRICE >= 0)"));
        // The keyword keeps the case written by the user
        Assertions.assertEquals(
            "(PRICE * QTY) < 1000 AND NAME <> ''",
            FireBirdTableConstraint.extractCheckCondition("check ((PRICE * QTY) < 1000 AND NAME <> '')"));
        Assertions.assertEquals("QTY > 0", FireBirdTableConstraint.extractCheckCondition("  CHECK(QTY > 0)\n"));
        Assertions.assertNull(FireBirdTableConstraint.extractCheckCondition(null));
    }

    @Test
    public void keepsParenthesesThatDoNotEncloseTheWholeCondition() {
        // The first parenthesis closes before the end, so it must not be stripped
        Assertions.assertEquals(
            "(A > 0) AND (B > 0)",
            FireBirdTableConstraint.extractCheckCondition("CHECK ((A > 0) AND (B > 0))"));
        Assertions.assertEquals(
            "(A > 0) AND (B > 0)",
            FireBirdTableConstraint.extractCheckCondition("CHECK (A > 0) AND (B > 0)"));
        // Parentheses inside string literals and quoted names are ignored
        Assertions.assertEquals(
            "NAME <> ')' AND \"Col)\" > 0",
            FireBirdTableConstraint.extractCheckCondition("CHECK (NAME <> ')' AND \"Col)\" > 0)"));
    }

    @Test
    public void readsConstraintTypes() throws Exception {
        FireBirdMetaModel metaModel = new FireBirdMetaModel();
        Assertions.assertEquals(DBSEntityConstraintType.CHECK, metaModel.getUniqueConstraintType(constraintRow("CHECK      ")));
        Assertions.assertEquals(DBSEntityConstraintType.PRIMARY_KEY, metaModel.getUniqueConstraintType(constraintRow("PRIMARY KEY")));
        Assertions.assertEquals(DBSEntityConstraintType.UNIQUE_KEY, metaModel.getUniqueConstraintType(constraintRow("UNIQUE     ")));
        Assertions.assertTrue(metaModel.supportsCheckConstraints());
    }

    @Test
    public void createsCheckConstraintWithCondition() throws Exception {
        GenericTableBase table = Mockito.mock(GenericTableBase.class);
        Mockito.when(table.getDataSource()).thenReturn(Mockito.mock(GenericDataSource.class));
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("CHECK_SOURCE")).thenReturn("CHECK (PRICE >= 0)");

        GenericUniqueKey constraint = new FireBirdMetaModel()
            .createConstraintImpl(table, "CK_PRICE", DBSEntityConstraintType.CHECK, resultSet, true);

        FireBirdTableConstraint check = Assertions.assertInstanceOf(FireBirdTableConstraint.class, constraint);
        Assertions.assertEquals("PRICE >= 0", check.getCheckConstraintDefinition());
        Assertions.assertTrue(new FireBirdTableConstraint.CheckConstraintValidator().isValidValue(check, null));
    }

    @Test
    public void createsNewConstraintThatCanBecomeCheckConstraint() {
        GenericTableBase table = Mockito.mock(GenericTableBase.class);
        Mockito.when(table.getDataSource()).thenReturn(Mockito.mock(GenericDataSource.class));

        // A constraint created in the UI starts as primary key without a result set
        GenericUniqueKey constraint = new FireBirdMetaModel()
            .createConstraintImpl(table, "NEW_CONSTRAINT", DBSEntityConstraintType.PRIMARY_KEY, null, false);

        FireBirdTableConstraint check = Assertions.assertInstanceOf(FireBirdTableConstraint.class, constraint);
        Assertions.assertNull(check.getCheckConstraintDefinition());
        Assertions.assertFalse(new FireBirdTableConstraint.CheckConstraintValidator().isValidValue(check, null));
    }

    private JDBCResultSet constraintRow(String constraintType) throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("CONSTRAINT_TYPE")).thenReturn(constraintType);
        return resultSet;
    }
}
