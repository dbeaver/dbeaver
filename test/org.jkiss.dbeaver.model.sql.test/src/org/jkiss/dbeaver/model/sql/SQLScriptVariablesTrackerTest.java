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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.tracking.SQLScriptVariablesTracker;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SQLScriptVariablesTrackerTest extends DBeaverUnitTest {
    @Test
    public void orderedTrackerReportsInclusiveCoveredPosition() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        fixture.tracker.trackElements(List.of(variableQuery("DECLARE @value INT", 10, 18)));

        Assertions.assertEquals(-1, fixture.tracker.getLastCoveredPosition());
        fixture.visibleAt(40);
        Assertions.assertEquals(40, fixture.tracker.getLastCoveredPosition());

        fixture.tracker.applyDelta(30, 1, 0);
        Assertions.assertEquals(29, fixture.tracker.getLastCoveredPosition());
    }

    @Test
    public void nonOrderedTrackerDoesNotRequestGapRecovery() {
        Assertions.assertEquals(
            Integer.MAX_VALUE,
            createTracker(SQLScriptVariableScope.SESSION).tracker.getLastCoveredPosition()
        );
    }

    @Test
    public void lookupAtStatementStartDoesNotAnalyzeCurrentOrFutureStatements() {
        CountingProgressMonitor monitor = new CountingProgressMonitor();
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT, monitor);
        fixture.tracker.trackElements(List.of(
            variableQuery("DECLARE @first INT", 0, 18),
            variableQuery("DECLARE @second INT", 30, 19)
        ));

        Assertions.assertTrue(fixture.visibleAt(0).getVariables().isEmpty());
        Assertions.assertEquals(1, monitor.getCheckCount());
    }

    @Test
    public void normallyAnalyzedResultIsReusedByTracker() {
        CountingProgressMonitor monitor = new CountingProgressMonitor();
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT, monitor);
        SQLQuery query = variableQuery("DECLARE @value INT", 0, 18);
        fixture.tracker.trackElements(List.of(query));

        SQLQueryVariablesSubset inputVariables = fixture.visibleAt(0);
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            query.getOriginalText(),
            inputVariables
        );
        Assertions.assertNotNull(model);
        Assertions.assertNull(inputVariables.getIntroducedVariables());
        SQLQueryVariableStatementModel statement = Assertions.assertInstanceOf(
            SQLQueryVariableStatementModel.class,
            model.getQueryModel()
        );
        SQLQueryVariablesSubset resultingVariables = statement.getResultingVariables();
        Assertions.assertNotNull(resultingVariables.getIntroducedVariables());
        Assertions.assertEquals(
            SQLQueryVariableInfo.OperationKind.DECLARATION,
            resultingVariables.getIntroducedVariables().data.operationKind()
        );
        Assertions.assertNull(resultingVariables.getIntroducedVariables().next);
        fixture.tracker.acceptAnalysisResult(query, model);
        int checksBeforeLookup = monitor.getCheckCount();

        SQLQueryVariablesSubset visibleVariables = fixture.visibleAt(20);
        Assertions.assertEquals(List.of("@value"), names(visibleVariables));
        Assertions.assertNull(visibleVariables.getIntroducedVariables());
        Assertions.assertEquals(checksBeforeLookup + 1, monitor.getCheckCount());
    }

    @Test
    public void scriptAcceptsModelWhenParserHintIsMissing() {
        CountingProgressMonitor monitor = new CountingProgressMonitor();
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT, monitor);
        String text = "DECLARE @value INT";
        SQLQuery query = new SQLQuery(null, text, 0, text.length());
        Assertions.assertFalse(query.hasVariableKeyword());
        fixture.tracker.trackElements(List.of(query));

        SQLQueryModel model = recognizeVariableStatement(fixture, query);
        fixture.tracker.acceptAnalysisResult(query, model);
        int checksBeforeLookup = monitor.getCheckCount();

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(30)));
        Assertions.assertEquals(checksBeforeLookup + 1, monitor.getCheckCount());
    }

    @Test
    public void batchAcceptsModelWhenParserHintIsMissing() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        String text = "DECLARE @value INT";
        SQLQuery query = new SQLQuery(null, text, 0, text.length());
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 30, 2);
        Assertions.assertFalse(query.hasVariableKeyword());
        fixture.tracker.trackElements(List.of(query, delimiter));

        fixture.tracker.acceptAnalysisResult(query, recognizeVariableStatement(fixture, query));

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(29)));
        Assertions.assertTrue(fixture.visibleAt(30).getVariables().isEmpty());
    }

    @Test
    public void latePromotionReusesAcceptedModel() {
        CountingProgressMonitor monitor = new CountingProgressMonitor();
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT, monitor);
        String text = "DECLARE @value INT";
        SQLQuery query = new SQLQuery(null, text, 10, text.length());
        fixture.tracker.trackElements(List.of(query));
        Assertions.assertTrue(fixture.visibleAt(40).getVariables().isEmpty());

        SQLQueryModel model = recognizeVariableStatement(fixture, query);
        fixture.tracker.acceptAnalysisResult(query, model);
        int checksBeforeLookup = monitor.getCheckCount();

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(50)));
        Assertions.assertEquals(checksBeforeLookup + 1, monitor.getCheckCount());
    }

    @Test
    public void reconciliationOfAnalyzedStatementRemainsConservative() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        String text = "DECLARE @value INT";
        SQLQuery query = new SQLQuery(null, text, 0, text.length());
        fixture.tracker.acceptAnalysisResult(query, recognizeVariableStatement(fixture, query));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(30)));

        fixture.tracker.reconcileElements(0, text.length(), List.of(query));

        Assertions.assertTrue(fixture.visibleAt(30).getVariables().isEmpty());
        fixture.tracker.acceptAnalysisResult(query, recognizeVariableStatement(fixture, query));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(30)));
    }

    @Test
    public void acceptedBatchAssignmentDoesNotIntroduceVariable() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery query = variableQuery("SET @value = 1", 0, 14);
        fixture.tracker.trackElements(List.of(query));

        SQLQueryVariablesSubset inputVariables = fixture.visibleAt(0);
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            query.getOriginalText(),
            inputVariables
        );
        Assertions.assertNotNull(model);
        SQLQueryVariableStatementModel statement = Assertions.assertInstanceOf(
            SQLQueryVariableStatementModel.class,
            model.getQueryModel()
        );
        Assertions.assertNull(statement.getResultingVariables().getIntroducedVariables());
        fixture.tracker.acceptAnalysisResult(query, model);

        Assertions.assertTrue(fixture.visibleAt(20).getVariables().isEmpty());
    }

    @Test
    public void acceptedSessionAssignmentPreservesDeclaredType() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLQuery assignment = variableQuery("SET @value = 1", 30, 14);
        fixture.tracker.trackElements(List.of(declaration, assignment));

        SQLQueryVariablesSubset declarationInput = fixture.visibleAt(0);
        SQLQueryModel declarationModel = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            declaration.getOriginalText(),
            declarationInput
        );
        Assertions.assertNotNull(declarationModel);
        Assertions.assertTrue(declarationInput.getVariables().isEmpty());
        fixture.tracker.acceptAnalysisResult(declaration, declarationModel);
        Assertions.assertEquals(List.of("@value"), names(declarationInput));

        SQLQueryVariablesSubset assignmentInput = fixture.visibleAt(assignment.getOffset());
        Assertions.assertSame(declarationInput, assignmentInput);
        SQLQueryModel assignmentModel = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            assignment.getOriginalText(),
            assignmentInput
        );
        Assertions.assertNotNull(assignmentModel);
        fixture.tracker.acceptAnalysisResult(assignment, assignmentModel);

        Assertions.assertEquals("INT", firstVariable(fixture.visibleAt(60)).type().getDisplayName());
    }

    @Test
    public void standaloneBatchDelimiterResetsVariables() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 27, 2);
        fixture.tracker.trackElements(List.of(declaration, delimiter));

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(26)));
        Assertions.assertTrue(fixture.visibleAt(27).getVariables().isEmpty());
    }

    @Test
    public void reconciliationCanRemoveStandaloneBatchDelimiter() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 27, 2);
        fixture.tracker.trackElements(List.of(declaration, delimiter));
        Assertions.assertTrue(fixture.visibleAt(40).getVariables().isEmpty());

        fixture.tracker.reconcileElements(27, 2, List.of());

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(40)));
    }

    @Test
    public void reconciliationOnlyRemovesElementsOverlappingRange() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        SQLQuery firstDeclaration = variableQuery("DECLARE @first INT", 0, 18);
        SQLQuery middleDeclaration = variableQuery("DECLARE @middle INT", 30, 19);
        SQLQuery lastDeclaration = variableQuery("DECLARE @last INT", 60, 17);
        fixture.tracker.trackElements(List.of(firstDeclaration, middleDeclaration, lastDeclaration));
        Assertions.assertEquals(List.of("@first", "@last", "@middle"), names(fixture.visibleAt(80)));

        fixture.tracker.reconcileElements(31, 1, List.of());

        Assertions.assertEquals(List.of("@first"), names(fixture.visibleAt(59)));
        Assertions.assertEquals(List.of("@first", "@last"), names(fixture.visibleAt(80)));
    }

    @Test
    public void pendingReconciliationRangeFollowsDocumentDelta() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 20, 18);
        fixture.tracker.trackElements(List.of(declaration));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(50)));

        fixture.tracker.reconcileElements(20, 18, List.of());
        fixture.tracker.applyDelta(0, 0, 50);

        Assertions.assertTrue(fixture.visibleAt(100).getVariables().isEmpty());
    }

    @Test
    public void pendingReconciliationRangeExpandsWithOverlappingReplacement() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 55, 18);
        fixture.tracker.trackElements(List.of(declaration));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(100)));

        fixture.tracker.reconcileElements(30, 30, List.of());
        fixture.tracker.applyDelta(35, 5, 20);

        Assertions.assertTrue(fixture.visibleAt(120).getVariables().isEmpty());
    }

    @Test
    public void emptyDocumentReconciliationClearsTrackedElementsOnDrain() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        fixture.tracker.trackElements(List.of(variableQuery("DECLARE @value INT", 0, 18)));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(30)));

        fixture.tracker.reconcileElements(0, 0, List.of());

        Assertions.assertTrue(fixture.visibleAt(30).getVariables().isEmpty());
    }

    @Test
    public void reconciliationFromDamageOffsetPreservesAnalyzedPrefix() {
        CountingProgressMonitor monitor = new CountingProgressMonitor();
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT, monitor);
        SQLQuery firstDeclaration = variableQuery("DECLARE @first INT", 0, 18);
        SQLQuery secondDeclaration = variableQuery("DECLARE @second INT", 30, 19);
        fixture.tracker.trackElements(List.of(firstDeclaration, secondDeclaration));
        acceptVariableStatement(fixture, firstDeclaration);
        Assertions.assertEquals(List.of("@first"), names(fixture.visibleAt(29)));
        int checksBeforeReconciliation = monitor.getCheckCount();

        fixture.tracker.reconcileElements(30, 100, List.of(firstDeclaration, secondDeclaration));

        Assertions.assertEquals(List.of("@first"), names(fixture.visibleAt(29)));
        Assertions.assertEquals(checksBeforeReconciliation, monitor.getCheckCount());
    }

    @Test
    public void reconciliationKeepsElementStartingAtRangeEnd() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 30, 18);
        fixture.tracker.trackElements(List.of(declaration));

        fixture.tracker.reconcileElements(20, 10, List.of());

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(50)));
    }

    @Test
    public void modificationBeforeStandaloneBatchDelimiterPreservesIt() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 27, 2);
        fixture.tracker.trackElements(List.of(declaration, delimiter));
        Assertions.assertTrue(fixture.visibleAt(40).getVariables().isEmpty());

        fixture.tracker.applyDelta(25, 1, 1);

        Assertions.assertTrue(fixture.visibleAt(40).getVariables().isEmpty());
    }

    @Test
    public void modificationInsideStandaloneBatchDelimiterInvalidatesItUntilObservedAgain() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 27, 2);
        fixture.tracker.trackElements(List.of(declaration, delimiter));
        Assertions.assertTrue(fixture.visibleAt(40).getVariables().isEmpty());

        fixture.tracker.applyDelta(28, 1, 1);

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(40)));
        fixture.tracker.trackElements(List.of(delimiter));
        Assertions.assertTrue(fixture.visibleAt(40).getVariables().isEmpty());
    }

    @Test
    public void sessionRedefinitionUsesLatestAcceptedDefinition() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery firstDeclaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLQuery secondDeclaration = variableQuery("DECLARE @VALUE DATE", 30, 19);
        fixture.tracker.trackElements(List.of(firstDeclaration, secondDeclaration));

        acceptVariableStatement(fixture, secondDeclaration);
        Assertions.assertEquals("DATE", firstVariable(fixture.visibleAt(0)).type().getDisplayName());
        acceptVariableStatement(fixture, firstDeclaration);
        SQLQueryVariableInfo latestDefinition = firstVariable(fixture.visibleAt(0));
        Assertions.assertEquals("INT", latestDefinition.type().getDisplayName());
        Assertions.assertEquals("@value", latestDefinition.rawName());
        Assertions.assertEquals("value", latestDefinition.canonicalName());
    }

    @Test
    public void sessionAssignmentPreservesDeclaredType() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        SQLQuery assignment = variableQuery("SET @value = 1", 30, 14);
        fixture.tracker.trackElements(List.of(declaration, assignment));
        acceptVariableStatement(fixture, declaration);
        acceptVariableStatement(fixture, assignment);

        Assertions.assertEquals("INT", firstVariable(fixture.visibleAt(0)).type().getDisplayName());
    }

    @Test
    public void reconciliationDiscoveryIsLimitedToOrderedDocumentScopes() {
        Assertions.assertTrue(SQLScriptVariablesTracker.create(SQLScriptVariableScope.SCRIPT).usesReconciledElements());
        Assertions.assertTrue(SQLScriptVariablesTracker.create(SQLScriptVariableScope.BATCH).usesReconciledElements());
        Assertions.assertFalse(SQLScriptVariablesTracker.create(SQLScriptVariableScope.SESSION).usesReconciledElements());
        Assertions.assertFalse(SQLScriptVariablesTracker.create(null).usesReconciledElements());
    }

    @Test
    public void noOpTrackerIgnoresVariableStatements() {
        TrackerFixture fixture = new TrackerFixture(
            SQLScriptVariablesTracker.create(null),
            createContext(new VoidProgressMonitor())
        );
        SQLQuery declaration = variableQuery("DECLARE @value INT", 0, 18);
        fixture.tracker.trackElements(List.of(declaration));
        acceptVariableStatement(fixture, declaration);

        Assertions.assertTrue(fixture.visibleAt(20).getVariables().isEmpty());
    }

    @Test
    public void sessionUsesDocumentWideLiveView() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery firstDeclaration = variableQuery("DECLARE @first INT", 0, 18);
        SQLQuery secondDeclaration = variableQuery("DECLARE @second INT", 30, 19);
        fixture.tracker.trackElements(List.of(firstDeclaration, secondDeclaration));

        SQLQueryVariablesSubset variables = fixture.visibleAt(60);
        Assertions.assertTrue(variables.getVariables().isEmpty());
        acceptVariableStatement(fixture, firstDeclaration);
        acceptVariableStatement(fixture, secondDeclaration);
        Assertions.assertSame(variables, fixture.visibleAt(0));
        Assertions.assertSame(variables, fixture.visibleAt(60));
        Assertions.assertEquals(List.of("@first", "@second"), names(variables));

        fixture.tracker.clear();
        Assertions.assertTrue(variables.getVariables().isEmpty());
    }

    @Test
    public void sessionAssignmentIntroducesVariable() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery assignment = variableQuery("SET @value = 1", 0, 14);
        fixture.tracker.trackElements(List.of(assignment));
        acceptVariableStatement(fixture, assignment);

        SQLQueryVariablesSubset variables = fixture.visibleAt(20);
        Assertions.assertEquals(List.of("@value"), names(variables));
        Assertions.assertEquals(
            SQLQueryVariableInfo.OperationKind.ASSIGNMENT,
            firstVariable(variables).operationKind()
        );
    }

    @Test
    public void sessionAcceptsModelWhenParserHintIsMissing() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        String text = """
            SET
                @counter = 1,
                @description = 'initial value',
                @enabled = TRUE
            """;
        SQLQuery assignment = new SQLQuery(null, text, 0, text.length());
        Assertions.assertFalse(assignment.hasVariableKeyword());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            assignment.getOriginalText(),
            fixture.visibleAt(assignment.getOffset())
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryVariableStatementModel.class, model.getQueryModel());

        fixture.tracker.acceptAnalysisResult(assignment, model);

        Assertions.assertEquals(
            List.of("@counter", "@description", "@enabled"),
            names(fixture.visibleAt(text.length()))
        );
    }

    @Test
    public void assignmentBecomesEffectiveOperation() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        fixture.tracker.trackElements(List.of(
            variableQuery("DECLARE @value INT", 0, 18),
            variableQuery("SET @value = 1", 30, 14),
            variableQuery("SET @VALUE = 2", 60, 14)
        ));

        SQLQueryVariablesSubset variables = fixture.visibleAt(90);
        Assertions.assertEquals(1, variables.getVariables().size());
        Assertions.assertEquals("@VALUE", firstVariable(variables).rawName());
        Assertions.assertEquals(
            SQLQueryVariableInfo.OperationKind.ASSIGNMENT,
            firstVariable(variables).operationKind()
        );
        Assertions.assertTrue(firstVariable(variables).relativeOffset() >= 0);
    }

    @Test
    public void redeclarationAfterAssignmentBecomesEffectiveOperation() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        fixture.tracker.trackElements(List.of(
            variableQuery("DECLARE @value INT", 0, 18),
            variableQuery("SET @value = 1", 30, 14),
            variableQuery("DECLARE @VALUE DATE", 60, 19)
        ));

        SQLQueryVariablesSubset variables = fixture.visibleAt(90);
        SQLQueryVariableInfo effectiveVariable = firstVariable(variables);
        Assertions.assertEquals(SQLQueryVariableInfo.OperationKind.DECLARATION, effectiveVariable.operationKind());
        Assertions.assertEquals("DATE", effectiveVariable.type().getDisplayName());
    }

    @Test
    public void sessionDeclarationAfterUnboundAssignmentBecomesEffectiveOperation() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery assignment = variableQuery("SET @value = 1", 0, 14);
        SQLQuery declaration = variableQuery("DECLARE @value DATE", 30, 19);
        fixture.tracker.trackElements(List.of(assignment, declaration));
        acceptVariableStatement(fixture, assignment);
        acceptVariableStatement(fixture, declaration);

        SQLQueryVariablesSubset variables = fixture.visibleAt(60);
        SQLQueryVariableInfo effectiveVariable = firstVariable(variables);
        Assertions.assertEquals(SQLQueryVariableInfo.OperationKind.DECLARATION, effectiveVariable.operationKind());
        Assertions.assertEquals("DATE", effectiveVariable.type().getDisplayName());
    }

    @Test
    public void modificationInsideObservedSessionElementPreservesSessionVariables() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SESSION);
        SQLQuery firstDeclaration = variableQuery("DECLARE @first INT", 0, 18);
        SQLQuery secondDeclaration = variableQuery("DECLARE @second INT", 30, 19);
        fixture.tracker.trackElements(List.of(firstDeclaration, secondDeclaration));
        acceptVariableStatement(fixture, firstDeclaration);
        acceptVariableStatement(fixture, secondDeclaration);

        fixture.tracker.applyDelta(40, 1, 1);

        Assertions.assertEquals(List.of("@first", "@second"), names(fixture.visibleAt(0)));
    }

    @Test
    public void insertionBeforeTrackedElementShiftsDefinitionAndBatchBoundary() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 10, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 35, 2);
        fixture.tracker.trackElements(List.of(declaration, delimiter));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(34)));

        fixture.tracker.applyDelta(0, 0, 5);

        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(39)));
        Assertions.assertTrue(fixture.visibleAt(45).getVariables().isEmpty());
    }

    @Test
    public void deletionBeforeTrackedElementInvalidatesTailUntilObservedAgain() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery declaration = variableQuery("DECLARE @value INT", 10, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 35, 2);
        fixture.tracker.trackElements(List.of(declaration, delimiter));
        fixture.visibleAt(34);

        fixture.tracker.applyDelta(0, 5, 0);

        Assertions.assertTrue(fixture.visibleAt(29).getVariables().isEmpty());
        SQLQuery shiftedDeclaration = variableQuery("DECLARE @value INT", 5, 18);
        SQLBatchDelimiterElement shiftedDelimiter = new SQLBatchDelimiterElement(null, "GO", 30, 2);
        fixture.tracker.trackElements(List.of(shiftedDeclaration, shiftedDelimiter));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(29)));
        Assertions.assertTrue(fixture.visibleAt(35).getVariables().isEmpty());
    }

    @Test
    public void modificationInsideTrackedElementInvalidatesItUntilObservedAgain() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        fixture.tracker.trackElements(List.of(variableQuery("DECLARE @value INT", 0, 18)));
        Assertions.assertEquals(List.of("@value"), names(fixture.visibleAt(20)));

        fixture.tracker.applyDelta(10, 0, 1);
        Assertions.assertTrue(fixture.visibleAt(21).getVariables().isEmpty());

        fixture.tracker.trackElements(List.of(variableQuery("DECLARE @value BIGINT", 0, 22)));
        Assertions.assertEquals("BIGINT", firstVariable(fixture.visibleAt(24)).type().getDisplayName());
    }

    @Test
    public void scriptEditPreservesPrefixSnapshots() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        SQLQuery firstDeclaration = variableQuery("DECLARE @first INT", 0, 18);
        SQLQuery secondDeclaration = variableQuery("DECLARE @second DATE", 30, 20);
        fixture.tracker.trackElements(List.of(firstDeclaration, secondDeclaration));

        SQLQueryVariablesSubset prefixVariables = fixture.visibleAt(29);
        Assertions.assertEquals(List.of("@first", "@second"), names(fixture.visibleAt(60)));

        fixture.tracker.applyDelta(40, 0, 1);

        Assertions.assertSame(prefixVariables, fixture.visibleAt(29));
        Assertions.assertSame(prefixVariables, fixture.visibleAt(60));

        SQLQuery changedDeclaration = variableQuery("DECLARE @second TEXT ", 30, 21);
        fixture.tracker.trackElements(List.of(changedDeclaration));
        Assertions.assertSame(prefixVariables, fixture.visibleAt(29));
        SQLQueryVariablesSubset rebuiltVariables = fixture.visibleAt(60);
        Assertions.assertEquals(List.of("@first", "@second"), names(rebuiltVariables));
        Assertions.assertEquals(
            "TEXT",
            rebuiltVariables.getVariables().stream()
                .filter(variable -> variable.canonicalName().equals("second"))
                .findFirst()
                .orElseThrow()
                .type()
                .getDisplayName()
        );
    }

    @Test
    public void batchEditPreservesPrecedingScopeSnapshots() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        SQLQuery firstDeclaration = variableQuery("DECLARE @first INT", 0, 18);
        SQLBatchDelimiterElement delimiter = new SQLBatchDelimiterElement(null, "GO", 27, 2);
        SQLQuery secondDeclaration = variableQuery("DECLARE @second DATE", 40, 20);
        fixture.tracker.trackElements(List.of(firstDeclaration, delimiter, secondDeclaration));

        SQLQueryVariablesSubset firstBatchVariables = fixture.visibleAt(24);
        Assertions.assertEquals(List.of("@second"), names(fixture.visibleAt(70)));
        SQLQueryVariablesSubset secondBatchStartVariables = fixture.visibleAt(39);

        fixture.tracker.applyDelta(50, 1, 1);

        Assertions.assertSame(firstBatchVariables, fixture.visibleAt(24));
        Assertions.assertSame(secondBatchStartVariables, fixture.visibleAt(70));

        SQLQuery changedDeclaration = variableQuery("DECLARE @second TEXT", 40, 20);
        fixture.tracker.trackElements(List.of(changedDeclaration));
        Assertions.assertSame(firstBatchVariables, fixture.visibleAt(24));
        Assertions.assertSame(secondBatchStartVariables, fixture.visibleAt(39));
        Assertions.assertEquals("TEXT", firstVariable(fixture.visibleAt(70)).type().getDisplayName());
    }

    @Test
    public void visibleVariablesAreCachedAndResolvedCaseInsensitively() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.SCRIPT);
        fixture.tracker.trackElements(List.of(variableQuery("DECLARE @VaLuE INT", 0, 18)));

        SQLQueryVariablesSubset variables = fixture.visibleAt(20);
        SQLQueryVariableInfo definition = firstVariable(variables);

        Assertions.assertSame(variables, fixture.visibleAt(29));
        Assertions.assertEquals("@VaLuE", definition.rawName());
        Assertions.assertEquals("value", definition.canonicalName());

        fixture.context.reset();
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(fixture.context, "SELECT @VALUE", variables);
        Assertions.assertNotNull(model);
        Assertions.assertSame(
            definition,
            model.getAllSymbols().stream()
                .filter(symbol -> symbol.getRawName().equals("@VALUE"))
                .findFirst()
                .orElseThrow()
                .getDefinition()
        );
    }

    @Test
    public void batchAssignmentDoesNotIntroduceVariable() {
        TrackerFixture fixture = createTracker(SQLScriptVariableScope.BATCH);
        fixture.tracker.trackElements(List.of(variableQuery("SET @value = 1", 0, 14)));

        Assertions.assertTrue(fixture.visibleAt(20).getVariables().isEmpty());
    }

    @NotNull
    private static List<String> names(@NotNull SQLQueryVariablesSubset variables) {
        return variables.getVariables().stream().map(SQLQueryVariableInfo::rawName).sorted().toList();
    }

    @NotNull
    private static SQLQueryVariableInfo firstVariable(@NotNull SQLQueryVariablesSubset variables) {
        return variables.getVariables().iterator().next();
    }

    @NotNull
    private static SQLQuery variableQuery(@NotNull String text, int offset, int length) {
        SQLQuery query = new SQLQuery(null, text, offset, length);
        query.setHasVariableKeyword(true);
        return query;
    }

    @NotNull
    private static SQLQueryModel recognizeVariableStatement(
        @NotNull TrackerFixture fixture,
        @NotNull SQLQuery query
    ) {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            query.getOriginalText(),
            fixture.visibleAt(query.getOffset())
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryVariableStatementModel.class, model.getQueryModel());
        return model;
    }

    private static void acceptVariableStatement(@NotNull TrackerFixture fixture, @NotNull SQLQuery query) {
        fixture.tracker.acceptAnalysisResult(query, recognizeVariableStatement(fixture, query));
    }

    @NotNull
    private static TrackerFixture createTracker(@NotNull SQLScriptVariableScope scope) {
        return createTracker(scope, new VoidProgressMonitor());
    }

    @NotNull
    private static TrackerFixture createTracker(
        @NotNull SQLScriptVariableScope scope,
        @NotNull VoidProgressMonitor monitor
    ) {
        return new TrackerFixture(SQLScriptVariablesTracker.create(scope), createContext(monitor));
    }

    @NotNull
    private static SQLQueryRecognitionContext createContext(@NotNull VoidProgressMonitor monitor) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(BasicSQLDialect.INSTANCE, DBWorkbench.getPlatform().getPreferenceStore());
        return new SQLQueryRecognitionContext(
            monitor, null, false, false, syntaxManager, BasicSQLDialect.INSTANCE);
    }

    private static class CountingProgressMonitor extends VoidProgressMonitor {
        private int checkCount;

        @Override
        public boolean isCanceled() {
            this.checkCount++;
            return false;
        }

        private int getCheckCount() {
            return this.checkCount;
        }
    }

    private record TrackerFixture(
        @NotNull SQLScriptVariablesTracker tracker,
        @NotNull SQLQueryRecognitionContext context
    ) {
        @NotNull
        private SQLQueryVariablesSubset visibleAt(int position) {
            this.context.reset();
            SQLQueryVariablesSubset variables = this.tracker.getVisibleVariablesAt(this.context, position);
            this.context.reset();
            return variables;
        }
    }
}
