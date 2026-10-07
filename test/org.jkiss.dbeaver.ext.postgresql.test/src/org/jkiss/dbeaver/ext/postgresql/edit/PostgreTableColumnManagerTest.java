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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreExecutionContext;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeign;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class PostgreTableColumnManagerTest extends DBeaverUnitTest {

    private PostgreDataSource testDataSource;
    private PostgreSchema testSchema;
    private PostgreExecutionContext postgreExecutionContext;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");

        testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return true;
            }
        };

        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(monitor, "testdb", testUser, null, null, null);
        testSchema = new PostgreSchema(testDatabase, "test_schema", testUser);
        postgreExecutionContext = new PostgreExecutionContext(testDatabase, "Test");
    }

    @Test
    public void generateUsingClauseWhenChangingTypeOfRegularTableColumn() throws DBException {
        PostgreTableRegular table = new PostgreTableRegular(testSchema) {
            @Override
            public boolean isTablespaceSpecified() {
                return false;
            }
        };
        table.setName("test_table_regular");
        table.setPartition(false);
        table.setPersisted(true);
        PostgreTableColumn column = PostgreTestUtils.addColumn(table, "column1", "numeric", 1);
        column.setPersisted(true);

        String script = generateAlterColumnTypeScript(column);

        Assertions.assertTrue(script.startsWith("ALTER TABLE test_schema.test_table_regular ALTER COLUMN column1 TYPE "), script);
        Assertions.assertTrue(script.contains(" USING "), script);
    }

    @Test
    public void doNotGenerateUsingClauseWhenChangingTypeOfForeignTableColumn() throws DBException {
        PostgreTableForeign table = new PostgreTableForeign(testSchema);
        table.setName("test_foreign_table");
        table.setPersisted(true);
        PostgreTableColumn column = PostgreTestUtils.addColumn(table, "column1", "numeric", 1);
        column.setPersisted(true);

        String script = generateAlterColumnTypeScript(column);

        Assertions.assertTrue(script.startsWith("ALTER FOREIGN TABLE test_schema.test_foreign_table ALTER COLUMN column1 TYPE "), script);
        Assertions.assertFalse(script.contains(" USING "), script);
    }

    private String generateAlterColumnTypeScript(PostgreTableColumn column) throws DBException {
        TestCommandContext commandContext = new TestCommandContext(postgreExecutionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, column, column);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "fullTypeName", "numeric(38, 8)");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor, commandContext, postgreExecutionContext, Collections.emptyMap(), null);
        return SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);
    }
}