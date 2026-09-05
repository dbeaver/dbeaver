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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.Collections;
import java.util.List;

public class PostgreAlterTableColumnTest extends DBeaverUnitTest {

    private PostgreDataSource testDataSource;
    private PostgreDatabase testDatabase;
    private PostgreSchema testSchema;
    private PostgreTableRegular testTable;
    private PostgreTableColumn testColumn;
    private PostgreExecutionContext executionContext;

    @Before
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");

        testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return true;
            }

            @Nullable
            @Override
            public PostgreDataType getLocalDataType(String typeName) {
                return super.getLocalDataType(typeName);
            }
        };

        testDatabase = new PostgreDatabase(testDataSource, "testdb");
        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        testSchema = new PostgreSchema(testDatabase, "public", testUser);
        executionContext = new PostgreExecutionContext(testDatabase, "Test");

        testTable = new PostgreTableRegular(testSchema) {
            @Override
            public boolean isTablespaceSpecified() {
                return false;
            }
        };
        testTable.setName("table_name");
        testTable.setPersisted(true);

        testColumn = PostgreTestUtils.addColumn(testTable, "test_col", "varchar", 1);
        testColumn.setMaxLength(100);
        testColumn.setPersisted(true);
    }

    @Test
    public void generateAlterTableChangeVarcharToCharDoesNotUseDoubleCast() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable propertySource = new PropertySourceEditable(commandContext, testColumn, testColumn);
        propertySource.collectProperties();
        propertySource.setPropertyValue(monitor, "fullTypeName", "char(10)");

        PostgreSchema pgCatalog = testDatabase.getCatalogSchema();
        testColumn.setDataType(new PostgreDataType(pgCatalog, Types.OTHER, PostgreConstants.TYPE_CHAR));
        testColumn.setTypeMod(14);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        Assert.assertFalse("Must not generate chained char cast", script.contains("::char::char"));
        Assert.assertTrue(script.contains("USING \"test_col\"::char(10)"));
    }

    @Test
    public void typeCastClauseForInternalCharTypeAddsBareCharCast() throws DBException {
        PostgreSchema pgCatalog = testDatabase.getCatalogSchema();
        PostgreDataType charDataType = new PostgreDataType(pgCatalog, Types.OTHER, PostgreConstants.TYPE_CHAR);

        PostgreDialect dialect = new PostgreDialect();
        String typeCastClause = dialect.getTypeCastClause(charDataType, "test_col", true);
        Assert.assertEquals("test_col::char", typeCastClause);
    }
}
