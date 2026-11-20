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
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;

public class PostgreDataTypeTest extends DBeaverUnitTest {

    private PostgreDataSource testDataSource;
    private PostgreSchema testSchema;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");

        testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres");

        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(monitor, "testdb", testUser, null, null, null);
        testSchema = new PostgreSchema(testDatabase, "test_schema", testUser);
    }

    @Test
    public void getFullyQualifiedNameOfCatalogTypeReturnsSqlName() {
        assertCatalogTypeSqlName("int4", "integer");
        assertCatalogTypeSqlName("int8", "bigint");
        assertCatalogTypeSqlName("int2", "smallint");
        assertCatalogTypeSqlName("float4", "real");
        assertCatalogTypeSqlName("float8", "double precision");
        assertCatalogTypeSqlName("bool", "boolean");
        assertCatalogTypeSqlName("bpchar", "character");
    }

    @Test
    public void getFullyQualifiedNameOfCatalogTypeWithoutSqlNameReturnsTypeName() {
        assertCatalogTypeSqlName("varchar", "varchar");
        assertCatalogTypeSqlName("text", "text");
    }

    @Test
    public void getFullyQualifiedNameOfTypeOutsideCatalogSchemaReturnsQualifiedName() {
        PostgreDataType dataType = new PostgreDataType(testSchema, Types.OTHER, "int4");
        Assertions.assertEquals("test_schema.int4", dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
    }

    @Test
    public void resolveTypeFullNameWithSqlNameReturnsCatalogType() throws Exception {
        PostgreDataType dataType = PostgreUtils.resolveTypeFullName(monitor, testSchema, "double precision");
        Assertions.assertNotNull(dataType);
        Assertions.assertEquals(PostgreOid.FLOAT8, dataType.getObjectId());
        Assertions.assertEquals("double precision", dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
    }

    @Test
    public void getFullTypeNameOfColumnWithCatalogTypeReturnsSqlName() throws Exception {
        PostgreTableRegular table = new PostgreTableRegular(testSchema);
        table.setName("test_table");
        PostgreTestUtils.addColumn(table, "column1", "int4", 1);

        Assertions.assertEquals("integer", table.getCachedAttributes().get(0).getFullTypeName());
    }

    private void assertCatalogTypeSqlName(@NotNull String typeName, @NotNull String expectedSqlName) {
        PostgreDataType dataType = testDataSource.getLocalDataType(typeName);
        Assertions.assertNotNull(dataType, "Type '" + typeName + "' not found");
        Assertions.assertEquals(expectedSqlName, dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
    }
}
