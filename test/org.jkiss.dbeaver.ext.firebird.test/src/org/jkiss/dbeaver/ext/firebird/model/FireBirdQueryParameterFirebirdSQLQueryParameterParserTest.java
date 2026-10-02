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
package org.jkiss.dbeaver.ext.firebird.model;

import org.eclipse.jface.text.Document;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLQueryParameterParser;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class FireBirdQueryParameterFirebirdSQLQueryParameterParserTest extends DBeaverUnitTest {
    @NotNull
    private static final String BLOCK = """
        EXECUTE BLOCK (x INT = ?, y INT = ?)
        RETURNS (result INT)
        AS
        DECLARE var_x INT = 0;
        BEGIN
          SELECT 1 FROM RDB$DATABASE WHERE 1 = :var_x INTO :result;
          -- ? inside a comment
          result = x + y;
          SUSPEND;
        END
        """;

    @Test
    public void marksOnlyInputMarkersNativeWhenAnonymousParametersAreDisabled() {
        List<SQLQueryParameter> parameters = parse(BLOCK, false);

        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(2, parameters.size());
        Assertions.assertEquals(List.of(BLOCK.indexOf('?'), BLOCK.indexOf('?', BLOCK.indexOf('?') + 1)),
            parameters.stream().map(SQLQueryParameter::getTokenOffset).toList());
        Assertions.assertTrue(parameters.stream().allMatch(SQLQueryParameter::isNativeBinding));
    }

    @Test
    public void doesNotDuplicateInputMarkersWhenAnonymousParametersAreEnabled() {
        List<SQLQueryParameter> parameters = parse(BLOCK, true);

        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(2, parameters.size());
        Assertions.assertTrue(parameters.stream().allMatch(SQLQueryParameter::isNativeBinding));
    }

    @Test
    public void preservesExecuteProcedureParameterHandling() {
        Assertions.assertNull(parse("EXECUTE PROCEDURE demo(:arg, ?)", true));
    }

    @Test
    public void retainsNamedParametersWithoutEnablingAnonymousParametersOutsideBlocks() {
        List<SQLQueryParameter> parameters = parse("SELECT :id, ? FROM RDB$DATABASE", false);

        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(1, parameters.size());
        Assertions.assertEquals("id", parameters.get(0).getName());
        Assertions.assertFalse(parameters.get(0).isNativeBinding());
    }

    @Test
    public void usesInlineParametersOutsideBlocksWhenAnonymousParametersAreEnabled() {
        List<SQLQueryParameter> parameters = parse("SELECT :id, ? FROM RDB$DATABASE", true);

        Assertions.assertNotNull(parameters);
        Assertions.assertEquals(List.of("id", "?"), parameters.stream().map(SQLQueryParameter::getName).toList());
        Assertions.assertTrue(parameters.stream().noneMatch(SQLQueryParameter::isNativeBinding));
    }

    @Nullable
    private static List<SQLQueryParameter> parse(@NotNull String query, boolean anonymousParametersEnabled) {
        FireBirdDataSource dataSource = Mockito.mock(FireBirdDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        FireBirdSQLDialect dialect = new FireBirdSQLDialect();
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.doCallRealMethod().when(dataSource).getAdapter(SQLQueryParameterParser.class);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        Mockito.when(container.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)).thenReturn(true);
        Mockito.when(preferences.getBoolean(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED))
            .thenReturn(anonymousParametersEnabled);
        Mockito.when(preferences.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX)).thenReturn(":");

        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, preferences);
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        SQLParserContext context = new SQLParserContext(dataSource, syntaxManager, ruleManager, new Document(query));
        return SQLScriptParser.parseParametersAndVariables(context, 0, query.length());
    }
}
