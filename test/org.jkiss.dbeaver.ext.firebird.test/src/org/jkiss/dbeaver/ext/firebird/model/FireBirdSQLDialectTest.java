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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class FireBirdSQLDialectTest extends DBeaverUnitTest {

    private FireBirdSQLDialect dialect;

    @BeforeEach
    public void setUp() {
        dialect = new FireBirdSQLDialect();
    }

    @Test
    public void generatesExecutableProcedureCallWithOnlyInputArguments() {
        List<DBSProcedureParameter> parameters = List.of(
            parameter("RET1", DBSProcedureParameterKind.OUT),
            parameter("ARG1", DBSProcedureParameterKind.IN),
            parameter("ARG2", DBSProcedureParameterKind.IN)
        );
        Assertions.assertEquals("execute procedure TEST_PROC(:ARG1, :ARG2);", generateCall(true, false, parameters));
    }

    @Test
    public void generatesSelectableProcedureCallWithCastsOnlyForInputArguments() {
        List<DBSProcedureParameter> parameters = List.of(
            parameter("ARG1", DBSProcedureParameterKind.IN),
            parameter("RET1", DBSProcedureParameterKind.OUT),
            parameter("RET2", DBSProcedureParameterKind.OUT)
        );
        Assertions.assertEquals("select * from TEST_PROC(cast(:ARG1 as INTEGER));", generateCall(false, true, parameters));
    }

    @Test
    public void omitsParenthesesWhenProcedureHasNoInputArguments() {
        List<DBSProcedureParameter> parameters = List.of(parameter("RET1", DBSProcedureParameterKind.OUT));
        Assertions.assertEquals("execute procedure TEST_PROC;", generateCall(true, false, parameters));
        Assertions.assertEquals("select * from TEST_PROC;", generateCall(false, false, parameters));
    }

    @Test
    public void reloadsCachedProcedureParametersAfterRefresh() throws DBException {
        GenericStructContainer container = Mockito.mock(GenericStructContainer.class);
        FireBirdProcedure procedure = Mockito.spy(new FireBirdProcedure(
            container, "TEST_PROC", "TEST_PROC", null, DBSProcedureType.PROCEDURE, null));
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Mockito.doNothing().when(procedure).loadProcedureColumns(monitor);

        procedure.getParameters(monitor);
        procedure.getParameters(monitor);
        Mockito.verify(procedure).loadProcedureColumns(monitor);

        procedure.refreshObject(monitor);
        procedure.getParameters(monitor);
        Mockito.verify(procedure, Mockito.times(2)).loadProcedureColumns(monitor);
    }

    @NotNull
    private String generateCall(boolean executable, boolean castParams, @NotNull List<DBSProcedureParameter> parameters) {
        FireBirdProcedure procedure = Mockito.mock(FireBirdProcedure.class);
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(procedure.getProcedureType()).thenReturn(DBSProcedureType.PROCEDURE);
        Mockito.when(procedure.getFullyQualifiedName(DBPEvaluationContext.DML)).thenReturn("TEST_PROC");
        Mockito.when(procedure.isExecutable()).thenReturn(executable);
        Mockito.when(procedure.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX)).thenReturn(":");

        StringBuilder sql = new StringBuilder();
        dialect.generateStoredProcedureCall(sql, procedure, parameters, castParams);
        return sql.toString().stripTrailing();
    }

    @NotNull
    private static DBSProcedureParameter parameter(@NotNull String name, @NotNull DBSProcedureParameterKind kind) {
        DBSProcedureParameter parameter = Mockito.mock(DBSProcedureParameter.class);
        DBSTypedObject type = Mockito.mock(DBSTypedObject.class);
        Mockito.when(parameter.getName()).thenReturn(name);
        Mockito.when(parameter.getParameterKind()).thenReturn(kind);
        Mockito.when(parameter.getParameterType()).thenReturn(type);
        Mockito.when(type.getFullTypeName()).thenReturn("INTEGER");
        return parameter;
    }

    // ---- Block headers and bounds ----

    @Test
    public void testBlockHeaderStringsContainExecuteBlock() {
        String[] headers = dialect.getBlockHeaderStrings();
        Assertions.assertNotNull(headers, "Block headers should not be null");
        List<String> headerList = Arrays.asList(headers);
        Assertions.assertTrue(headerList.contains("EXECUTE BLOCK"), "Block headers must contain EXECUTE BLOCK");
    }

    @Test
    public void testBlockBoundStringsContainBeginEnd() {
        String[][] bounds = dialect.getBlockBoundStrings();
        Assertions.assertNotNull(bounds, "Block bounds should not be null");
        Assertions.assertEquals(1, bounds.length, "Should have exactly one BEGIN/END pair");
        Assertions.assertEquals("BEGIN", bounds[0][0]);
        Assertions.assertEquals("END", bounds[0][1]);
    }

    // ---- Boolean behavior methods ----

    @Test
    public void testSupportsAliasInSelect() {
        Assertions.assertTrue(dialect.supportsAliasInSelect(), "Firebird supports alias in SELECT");
    }

    @Test
    public void testSupportsAliasInHaving() {
        Assertions.assertFalse(dialect.supportsAliasInHaving(), "Firebird does not support alias in HAVING");
    }

    @Test
    public void testSupportsInsertAllDefaultValues() {
        Assertions.assertTrue(dialect.supportsInsertAllDefaultValuesStatement(),
            "Firebird supports INSERT with all default values");
    }

    // ---- Identifier handling ----

    @Test
    public void testValidIdentifierPartAcceptsDollarSign() {
        Assertions.assertTrue(dialect.validIdentifierPart('$', false), "Dollar sign must be valid in identifiers");
    }

    @Test
    public void testValidIdentifierPartAcceptsUnderscore() {
        Assertions.assertTrue(dialect.validIdentifierPart('_', false), "Underscore must be valid in identifiers");
    }

    @Test
    public void testValidIdentifierPartRejectsSpace() {
        Assertions.assertFalse(dialect.validIdentifierPart(' ', false), "Space must not be valid in unquoted identifiers");
    }

    // ---- Keyword array structure (via reflection) ----

    @Test
    public void testFirebirdKeywordsArrayIsSorted() throws Exception {
        String[] keywords = getStaticStringArray("FIREBIRD_KEYWORDS");
        for (int i = 1; i < keywords.length; i++) {
            Assertions.assertTrue(keywords[i - 1].compareTo(keywords[i]) < 0,
                "Keywords array must be sorted alphabetically: '" + keywords[i - 1] + "' before '" + keywords[i] + "'");
        }
    }

    // ---- Helper methods ----

    private String[] getStaticStringArray(String fieldName) throws Exception {
        Field field = FireBirdSQLDialect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
}
