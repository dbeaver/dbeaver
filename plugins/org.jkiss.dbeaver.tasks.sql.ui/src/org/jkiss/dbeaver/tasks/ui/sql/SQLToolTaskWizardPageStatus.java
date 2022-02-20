/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
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
import org.jkiss.dbeaver.tasks.ui.sql.internal.TasksSQLUIMessages;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.IWizardPageNavigable;
import org.jkiss.dbeaver.ui.navigator.itemlist.ObjectListControl;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


class SQLToolTaskWizardPageStatus extends ActiveWizardPage<SQLToolTaskWizard> implements IWizardPageNavigable {

    private static final Log log = Log.getLog(SQLToolTaskWizardPageStatus.class);

    private PrintStream writer;
    private MessageConsole console;
    private ObjectListControl<SQLToolStatistics> statusTable;
    private AbstractJob statusUpdateJob;
    private final List<SQLToolStatistics> toolStatistics = new ArrayList<>();

    SQLToolTaskWizardPageStatus(SQLToolTaskWizard wizard) {
        super(TasksSQLUIMessages.sql_tool_task_wizard_page_status_name);
        setTitle(TasksSQLUIMessages.sql_tool_task_wizard_page_status_title);
        setDescription(TasksSQLUIMessages.sql_tool_task_wizard_page_status_description);
    }

    @Override
    public void activatePage() {
        setTitle(NLS.bind(TasksSQLUIMessages.sql_tool_task_wizard_page_status_activate_page_title, getWizard().getTaskType().getName()));
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
            protected LoadingJob<Collection<SQLToolStatistics>> createLoadService(boolean forUpdate) {
                return LoadingJob.createService(
                    new DummyLoadService(),
                    new ObjectsLoadVisualizer());
            }
        };

        console = new MessageConsole(TasksSQLUIMessages.sql_tool_task_wizard_page_status_message_console_name_tool_log, null);
        LogConsoleViewer consoleViewer = new LogConsoleViewer(partDivider);
        console.setWaterMarks(1024 * 1024 * 3, 1024 * 1024 * 4);

        try {
            writer = new PrintStream(console.newMessageStream(), true, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            writer = new PrintStream(console.newMessageStream(), true);
        }

        setControl(composite);
    }

    PrintStream getLogWriter() {
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
        writer.print(line + "\n");
        writer.flush();
    }

    void addStatistics(DBPObject object, List<? extends SQLToolStatistics> statistics) {
        synchronized (toolStatistics) {
            toolStatistics.addAll(statistics);
        }
        if (statusUpdateJob == null) {
            statusUpdateJob = new AbstractJob(TasksSQLUIMessages.sql_tool_task_wizard_page_status_update_job_name_update_tool) {
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

    @Override
    public boolean isPageNavigable() {
        return false;
    }

    @Override
    public boolean isPageApplicable() {
        return true;
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
            super(TasksSQLUIMessages.sql_tool_task_wizard_page_status_dummy_load_service_name);
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
