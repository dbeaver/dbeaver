/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tasks.ui.sql;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;


class SQLToolTaskWizardPageStatus extends ActiveWizardPage<SQLToolTaskWizard> {

    private static final Log log = Log.getLog(SQLToolTaskWizardPageStatus.class);

    private OutputStreamWriter writer;
    private MessageConsole console;

    public SQLToolTaskWizardPageStatus(SQLToolTaskWizard wizard)
    {
        super("Tool status");
        setTitle("Execution status");
        setDescription("Tool execution status");
    }

    @Override
    public void activatePage() {
        setTitle("Tool " + getWizard().getTaskType().getName() + "");
        super.activatePage();
    }

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FillLayout());

        console = new MessageConsole("tool-log-console", null);
        LogConsoleViewer consoleViewer = new LogConsoleViewer(composite);
        console.setWaterMarks(1024*1024*3, 1024*1024*4);

        writer = new OutputStreamWriter(console.newMessageStream(), StandardCharsets.UTF_8);

        setControl(composite);
    }

    public Writer getLogWriter() {
        return writer;
    }

    public void appendLine(final String line)
    {
        if (getShell().isDisposed()) {
            return;
        }
        try {
            writer.write(line + "\n");
            writer.flush();
        } catch (IOException e) {
            log.debug(e);
        }
    }

    private class LogConsoleViewer extends TextConsoleViewer implements IDocumentListener {
        LogConsoleViewer(Composite composite) {
            super(composite, console);
        }

        @Override
        public void setDocument(IDocument document) {
            IDocument oldDocument= getDocument();
            super.setDocument(document);
            if (oldDocument != null) {
                oldDocument.removeDocumentListener(this);
            }
            if (document != null) {
                document.addDocumentListener(this);
            }
        }

        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            revealEndOfDocument();
        }
    }
}
