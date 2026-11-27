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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.tracking.SQLScriptVariablesTracker;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLReconcilingStrategy;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class SQLBackgroundParsingJob {

    private static final Log log = Log.getLog(SQLBackgroundParsingJob.class);
    private static final boolean DEBUG = false;

    private static final long schedulingTimeoutMilliseconds = 250;

    private static class QueuedRegionInfo {
        public int length;

        public QueuedRegionInfo(int length) {
            this.length = length;
        }
    }

    // A null workRegion means the run stopped before discovering its parse range.
    private record ActiveParse(
        @NotNull CompletableFuture<Interval> workRegion,
        @NotNull CompletableFuture<Long> finishStamp
    ) {
    }

    private final class ScriptVariablesTrackingContext {
        @NotNull
        private final Object lock = new Object();
        @Nullable
        private SQLReconcilingStrategy.ReconciliationListener reconciliationListener;
        @Nullable
        private SQLScriptVariablesTracker tracker;
        @Nullable
        private SQLScriptVariableScope variableScope;
        @Nullable
        private SQLReconcilingStrategy subscribedReconcilingStrategy;
        private volatile long documentGeneration;
        private long trackerGeneration;
        private long invalidatedDocumentStamp = Long.MIN_VALUE;

        public void setup(@NotNull SQLDialect dialect) {
            synchronized (this.lock) {
                this.unsubscribe();
                this.documentGeneration++;
                this.trackerGeneration++;
                this.invalidatedDocumentStamp = Long.MIN_VALUE;
                this.variableScope = editor.getActivePreferenceStore()
                    .getBoolean(SQLModelPreferences.TRACK_SCRIPT_VARIABLES)
                    ? dialect.getScriptVariableScope()
                    : null;
                this.tracker = SQLScriptVariablesTracker.create(this.variableScope);
                this.subscribe();
            }
        }

        public void dispose() {
            synchronized (this.lock) {
                this.documentGeneration++;
                this.trackerGeneration++;
                this.unsubscribe();
                this.tracker = null;
                this.variableScope = null;
                this.invalidatedDocumentStamp = Long.MIN_VALUE;
            }
        }

        public void applyDelta(int offset, int oldLength, int newLength) {
            synchronized (this.lock) {
                this.documentGeneration++;
                long documentStamp = getDocumentModificationStamp(document);
                this.invalidatedDocumentStamp = documentStamp == IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP
                    ? Long.MIN_VALUE
                    : documentStamp;
                if (this.tracker != null) {
                    this.tracker.applyDelta(offset, oldLength, newLength);
                }
            }
        }

        public void clear() {
            synchronized (this.lock) {
                this.documentGeneration++;
                this.trackerGeneration++;
                this.unsubscribe();
                this.invalidatedDocumentStamp = Long.MIN_VALUE;
                if (this.tracker != null) {
                    this.tracker = SQLScriptVariablesTracker.create(this.variableScope);
                    this.subscribe();
                }
            }
        }

        public long getDocumentGeneration() {
            return this.documentGeneration;
        }

        @Nullable
        public SQLScriptVariablesTracker prepareForAnalysis(
            @NotNull SQLParserContext parserContext,
            @NotNull List<SQLScriptElement> elements,
            long expectedDocumentGeneration
        ) {
            SQLScriptVariablesTracker currentTracker;
            int firstUnobservedPosition;
            synchronized (this.lock) {
                if (expectedDocumentGeneration != this.documentGeneration || this.tracker == null) {
                    return null;
                }
                currentTracker = this.tracker;
                int lastCoveredPosition = currentTracker.getLastCoveredPosition();
                firstUnobservedPosition = lastCoveredPosition == Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : lastCoveredPosition + 1;
            }

            List<SQLScriptElement> gapElements = List.of();
            int analysisStart = elements.getFirst().getOffset();
            int analysisEnd = elements.getLast().getOffset() + elements.getLast().getLength();
            int observedRangeStart = analysisStart;
            if (firstUnobservedPosition < analysisStart) {
                observedRangeStart = firstUnobservedPosition;
                SQLScriptElement containingElement = SQLScriptParser.extractQueryAtPos(
                    parserContext,
                    firstUnobservedPosition,
                    false
                );
                if (containingElement != null) {
                    observedRangeStart = Math.min(observedRangeStart, containingElement.getOffset());
                }
                gapElements = SQLScriptParser.extractScriptQueries(
                    parserContext,
                    observedRangeStart,
                    analysisStart - observedRangeStart,
                    false,
                    false,
                    false,
                    true
                );
            }

            List<SQLScriptElement> relevantElements = Stream.concat(gapElements.stream(), elements.stream())
                .filter(SQLScriptVariablesTracker::isRelevant)
                .toList();
            synchronized (this.lock) {
                if (expectedDocumentGeneration == this.documentGeneration && currentTracker == this.tracker) {
                    if (firstUnobservedPosition < analysisEnd) {
                        currentTracker.reconcileElements(
                            observedRangeStart,
                            analysisEnd - observedRangeStart,
                            relevantElements
                        );
                    } else {
                        currentTracker.trackElements(relevantElements);
                    }
                    return currentTracker;
                }
                return null;
            }
        }

        public void acceptAnalysisResult(
            @NotNull SQLScriptElement element,
            @NotNull SQLQueryModel model,
            long expectedDocumentGeneration
        ) {
            synchronized (this.lock) {
                if (expectedDocumentGeneration == this.documentGeneration && this.tracker != null) {
                    this.tracker.acceptAnalysisResult(element, model);
                }
            }
        }

        private void trackReconciledElements(
            int offset,
            int length,
            @NotNull List<SQLScriptElement> elements,
            long documentModificationStamp,
            long expectedTrackerGeneration
        ) {
            synchronized (this.lock) {
                if (expectedTrackerGeneration != this.trackerGeneration ||
                    documentModificationStamp == this.invalidatedDocumentStamp ||
                    documentModificationStamp != getDocumentModificationStamp(document)
                ) {
                    return;
                }
            }
            List<SQLScriptElement> relevantElements = elements.stream()
                .filter(SQLScriptVariablesTracker::isRelevant)
                .toList();
            synchronized (this.lock) {
                if (expectedTrackerGeneration == this.trackerGeneration &&
                    documentModificationStamp != this.invalidatedDocumentStamp &&
                    documentModificationStamp == getDocumentModificationStamp(document)
                ) {
                    if (this.tracker != null) {
                        this.tracker.reconcileElements(offset, length, relevantElements);
                    }
                }
            }
        }

        private void unsubscribe() {
            if (this.subscribedReconcilingStrategy != null && this.reconciliationListener != null) {
                this.subscribedReconcilingStrategy.removeReconciliationListener(this.reconciliationListener);
            }
            this.subscribedReconcilingStrategy = null;
            this.reconciliationListener = null;
        }

        private void subscribe() {
            if (this.tracker == null || !this.tracker.usesReconciledElements()) {
                return;
            }
            SQLReconcilingStrategy reconcilingStrategy = editor.getViewerConfiguration().getReconcilingStrategy();
            if (reconcilingStrategy != null) {
                long expectedTrackerGeneration = this.trackerGeneration;
                this.reconciliationListener = (offset, length, elements, documentModificationStamp) ->
                    this.trackReconciledElements(
                        offset,
                        length,
                        elements,
                        documentModificationStamp,
                        expectedTrackerGeneration
                    );
                reconcilingStrategy.addReconciliationListener(this.reconciliationListener);
                this.subscribedReconcilingStrategy = reconcilingStrategy;
            }
        }

        private static long getDocumentModificationStamp(@Nullable IDocument document) {
            return document instanceof IDocumentExtension4 documentExtension
                ? documentExtension.getModificationStamp()
                : IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP;
        }
    }

    // TODO consider if we don't need such a detailed collection for reparse regions, and one expandable input region is enough
    @NotNull
    private final OffsetKeyedTreeMap<QueuedRegionInfo> queuedForReparse = new OffsetKeyedTreeMap<>();
    @NotNull
    private final Object syncRoot = new Object();
    @NotNull
    private final SQLEditorBase editor;
    @NotNull
    private final SQLBackgroundParsingEnvironment environment;
    @NotNull
    private final SQLDocumentSyntaxContext context = new SQLDocumentSyntaxContext();
    @NotNull
    private final ScriptVariablesTrackingContext variablesTrackingContext = new ScriptVariablesTrackingContext();
    @Nullable
    private IDocument document = null;
    private boolean isSetup;
    @NotNull
    private final SQLBackgroundParsingEnvironment.Job job;
    private CompletableFuture<Long> lastQueuedParseFinishStamp = CompletableFuture.completedFuture(0L);
    @Nullable
    private ActiveParse activeParse;

    private volatile boolean isRunning = false;
    private volatile int knownRegionStart = 0;
    private volatile int knownRegionEnd = 0;

    @NotNull
    private final DocumentLifecycleListener documentListener = new DocumentLifecycleListener();

    public SQLBackgroundParsingJob(@NotNull SQLEditorBase editor) {
        this(editor, SQLBackgroundParsingWorkbenchEnv.INSTANCE);
    }

    public SQLBackgroundParsingJob(@NotNull SQLEditorBase editor, @NotNull SQLBackgroundParsingEnvironment environment) {
        this.editor = editor;
        this.environment = environment;
        this.job = environment.createJob(this::runParsingJob);
    }

    @NotNull
    private IStatus runParsingJob(@NotNull DBRProgressMonitor monitor) {
        boolean accomplished = false;
        try {
            accomplished = this.doWork(monitor);
            return Status.OK_STATUS;
        } catch (BadLocationException e) {
            log.debug(e);
            return Status.CANCEL_STATUS;
        } finally {
            if (!accomplished) {
                this.signalAccomplished();
            }
        }
    }

    @NotNull
    public SQLDocumentSyntaxContext getCurrentContext() {
        return context;
    }

    /**
     * Setup job - add listeners, schedule
     */
    public void setup() {
        synchronized (this.syncRoot) {
            SQLDialect dialect = this.obtainCurrentSqlDialect(this.editor.getExecutionContext());
            this.variablesTrackingContext.setup(dialect);
            if (this.editor.getTextViewer() != null) {
                if (!this.isSetup) {
                    this.editor.getTextViewer().addTextInputListener(this.documentListener);
                    this.editor.getTextViewer().addViewportListener(this.documentListener);
                    this.isSetup = true;
                }
                if (this.document == null) {
                    IDocument document = this.editor.getTextViewer().getDocument();
                    if (document != null) {
                        this.document = document;
                        this.document.addDocumentListener(this.documentListener);
                    }
                }
                this.reset();
            }
        }
    }

    /**
     * Dispose job - cancel schedule and remove listeners.
     */
    public void dispose() {
        synchronized (this.syncRoot) {
            this.cancel();
            this.lastQueuedParseFinishStamp.cancel(false);
            if (this.activeParse != null) {
                this.activeParse.workRegion().cancel(false);
                this.activeParse.finishStamp().cancel(false);
            }
            TextViewer textViewer = this.editor.getTextViewer();
            if (textViewer != null) {
                textViewer.removeViewportListener(this.documentListener);
                textViewer.removeTextInputListener(this.documentListener);
                if (this.document != null) {
                    this.document.removeDocumentListener(this.documentListener);
                }
            }
            this.variablesTrackingContext.dispose();
            this.isSetup = false;
        }
    }

    // TODO consider moving to utility class (see ImportProjectToTEHandler)
    private static <T> T getFutureOrCancel(Future<T> future, IProgressMonitor monitor) throws ExecutionException, InterruptedException {
        while (!monitor.isCanceled()) {
            try {
                return future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                // proceed with job cancellation check
            }
        }
        throw new CancellationException();
    }

    /**
     * Prepare completion context for the specified position in the text
     */
    @NotNull
    public SQLQueryCompletionContext obtainCompletionContext(DBRProgressMonitor monitor, @NotNull Position completionRequestPosition) {
        CompletableFuture<?> lastAwaitedFuture = null;
        do {
            CompletableFuture<?> futureToAwait = null;
            synchronized (this.syncRoot) {
                if (completionRequestPosition.isDeleted()) {
                    break;
                }
                int requestOffset = completionRequestPosition.getOffset();
                if (this.isPositionQueued(requestOffset)) {
                    futureToAwait = this.lastQueuedParseFinishStamp;
                } else {
                    ActiveParse parse = this.activeParse;
                    if (parse != null) {
                        if (parse.workRegion().isCompletedExceptionally()) {
                            return SQLQueryCompletionContext.prepareEmpty(0, requestOffset);
                        }
                        if (!parse.workRegion().isDone()) {
                            futureToAwait = parse.workRegion();
                        } else {
                            Interval workRegion = parse.workRegion().getNow(null);
                            if (workRegion != null && requestOffset >= workRegion.a && requestOffset <= workRegion.b) {
                                futureToAwait = parse.finishStamp();
                            }
                        }
                    }
                    if (futureToAwait == null) {
                        SQLScriptItemAtOffset scriptItem = this.context.findScriptItem(requestOffset - 1);
                        if (scriptItem != null && !scriptItem.item.isDirty()) {
                            DBCExecutionContext executionContext = this.editor.getExecutionContext();
                            SQLDialect dialect = this.obtainCurrentSqlDialect(executionContext);
                            return SQLQueryCompletionContext.prepareCompletionContext(
                                scriptItem, requestOffset, executionContext, dialect
                            );
                        } else if (scriptItem == null) {
                            return SQLQueryCompletionContext.prepareOffquery(0, requestOffset);
                        } else {
                            return SQLQueryCompletionContext.prepareEmpty(0, requestOffset);
                        }
                    }
                }
                if (futureToAwait == lastAwaitedFuture && futureToAwait.isDone()) {
                    return SQLQueryCompletionContext.prepareEmpty(0, requestOffset);
                }
            }

            try {
                // A before-change request can precede scheduling; job.join() would return prematurely.
                getFutureOrCancel(futureToAwait, monitor.getNestedMonitor());
                lastAwaitedFuture = futureToAwait;
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                break;
            }
        } while (!completionRequestPosition.isDeleted());
        return SQLQueryCompletionContext.prepareEmpty(0, completionRequestPosition.getOffset());
    }

    // Caller holds syncRoot.
    private boolean isPositionQueued(int requestOffset) {
        NodesIterator<QueuedRegionInfo> qit = this.queuedForReparse.nodesIteratorAt(requestOffset);
        QueuedRegionInfo region = qit.getCurrValue() != null ? qit.getCurrValue() : (qit.prev() ? qit.getCurrValue() : null);
        return region != null && (saturatedSum(qit.getCurrOffset(), region.length) >= requestOffset);
    }

    @NotNull
    private SQLDocumentSyntaxContext getContext() {
        return this.context;
    }

    private void beforeDocumentModification(@NotNull DocumentEvent event) {
        this.cancel();
        int insertedLength = event.getText() == null ? 0 : event.getText().length();
        this.variablesTrackingContext.applyDelta(event.getOffset(), event.getLength(), insertedLength);
        IRegion regionToReparse = this.context.applyDelta(event.getOffset(), event.getLength(), insertedLength);
        int reparseStart = regionToReparse.getOffset();
        int reparseLength = 0;
        if (regionToReparse.getLength() < Integer.MAX_VALUE) {
            reparseLength = regionToReparse.getLength();
        } else {
            if (event.getOffset() + insertedLength > this.editor.getTextViewer().getBottomIndexEndOffset()) {
                reparseLength = event.getOffset() + insertedLength;
            } else {
                reparseLength = this.editor.getTextViewer().getBottomIndexEndOffset() - reparseStart;
            }
        }
        if (DEBUG) {
            log.debug("reparse region @" + reparseStart + "+" + reparseLength);
        }

        // TODO if these further actions are heavy, maybe use background thread for them too
        synchronized (this.syncRoot) {
            int delta = insertedLength - event.getLength();
            if (delta > 0) { // just expand the region to reparse
                this.queuedForReparse.applyOffset(event.getOffset(), delta);
                if (DEBUG) {
                    log.debug("beforeDocumentModification, delta > 0: queuedForReparse count is " + queuedForReparse);
                }
                this.enqueueToReparse(reparseStart, reparseLength);
            } else {
                // TODO remove just the affected fragment and enqueue regionToReparse

                // for now removing the whole tail as its offsets are being invalidated
                ListNode<Integer> keyOffsetsToRemove = null;
                NodesIterator<QueuedRegionInfo> it = this.queuedForReparse.nodesIteratorAt(reparseStart);
                int firstAffectedReparseOffset;
                if (it.getCurrValue() != null || it.prev()) {
                    firstAffectedReparseOffset = it.getCurrOffset();
                    if (firstAffectedReparseOffset < reparseStart &&
                        firstAffectedReparseOffset + it.getCurrValue().length > reparseStart + insertedLength
                    ) {
                        this.resetLastQueuedParseFinishStamp();
                        return; // modified region is a subrange of already queued for reparse
                    }
                    keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, firstAffectedReparseOffset);
                }
                while (it.next()) {
                    keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, it.getCurrOffset());
                }
                for (ListNode<Integer> kn = keyOffsetsToRemove; kn != null; kn = kn.next) {
                    if (DEBUG) {
                        log.debug("remove " + kn.data + "+" + this.queuedForReparse.find(kn.data).length);
                    }
                    this.queuedForReparse.removeAt(kn.data);
                    if (DEBUG) {
                        log.debug("beforeDocumentModification, delta <= 0: queuedForReparse count is " + queuedForReparse.size());
                    }
                }
                this.enqueueToReparse(reparseStart, Integer.MAX_VALUE);
            }
            this.resetLastQueuedParseFinishStamp();
        }
    }

    private void enqueueToReparse(int toParseStart, int toParseLength) {
        synchronized (this.syncRoot) {
            NodesIterator<QueuedRegionInfo> it = this.queuedForReparse.nodesIteratorAt(toParseStart);
            QueuedRegionInfo region = it.getCurrValue();
            int regionOffset = it.getCurrOffset();
            if (region == null && it.prev()) {
                region = it.getCurrValue();
                regionOffset = it.getCurrOffset();
            }
            // enlarge existing or add enqueue new one
            if (region != null && regionOffset <= toParseStart && regionOffset + region.length > toParseStart) {
                region.length = Math.max(region.length, toParseStart + toParseLength - regionOffset);
            } else {
                this.queuedForReparse.put(toParseStart, new QueuedRegionInfo(toParseLength));
                if (DEBUG) {
                    log.debug("enqueueToReparse: queuedForReparse count is " + queuedForReparse.size());
                }
                this.resetLastQueuedParseFinishStamp();
            }
        }
    }

    private void resetLastQueuedParseFinishStamp() {
        synchronized (this.syncRoot) {
            // Edits after a run claimed its queue belong to the next run, not the active one.
            if (this.lastQueuedParseFinishStamp.isDone() ||
                this.activeParse != null && this.lastQueuedParseFinishStamp == this.activeParse.finishStamp()
            ) {
                this.lastQueuedParseFinishStamp = new CompletableFuture<>();
            }
        }
    }

    private void ensureVisibleRangeIsParsed() {
        TextViewer viewer = this.editor.getTextViewer();
        if (viewer == null || viewer.getDocument() == null) {
            return;
        }
        int startOffset = viewer.getTopIndexStartOffset();
        int endOffset = viewer.getBottomIndexEndOffset();
        Interval visibleRange = new Interval(startOffset, endOffset);
        Interval knownRange = new Interval(this.knownRegionStart, this.knownRegionEnd);
        if (DEBUG) {
            log.debug("ensureVisibleRangeIsParsed: knownRange is " + knownRange);
            log.debug("ensureVisibleRangeIsParsed: visibleRange is " + visibleRange);
        }
        if (!knownRange.properlyContains(visibleRange)) {
            Interval unknownRange = visibleRange.differenceNotProperlyContained(knownRange);
            if (unknownRange == null) {
                unknownRange = visibleRange;
            }
            if (DEBUG) {
                log.debug("ensureVisibleRangeIsParsed: unknownRange is " + unknownRange);
            }
            this.enqueueToReparse(unknownRange.a, unknownRange.length());
            this.schedule(null);
        }
    }

    private void schedule(@Nullable DocumentEvent event) {
        synchronized (this.syncRoot) {
            if (this.editor.getRuleManager() == null || !this.editor.isAdvancedHighlightingEnabled() ||
                !this.environment.isParserApplied(this.editor.getEditorInput())
            ) {
                return;
            }

            if (this.job.getState() != Job.RUNNING) {
                this.job.cancel();
            }
            long delay;
            if (event != null && SQLConstants.DOT.equals(event.getText())) {
                delay = this.isRunning ? schedulingTimeoutMilliseconds / 2 : 0;
            } else {
                delay = schedulingTimeoutMilliseconds * (this.isRunning ? 2 : 1);
            }
            this.job.schedule(delay);
        }
    }

    private void cancel() {
        synchronized (this.syncRoot) {
            this.job.cancel();
        }
    }

    private void setDocument(@Nullable IDocument newDocument) {
        synchronized (this.syncRoot) {
            if (this.document != null) {
                this.cancel();
            }

            if (newDocument != null && this.environment.isParserApplied(editor.getEditorInput())) {
                this.document = newDocument;
                this.reset();
            }
        }
    }

    private void reset() {
        synchronized (this.syncRoot) {
            if (DEBUG) {
                log.debug("reset background parsing job");
            }
            this.context.clear();
            this.variablesTrackingContext.clear();
            this.queuedForReparse.clear();
            this.knownRegionEnd = 0;
            this.knownRegionStart = 0;
            this.ensureVisibleRangeIsParsed();
        }
    }

    // True only after this run's parsed range and completion have been published together.
    private boolean doWork(@NotNull DBRProgressMonitor monitor) throws BadLocationException {
        TextViewer viewer = this.editor.getTextViewer();
        if (viewer == null || this.editor.getRuleManager() == null) {
            return false;
        }
        Interval actualFragment = this.environment.getVisibleFragment(viewer);

        if (actualFragment == null) {
            return false;
        }
        int workOffset;
        int workLength;
        ActiveParse parse;
        try {
            synchronized (this.syncRoot) {
                if (monitor.isCanceled()) {
                    // An edit may have queued a new run while the visible fragment was obtained.
                    return false;
                }
                this.isRunning = true;

                // drop unnecessary items
                if (DEBUG) {
                    log.debug("actual region is " + actualFragment.a + "-" + actualFragment.b);
                }
                Interval preservedRegion = this.context.dropInvisibleScriptItems(actualFragment);
                this.knownRegionStart = preservedRegion.a;
                this.knownRegionEnd = preservedRegion.b;
                if (DEBUG) {
                    log.debug("preserved is " + knownRegionStart + "-" + knownRegionEnd);
                    log.debug("queued ranges total: " + this.queuedForReparse.size());
                }

                // TODO reparse only changed elements
                // for now just cover the region of interest
                {
                    NodesIterator<QueuedRegionInfo> it = this.queuedForReparse.nodesIteratorAt(0);
                    workOffset = (it.getCurrValue() != null || it.next()) ? it.getCurrOffset() : 0;
                }
                {
                    NodesIterator<QueuedRegionInfo> it = this.queuedForReparse.nodesIteratorAt(Integer.MAX_VALUE);
                    workLength = (it.getCurrValue() != null || it.prev())
                        ? (it.getCurrOffset() + it.getCurrValue().length - workOffset) : 0;
                }

                // truncate work region to fit within actualFragment,
                // as we've dropped what is outside already, so not point to parse outside of it
                Interval workInterval = new Interval(workOffset, saturatedSum(workOffset, workLength));
                if (!actualFragment.properlyContains(workInterval)) {
                    workInterval = actualFragment.intersection(workInterval);
                    workOffset = workInterval.a;
                    workLength = workInterval.length();
                }

                int docTailDelta = this.document.getLength() - saturatedSum(workOffset, workLength);
                if (docTailDelta < 0) {
                    workLength += docTailDelta;
                }
                if (workOffset == this.document.getLength() && workOffset > 0) {
                    workOffset--;
                    workLength++;
                }
                if (DEBUG) {
                    {
                        log.debug("requested " + workOffset + "+" + workLength);
                        NodesIterator<QueuedRegionInfo> it = this.queuedForReparse.nodesIteratorAt(Integer.MAX_VALUE);
                        while (it.prev()) {
                            log.debug("\t@" + it.getCurrOffset() + "+" + it.getCurrValue().length);
                        }
                    }
                }

                // Claim the queued future with its work; later edits will receive a different future.
                parse = new ActiveParse(new CompletableFuture<>(), this.lastQueuedParseFinishStamp);
                this.activeParse = parse;
                this.queuedForReparse.clear();
                if (DEBUG) {
                    log.debug("doWork: queuedForReparse count is " + queuedForReparse.size());
                }
            }
        } catch (Throwable ex) {
            log.error(ex);
            return false;
        }

        boolean monitorFinished = false;
        try {
            if (workLength == 0) {
                return false;
            }

            final long variablesDocumentGeneration = this.variablesTrackingContext.getDocumentGeneration();
            SQLParserContext parserContext = new SQLParserContext(
                editor.getDataSource(), editor.getSyntaxManager(), editor.getRuleManager(), document
            );
            if (DEBUG) {
                log.debug("discovering " + workOffset + "+" + workLength);
            }
            {
                SQLScriptElement firstElement = SQLScriptParser.extractQueryAtPos(parserContext, workOffset, false);
                if (firstElement != null) {
                    workLength = Math.max(workOffset + workLength, firstElement.getOffset() + firstElement.getLength());
                    workOffset = Math.min(workOffset, firstElement.getOffset());
                    workLength -= workOffset;
                }
            }
            List<SQLScriptElement> elements = this.environment.extractScriptQueries(
                parserContext, workOffset, workLength, false, false, false, true
            );
            if (elements.isEmpty()) {
                if (DEBUG) {
                    log.debug("No script elements to parse in range " + workOffset + "+" + workLength);
                }
                parse.workRegion().complete(new Interval(workOffset, saturatedSum(workOffset, workLength)));
                monitor.done();
                monitorFinished = true;
                this.accomplishWork(workOffset, workLength);
                return true;
            } else {
                SQLScriptElement element = SQLScriptParser.extractQueryAtPos(parserContext, elements.get(0).getOffset(), false);
                if (elements.get(0) instanceof SQLQuery &&
                    element != null && element.getOffset() < elements.get(0).getOffset()
                ) {
                    elements.set(0, element);
                }
                int lastElementIndex = elements.size() - 1;
                SQLScriptElement lastElement = elements.get(lastElementIndex);
                if (elements.size() > 1 && lastElement instanceof SQLQuery) {
                    element = SQLScriptParser.extractQueryAtPos(parserContext, lastElement.getOffset(), false);
                    if (element != null) {
                        elements.set(lastElementIndex, element);
                        lastElement = element;
                    }
                }
                {
                    SQLScriptElement followingElement = SQLScriptParser.extractNextQuery(parserContext, lastElement, true);
                    if (followingElement != null &&
                        followingElement.getOffset() < workOffset + workLength &&
                        followingElement.getOffset() > lastElement.getOffset() + lastElement.getLength()) {
                        elements.add(followingElement);
                    }
                }
            }

            {
                SQLScriptElement lastElement = elements.get(elements.size() - 1);
                if (lastElement == null) {
                    parse.workRegion().complete(new Interval(workOffset, saturatedSum(workOffset, workLength)));
                    monitor.done();
                    monitorFinished = true;
                    this.accomplishWork(workOffset, workLength);
                    return true;
                }
                workOffset = elements.get(0).getOffset();
                workLength = lastElement.getOffset() + lastElement.getLength() - workOffset;
                // Completion must know the region before recognition can block on metadata.
                parse.workRegion().complete(new Interval(workOffset, saturatedSum(workOffset, workLength)));
                if (DEBUG) {
                    log.debug("firstElement@" + elements.get(0).getOffset() + ":" + elements.get(0).getText());
                    log.debug(
                        "lastElement@" + elements.get(elements.size() - 1).getOffset() + ":" + elements.get(elements.size() - 1).getText());
                    log.debug("parsing " + workOffset + "+" + workLength);
                }
            }
            if (DEBUG) {
                log.debug("{");
                for (var e : elements) {
                    log.debug("    @" + e.getOffset() + "+" + e.getLength() + " " + (e instanceof SQLQuery q && q.isEndsWithDelimiter()));
                }
                log.debug("}");
            }

            boolean useRealMetadata = this.isReadMetadataEnabled();
            boolean validateFunctions = this.isValidateFunctionsEnabled();
            DBCExecutionContext executionContext = this.editor.getExecutionContext();

            monitor.beginTask("Background query analysis for " + editor.getTitle(), 1 + elements.size());
            monitor.worked(1);

            SQLSyntaxManager syntaxManager = this.editor.getSyntaxManager();
            SQLDialect dialect = this.obtainCurrentSqlDialect(executionContext);

            SQLQueryRecognitionContext recognitionContext = new SQLQueryRecognitionContext(
                monitor, executionContext, useRealMetadata, validateFunctions, syntaxManager, dialect
            );
            SQLScriptVariablesTracker variablesTracker = monitor.isCanceled()
                ? null
                : this.variablesTrackingContext.prepareForAnalysis(parserContext, elements, variablesDocumentGeneration);

            int i = 1;
            for (SQLScriptElement element : elements) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (element instanceof SQLBatchDelimiterElement) {
                    continue;
                }
                try {
                    recognitionContext.reset();
                    SQLQueryModel queryModel;
                    if (element instanceof SQLControlCommand) {
                        queryModel = SQLCommandModelRecognizer.recognizeCommand(
                            recognitionContext,
                            element.getText(),
                            this.editor instanceof SQLEditor e ? e.getGlobalScriptContext() : null
                        );
                    } else {
                        SQLQueryVariablesSubset varsSubset = variablesTracker == null
                            ? SQLQueryVariablesSubset.EMPTY
                            : variablesTracker.getVisibleVariablesAt(recognitionContext, element.getOffset());
                        recognitionContext.reset();
                        queryModel = this.environment.recognizeQuery(recognitionContext, element.getOriginalText(), varsSubset);
                        if (queryModel != null) {
                            this.variablesTrackingContext.acceptAnalysisResult(element, queryModel, variablesDocumentGeneration);
                        }
                    }
                    if (monitor.isCanceled()) {
                        break;
                    }
                    if (queryModel != null) {
                        if (DEBUG) {
                            log.debug("registering script item @" + element.getOffset() + "+" + element.getLength());
                        }
                        SQLDocumentScriptItemSyntaxContext itemContext = this.context.registerScriptItemContext(
                            element.getOriginalText(),
                            queryModel,
                            element.getOffset(),
                            element.getLength(),
                            element instanceof SQLQuery queryElement && Boolean.TRUE.equals(queryElement.isEndsWithDelimiter())
                        );
                        itemContext.clear();
                        List<SQLQueryRecognitionProblemInfo> problems = recognitionContext.getProblems();
                        if (problems.size() >= SQLQueryRecognitionProblemInfo.PER_QUERY_LIMIT && queryModel.getQueryModel() != null) {
                            problems.add(new SQLQueryRecognitionProblemInfo(
                                SQLQueryRecognitionProblemInfo.Severity.WARNING,
                                queryModel.getSyntaxNode(),
                                null,
                                "Too many errors found in one query of " + this.editor.getTitle() + "!" +
                                    " Displaying first " + SQLQueryRecognitionProblemInfo.PER_QUERY_LIMIT + " of them.",
                                null
                            ));
                        }
                        itemContext.setProblems(problems);
                        for (SQLQuerySymbolEntry entry : queryModel.getAllSymbols()) {
                            itemContext.registerToken(entry.getInterval().a, entry);
                        }
                        itemContext.refreshCompleted();
                    }
                } catch (Throwable ex) {
                    log.debug("Error while analyzing query text: " + element.getOriginalText(), ex);
                }
                monitor.worked(1);
                monitor.subTask("Background query analysis: subtask #" + i + " of " + elements.size());
                i++;
            }
            this.context.resetLastAccessCache();
        } catch (Throwable ex) {
            log.debug(ex);
        } finally {
            // Early exits signal the monitor before accomplishWork publishes results to consumers.
            // Do not signal it again after those consumers have been notified.
            if (!monitorFinished) {
                monitor.done();
            }
        }

        int parsedOffset = workOffset;
        int parsedLength = workLength;

        this.environment.invalidatePresentation(viewer, parsedOffset, parsedLength);
        this.accomplishWork(parsedOffset, parsedLength);
        return true;
    }

    @NotNull
    private SQLDialect obtainCurrentSqlDialect(@Nullable DBCExecutionContext executionContext) {
        try {
            DBPDataSourceContainer dsContainer = this.environment.getInputDataSource(this.editor.getEditorInput());
            SQLDialect dialect = executionContext != null && executionContext.getDataSource() != null
                ? executionContext.getDataSource().getSQLDialect()
                : dsContainer != null ? dsContainer.getScriptDialect().createInstance() : BasicSQLDialect.INSTANCE;
            return dialect;
        } catch (DBException ex) {
            return BasicSQLDialect.INSTANCE;
        }
    }

    private void signalAccomplished() {
        synchronized (this.syncRoot) {
            this.isRunning = false;
            ActiveParse finishedParse = this.activeParse;
            this.activeParse = null;
            if (finishedParse != null) {
                finishedParse.workRegion().complete(null);
                finishedParse.finishStamp().complete(System.currentTimeMillis());
            } else if (this.queuedForReparse.size() == 0) {
                // Do not finish work queued while a canceled run was starting.
                this.lastQueuedParseFinishStamp.complete(System.currentTimeMillis());
            }
        }
    }

    private void accomplishWork(int parsedOffset, int parsedLength) {
        synchronized (this.syncRoot) {
            this.knownRegionStart = Math.min(this.knownRegionStart, parsedOffset);
            this.knownRegionEnd = Math.max(this.knownRegionEnd, parsedOffset + parsedLength);
            if (DEBUG) {
                log.debug("known is " + knownRegionStart + "-" + knownRegionEnd);
            }
            this.signalAccomplished();
        }
    }

    private static int saturatedSum(int a, int b) {
        int r = a + b;
        if (r < a || r < b) {
            return Integer.MAX_VALUE;
        } else {
            return r;
        }
    }

    private boolean isReadMetadataEnabled() {
        DBPPreferenceStore prefStore = this.editor.getActivePreferenceStore();
        return prefStore.getBoolean(SQLModelPreferences.READ_METADATA_FOR_SEMANTIC_ANALYSIS)
            && !prefStore.getBoolean(ModelPreferences.META_DISABLE_EXTRA_READ);
    }

    private boolean isValidateFunctionsEnabled() {
        DBPPreferenceStore prefStore = this.editor.getActivePreferenceStore();
        return prefStore.getBoolean(SQLModelPreferences.VALIDATE_FUNCTIONS) && this.isReadMetadataEnabled();
    }

    private class DocumentLifecycleListener implements IDocumentListener, ITextInputListener, IViewportListener {

        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.beforeDocumentModification(event);
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.schedule(event);
        }

        @Override
        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
            if (oldInput != null) {
                SQLBackgroundParsingJob.this.cancel();
                oldInput.removeDocumentListener(this);
            }
        }

        @Override
        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            if (newInput != null) {
                newInput.addDocumentListener(this);
                SQLBackgroundParsingJob.this.setDocument(newInput);
            }
        }

        @Override
        public void viewportChanged(int verticalOffset) {
            SQLBackgroundParsingJob.this.ensureVisibleRangeIsParsed();
        }
    }
}
