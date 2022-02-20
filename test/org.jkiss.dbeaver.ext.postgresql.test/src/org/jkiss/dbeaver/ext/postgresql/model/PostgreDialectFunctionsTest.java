/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Types;

@RunWith(MockitoJUnitRunner.class)
public class PostgreDialectFunctionsTest {

    @Mock
    DBRProgressMonitor monitor;
    @Mock
    DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    DBDAttributeBinding mockAttributeBinding;

    private PostgreDialect postgreDialect;
    private PostgreDataSource testDataSource;
    private PostgreSchema testSchema;
    private PostgreSchema PGCatalogTestSchema;
    private PostgreTableRegular testTableRegular;

    @Before
    public void setUp() throws Exception {
        postgreDialect = new PostgreDialect();

        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgresql"));

        testDataSource = new PostgreDataSource(mockDataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return major <= 10;
            }

            @Override
            public PostgreDataType getLocalDataType(String typeName) {
                return super.getLocalDataType(typeName);
            }
        };

        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(monitor, "testdb", testUser, null, null, null);
        testSchema = new PostgreSchema(testDatabase, "testSchema", testUser);
        PGCatalogTestSchema = new PostgreSchema(testDatabase, PostgreConstants.CATALOG_SCHEMA_NAME, testUser); // Test PG_catalog schema for the right define default data types fully qualified names

        testTableRegular = new PostgreTableRegular(testSchema) {
            @Override
            public boolean isTablespaceSpecified() {
                return false;
            }
        };
        testTableRegular.setName("testTable");
        testTableRegular.setPartition(false);
    }

    @Test
    public void generateTypeCastClauseForNumericColumnForUpdateTableCaseInTableWithoutKeys() throws DBException {
        PostgreTableColumn column1 = PostgreTestUtils.addColumn(testTableRegular, "column1", "int4", 2);

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(column1.getDataType());

        String typeCastClause = postgreDialect.getTypeCastClause(mockAttributeBinding, "?", true);
        String expectedTypeCast = "?"; // We are not expecting casting in this clause
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }

    @Test
    public void generateCastedAttributeNameForXMLColumnForUpdateTableCaseInTableWithoutKeys() throws DBException {
        PostgreTableColumn column = PostgreTestUtils.addColumn(testTableRegular, "column1", "xml", 1);

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(column.getDataType());
        Mockito.when(mockAttributeBinding.getDataSource()).thenReturn(testDataSource);
        Mockito.when(mockAttributeBinding.getName()).thenReturn(column.getName());
        Mockito.when(mockAttributeBinding.getFullyQualifiedName(DBPEvaluationContext.DML)).thenReturn(column.getName());

        String typeCastClause = postgreDialect.getCastedAttributeName(mockAttributeBinding);
        String expectedTypeCast = "column1::text"; // We use this method only for column names in condition. JSON column name must be casted to text as in getTypeCastClause will be casted column data
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }

    @Test
    public void generateTypeCastClauseForXMLColumnForInsertTableCaseInTableWithoutKeys() throws DBException {
        PostgreTableColumn column1 = PostgreTestUtils.addColumn(testTableRegular, "column1", "xml", 1);

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(column1.getDataType());

        String typeCastClause = postgreDialect.getTypeCastClause(mockAttributeBinding, "?", false);
        String expectedTypeCast = "?"; // XML type does not have any casting in other cases
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }

    @Test
    public void generateTypeCastClauseForXMLColumnForUpdateTableCaseInTableWithoutKeys() throws DBException {
        PostgreTableColumn column1 = PostgreTestUtils.addColumn(testTableRegular, "column1", "xml", 1);

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(column1.getDataType());

        String typeCastClause = postgreDialect.getTypeCastClause(mockAttributeBinding, "?", true);
        String expectedTypeCast = "?::text"; // We are forced to add text casting to the XML type if this xml column is used in the WHERE condition and there are no keys in the table. Otherwise PostgreSQL returns an error that xml can be cast to xml.
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }

    @Test
    public void generateCastedAttributeNameForJSONColumnForUpdateTableCaseInTableWithoutKeys() throws DBException {
        PostgreTableColumn column = PostgreTestUtils.addColumn(testTableRegular, "column1", "json", 1);

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(column.getDataType());
        Mockito.when(mockAttributeBinding.getDataSource()).thenReturn(testDataSource);
        Mockito.when(mockAttributeBinding.getName()).thenReturn(column.getName());
        Mockito.when(mockAttributeBinding.getFullyQualifiedName(DBPEvaluationContext.DML)).thenReturn(column.getName());

        String typeCastClause = postgreDialect.getCastedAttributeName(mockAttributeBinding);
        String expectedTypeCast = "column1::text"; // We use this method only for column names in condition. JSON column name must be casted to text as in getTypeCastClause will be casted column data
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }

    @Test
    public void generateTypeCastClauseForJSONColumnForInsertTableCaseInTableWithoutKeys() throws DBException {
        PostgreTestUtils.addColumn(testTableRegular, "column1", "json", 1);

        PostgreDataType jsonDataType = new PostgreDataType(PGCatalogTestSchema, Types.OTHER, "json");

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(jsonDataType);

        String typeCastClause = postgreDialect.getTypeCastClause(mockAttributeBinding, "?", false);
        String expectedTypeCast = "?::json";
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }

    @Test
    public void generateTypeCastClauseForJSONColumnForUpdateTableCaseInTableWithoutKeys() throws DBException {
        PostgreTableColumn column1 = PostgreTestUtils.addColumn(testTableRegular, "column1", "json", 1);

        Mockito.when(mockAttributeBinding.getDataType()).thenReturn(column1.getDataType());

        String typeCastClause = postgreDialect.getTypeCastClause(mockAttributeBinding, "?", true);
        String expectedTypeCast = "?::text"; // We are forced to add text casting to the JSON type if this json column is used in the WHERE condition and there are no keys in the table. Otherwise PostgreSQL returns an error that json can be cast to json.
        Assert.assertEquals(expectedTypeCast, typeCastClause);
    }
}
