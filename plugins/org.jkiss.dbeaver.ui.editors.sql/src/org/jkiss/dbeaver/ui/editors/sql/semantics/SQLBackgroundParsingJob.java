/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.IRule;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSemanticAnalysisDepth;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionModel;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;

import java.util.*;

public class SQLBackgroundParsingJob {

    private static final Log log = Log.getLog(SQLBackgroundParsingJob.class);

    private static final Timer schedulingTimer = new Timer("SQLBackgroundParsingJob.schedulingTimer.thread", true); //$NON-NLS-1
    private static final long schedulingTimeoutMilliseconds = 500;

    private final Object syncRoot = new Object();
    private final SQLEditorBase editor;
    private SQLDocumentSyntaxContext context = null;
    private IDocument document = null;
    private volatile TimerTask task = null;
    private volatile boolean isRunning = false;
    private final List<DocumentEvent> documentEvents = new LinkedList<>();

    private final DocumentLifecycleListener documentListener = new DocumentLifecycleListener();

    public SQLBackgroundParsingJob(SQLEditorBase editor) {
        this.editor = editor;
    }

    public SQLDocumentSyntaxContext getCurrentContext() {
        return context;
    }

    /**
     * Setup job - add listeners, schedule
     */
    public void setup() {
        synchronized (syncRoot) {
            if (this.editor.getTextViewer() != null) {
                this.editor.getTextViewer().addTextInputListener(documentListener);
                this.editor.getTextViewer().addViewportListener(documentListener);                
                if (this.document == null) {
                    IDocument document = this.editor.getTextViewer().getDocument();
                    if (document != null) {
                        this.document = document;
                        this.document.addDocumentListener(documentListener);
                    }
                }
                this.schedule(null);
            }
        }
    }

    /**
     * Dispose job - cancel schedule and remove listeners.
     */
    public void dispose() {
        synchronized (syncRoot) {
            this.cancel();
            TextViewer textViewer = this.editor.getTextViewer();
            if (textViewer != null) {
                textViewer.removeViewportListener(documentListener);
                textViewer.removeTextInputListener(documentListener);
                if (this.document != null) {
                    this.document.removeDocumentListener(documentListener);
                }
            }
        }
    }

    @NotNull
    private SQLDocumentSyntaxContext getContext() {
        if (this.context == null) {
            this.context = new SQLDocumentSyntaxContext(this.editor.getDocument());
        }
        return this.context;
    }

    /**
     * Prepare list of syntax rules by token types
     */
    @NotNull
    public IRule[] prepareRules(@NotNull SQLRuleScanner sqlRuleScanner) {
        this.getContext();
        return new IRule[] {
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_TABLE),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_TABLE_ALIAS),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_COLUMN),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_COLUMN_DERIVED),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_SCHEMA),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_SEMANTIC_ERROR)
        };
    }

    private void schedule(@Nullable DocumentEvent event) {
        synchronized (this.syncRoot) {
            if (this.editor.getRuleManager() == null || this.editor.getSemanticAnalysisDepth().equals(SQLSemanticAnalysisDepth.None) ||
                !SQLEditorUtils.isSQLSyntaxParserApplied(this.editor.getEditorInput())
            ) {
                this.context = null;
                return;
            }

            if (this.task == null) { // test wether we really need to create new task instance each time even when rescheduling
	            this.task = new TimerTask() {
	                @Override
	                public void run() {
	                    try {
	                        SQLBackgroundParsingJob.this.doWork();
	                    } catch (BadLocationException e) {
	                        log.debug(e);
	                    }
	                }
	            };
            }
            if (event != null) {
                // TODO drop only on lines-set change and apply in line offset on local insert or remove
                // this.getContext().dropLineOfOffset(event.getOffset());
                this.getContext().dropLinesOfRange(event.getOffset(), event.getLength());
                //documentEvents.add(event);
                // System.out.println(event);
                // this.getContext().replace(event.getOffset(), event.getLength(), event.getText().length());
            }
            schedulingTimer.schedule(this.task, schedulingTimeoutMilliseconds * (this.isRunning ? 2 : 1));
        }
    }

    private void cancel() {
        synchronized (this.syncRoot) {
            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
        }
    }

    private void setDocument(@Nullable IDocument newDocument) {
        synchronized (this.syncRoot) {
            if (this.document != null) {
                this.cancel();
                this.document = null;
                this.context = null;
            }

            if (newDocument != null && SQLEditorUtils.isSQLSyntaxParserApplied(editor.getEditorInput())) {
                this.context = new SQLDocumentSyntaxContext(newDocument);
                this.document = newDocument;
                this.schedule(null);
            }
        }
    }

    private void doWork() throws BadLocationException {
        synchronized (this.syncRoot) {
            this.task = null;
            this.isRunning = true;
        }
        IProgressMonitor monitor = Job.getJobManager().createProgressGroup();
        SQLDocumentSyntaxContext context = new SQLDocumentSyntaxContext(document);
        try {
            TextViewer viewer = editor.getTextViewer();
            if (viewer == null) {
                return;
            }
            IRegion region = UIUtils.syncExec(new RunnableWithResult<>() {
                @Override
                public IRegion runWithResult() {
                    return viewer.getVisibleRegion();
                }
            });
            if (region == null) {
                return;
            }
            if (editor.getRuleManager() == null) {
                return;
            }
            List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(
                new SQLParserContext(editor.getDataSource(), editor.getSyntaxManager(), editor.getRuleManager(), document),
                region.getOffset(),
                region.getLength(),
                false,
                false,
                false
            );

            SQLSemanticAnalysisDepth analysisDepth = editor.getSemanticAnalysisDepth();
            DBCExecutionContext executionContext = editor.getExecutionContext();
            
            monitor.beginTask("Background query analysis", 1 + elements.size());
            monitor.worked(1);

            int i = 1;
            for (SQLScriptElement element : elements) {
                try {
                    SQLQueryModelRecognizer recognizer = new SQLQueryModelRecognizer(analysisDepth, executionContext);
                    SQLQuerySelectionModel queryModel = recognizer.recognizeQuery(element.getOriginalText());
                
                    if (queryModel != null) {
                        for (SQLQuerySymbolEntry entry : queryModel.getAllSymbols()) {
                            context.registerToken(new SQLDocumentSyntaxTokenEntry(element, entry));
                        }
                    }
                } catch (Throwable ex) {
                    log.debug(ex);
                }
                monitor.worked(1);
                monitor.setTaskName("Background query analysis: subtask #" + (i++));
            }
        } catch (Throwable ex) {
            log.debug(ex);
        } finally {
            monitor.done();
        }

        synchronized (this.syncRoot) {
            this.context = context;
            this.isRunning = false;
        }
        
        UIUtils.asyncExec(() -> {
            TextViewer viewer = editor.getTextViewer();
            if (viewer != null) {
                viewer.invalidateTextPresentation(0, this.document.getLength());
            }
        });
    }

    private class DocumentLifecycleListener implements IDocumentListener, ITextInputListener, IViewportListener {

        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.cancel();
            // TODO apply offset at the current location
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
            //SQLBackgroundParsingJob.this.schedule(null);
            // TODO drop newly hidden elements' info 
        }
    }
}
