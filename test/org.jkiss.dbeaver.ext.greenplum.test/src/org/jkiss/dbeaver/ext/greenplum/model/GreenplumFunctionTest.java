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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;

public class GreenplumFunctionTest extends DBeaverUnitTest {
    @Mock
    PostgreSchema mockSchema;

    @Mock
    PostgreDatabase mockDatabase;

    @Mock
    JDBCResultSet mockResults;

    @Mock
    JDBCExecutionContext mockContext;

    @Mock
    GreenplumDataSource mockDataSource;

    @Mock
    DBRProgressMonitor mockMonitor;

    @Mock
    PostgreSchema.TableCache mockTableCache;

    @Mock
    PostgreSchema.ConstraintCache mockConstraintCache;

    private GreenplumTable table;

    private final String exampleDatabaseName = "sampleDatabase";
    private final String exampleSchemaName = "sampleSchema";
    private final String exampleTableName = "sampleTable";
    private PostgreLanguage postgreLanguage;

    @Before
    public void setUp() throws Exception {
//        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockSchema.getName()).thenReturn(exampleSchemaName);
//        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);
//        Mockito.when(mockSchema.getConstraintCache()).thenReturn(mockConstraintCache);

        Mockito.when(mockDataSource.getServerType()).thenReturn(new PostgreServerGreenplum(mockDataSource));
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(false);
//        Mockito.when(mockDataSource.getDefaultInstance()).thenReturn(mockDatabase);

//        Mockito.when(mockDatabase.getName()).thenReturn(exampleDatabaseName);
//        Mockito.when(mockDatabase.getDefaultContext(Mockito.any(), Mockito.anyBoolean())).thenReturn(mockContext);

        Mockito.when(mockResults.getString("proname")).thenReturn("sampleFunction");
        postgreLanguage = new PostgreLanguage(mockDatabase, mockResults);
    }

    @Test
    public void onCreationWithDbResult_whenGreenplumVersionIsSixAndAbove_thenExecutionLocationIsLoaded()
            throws SQLException {
        Mockito.when(mockResults.getString("proexeclocation")).thenReturn("a");

        withGreenplumVersion6AndAbove(() -> {
            GreenplumFunction function = new GreenplumFunction(mockMonitor, mockSchema, mockResults);
            Assert.assertEquals(GreenplumFunction.FunctionExecLocation.a, function.getExecutionLocation());
        });
    }

    @Test
    public void onCreationWithDbResult_whenGreenplumVersionIsBelowSix_thenExecutionLocationIsNull()
            throws SQLException {
        withGreenplumVersionLessThan6(() -> {
            GreenplumFunction function = new GreenplumFunction(mockMonitor, mockSchema, mockResults);
            Assert.assertNull(function.getExecutionLocation());
        });

        Mockito.verify(mockResults, Mockito.times(0)).getString("proexeclocation");
    }

    @Test
    public void onCreation_whenGreenplumVersionIsSixAndAbove_thenExecutionLocationDefaultsToANY() {
        withGreenplumVersion6AndAbove(() -> {
            GreenplumFunction function = new GreenplumFunction(mockSchema);
            Assert.assertEquals(GreenplumFunction.FunctionExecLocation.a, function.getExecutionLocation());
        });
    }

    @Test
    public void onCreation_whenGreenplumVersionIsBelowSix_thenExecutionLocationIsNotSet() {
        withGreenplumVersionLessThan6(() -> {
            GreenplumFunction function = new GreenplumFunction(mockSchema);
            Assert.assertNull(function.getExecutionLocation());
        });
    }

    @Test
    public void generateFunctionDeclaration_whenExecLocationIsNotSupported_thenShouldNotRetainAnyTypeOfExecutionLocationInDeclaration()
            throws SQLException, DBException {
        withGreenplumVersionLessThan6(() -> {
            GreenplumFunction function = new GreenplumFunction(mockMonitor, mockSchema, mockResults);
            Assert.assertFalse(function.generateFunctionDeclaration(postgreLanguage, "someName", "funcBody")
                    .contains("EXECUTE ON"));
        });
    }

    @Test
    public void generateFunctionDeclaration_whenExecLocationIsSupportedAndSetToANY_thenShouldIncludeExecLocationClauseInDeclaration()
            throws SQLException, DBException {
        assertExecuteOnClauseExists("a", "EXECUTE ON ANY");
    }

    @Test
    public void generateFunctionDeclaration_whenExecLocationIsSupportedAndSetToALL_thenShouldIncludeExecLocationClauseInDeclaration()
            throws SQLException, DBException {
        assertExecuteOnClauseExists("s", "EXECUTE ON ALL SEGMENTS");
    }

    @Test
    public void generateFunctionDeclaration_whenExecLocationIsSupportedAndSetToMASTER_thenShouldIncludeExecLocationClauseInDeclaration()
            throws SQLException, DBException {
        assertExecuteOnClauseExists("m", "EXECUTE ON MASTER");
    }

    private void assertExecuteOnClauseExists(String executionLocationCode, String expectedClause) throws SQLException {
        Mockito.when(mockResults.getString("proexeclocation")).thenReturn(executionLocationCode);
        withGreenplumVersion6AndAbove(() -> {
            GreenplumFunction function = new GreenplumFunction(mockMonitor, mockSchema, mockResults);
            Assert.assertTrue(function.generateFunctionDeclaration(postgreLanguage, "someName", "funcBody")
                    .endsWith(expectedClause));
        });
    }

    private void withGreenplumVersion6AndAbove(Runnable testCase) {
        setGreenplumToVersion6();
        testCase.run();
    }

    private void withGreenplumVersionLessThan6(Runnable testCase) {
        setGreenplumToVersionLessThan6();
        testCase.run();
    }

    private void setGreenplumToVersionLessThan6() {
        // Greenplum 6 runs on Postgre 9.4.x
        Mockito.when(mockDataSource.isServerVersionAtLeast(9, 4)).thenReturn(false);
    }

    private void setGreenplumToVersion6() {
        // Greenplum 6 runs on Postgre 9.4.x
        Mockito.when(mockDataSource.isServerVersionAtLeast(9, 4)).thenReturn(true);
    }
}
