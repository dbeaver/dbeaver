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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.sql.task.SQLToolStatistics;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.itemlist.ObjectListControl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


class SQLToolTaskWizardPageStatus extends ActiveWizardPage<SQLToolTaskWizard> {

    private static final Log log = Log.getLog(SQLToolTaskWizardPageStatus.class);

    private OutputStreamWriter writer;
    private MessageConsole console;
    private ObjectListControl<SQLToolStatistics> statusTable;
    private AbstractJob statusUpdateJob;
    private final List<SQLToolStatistics> toolStatistics = new ArrayList<>();

    SQLToolTaskWizardPageStatus(SQLToolTaskWizard wizard) {
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
    public boolean isPageComplete() {
        return true;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FillLayout());

        SashForm partDivider = new SashForm(composite, SWT.VERTICAL);
        partDivider.setSashWidth(5);

        statusTable = new ObjectListControl<SQLToolStatistics>(partDivider, SWT.SHEET, new ListContentProvider()) {
            @NotNull
            @Override
            protected String getListConfigId(List<Class<?>> classList) {
                return "SQLToolStatus." + getWizard().getTaskType().getId();
            }

            @Override
            protected DBPImage getObjectImage(SQLToolStatistics item) {
                return DBValueFormatting.getObjectImage(item.getObject());
            }

            @Override
            protected LoadingJob<Collection<SQLToolStatistics>> createLoadService() {
                return LoadingJob.createService(
                    new DummyLoadService(),
                    new ObjectsLoadVisualizer());
            }
        };

        console = new MessageConsole("tool-log-console", null);
        LogConsoleViewer consoleViewer = new LogConsoleViewer(partDivider);
        console.setWaterMarks(1024 * 1024 * 3, 1024 * 1024 * 4);

        writer = new OutputStreamWriter(console.newMessageStream(), StandardCharsets.UTF_8);

        setControl(composite);
    }

    Writer getLogWriter() {
        return writer;
    }

    void clearLog() {
        statusTable.clearListData();
        console.clearConsole();
    }

    public void appendLine(final String line) {
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

    void addStatistics(DBPObject object, List<? extends SQLToolStatistics> statistics) {
        synchronized (toolStatistics) {
            toolStatistics.addAll(statistics);
        }
        if (statusUpdateJob == null) {
            statusUpdateJob = new AbstractJob("Update tool status") {
                {
                    setSystem(true);
                    setUser(false);
                }
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    List<SQLToolStatistics> statsCopy;
                    synchronized (toolStatistics) {
                        statsCopy = new ArrayList<>(SQLToolTaskWizardPageStatus.this.toolStatistics);
                        SQLToolTaskWizardPageStatus.this.toolStatistics.clear();
                    }
                    UIUtils.asyncExec(() -> {
                        statusTable.appendListData(statsCopy);
                        statusTable.repackColumns();
                    });
                    return Status.OK_STATUS;
                }
            };
        }
        statusUpdateJob.schedule(100);
    }

    private class LogConsoleViewer extends TextConsoleViewer implements IDocumentListener {
        LogConsoleViewer(Composite composite) {
            super(composite, console);
        }

        @Override
        public void setDocument(IDocument document) {
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
        public void documentAboutToBeChanged(DocumentEvent event) {
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            revealEndOfDocument();
        }
    }

    private class DummyLoadService extends AbstractLoadService<Collection<SQLToolStatistics>> {
        DummyLoadService() {
            super("Load status");
        }

        @Override
        public Collection<SQLToolStatistics> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
            try {
                return Collections.emptyList();
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }

        @Override
        public Object getFamily() {
            return getWizard().getTaskType();
        }
    }

}
