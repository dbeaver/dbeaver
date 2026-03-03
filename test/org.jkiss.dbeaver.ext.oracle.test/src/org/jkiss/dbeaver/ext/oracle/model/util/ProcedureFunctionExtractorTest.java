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
package org.jkiss.dbeaver.ext.oracle.model.util;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedurePackaged;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcedureFunctionExtractorTest {


    @Test
    public void notFoundDefinitionTest() {
        // given
        String notEmptyPackageDefinition = constructPackageBody(simpleNoArgsProc);
        ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(unknownProc.procedure, notEmptyPackageDefinition);
        // then
        assertEquals(ProcedureBodyExtractor.NO_DEFINITION_FOUND, procedureBodyExtractor.extractProcBody());
    }

    @Test
    public void simpleProcTest() {
        assertBodyFound(simpleNoArgsProc);
        assertBodyFound(simpleNoArgsNoEndProc);
    }

    @Test
    public void endIfTest() {
        assertBodyFound(ifEndIf);
    }

    @Test
    public void caseEndTest() {
        assertBodyFound(caseEndCase);
    }

    @Test
    public void allProceduresBodyExtractTest() {
        // given
        List<ProcTestCase> allTestCases = new ArrayList<>();
        allTestCases.add(simpleNoArgsProc);
        allTestCases.add(simpleNoArgsNoEndProc);
        allTestCases.add(ifEndIf);
        Collections.shuffle(allTestCases);

        String allProcs = allTestCases.stream().map(ptc -> ptc.procBody).collect(Collectors.joining("\n"));
        String packageBody = packageDefinitionTemplate.formatted(allProcs);
        // then
        for (ProcTestCase testCase : allTestCases) {
            ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(testCase.procedure, packageBody);
            assertEquals(testCase.procBody, procedureBodyExtractor.extractProcBody());
        }
    }

    private void assertBodyFound(@NotNull ProcTestCase testCase) {
        String packageDefinition = constructPackageBody(testCase);
        ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(testCase.procedure, packageDefinition);
        // then
        assertEquals(testCase.procBody, procedureBodyExtractor.extractProcBody());
    }

    private String constructPackageBody(@NotNull ProcTestCase testCase) {
        return packageDefinitionTemplate
            .formatted(testCase.procBody);
    }


    private final String packageDefinitionTemplate = """
        CREATE OR REPLACE PACKAGE BODY TEST_PACKAGE AS
        %s
        END TEST_PACKAGE;""";

    private final ProcTestCase unknownProc = new ProcTestCase("unknown", DBSProcedureType.UNKNOWN, "-- Procedure Body");
    private final ProcTestCase simpleNoArgsProc = new ProcTestCase(
        "simple_proc", DBSProcedureType.PROCEDURE, """
        PROCEDURE simple_proc IS
        BEGIN
          NULL;
        END simple_proc;"""
    );

    private final ProcTestCase simpleNoArgsNoEndProc = new ProcTestCase(
        "simple_no_end_proc", DBSProcedureType.PROCEDURE, """
        PROCEDURE simple_no_end_proc IS
        BEGIN
          NULL;
        END;"""
    );

    private final ProcTestCase ifEndIf = new ProcTestCase(
        "if_end_if", DBSProcedureType.PROCEDURE, """
        PROCEDURE if_end_if IS
        BEGIN
          IF 1=1 THEN
            NULL;
          END IF;
        END ;"""
    );

    private final ProcTestCase caseEndCase = new ProcTestCase(
        "case_end_case", DBSProcedureType.PROCEDURE, """
        PROCEDURE case_end_case IS
        BEGIN
          CASE 1
            WHEN 1 THEN NULL;
          END CASE;
        END ;"""
    );

    private final ProcTestCase loopEndLoop = new ProcTestCase(
        "loop_end_loop", DBSProcedureType.PROCEDURE, """
        PROCEDURE loop_end_loop IS
        BEGIN
          LOOP
            NULL;
            EXIT;
          END LOOP;
        END ;"""
    );

    private final ProcTestCase forLoopEndLoop = new ProcTestCase(
        "for_loop_end_loop", DBSProcedureType.PROCEDURE, """
        PROCEDURE for_loop_end_loop IS
        BEGIN
          FOR i IN 1..1 LOOP
            NULL;
          END LOOP;
        END ;"""
    );

    private final ProcTestCase whileLoopEndLoop = new ProcTestCase(
        "while_loop_end_loop", DBSProcedureType.PROCEDURE, """
        PROCEDURE while_loop_end_loop IS
        BEGIN
          DECLARE
            i NUMBER := 0;
          BEGIN
            WHILE i < 1 LOOP
              NULL;
              i := i + 1;
            END LOOP;
          END;
        END ;"""
    );

    private final ProcTestCase declareEnd = new ProcTestCase(
        "declare_end", DBSProcedureType.PROCEDURE, """
        PROCEDURE declare_end IS
        BEGIN
          DECLARE
            l_var NUMBER;
          BEGIN
            l_var := 42;
          END;
        END ;"""
    );

    private final ProcTestCase nestedProcEnd = new ProcTestCase(
        "nested_proc_end", DBSProcedureType.PROCEDURE, """
        PROCEDURE nested_proc_end IS
        BEGIN
          DECLARE
            PROCEDURE nested_proc IS
            BEGIN
              NULL;
            END nested_proc;
          BEGIN
            nested_proc;
          END;
        END ;"""
    );

    private final ProcTestCase nestedFuncEnd = new ProcTestCase(
        "nested_func_end", DBSProcedureType.PROCEDURE, """
        PROCEDURE nested_func_end IS
        BEGIN
          DECLARE
            FUNCTION nested_func RETURN NUMBER IS
            BEGIN
              RETURN 42;
            END nested_func;
          BEGIN
            NULL;
          END;
        END ;"""
    );


    private class ProcTestCase {
        private final String name;
        private final OracleProcedurePackaged procedure;
        private final String procBody;

        public ProcTestCase(@NotNull String name, @NotNull DBSProcedureType procType, @NotNull String procBody) {
            this.name = name;
            this.procedure = getProcedure(procType, name);
            this.procBody = procBody;
        }

        @NotNull
        private OracleProcedurePackaged getProcedure(@NotNull DBSProcedureType procType, @NotNull String procName) {
            OracleProcedurePackaged mockProc = mock(OracleProcedurePackaged.class);
            when(mockProc.getProcedureType()).thenReturn(procType);
            when(mockProc.getUniqueName()).thenReturn(procName);
            return mockProc;
        }
    }
}
