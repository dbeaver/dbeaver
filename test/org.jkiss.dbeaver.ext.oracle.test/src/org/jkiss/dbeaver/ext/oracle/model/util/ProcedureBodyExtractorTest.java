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
import org.jkiss.code.Nullable;
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

public class ProcedureBodyExtractorTest {

    private static final String packageDefinitionTemplate = """
        CREATE OR REPLACE PACKAGE BODY TEST_PACKAGE AS
        %s
        END TEST_PACKAGE;""";

    private final ProcTestCase unknownProc = new ProcTestCase(
        "unknown", DBSProcedureType.UNKNOWN, """
        PROCEDURE unknown IS
        BEGIN
          NULL;
        END unknown;"""
    );

    private final ProcTestCase noNameMatch = new ProcTestCase(
        "no_name_match", DBSProcedureType.PROCEDURE, """
        PROCEDURE unknown IS
        BEGIN
          NULL;
        END unknown;"""
    );

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
          print("false_end;end_false;")
        END;"""
    );

    private final ProcTestCase procWithParams = new ProcTestCase(
        "proc_with_params",
        DBSProcedureType.PROCEDURE,
        """
            PROCEDURE proc_with_params(
               p_id    IN     NUMBER,
               p_name  IN OUT VARCHAR2,
               p_flag  OUT    NUMBER
             ) IS
             BEGIN
               DBMS_OUTPUT.PUT_LINE('proc_with_params');
             END proc_with_params;"""
    );


    private final ProcTestCase simpleFunc = new ProcTestCase(
        "simple_func", DBSProcedureType.FUNCTION, """
        FUNCTION simple_func RETURN NUMBER IS
          l_var NUMBER := 42;
        BEGIN
          RETURN l_var;
        END simple_func;"""
    );

    private final ProcTestCase simpleFuncNoEnd = new ProcTestCase(
        "func_no_end", DBSProcedureType.FUNCTION, """
        FUNCTION func_no_end RETURN NUMBER IS
          l_var NUMBER := 42;
        BEGIN
          RETURN l_var;
        END ;"""
    );

    private final ProcTestCase funcWithParams = new ProcTestCase(
        "func_with_params",
        DBSProcedureType.FUNCTION,
        """
            FUNCTION func_with_params(p_qty IN NUMBER, p_price IN NUMBER DEFAULT 1)\s
              RETURN NUMBER IS
            BEGIN
              RETURN p_qty * p_price;
            END;"""
    );

    private final ProcTestCase simpleOneLineComment = new ProcTestCase(
        "proc_with_simple_comment",
        DBSProcedureType.FUNCTION, """
        FUNCTION proc_with_simple_comment RETURN NUMBER IS
          l_var NUMBER := 42;
          l_msg VARCHAR2(100);
          --END proc_with_simple_comment;
        END;"""
    );

    private final ProcTestCase oneLineComments = new ProcTestCase(
        "proc_with_comment",
        DBSProcedureType.FUNCTION, """
        FUNCTION proc_with_comment RETURN NUMBER IS
        -- extra BEGIN 
        --more BEGIN
          l_var NUMBER := 42; --some end; and another begin loop
          l_msg --sudden inline comment -- included comment VARCHAR2(100);
          --END proc_with_comment;
        END;"""
    );

    private final ProcTestCase simpleMultiLineComment = new ProcTestCase(
        "simple_with_multiline_comment",
        DBSProcedureType.FUNCTION, """
        FUNCTION simple_with_multiline_comment RETURN NUMBER IS
          l_var NUMBER := 42; --some end; and another begin loop
          /*END proc_with_multiline_comment;*/
          /*END simple_with_multiline_comment;
          END simple_with_multiline_comment;
          */
        END;"""
    );

    private final ProcTestCase multiLineComments = new ProcTestCase(
        "proc_with_multiline_comment",
        DBSProcedureType.FUNCTION, """
        FUNCTION proc_with_multiline_comment RETURN NUMBER IS
        /* extra BEGIN 
         more BEGIN
         */
          l_var NUMBER := 42; --some end; and another begin loop
          /* l_msg VARCHAR2(100);
          END proc_with_multiline_comment; */
          /*END proc_with_multiline_comment;*/
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

