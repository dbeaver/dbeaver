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
    public void getObjectType_whenTypeCodeIsKnown_shouldReturnReadableName() {
        PostgreDependency dependency = new PostgreDependency(
            null, 0, "a", "some_object", null, "r0", null, null);
        Assertions.assertEquals("Table", dependency.getObjectType());
    }

    @Test
    public void getObjectType_whenSequence_shouldReturnSequence() {
        Assertions.assertEquals("Sequence", testType("S0"));
    }

    @Test
    public void getObjectType_whenToastTable_shouldReturnToastTable() {
        Assertions.assertEquals("TOAST Table", testType("t0"));
    }

    @Test
    public void getObjectType_whenDataType_shouldReturnType() {
        Assertions.assertEquals("Type", testType("y"));
    }

    @Test
    public void getObjectType_whenProcedure_shouldReturnProcedure() {
        Assertions.assertEquals("Procedure", testType("p"));
    }

    @Test
    public void getObjectType_whenPartitionedTable_shouldReturnPartitionedTable() {
        Assertions.assertEquals("Partitioned Table", testType("p0"));
    }

    @Test
    public void getObjectType_whenSchema_shouldReturnSchema() {
        Assertions.assertEquals("Schema", testType("n"));
    }

    @Test
    public void getObjectType_whenLanguage_shouldReturnLanguage() {
        Assertions.assertEquals("Language", testType("l"));
    }

    @Test
    public void getObjectType_whenRule_shouldReturnRule() {
        Assertions.assertEquals("Rule", testType("R"));
    }

    @Test
    public void getObjectType_whenTrigger_shouldReturnTrigger() {
        Assertions.assertEquals("Trigger", testType("T"));
    }

    @Test
    public void getObjectType_whenAttribute_shouldReturnAttribute() {
        Assertions.assertEquals("Attribute", testType("A"));
    }

    @Test
    public void getObjectType_whenIndex_shouldReturnIndex() {
        Assertions.assertEquals("Index", testType("i0"));
    }

    @Test
    public void getObjectType_whenView_shouldReturnView() {
        Assertions.assertEquals("View", testType("v0"));
    }

    @Test
    public void getObjectType_whenMaterializedView_shouldReturnMaterializedView() {
        Assertions.assertEquals("Materialized View", testType("m0"));
    }

    @Test
    public void getObjectType_whenCompositeType_shouldReturnCompositeType() {
        Assertions.assertEquals("Composite Type", testType("c0"));
    }

    @Test
    public void getObjectType_whenForeignTable_shouldReturnForeignTable() {
        Assertions.assertEquals("Foreign Table", testType("f0"));
    }

    @Test
    public void getObjectType_whenPrimaryKeyConstraint_shouldReturnPrimaryKey() {
        Assertions.assertEquals("Primary Key", testType("Cp"));
    }

    @Test
    public void getObjectType_whenForeignKeyConstraint_shouldReturnForeignKey() {
        Assertions.assertEquals("Foreign Key", testType("Cf"));
    }

    @Test
    public void getObjectType_whenUniqueConstraint_shouldReturnUniqueConstraint() {
        Assertions.assertEquals("Unique Constraint", testType("Cu"));
    }

    @Test
    public void getObjectType_whenCheckConstraint_shouldReturnCheckConstraint() {
        Assertions.assertEquals("Check Constraint", testType("Cc"));
    }

    @Test
    public void getObjectType_whenExclusionConstraint_shouldReturnExclusionConstraint() {
        Assertions.assertEquals("Exclusion Constraint", testType("Cx"));
    }

    @Test
    public void getObjectType_whenUnknownCode_shouldReturnCodeAsIs() {
        Assertions.assertEquals("zz", testType("zz"));
    }

    @Test
    public void getObjectType_whenCodeIsMissing_shouldReturnEmptyString() {
        Assertions.assertEquals("", testType(""));
    }

    private static String testType(String objectType) {
        PostgreDependency dependency = new PostgreDependency(null, 0, "a", "some_object", null, objectType, null, null);
        return dependency.getObjectType();
    }
}
