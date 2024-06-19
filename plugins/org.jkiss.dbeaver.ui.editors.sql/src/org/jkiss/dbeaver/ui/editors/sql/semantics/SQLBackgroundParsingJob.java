/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SQLBackgroundParsingJob {

    private static final Log log = Log.getLog(SQLBackgroundParsingJob.class);
    private static final boolean DEBUG = false;

    private static final long schedulingTimeoutMilliseconds = 500;
    
    private static class QueuedRegionInfo {
        public int length;
        
        public QueuedRegionInfo(int length) {
            this.length = length;
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
    private final SQLDocumentSyntaxContext context = new SQLDocumentSyntaxContext();
    @Nullable
    private IDocument document = null;
    @NotNull
    private final AbstractJob job = new AbstractJob("Background parsing job") {
        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                SQLBackgroundParsingJob.this.doWork(monitor);
                return Status.OK_STATUS;
            } catch (BadLocationException e) {
                log.debug(e);
                return Status.CANCEL_STATUS;
            }
        }
    };

    private volatile boolean isRunning = false;
    private volatile int knownRegionStart = 0;
    private volatile int knownRegionEnd = 0;
    @NotNull
    private final DocumentLifecycleListener documentListener = new DocumentLifecycleListener();

    public SQLBackgroundParsingJob(@NotNull SQLEditorBase editor) {
        this.editor = editor;
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
            if (this.editor.getTextViewer() != null) {
                this.editor.getTextViewer().addTextInputListener(this.documentListener);
                this.editor.getTextViewer().addViewportListener(this.documentListener);                
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
            TextViewer textViewer = this.editor.getTextViewer();
            if (textViewer != null) {
                textViewer.removeViewportListener(this.documentListener);
                textViewer.removeTextInputListener(this.documentListener);
                if (this.document != null) {
                    this.document.removeDocumentListener(this.documentListener);
                }
            }
        }
    }
    
    private final Set<Integer> knownIdentifierPartTerms = Set.of(
        SQLStandardLexer.Period,
        SQLStandardLexer.Identifier,
        SQLStandardLexer.DelimitedIdentifier,
        SQLStandardLexer.Quotted
    );

    /**
     * Prepare completion context for the specified position in the text
     */
    @NotNull
    public SQLQueryCompletionContext obtainCompletionContext(int offset) {
        SQLScriptItemAtOffset scriptItem = null;
        do {
            synchronized (this.syncRoot) {
                if (scriptItem == null || this.queuedForReparse.size() == 0) {
                    scriptItem = this.context.findScriptItem(offset);
                    if (scriptItem != null) { // TODO consider statements separation which is ignored for now
                        if (scriptItem.item.isDirty()) {
                            // awaiting reparse, so proceed to release lock and wait for the job to finish, then retry
                        } else {
                            return this.prepareCompletionContext(scriptItem, offset); 
                        }
                    } else {
                        // no script items here, so fallback to offquery context
                        break;
                    }
                }
            }
            
            try {
                this.job.join();
            } catch (InterruptedException e) {
                break;
            }
        } while (scriptItem != null);
        return SQLQueryCompletionContext.prepareOffquery(0);
    }

    @NotNull
    private SQLQueryCompletionContext prepareCompletionContext(@NotNull SQLScriptItemAtOffset scriptItem, int offset) {
        int position = offset - scriptItem.offset;
    
        SQLQueryModel model = scriptItem.item.getQueryModel();
        if (model != null) {
            STMTreeNode syntaxNode = model.getSyntaxNode();
            LSMInspections.SyntaxInspectionResult syntaxInspectionResult = LSMInspections.prepareAbstractSyntaxInspection(syntaxNode, position);
            SQLQueryDataContext context = null;
            SQLQueryNodeModel node = model.findNodeContaining(position);
            SQLQueryLexicalScopeItem lexicalItem = null;
            if (node != null) {
                SQLQueryLexicalScope scope = node.findLexicalScope(position);
                if (scope != null) {
                    context = scope.getContext();
                    lexicalItem = scope.findItem(position);
                }
                if (context == null) {
                    context = node.getGivenDataContext();
                }
            }
            
            ArrayDeque<STMTreeTermNode> nameNodes = new ArrayDeque<>();
            List<STMTreeTermNode> allTerms = LSMInspections.prepareTerms(syntaxNode);
            int index = STMUtils.binarySearchByKey(allTerms, t -> t.getRealInterval().a, position, Comparator.comparingInt(k -> k));
            if (index < 0) {
                index = ~index - 1;
            }
            if (allTerms.get(index).getRealInterval().b == position  - 1) {
                for (int i = index; i >= 0; i--) {
                    STMTreeTermNode term = allTerms.get(i);
                    if (knownIdentifierPartTerms.contains(term.symbol.getType())
                        || (term.getStmParent() != null && term.getStmParent().getNodeKindId() == SQLStandardParser.RULE_nonReserved)
                    ) {
                        nameNodes.addFirst(term);
                    } else {
                        break;
                    }
                }
            }
            return SQLQueryCompletionContext.prepare(
                scriptItem,
                this.editor.getExecutionContext(),
                syntaxInspectionResult,
                context,
                lexicalItem,
                nameNodes.toArray(STMTreeTermNode[]::new)
            );
        } else {
            return SQLQueryCompletionContext.EMPTY;
        }
    }

    @NotNull
    private SQLDocumentSyntaxContext getContext() {
        return this.context;
    }

    private void beforeDocumentModification(DocumentEvent event) {
        this.cancel();
        
        int insertedLength = event.getText() == null ? 0 : event.getText().length();
        
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
                }
                this.queuedForReparse.put(reparseStart, new QueuedRegionInfo(reparseLength));
            }
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
        if (!knownRange.properlyContains(visibleRange)) {
            Interval unknownRange = visibleRange.differenceNotProperlyContained(knownRange);
            if (unknownRange == null) {
                unknownRange = visibleRange;
            }
            this.enqueueToReparse(unknownRange.a, unknownRange.length());
            this.schedule(null);
        }
    }
    
    private void schedule(@Nullable DocumentEvent event) {
        synchronized (this.syncRoot) {
            if (this.editor.getRuleManager() == null || !this.editor.isAdvancedHighlightingEnabled() ||
                !SQLEditorUtils.isSQLSyntaxParserApplied(this.editor.getEditorInput())
            ) {
                return;
            }

            if (this.job.getState() != Job.RUNNING) {
                this.job.cancel();
            }
            this.job.schedule(schedulingTimeoutMilliseconds * (this.isRunning ? 2 : 1));
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
            
            if (newDocument != null && SQLEditorUtils.isSQLSyntaxParserApplied(editor.getEditorInput())) {
                this.document = newDocument;
                this.reset();
            }
        }
    }
    
    private void reset() {
        synchronized (this.syncRoot) {
            this.context.clear();
            this.queuedForReparse.clear();
            this.knownRegionEnd = 0;
            this.knownRegionStart = 0;
            this.ensureVisibleRangeIsParsed();
        }
    }

    private void doWork(DBRProgressMonitor jobMonitor) throws BadLocationException {
        TextViewer viewer = editor.getTextViewer();
        if (viewer == null || this.editor.getRuleManager() == null) {
            return;
        }
        Interval visibleFragment = UIUtils.syncExec(new RunnableWithResult<>() {
            public Interval runWithResult() {
                if (viewer == null || viewer.getDocument() == null) {
                    return null;
                }
                int startOffset = viewer.getTopIndexStartOffset();
                int endOffset = viewer.getBottomIndexEndOffset();
                return new Interval(startOffset, endOffset);
            }
        });

        if (visibleFragment == null) {
            return;
        }
        int workOffset;
        int workLength;
        try {
            synchronized (this.syncRoot) {
                this.isRunning = true;
                
                int stepsToKeep = 2;
                int rangeStart = Math.max(0, visibleFragment.a - visibleFragment.length() * stepsToKeep); 
                int rangeEnd = Math.max(0, visibleFragment.b + visibleFragment.length() * stepsToKeep);
                Interval actualFragment = new Interval(rangeStart, rangeEnd);
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
                Interval workInterval = new Interval(workOffset, workOffset + workLength);
                if (!actualFragment.properlyContains(workInterval)) {
                    workInterval = actualFragment.intersection(workInterval);
                    workOffset = workInterval.a;
                    workLength = workInterval.length();
                }

                int docTailDelta = this.document.getLength() - (workOffset + workLength);
                if (docTailDelta < 0) {
                    workLength += docTailDelta; 
                }
                if (DEBUG) {
                    {
                        NodesIterator<QueuedRegionInfo> it = this.queuedForReparse.nodesIteratorAt(Integer.MAX_VALUE);
                        while (it.prev()) {
                            log.debug("\t@" + it.getCurrOffset() + "+" + it.getCurrValue().length);
                        }
                    }
                }
                
                this.queuedForReparse.clear();
            }
        } catch (Throwable ex) {
            log.error(ex);
            return;
        }

        IProgressMonitor monitor = jobMonitor.getNestedMonitor(); //Job.getJobManager().createProgressGroup();
        try {
            if (workLength == 0) {
                return;
            }
            
            SQLParserContext parserContext = new SQLParserContext(this.editor.getDataSource(), this.editor.getSyntaxManager(), this.editor.getRuleManager(), this.document);
            List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(parserContext, workOffset, workLength, false, false, false);
            if (elements.isEmpty()) {
                if (DEBUG) {
                    log.debug("No script elements to parse in range " + workOffset + "+" + workLength);
                }
                return;
            } else {
                SQLScriptElement element = SQLScriptParser.extractQueryAtPos(parserContext, elements.get(0).getOffset());
                if (element != null) {
                    elements.set(0, element);
                }
                if (elements.size() > 1) {
                    int index = elements.size() - 1;
                    element = SQLScriptParser.extractQueryAtPos(parserContext, elements.get(index).getOffset());
                    if (element != null) {
                        elements.set(index, element);
                    }
                }
            }
            
            {
                SQLScriptElement lastElement = elements.get(elements.size() - 1);
                if (lastElement == null) {
                    return;
                }
                workOffset = elements.get(0).getOffset();
                workLength = lastElement.getOffset() + lastElement.getLength() - workOffset;
                if (DEBUG) {
                    log.debug("parsing " + workOffset + "+" + workLength);
                }
            }

            boolean useRealMetadata = this.editor.isReadMetadataForQueryAnalysisEnabled();
            DBCExecutionContext executionContext = this.editor.getExecutionContext();
            
            monitor.beginTask("Background query analysis for " + editor.getTitle(), 1 + elements.size());
            monitor.worked(1);

            SQLSyntaxManager syntaxManager = this.editor.getSyntaxManager();

            int i = 1;
            for (SQLScriptElement element : elements) {
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    SQLQueryModelRecognizer recognizer = new SQLQueryModelRecognizer(executionContext, useRealMetadata, syntaxManager);
                    SQLQueryModel queryModel = recognizer.recognizeQuery(
                        element.getOriginalText(),
                        RuntimeUtils.makeMonitor(monitor)
                    );
                
                    if (queryModel != null) {
                        if (DEBUG) {
                            log.debug("registering script item @" + element.getOffset() + "+" + element.getLength());
                        }
                        SQLDocumentScriptItemSyntaxContext itemContext = this.context.registerScriptItemContext(
                            element.getOriginalText(), 
                            queryModel,
                            element.getOffset(),
                            element.getLength()
                        );
                        itemContext.clear();
                        for (SQLQuerySymbolEntry entry : queryModel.getAllSymbols()) {
                            itemContext.registerToken(entry.getInterval().a, entry);
                        }
                        itemContext.refreshCompleted();
                    }
                } catch (Throwable ex) {
                    log.debug("Error while analyzing query text: " + element.getOriginalText(), ex);
                }
                monitor.worked(1);
                monitor.setTaskName("Background query analysis: subtask #" + (i++));
            }
            this.context.resetLastAccessCache();
        } catch (Throwable ex) {
            log.debug(ex);
        } finally {
            monitor.done();
        }
        
        int parsedOffset = workOffset;
        int parsedLength = workLength;
        
        synchronized (this.syncRoot) {
            this.knownRegionStart = Math.min(this.knownRegionStart, parsedOffset);
            this.knownRegionEnd = Math.max(this.knownRegionEnd, parsedOffset + parsedLength);
            if (DEBUG) {
                log.debug("known is " + knownRegionStart + "-" + knownRegionEnd);
            }
            this.isRunning = false;
        }
        
        UIUtils.asyncExec(() -> {
            viewer.invalidateTextPresentation(parsedOffset, parsedLength);
        });
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
