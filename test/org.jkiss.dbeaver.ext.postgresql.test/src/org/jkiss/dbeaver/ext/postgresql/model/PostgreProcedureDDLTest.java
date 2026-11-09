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

import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.yellowbrick.PostgreServerYellowBrick;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class PostgreProcedureDDLTest {
    @Test
    public void preservesQuotedParameterNamesAndYellowbrickReturnType() {
        PostgreProcedure procedure = createProcedure(true);
        PostgreDataType integerType = createType(procedure.getSchema(), "int4");
        procedure.params.add(new PostgreProcedureParameter(procedure, "Param1", integerType, PostgreProcedure.ArgumentMode.i, 1));
        procedure.params.add(new PostgreProcedureParameter(procedure, "a\"b", integerType, PostgreProcedure.ArgumentMode.i, 2));
        String ddl = procedure.generateFunctionDeclaration(null, "integer", "BEGIN RETURN 1; END;");
        Assertions.assertTrue(ddl.contains("my_proc(\"Param1\" int4, \"a\"\"b\" int4)"));
        Assertions.assertTrue(ddl.contains("RETURNS integer"));
        Assertions.assertTrue(ddl.contains("CREATE OR REPLACE PROCEDURE"));
    }

    @Test
    public void omitsReturnClauseForPostgresqlProcedures() {
        PostgreProcedure procedure = createProcedure(false);
        Assertions.assertFalse(procedure.generateFunctionDeclaration(null, "void", "").contains("RETURNS"));
    }

    @Test
    public void preservesFunctionReturnClause() {
        PostgreProcedure procedure = createProcedure(false);
        procedure.kind = PostgreProcedureKind.f;
        Assertions.assertTrue(procedure.generateFunctionDeclaration(null, "integer", "").contains("RETURNS integer"));
    }

    @Test
    public void skipsUnnamedParametersAndKeepsDisplayNamesUnquoted() {
        PostgreProcedure procedure = createProcedure(true);
        PostgreDataType type = createType(procedure.getSchema(), "int4");
        List<PostgreProcedureParameter> params = List.of(
            new PostgreProcedureParameter(procedure, "$1", type, PostgreProcedure.ArgumentMode.i, 1));
        Assertions.assertEquals("my_proc(int4)", PostgreProcedure.makeOverloadedName(
            procedure.getSchema(), "my_proc", params, true, true, true));
        Assertions.assertEquals("my_proc($1 int4)", PostgreProcedure.makeOverloadedName(
            procedure.getSchema(), "my_proc", params, false, true, false));
    }

    private PostgreProcedure createProcedure(boolean yellowbrick) {
        PostgreDataSource dataSource = Mockito.mock(PostgreDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(dataSource.getServerType()).thenReturn(yellowbrick
            ? new PostgreServerYellowBrick(dataSource) : new PostgreServerPostgreSQL(dataSource));
        PostgreSchema schema = Mockito.mock(PostgreSchema.class);
        Mockito.when(schema.getDataSource()).thenReturn(dataSource);
        Mockito.when(schema.getName()).thenReturn("public");
        Mockito.when(schema.isPublicSchema()).thenReturn(true);
        PostgreProcedure procedure = new PostgreProcedure(schema);
        procedure.kind = PostgreProcedureKind.p;
        procedure.setName("my_proc");
        return procedure;
    }

    private PostgreDataType createType(PostgreSchema schema, String name) {
        PostgreDataType type = Mockito.mock(PostgreDataType.class);
        Mockito.when(type.getParentObject()).thenReturn(schema);
        Mockito.when(type.getName()).thenReturn(name);
        return type;
    }
}
