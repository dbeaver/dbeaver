/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DBUtilsTest extends DBeaverUnitTest {

    BasicSQLDialect sqlDialect = new BasicSQLDialect() {
        @NotNull
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][] { { "\"", "\""} };
        }

        @NotNull
        @Override
        public DBPIdentifierCase storesUnquotedCase() {
            return DBPIdentifierCase.LOWER;
        }

        @NotNull
        @Override
        public DBPIdentifierCase storesQuotedCase() {
            return DBPIdentifierCase.MIXED;
        }
    };
    @Mock
    JDBCDataSource mockDataSource;
    @Mock
    private JDBCDataSource mockDataSourceSchemaTable;
    @Mock
    private JDBCDataSource mockDataSourceTable;
    @Mock
    private JDBCDataSource mockDataSourceCatalogSchema;
    @Mock
    private DBRProgressMonitor monitor;
    @Mock
    private JDBCRemoteInstance mockRemoteInstance;
    @Mock
    private JDBCRemoteInstance mockRemoteInstanceSchema;
    @Mock
    private JDBCRemoteInstance mockRemoteInstanceTable;
    @Mock
    private JDBCRemoteInstance mockRemoteInstanceCatalogSchema;
    @Mock
    private DBSCatalog mockCatalog;
    @Mock
    private DBSCatalog mockCatalogDBO;
    @Mock
    private DBSSchema mockSchema;
    @Mock
    private DBSSchema mockSchemaDBO;
    @Mock
    private DBSEntity mockEntity;
    @Mock
    private DBSEntity mockEntityDBO;

    private JDBCExecutionContext executionContext;
    private JDBCExecutionContext executionContextSchema;
    private JDBCExecutionContext executionContextTable;
    private JDBCExecutionContext executionContextCatalogSchema;

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(sqlDialect);

        // Datasource 1. Catalog-table structure
        Mockito.lenient().when(mockRemoteInstance.getDataSource()).thenReturn(mockDataSource);
        Mockito.lenient().when(mockDataSource.getName()).thenReturn("test_name");
        executionContext = new JDBCExecutionContext(mockRemoteInstance, true);
        Mockito.lenient().<Class<?>>when(mockDataSource.getPrimaryChildType(monitor)).thenReturn(DBSCatalog.class);
        Mockito.lenient().when(mockDataSource.getChild(monitor, "catalog_test")).thenReturn(mockCatalog);
        Mockito.lenient().<Class<?>>when(mockCatalog.getPrimaryChildType(monitor)).thenReturn(DBSTable.class);
        Mockito.lenient().when(mockCatalog.getChild(monitor, "table_test")).thenReturn(mockEntity);

        // Datasource 2. Schema-table structure
        Mockito.lenient().when(mockRemoteInstanceSchema.getDataSource()).thenReturn(mockDataSourceSchemaTable);
        Mockito.lenient().when(mockDataSourceSchemaTable.getName()).thenReturn("test_name");
        executionContextSchema = new JDBCExecutionContext(mockRemoteInstanceSchema, true);
        Mockito.lenient().<Class<?>>when(mockDataSourceSchemaTable.getPrimaryChildType(monitor)).thenReturn(DBSSchema.class);
        Mockito.lenient().when(mockDataSourceSchemaTable.getChild(monitor, "schema_test")).thenReturn(mockSchema);
        Mockito.lenient().when(mockSchema.getChild(monitor, "table_test")).thenReturn(mockEntity);

        // Datasource 3. Datasource-table structure (like SQLite)
        Mockito.lenient().when(mockRemoteInstanceTable.getDataSource()).thenReturn(mockDataSourceTable);
        Mockito.lenient().when(mockDataSourceTable.getName()).thenReturn("test_name");
        executionContextTable = new JDBCExecutionContext(mockRemoteInstanceTable, true);
        Mockito.lenient().<Class<?>>when(mockDataSourceTable.getPrimaryChildType(monitor)).thenReturn(DBSTable.class);
        Mockito.lenient().when(mockDataSourceTable.getChild(monitor, "table_test")).thenReturn(mockEntity);

        // Datasource 4. Catalog-schema-table structure
        Mockito.lenient().when(mockRemoteInstanceCatalogSchema.getDataSource()).thenReturn(mockDataSourceCatalogSchema);
        Mockito.lenient().when(mockDataSourceCatalogSchema.getName()).thenReturn("test_name");
        executionContextCatalogSchema = new JDBCExecutionContext(mockRemoteInstanceCatalogSchema, true);
        Mockito.lenient().<Class<?>>when(mockDataSourceCatalogSchema.getPrimaryChildType(monitor)).thenReturn(DBSCatalog.class);
        Mockito.lenient().when(mockDataSourceCatalogSchema.getChild(monitor, "catalog_test")).thenReturn(mockCatalog);
        Mockito.lenient().when(mockDataSourceCatalogSchema.getChild(monitor, "DBO")).thenReturn(mockCatalogDBO);
        Mockito.lenient().<Class<?>>when(mockCatalog.getPrimaryChildType(monitor)).thenReturn(DBSSchema.class);
        Mockito.lenient().when(mockCatalog.getChild(monitor, "schema_test")).thenReturn(mockSchema);
        Mockito.lenient().when(mockCatalogDBO.getChild(monitor, "DBO")).thenReturn(mockSchemaDBO);
        Mockito.lenient().<Class<?>>when(mockSchema.getPrimaryChildType(monitor)).thenReturn(DBSTable.class);
        Mockito.lenient().when(mockSchema.getChild(monitor, "table_test")).thenReturn(mockEntity);
        Mockito.lenient().when(mockSchemaDBO.getChild(monitor, "table_test")).thenReturn(mockEntityDBO);
    }

    @Test
    public void checkIdentifiersQuote() throws Exception {
        Assert.assertEquals(DBUtils.getQuotedIdentifier(mockDataSource, "table_name"), "table_name");
        Assert.assertEquals(DBUtils.getQuotedIdentifier(mockDataSource, "table name"), "\"table name\"");
        Assert.assertEquals(DBUtils.getQuotedIdentifier(mockDataSource, "TableName"), "\"TableName\"");
    }

    @Test
    public void testExtractTypeModifiers() throws DBException {
        Assert.assertEquals(new Pair<>("NUMBER", new String[0]), DBUtils.getTypeModifiers("NUMBER"));
        Assert.assertEquals(new Pair<>("NUMBER", new String[]{"5"}), DBUtils.getTypeModifiers("NUMBER(5)"));
        Assert.assertEquals(new Pair<>("NUMBER", new String[]{"10", "5"}), DBUtils.getTypeModifiers("NUMBER(10,   5)"));
        Assert.assertEquals(new Pair<>("NUMBER", new String[]{"10", "5", "TEST"}), DBUtils.getTypeModifiers("NUMBER (10, 5, TEST)"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER()"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER("));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER)"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("NUMBER(5, 10"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("(5, 10)"));
        Assert.assertThrows(DBException.class, () -> DBUtils.getTypeModifiers("()"));
    }

    @Test
    public void testMainServices() {

/*
        org.eclipse.equinox.launcher.Main.main(new String[] {
            "-product", "org.jkiss.dbeaver.product"}
        );
        System.out.println("OSGI started");
*/
    }

    @Test
    public void testGetCatalogWithEntityByPath() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContext,
            mockDataSource,
            "catalog_test",
            null,
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testGetCatalogWithEntityByPathWithSchemaNameOnly() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContext,
            mockDataSource,
            null,
            "catalog_test",
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testReturnDatasourceIfNoNamesWithSearchByPath() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContext,
            mockDataSource,
            null,
            null,
            null);
        Assert.assertEquals(dbsObject, mockDataSource);
    }

    @Test
    public void testGetSchemaWithEntityByPath() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContextSchema,
            mockDataSourceSchemaTable,
            null,
            "schema_test",
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testGetSchemaWithEntityByPathIgnoreCatalogName() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContextSchema,
            mockDataSourceSchemaTable,
            "catalog_name",
            "schema_test",
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testGetEntityByPathFromDatasource() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContextTable,
            mockDataSourceTable,
            null,
            null,
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testGetEntityByPathIgnoreCatalogName() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContextTable,
            mockDataSourceTable,
            "catalog_name",
            null,
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testGetEntityByPathWithAllNames() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContextCatalogSchema,
            mockDataSourceCatalogSchema,
            "catalog_test",
            "schema_test",
            "table_test");
        Assert.assertEquals(dbsObject, mockEntity);
    }

    @Test
    public void testGetEntityByPathWithEqualDatabaseAndSchemaNames() throws DBException {
        DBSObject dbsObject = DBUtils.getObjectByPath(
            monitor,
            executionContextCatalogSchema,
            mockDataSourceCatalogSchema,
            "DBO",
            "DBO",
            "table_test");
        Assert.assertEquals(dbsObject, mockEntityDBO);
        Assert.assertNotEquals(dbsObject, mockEntity);
    }

}