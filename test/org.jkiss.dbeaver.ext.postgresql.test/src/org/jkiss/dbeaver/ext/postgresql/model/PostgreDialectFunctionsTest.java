/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.model.DBPAttributeReferencePurpose;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Types;

public class PostgreDialectFunctionsTest extends DBeaverUnitTest {

    @Mock
    DBRProgressMonitor monitor;
    @Mock
    DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    DBDAttributeBinding mockAttributeBinding;
    @Mock
    DBSTypedObject mockTypedObject;

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

            @Nullable
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
        Mockito.when(mockAttributeBinding.getFullyQualifiedName(DBPEvaluationContext.DML, DBPAttributeReferencePurpose.UNSPECIFIED))
            .thenReturn(column.getName());

        String typeCastClause = postgreDialect.getCastedAttributeName(mockAttributeBinding, mockAttributeBinding.getName());
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
        Mockito.when(mockAttributeBinding.getFullyQualifiedName(DBPEvaluationContext.DML, DBPAttributeReferencePurpose.UNSPECIFIED))
            .thenReturn(column.getName());

        String typeCastClause = postgreDialect.getCastedAttributeName(mockAttributeBinding, mockAttributeBinding.getName());
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

    @Test
    public void generateCorrectDataTypeNameFromXMLDataType() throws DBException {
        PostgreTableColumn column1 = PostgreTestUtils.addColumn(testTableRegular, "column1", "xml", 1);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, column1, testDataSource);
        Assert.assertEquals("xml", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromXMLTypeDataType() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("xmltype");
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("xml", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromSYSXMLTypeDataType() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("sys.xmltype");
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("xml", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromNVACRHARDataType() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("nvarchar");
        Mockito.when(mockTypedObject.getMaxLength()).thenReturn(42L);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("varchar(42)", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromVACRHAR2DataType() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("varchar2");
        Mockito.when(mockTypedObject.getMaxLength()).thenReturn(33L);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("varchar(33)", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromNCRHARDataType() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("nchar");
        Mockito.when(mockTypedObject.getMaxLength()).thenReturn(67L);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("varchar(67)", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameWithModifiersFromNUMBERWithoutModifiers() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("number");
        Mockito.when(mockTypedObject.getPrecision()).thenReturn(null);
//        Mockito.when(mockTypedObject.getScale()).thenReturn(null);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("numeric", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameNUMBERWithPrecisionOnly() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("number");
        Mockito.when(mockTypedObject.getPrecision()).thenReturn(28);
        Mockito.when(mockTypedObject.getScale()).thenReturn(null);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("numeric(28)", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromNUMBERWithPrecisionAndScale() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("number");
        Mockito.when(mockTypedObject.getPrecision()).thenReturn(15);
        Mockito.when(mockTypedObject.getScale()).thenReturn(5);
        String actualDataType = postgreDialect.convertExternalDataType(postgreDialect, mockTypedObject, testDataSource);
        Assert.assertEquals("numeric(15,5)", actualDataType);
    }
}
