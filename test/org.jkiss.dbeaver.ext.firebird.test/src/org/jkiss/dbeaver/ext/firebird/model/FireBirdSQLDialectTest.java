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
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
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
import java.util.concurrent.atomic.AtomicInteger;

public class FireBirdSQLDialectTest extends DBeaverUnitTest {

    private FireBirdSQLDialect dialect;

    @BeforeEach
    public void setUp() {
        dialect = new FireBirdSQLDialect();
    }

    @Test
    public void generatesExecutableProcedureCallWithOnlyInputArguments() {
        List<GenericProcedureParameter> parameters = List.of(
            parameter("RET1", DBSProcedureParameterKind.OUT),
            parameter("ARG1", DBSProcedureParameterKind.IN),
            parameter("ARG2", DBSProcedureParameterKind.IN)
        );
        Assertions.assertEquals("execute procedure TEST_PROC(:ARG1, :ARG2);", generateCall(true, false, parameters));
    }

    @Test
    public void generatesSelectableProcedureCallWithCastsOnlyForInputArguments() {
        List<GenericProcedureParameter> parameters = List.of(
            parameter("ARG1", DBSProcedureParameterKind.IN),
            parameter("RET1", DBSProcedureParameterKind.OUT),
            parameter("RET2", DBSProcedureParameterKind.OUT)
        );
        Assertions.assertEquals("select * from TEST_PROC(cast(:ARG1 as INTEGER));", generateCall(false, true, parameters));
    }

    @Test
    public void omitsParenthesesWhenProcedureHasNoInputArguments() {
        List<GenericProcedureParameter> parameters = List.of(parameter("RET1", DBSProcedureParameterKind.OUT));
        Assertions.assertEquals("execute procedure TEST_PROC;", generateCall(true, false, parameters));
        Assertions.assertEquals("select * from TEST_PROC;", generateCall(false, false, parameters));
    }

    @Test
    public void refreshKeepsThePreviousSnapshotUsable() throws DBException {
        GenericStructContainer container = Mockito.mock(GenericStructContainer.class);
        AtomicInteger loads = new AtomicInteger();
        FireBirdProcedure procedure = new FireBirdProcedure(container, "TEST_PROC", null, null, DBSProcedureType.PROCEDURE, null) {
            @NotNull
            @Override
            ProcedureParameters readParameters(@NotNull DBRProgressMonitor monitor) {
                return new ProcedureParameters(List.of(), loads.incrementAndGet() == 1);
            }

            @NotNull
            @Override
            public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
                return getName();
            }
        };
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        var beforeRefresh = procedure.getParameters(monitor);
        Assertions.assertSame(beforeRefresh, procedure.getParameters(monitor));

        procedure.refreshObject(monitor);
        var afterRefresh = procedure.getParameters(monitor);
        StringBuilder sql = new StringBuilder();
        dialect.generateStoredProcedureCall(sql, procedure, beforeRefresh, false);
        dialect.generateStoredProcedureCall(sql, procedure, afterRefresh, false);
        Assertions.assertEquals("execute procedure TEST_PROC;\n\nselect * from TEST_PROC;", sql.toString().stripTrailing());
        Assertions.assertEquals(2, loads.get());
    }

    @Test
    public void refreshDuringLoadDoesNotCacheTheOldSnapshot() throws DBException {
        GenericStructContainer container = Mockito.mock(GenericStructContainer.class);
        AtomicInteger loads = new AtomicInteger();
        FireBirdProcedure procedure = new FireBirdProcedure(container, "TEST_PROC", null, null, DBSProcedureType.PROCEDURE, null) {
            @NotNull
            @Override
            ProcedureParameters readParameters(@NotNull DBRProgressMonitor monitor) throws DBException {
                if (loads.incrementAndGet() == 1) {
                    refreshObject(monitor);
                }
                return new ProcedureParameters(List.of(), false);
            }
        };
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        var oldSnapshot = procedure.getParameters(monitor);
        var newSnapshot = procedure.getParameters(monitor);
        Assertions.assertNotSame(oldSnapshot, newSnapshot);
        Assertions.assertSame(newSnapshot, procedure.getParameters(monitor));
        Assertions.assertEquals(2, loads.get());
    }

    @NotNull
    private String generateCall(boolean executable, boolean castParams, @NotNull List<GenericProcedureParameter> parameters) {
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        GenericStructContainer structContainer = Mockito.mock(GenericStructContainer.class);
        Mockito.when(structContainer.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX)).thenReturn(":");

        FireBirdProcedure procedure = new FireBirdProcedure(structContainer, "TEST_PROC", null, null, DBSProcedureType.PROCEDURE, null) {
            @NotNull
            @Override
            public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
                return getName();
            }
        };
        StringBuilder sql = new StringBuilder();
        dialect.generateStoredProcedureCall(sql, procedure, new FireBirdProcedure.ProcedureParameters(parameters, executable), castParams);
        return sql.toString().stripTrailing();
    }

    @NotNull
    private static GenericProcedureParameter parameter(@NotNull String name, @NotNull DBSProcedureParameterKind kind) {
        GenericProcedureParameter parameter = Mockito.mock(GenericProcedureParameter.class);
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
