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
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.StringJoiner;

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
    
    @Test
    public void parseSnowflakeCreateProcedureWithIfStatements() throws DBException {
        String[] query = new String[]{ 
            "CREATE OR REPLACE PROCEDURE testproc()\n"
            + "RETURNS varchar\n"
            + "LANGUAGE SQL AS\n"
            + "$$\n"
            + "  DECLARE\n"
            + "    i int;\n"
            + "  BEGIN\n"
            + "    i:=1;\n"
            + "    IF (i=1) THEN\n"
            + "      i:=2;\n"
            + "    END IF;\n"
            + "    IF (i=2) THEN\n"
            + "      i:=3;\n"
            + "    END IF;\n"
            + "  END\n"
            + "$$"
        };
        assertParse("snowflake", query);
    }

    @Test
    public void parseNamedParameters() throws DBException {
        List<String> expectedParamNames = List.of(":1", ":\"SYS_B_1\"", ":\"MyVar8\"", ":ABC", ":\"#d2\"");
        List<String> invalidParamNames = List.of("&6^34", ":%#2", ":\"\"\"\"");
        StringJoiner joiner = new StringJoiner(", ", "select ", " from dual");
        expectedParamNames.stream().forEach(joiner::add);
        invalidParamNames.stream().forEach(joiner::add);
        String query = joiner.toString();
        SQLParserContext context = createParserContext(setDialect("snowflake"), query);
        List<SQLQueryParameter> params = SQLScriptParser.parseParametersAndVariables(context, 0, query.length());
        List<String> actualParamNames = new ArrayList<String>();
        for (SQLQueryParameter sqlQueryParameter : params) {
            actualParamNames.add(sqlQueryParameter.getName());
        }
        Assert.assertEquals(expectedParamNames, actualParamNames);
    }
    
    private void assertParse(String dialectName, String[] expected) throws DBException {
        String source = Arrays.stream(expected).filter(e -> e != null).collect(Collectors.joining());
        List<String> expectedParts = new ArrayList<>(expected.length);
        for (int i = 0; i < expected.length; i++) {
            if (i + 1 < expected.length && expected[i + 1] == null) {
                expectedParts.add(expected[i].replaceAll("[\\;]+$", ""));
                i++;
            } else {
                expectedParts.add(expected[i]);
            }
        }
        assertParse(dialectName, source, expectedParts.toArray(new String[0]));
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
