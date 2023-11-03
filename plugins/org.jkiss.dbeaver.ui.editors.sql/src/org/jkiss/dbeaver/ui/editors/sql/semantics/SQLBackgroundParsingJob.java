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
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.rules.IRule;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
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

    private final DocumentLifecycleListener documentListener = new DocumentLifecycleListener();

    public SQLBackgroundParsingJob(SQLEditorBase editor) {
        this.editor = editor;
    }

    public SQLDocumentSyntaxContext getCurrentContext() {
        return context;
    }

    public void setup() {
        synchronized (syncRoot) {
            if (this.editor.getTextViewer() != null) {
                this.editor.getTextViewer().addTextInputListener(documentListener);
                if (this.document == null) {
                    IDocument document = this.editor.getTextViewer().getDocument();
                    if (document != null) {
                        this.document = document;
                        this.document.addDocumentListener(documentListener);
                        this.schedule(null);
                    }
                }
            }
        }
    }

    public void dispose() {
        synchronized (syncRoot) {
            this.cancel();
            this.editor.getTextViewer().removeTextInputListener(documentListener);
            if (this.document != null) {
                this.document.removeDocumentListener(documentListener);
            }
        }
    }

    private SQLDocumentSyntaxContext getContext() {
        if (this.context == null) {
            this.context = new SQLDocumentSyntaxContext(this.editor.getDocument());
        }
        return this.context;
    }

    public IRule[] prepareRules(SQLRuleScanner sqlRuleScanner) {
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

    private void schedule(DocumentEvent event) {
        synchronized (syncRoot) {
            if (editor.getRuleManager() == null) {
                return;
            }

            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        SQLBackgroundParsingJob.this.doWork();
                    } catch (BadLocationException e) {
                        log.debug(e);
                    }
                }
            };
            if (event != null) {
                // TODO drop only on lines-set change and apply in line offset on local insert or remove
                this.getContext().dropLineOfOffset(event.getOffset());
            }
            schedulingTimer.schedule(task, schedulingTimeoutMilliseconds * (this.isRunning ? 2 : 1));
        }
    }

    private void cancel() {
        synchronized (syncRoot) {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    private void setDocument(IDocument newDocument) {
        synchronized (syncRoot) {
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
        synchronized (syncRoot) {
            this.task = null;
            this.isRunning = true;
        }
        IProgressMonitor monitor = Job.getJobManager().createProgressGroup();
        SQLDocumentSyntaxContext context = new SQLDocumentSyntaxContext(document);
        try {
            
//            monitor.beginTask("Background query analysis", 100);
            
            List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(
                new SQLParserContext(editor.getDataSource(), editor.getSyntaxManager(), editor.getRuleManager(), document),
                0,
                document.getLength(),
                true,
                false,
                false
            );
//            monitor.worked(10);

            DBCExecutionContext executionContext = editor.getExecutionContext();
            
            monitor.beginTask("Background query analysis", 1 + elements.size());
            monitor.worked(1);
            
//                IProgressMonitor pm = monitor.slice(90);
//                pm.beginTask("Analyzing queries", elements.size());
            int i = 1;
            for (SQLScriptElement element : elements) {
                SQLQueryModelRecognizer recognizer = new SQLQueryModelRecognizer(executionContext);
                SQLQuerySelectionModel queryModel = recognizer.recognizeQuery(element.getOriginalText());

                if (queryModel != null) {
                    for (SQLQuerySymbolEntry entry: queryModel.getAllSymbols()) {
//                            System.out.println(entry);
                        context.registerToken(new SQLDocumentSyntaxTokenEntry(element, entry));
                    }
                }
                
                // Thread.sleep(2000);
                monitor.worked(1);
                monitor.setTaskName("#" + (i++));
//                    pm.worked(1);
            }
//                pm.done();
            
            UIUtils.asyncExec(() -> {
                editor.getTextViewer().invalidateTextPresentation(0, document.getLength());
            });            
            
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            monitor.done();
        }

        synchronized (syncRoot) {
            this.context = context;
            this.isRunning = false;
        }
    }

    private class DocumentLifecycleListener implements IDocumentListener, ITextInputListener {

        public void documentAboutToBeChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.cancel();
        }

        public void documentChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.schedule(event);
        }

        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
            if (oldInput != null) {
                SQLBackgroundParsingJob.this.cancel();
                oldInput.removeDocumentListener(this);
            }
        }

        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            if (newInput != null) {
                newInput.addDocumentListener(this);
                SQLBackgroundParsingJob.this.setDocument(newInput);
            }
        }
    }
}
