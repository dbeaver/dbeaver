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
package org.jkiss.dbeaver.tasks.ui.sql.script;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.dialogs.IWizardPageNavigable;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Embedded log page shown inside the "Execute SQL Scripts" wizard while the script is running.
 */
class SQLScriptTaskPageLog extends WizardPage implements IWizardPageNavigable {

    private MessageConsole console;
    private PrintStream writer;

    SQLScriptTaskPageLog() {
        super("sqlScriptExecuteLog");
        setTitle("Execution log");
        setDescription("SQL script execution progress");
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FillLayout());

        console = new MessageConsole("sql-script-execute-log", null);
        console.setWaterMarks(1024 * 1024 * 3, 1024 * 1024 * 4);
        new LogConsoleViewer(composite);

        writer = new PrintStream(console.newMessageStream(), true, StandardCharsets.UTF_8);

        setControl(composite);
    }

    public PrintStream getLogWriter() {
        return writer;
    }

    @Override
    public boolean isPageNavigable() {
        return false;
    }

    @Override
    public boolean isPageApplicable() {
        return true;
    }

    private class LogConsoleViewer extends TextConsoleViewer implements IDocumentListener {
        LogConsoleViewer(@NotNull Composite composite) {
            super(composite, SQLScriptTaskPageLog.this.console);
        }

        @Override
        protected void createControl(@NotNull Composite parent, int styles) {
            super.createControl(parent, styles);
            getTextWidget().setWordWrap(true);
        }

        @Override
        public void setDocument(@Nullable IDocument document) {
            IDocument oldDocument = getDocument();
            super.setDocument(document);
            if (oldDocument != null) {
                oldDocument.removeDocumentListener(this);
            }
            if (document != null) {
                document.addDocumentListener(this);
            }
        }

        @Override
        public void documentAboutToBeChanged(@NotNull DocumentEvent event) {
        }

        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            revealEndOfDocument();
        }
    }
}
