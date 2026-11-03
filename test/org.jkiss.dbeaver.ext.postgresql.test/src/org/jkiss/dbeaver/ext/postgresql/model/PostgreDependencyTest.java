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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreDependencyTest extends DBeaverUnitTest {

    @Test
    public void getObjectTypeWhenKnownCodeShouldReturnReadableName() {
        PostgreDependency dependency = new PostgreDependency(
            null, 0, "a", "some_object", null, "r0", null, null);
        Assertions.assertEquals("Table", dependency.getObjectType());
    }

    @Test
    public void getObjectTypeWhenSequenceShouldReturnSequence() {
        Assertions.assertEquals("Sequence", testType("S0"));
    }

    @Test
    public void getObjectTypeWhenToastTableShouldReturnToastTable() {
        Assertions.assertEquals("TOAST Table", testType("t0"));
    }

    @Test
    public void getObjectTypeWhenDataTypeShouldReturnType() {
        Assertions.assertEquals("Type", testType("y"));
    }

    @Test
    public void getObjectTypeWhenProcedureShouldReturnProcedure() {
        Assertions.assertEquals("Procedure", testType("p"));
    }

    @Test
    public void getObjectTypeWhenPartitionedTableShouldReturnPartitionedTable() {
        Assertions.assertEquals("Partitioned Table", testType("p0"));
    }

    @Test
    public void getObjectTypeWhenSchemaShouldReturnSchema() {
        Assertions.assertEquals("Schema", testType("n"));
    }

    @Test
    public void getObjectTypeWhenLanguageShouldReturnLanguage() {
        Assertions.assertEquals("Language", testType("l"));
    }

    @Test
    public void getObjectTypeWhenRuleShouldReturnRule() {
        Assertions.assertEquals("Rule", testType("R"));
    }

    @Test
    public void getObjectTypeWhenTriggerShouldReturnTrigger() {
        Assertions.assertEquals("Trigger", testType("T"));
    }

    @Test
    public void getObjectTypeWhenAttributeShouldReturnAttribute() {
        Assertions.assertEquals("Attribute", testType("A"));
    }

    @Test
    public void getObjectTypeWhenIndexShouldReturnIndex() {
        Assertions.assertEquals("Index", testType("i0"));
    }

    @Test
    public void getObjectTypeWhenViewShouldReturnView() {
        Assertions.assertEquals("View", testType("v0"));
    }

    @Test
    public void getObjectTypeWhenMaterializedViewShouldReturnMaterializedView() {
        Assertions.assertEquals("Materialized View", testType("m0"));
    }

    @Test
    public void getObjectTypeWhenCompositeTypeShouldReturnCompositeType() {
        Assertions.assertEquals("Composite Type", testType("c0"));
    }

    @Test
    public void getObjectTypeWhenForeignTableShouldReturnForeignTable() {
        Assertions.assertEquals("Foreign Table", testType("f0"));
    }

    @Test
    public void getObjectTypeWhenPartitionedIndexShouldReturnPartitionedIndex() {
        Assertions.assertEquals("Partitioned Index", testType("I0"));
    }

    @Test
    public void getObjectTypeWhenPrimaryKeyConstraintShouldReturnPrimaryKey() {
        Assertions.assertEquals("Primary Key", testType("Cp"));
    }

    @Test
    public void getObjectTypeWhenForeignKeyConstraintShouldReturnForeignKey() {
        Assertions.assertEquals("Foreign Key", testType("Cf"));
    }

    @Test
    public void getObjectTypeWhenUniqueConstraintShouldReturnUniqueConstraint() {
        Assertions.assertEquals("Unique Constraint", testType("Cu"));
    }

    @Test
    public void getObjectTypeWhenCheckConstraintShouldReturnCheckConstraint() {
        Assertions.assertEquals("Check Constraint", testType("Cc"));
    }

    @Test
    public void getObjectTypeWhenExclusionConstraintShouldReturnExclusionConstraint() {
        Assertions.assertEquals("Exclusion Constraint", testType("Cx"));
    }

    @Test
    public void getObjectTypeWhenUnknownCodeShouldReturnCodeAsIs() {
        Assertions.assertEquals("zz", testType("zz"));
    }

    @Test
    public void getObjectTypeWhenCodeIsMissingShouldReturnEmptyString() {
        Assertions.assertEquals("", testType(""));
    }

    private static String testType(String objectType) {
        PostgreDependency dependency = new PostgreDependency(null, 0, "a", "some_object", null, objectType, null, null);
        return dependency.getObjectType();
    }
}
