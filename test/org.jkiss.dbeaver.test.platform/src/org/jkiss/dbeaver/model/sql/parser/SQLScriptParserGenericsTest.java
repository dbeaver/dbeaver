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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.Document;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SQLScriptParserGenericsTest {
    @Mock
    private DBPDriver driver;
    @Mock
    private GenericDataSource dataSource;
    @Mock
    private GenericMetaModel metaModel;
    @Mock
    private DBPDataSourceContainer dataSourceContainer;
    @Mock
    private DBCExecutionContext executionContext;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCDatabaseMetaData databaseMetaData;

    @Before
    public void init() {
        DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.lenient().when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.lenient().when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
        Mockito.when(dataSourceContainer.getDriver()).thenReturn(driver);
        Mockito.lenient().when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(driver.getDriverParameter(Mockito.anyString())).thenReturn(null);
        Mockito.when(dataSource.getMetaModel()).thenReturn(metaModel);
        Mockito.when(metaModel.supportsUpsertStatement()).thenReturn(false);
    }
    
    @Test
    public void parseBeginTransaction() throws DBException {
        assertParse("snowflake",
            "begin transaction;\nselect 1 from dual;",
            new String[]{"begin transaction", "select 1 from dual"}
        );
    }
    
    @Test
    public void parseFromCursorPositionBeginTransaction() throws DBException {
        String query = "begin transaction;\nselect 1 from dual;";
        SQLScriptElement element;
        SQLParserContext context = createParserContext(setDialect("snowflake"), query);
        int[] positions = new int[]{4, 18};
        for (int pos : positions) {
            element = SQLScriptParser.parseQuery(context, 0, query.length(), pos, false, false);
            Assert.assertEquals("begin transaction", element.getText());
        }
    }
    
    private void assertParse(String dialectName, String query, String[] expected) throws DBException {
        SQLParserContext context = createParserContext(setDialect(dialectName), query);
        int docLen = context.getDocument().getLength();
        List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(context, 0, docLen, false, false, false);
        Assert.assertEquals(expected.length, elements.size());
        for (int index = 0; index < expected.length; index++) {
            Assert.assertEquals(expected[index], elements.get(index).getText());
        }
    }

    private SQLParserContext createParserContext(SQLDialect dialect, String query) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, dataSourceContainer.getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        return new SQLParserContext(dataSource, syntaxManager, ruleManager, new Document(query));
    }

    private SQLDialect setDialect(String name) throws DBException {
        SQLDialectRegistry registry = SQLDialectRegistry.getInstance();
        SQLDialect dialect = registry.getDialect(name).createInstance();
        ((JDBCSQLDialect) dialect).initDriverSettings(session, dataSource, databaseMetaData);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        return dialect;
    }

}