    private final ProcTestCase tripleNestedBegins = new ProcTestCase(
        "triple_nested_begins", DBSProcedureType.PROCEDURE, """
        PROCEDURE triple_nested_begins IS
        BEGIN                    -- #1 
          BEGIN                  -- #2 
            BEGIN                -- #3 
              NULL;
            END;                 -- Closes #3
          END;                   -- Closes #2
        END;"""
    );

    private final ProcTestCase openClosedBeginsInsideMain = new ProcTestCase(
        "two_nested_inside_main", DBSProcedureType.PROCEDURE, """
        PROCEDURE two_nested_inside_main IS
        BEGIN                       -- first BEGIN
          BEGIN                     -- Nested #1
            NULL;
          END;                      -- Closes nested #1
          BEGIN                     -- Nested #2  
            NULL;
          END;                      -- Closes nested #2
        END;"""
    );

    private final ProcTestCase combinedNestedBegins = new ProcTestCase(
        "combined_nested_begins", DBSProcedureType.PROCEDURE, """
        PROCEDURE combined_nested_begins IS
        BEGIN                          -- Main  #1 BEGIN
          BEGIN                        -- Triple nest #1
            BEGIN                      -- Triple nest #2
              BEGIN                    -- Triple nest #3
                NULL;
              END;                     -- Closes triple #3
            END;                       -- Closes triple #2
          END;                         -- Closes triple #1
        
          BEGIN                        -- Inside main #1
            NULL;
          END;                         -- Closes inside #1
          BEGIN                        -- Inside main #2
            NULL;
          END;                         -- Closes inside #2
        END;"""
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

    private final ProcTestCase tripleNestedProc = new ProcTestCase(
        "nested_proc_end", DBSProcedureType.PROCEDURE, """
        PROCEDURE nested_proc_end IS
        BEGIN
          DECLARE
            PROCEDURE nested_proc_1 IS
              PROCEDURE nested_proc_2 IS
                PROCEDURE nested_proc_3 IS
                BEGIN
                  NULL;
                END ;
              BEGIN
                nested_proc_3;
              END ;
            BEGIN
              nested_proc_2;
            END;
          BEGIN
            nested_proc_1;
          END;
        END;"""
    );

    private final ProcTestCase outerBeginEndInner = new ProcTestCase(
        "outer_proc", DBSProcedureType.PROCEDURE, """
        PROCEDURE outer_proc IS
               PROCEDURE inner_proc_labeled IS
               BEGIN
                 NULL;
               END inner_proc_labeled;
               PROCEDURE inner_proc_unlabeled IS
               BEGIN
                 NULL;
               END;
        
             BEGIN
               inner_proc_unlabeled ;
               inner_proc_labeled;
             END;"""
    );

    private final ProcTestCase tripleNestedFunc = new ProcTestCase(
        "nested_func_end", DBSProcedureType.FUNCTION, """
        FUNCTION nested_func_end RETURN NUMBER IS
        BEGIN
          DECLARE
            FUNCTION nested_func_1 RETURN NUMBER IS
              FUNCTION nested_func_2 RETURN NUMBER IS
                FUNCTION nested_func_3 RETURN NUMBER IS
                BEGIN
                  RETURN 1;
                END;
              BEGIN
                RETURN nested_func_3;
              END;
            BEGIN
              RETURN nested_func_2;
            END;
          BEGIN
            RETURN nested_func_1;
          END;
        END;"""
    );


    private final ProcTestCase outerBeginEndInnerFunc = new ProcTestCase(
        "outer_func", DBSProcedureType.FUNCTION, """
        FUNCTION outer_func RETURN NUMBER IS
               FUNCTION inner_func_labeled RETURN NUMBER IS
               BEGIN
                 RETURN 1;
               END inner_func_labeled;
        
               FUNCTION inner_func_unlabeled RETURN NUMBER IS
               BEGIN
                 RETURN 2;
               END;
        
             BEGIN
               RETURN inner_func_unlabeled + inner_func_labeled;
             END;"""
    );

    // overloads
    private static final String overLoadProcName = "overloaded_proc";
    private final ProcTestCase overloadedProc1 = new ProcTestCase(
        overLoadProcName, DBSProcedureType.PROCEDURE,
        """
            PROCEDURE overloaded_proc(p_id NUMBER) IS
              BEGIN
                DBMS_OUTPUT.PUT_LINE('Single parameter: ' || p_id);
              END;""",
        1
    );

    private final ProcTestCase overloadedProc2 = new ProcTestCase(
        overLoadProcName, DBSProcedureType.PROCEDURE,
        """
            PROCEDURE overloaded_proc(p_id NUMBER, p_name VARCHAR2) IS
            BEGIN
             DBMS_OUTPUT.PUT_LINE('Two parameters: ' || p_id || ', ' || p_name);
            END;""",
        2
    );

    private final ProcTestCase overloadedProc3 = new ProcTestCase(
        overLoadProcName, DBSProcedureType.PROCEDURE,
        """
            
            PROCEDURE overloaded_proc(p_flag CHAR) IS
            BEGIN
             DBMS_OUTPUT.PUT_LINE('Flag parameter: ' || p_flag);
            END;""",
        3
    );

    private static final String overLoadFuncName = "overloaded_func";
    private final ProcTestCase overloadedFunc1 = new ProcTestCase(
        overLoadFuncName, DBSProcedureType.FUNCTION,
        """
            FUNCTION overloaded_func(p_id NUMBER) RETURN VARCHAR2 IS
            BEGIN
              RETURN 'Single parameter: ' || p_id;
            END;""",
        1
    );

    private final ProcTestCase overloadedFunc2 = new ProcTestCase(
        overLoadFuncName, DBSProcedureType.FUNCTION,
        """
            FUNCTION overloaded_func(p_id NUMBER, p_name VARCHAR2) RETURN VARCHAR2 IS
            BEGIN
             RETURN 'Two parameters: ' || p_id || ', ' || p_name;
            END;""",
        2
    );

    private final ProcTestCase overloadedFunc3 = new ProcTestCase(
        overLoadFuncName, DBSProcedureType.FUNCTION,
        """
            FUNCTION overloaded_func(p_flag CHAR) RETURN VARCHAR2 IS
            BEGIN
             RETURN 'Flag parameter: ' || p_flag;
            END;""",
        3
    );


    @Test
    public void unknownFunctionType() {
        // given
        String notEmptyPackageDefinition = constructPackageBody(simpleNoArgsProc);
        ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(unknownProc.procedure, notEmptyPackageDefinition);
        // then
        assertEquals(ProcedureBodyExtractor.NO_DEFINITION_FOUND, procedureBodyExtractor.extractProcBody());
    }

    @Test
    public void notFoundDefinitionTest() {
        // given
        String notEmptyPackageDefinition = constructPackageBody(noNameMatch);
        ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(noNameMatch.procedure, notEmptyPackageDefinition);
        // then
        assertEquals(ProcedureBodyExtractor.NO_DEFINITION_FOUND, procedureBodyExtractor.extractProcBody());
    }

    @Test
    public void simpleProcFuncTest() {
        assertBodyFound(simpleNoArgsProc);
        assertBodyFound(simpleFunc);
    }

    @Test
    public void simpleProcFuncNoLabeledEndTest() {
        assertBodyFound(simpleNoArgsNoEndProc);
        assertBodyFound(procWithParams);

        assertBodyFound(simpleFuncNoEnd);
        assertBodyFound(funcWithParams);
    }

    @Test
    public void commentsTest() {
        assertBodyFound(simpleOneLineComment);
        assertBodyFound(oneLineComments);
        assertBodyFound(simpleMultiLineComment);
        assertBodyFound(multiLineComments);
    }

    @Test
    public void functionStartInOneLineCommentLineIsIgnoredTest() {
        // given
        String funcBodyWithCommentFalseStart = "--PROCEDURE simple_proc IS\n" + simpleNoArgsProc.procBody;
        ProcedureBodyExtractor extractor = new ProcedureBodyExtractor(
            simpleNoArgsProc.procedure,
            packageDefinitionTemplate.formatted(funcBodyWithCommentFalseStart)
        );
        // then
        assertEquals(simpleNoArgsProc.procBody, extractor.extractProcBody());
    }

    @Test
    public void functionStartInMultiLineCommentLineIsIgnoredTest() {
        // given
        String funcBodyWithCommentFalseStart = """
            /*PROCEDURE simple_proc IS;
            PROCEDURE simple_proc IS;
            PROCEDURE simple_proc IS;*/""" + simpleNoArgsProc.procBody;
        ProcedureBodyExtractor extractor = new ProcedureBodyExtractor(
            simpleNoArgsProc.procedure,
            packageDefinitionTemplate.formatted(funcBodyWithCommentFalseStart)
        );
        // then
        assertEquals(simpleNoArgsProc.procBody, extractor.extractProcBody());
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
    public void loopsTest() {
        assertBodyFound(loopEndLoop);
        assertBodyFound(forLoopEndLoop);
    }

    @Test
    public void nestedBeginTest() {
        assertBodyFound(tripleNestedBegins);
    }

    @Test
    public void openClosedNestedBeginsTest() {
        assertBodyFound(openClosedBeginsInsideMain);
    }

    @Test
    public void combinedNestedBegins() {
        assertBodyFound(combinedNestedBegins);
    }

    @Test
    public void whileTest() {
        assertBodyFound(whileLoopEndLoop);
    }

    @Test
    public void declareTest() {
        assertBodyFound(declareEnd);
    }

    @Test
    public void nestedProcedures() {
        assertBodyFound(tripleNestedProc);
        assertBodyFound(outerBeginEndInner);
    }

    @Test
    public void nestedFunctions() {
        assertBodyFound(tripleNestedFunc);
        assertBodyFound(outerBeginEndInnerFunc);
    }

    @Test
    public void overloadedProcsTest() {
        // given
        String packageDefinitionWillAllOverloadedProcs =
            packageDefinitionTemplate
                .formatted(String.join("\n\n", List.of(overloadedProc1.procBody, overloadedProc2.procBody, overloadedProc3.procBody)));

        // then
        assertBodyFound(overloadedProc1, packageDefinitionWillAllOverloadedProcs);
        assertBodyFound(overloadedProc2, packageDefinitionWillAllOverloadedProcs);
        assertBodyFound(overloadedProc3, packageDefinitionWillAllOverloadedProcs);
    }

    @Test
    public void overloadedFunctionsTest() {
        // given
        String packageDefinitionWillAllOverloadedFunctions =
            packageDefinitionTemplate
                .formatted(String.join("\n\n", List.of(overloadedFunc1.procBody, overloadedFunc2.procBody, overloadedFunc3.procBody)));

        // then
        assertBodyFound(overloadedFunc1, packageDefinitionWillAllOverloadedFunctions);
        assertBodyFound(overloadedFunc2, packageDefinitionWillAllOverloadedFunctions);
        assertBodyFound(overloadedFunc3, packageDefinitionWillAllOverloadedFunctions);
    }

    @Test
    public void allProceduresBodyExtractTest() {
        // given
        List<ProcTestCase> allTestCases = new ArrayList<>();
        allTestCases.add(simpleNoArgsProc);
        allTestCases.add(simpleNoArgsNoEndProc);
        allTestCases.add(procWithParams);

        allTestCases.add(simpleFunc);
        allTestCases.add(simpleFuncNoEnd);
        allTestCases.add(funcWithParams);

        allTestCases.add(simpleOneLineComment);
        allTestCases.add(oneLineComments);
        allTestCases.add(simpleMultiLineComment);
        allTestCases.add(multiLineComments);

        allTestCases.add(ifEndIf);
        allTestCases.add(caseEndCase);

        allTestCases.add(forLoopEndLoop);
        allTestCases.add(loopEndLoop);

        allTestCases.add(tripleNestedBegins);
        allTestCases.add(openClosedBeginsInsideMain);
        allTestCases.add(combinedNestedBegins);

        allTestCases.add(whileLoopEndLoop);

        allTestCases.add(declareEnd);

        allTestCases.add(tripleNestedProc);
        allTestCases.add(outerBeginEndInner);

        allTestCases.add(tripleNestedFunc);
        allTestCases.add(outerBeginEndInnerFunc);

        List<ProcTestCase> reversedAllCases = new ArrayList<>(allTestCases);
        Collections.reverse(reversedAllCases);

        // can be tested only in straight order, since overload number works sequentially
        List<ProcTestCase> overloadedCases = List.of(
            overloadedProc1,
            overloadedProc2,
            overloadedProc3,

            overloadedFunc1,
            overloadedFunc2,
            overloadedFunc3
        );
        allTestCases.addAll(overloadedCases);

        String allProcs = allTestCases.stream().map(ptc -> ptc.procBody).collect(Collectors.joining("\n\n"));
        String reversedAllProcs = reversedAllCases.stream().map(ptc -> ptc.procBody).collect(Collectors.joining("\n\n"));

        String packageBody = packageDefinitionTemplate.formatted(allProcs);
        String reversedPackageBody = packageDefinitionTemplate.formatted(reversedAllProcs);

        // then
        for (ProcTestCase procToSearch : allTestCases) {
            ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(procToSearch.procedure, packageBody);
            assertEquals(procToSearch.procBody, procedureBodyExtractor.extractProcBody());
        }

        for (ProcTestCase procToSearch : reversedAllCases) {
            ProcedureBodyExtractor procedureBodyExtractorReversed = new ProcedureBodyExtractor(procToSearch.procedure, reversedPackageBody);
            assertEquals(procToSearch.procBody, procedureBodyExtractorReversed.extractProcBody());
        }
    }

    private void assertBodyFound(@NotNull ProcTestCase testCase) {
        assertBodyFound(testCase, constructPackageBody(testCase));
    }

    private void assertBodyFound(@NotNull ProcTestCase testCase, @NotNull String packageBodyDefinition) {
        ProcedureBodyExtractor procedureBodyExtractor = new ProcedureBodyExtractor(testCase.procedure, packageBodyDefinition);
        // then
        assertEquals(testCase.procBody, procedureBodyExtractor.extractProcBody());
    }

    private String constructPackageBody(@NotNull ProcTestCase testCase) {
        return packageDefinitionTemplate
            .formatted(testCase.procBody);
    }


    private class ProcTestCase {
        private final OracleProcedurePackaged procedure;
        private final String procBody;

        public ProcTestCase(@NotNull String name, @NotNull DBSProcedureType procType, @NotNull String procBody) {
            this(name, procType, procBody, null);
        }

        public ProcTestCase(
            @NotNull String name,
            @NotNull DBSProcedureType procType,
            @NotNull String procBody,
            @Nullable Integer overloadNumber
        ) {
            this.procedure = getProcedure(procType, name, overloadNumber);
            this.procBody = procBody.trim();
        }

        @NotNull
        private OracleProcedurePackaged getProcedure(
            @NotNull DBSProcedureType procType,
            @NotNull String procName,
            @Nullable Integer overloadNumber
        ) {
            OracleProcedurePackaged mockProc = mock(OracleProcedurePackaged.class);
            when(mockProc.getProcedureType()).thenReturn(procType);
            when(mockProc.getName()).thenReturn(procName);
            when(mockProc.getOverloadNumber()).thenReturn(overloadNumber);
            return mockProc;
        }
    }
}
