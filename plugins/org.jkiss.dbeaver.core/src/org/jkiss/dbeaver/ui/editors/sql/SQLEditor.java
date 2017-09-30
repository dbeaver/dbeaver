/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.rulers.IColumnSupport;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.eclipse.ui.texteditor.rulers.RulerColumnRegistry;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.*;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.DefaultServerOutputReader;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLResultsConsumer;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetListener;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.INonPersistentEditorInput;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogPanel;
import org.jkiss.dbeaver.ui.editors.text.ScriptPositionColumn;
import org.jkiss.dbeaver.ui.views.SQLResultsView;
import org.jkiss.dbeaver.ui.views.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL Executor
 */
public class SQLEditor extends SQLEditorBase implements
    IDataSourceContainerProviderEx,
    DBPContextProvider,
    DBPEventListener,
    ISaveablePart2,
    DBPDataSourceUser,
    DBPDataSourceHandler,
    DBPPreferenceListener
{
    private static final long SCRIPT_UI_UPDATE_PERIOD = 100;

    private static Image IMG_DATA_GRID = DBeaverActivator.getImageDescriptor("/icons/sql/page_data_grid.png").createImage(); //$NON-NLS-1$
    private static Image IMG_DATA_GRID_LOCKED = DBeaverActivator.getImageDescriptor("/icons/sql/page_data_grid_locked.png").createImage(); //$NON-NLS-1$
    private static Image IMG_EXPLAIN_PLAN = DBeaverActivator.getImageDescriptor("/icons/sql/page_explain_plan.png").createImage(); //$NON-NLS-1$
    private static Image IMG_LOG = DBeaverActivator.getImageDescriptor("/icons/sql/page_error.png").createImage(); //$NON-NLS-1$
    private static Image IMG_OUTPUT = DBeaverActivator.getImageDescriptor("/icons/sql/page_output.png").createImage(); //$NON-NLS-1$
    private static Image IMG_OUTPUT_ALERT = DBeaverActivator.getImageDescriptor("/icons/sql/page_output_alert.png").createImage(); //$NON-NLS-1$

    public static final String VAR_CONNECTION_NAME = "connectionName";
    public static final String VAR_FILE_NAME = "fileName";
    public static final String VAR_FILE_EXT = "fileExt";
    public static final String VAR_DRIVER_NAME = "driverName";

    public static final String DEFAULT_TITLE_PATTERN = "<${" + VAR_CONNECTION_NAME + "}> ${" + VAR_FILE_NAME + "}";

    private ResultSetOrientation resultSetOrientation = ResultSetOrientation.HORIZONTAL;
    private SashForm sashForm;
    private Control editorControl;
    private CTabFolder resultTabs;
    private ToolItem toolOutputItem;

    private SQLLogPanel logViewer;
    private SQLEditorOutputViewer outputViewer;

    private volatile QueryProcessor curQueryProcessor;
    private final List<QueryProcessor> queryProcessors = new ArrayList<>();

    private DBPDataSourceContainer dataSourceContainer;
    private DBPDataSource curDataSource;
    private volatile DBCExecutionContext executionContext;
    private volatile DBCExecutionContext lastExecutionContext;
    private SQLScriptContext globalScriptContext;
    private volatile boolean syntaxLoaded = false;
    private volatile boolean ownContext = false;
    private final FindReplaceTarget findReplaceTarget = new FindReplaceTarget();
    private final List<SQLQuery> runningQueries = new ArrayList<>();
    private QueryResultsContainer curResultsContainer;
    private Image editorImage;
    private ToolItem toolLogItem;

    public SQLEditor()
    {
        super();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Nullable
    public IProject getProject()
    {
        IFile file = EditorUtils.getFileFromInput(getEditorInput());
        return file == null ? null : file.getProject();
    }

    @Nullable
    @Override
    public int[] getCurrentLines()
    {
        synchronized (runningQueries) {
            Document document = getDocument();
            if (document == null || runningQueries.isEmpty()) {
                return null;
            }
            List<Integer> lines = new ArrayList<>(runningQueries.size() * 2);
            for (SQLQuery statementInfo : runningQueries) {
                try {
                    int firstLine = document.getLineOfOffset(statementInfo.getOffset());
                    int lastLine = document.getLineOfOffset(statementInfo.getOffset() + statementInfo.getLength());
                    for (int k = firstLine; k <= lastLine; k++) {
                        lines.add(k);
                    }
                } catch (BadLocationException e) {
                    // ignore - this may happen is SQL was edited after execution start
                }
            }
            if (lines.isEmpty()) {
                return null;
            }
            int[] results = new int[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                results[i] = lines.get(i);
            }
            return results;
        }
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }

    @Override
    public boolean setDataSourceContainer(@Nullable DBPDataSourceContainer container)
    {
        if (container == dataSourceContainer) {
            fireDataSourceChange();
            return true;
        }

        // Release ds container
        releaseContainer();
        closeAllJobs();

        dataSourceContainer = container;
        if (dataSourceContainer != null) {
            dataSourceContainer.getPreferenceStore().addPropertyChangeListener(this);
            dataSourceContainer.getRegistry().addDataSourceListener(this);
        }
        IEditorInput input = getEditorInput();
        if (input != null) {
            EditorUtils.setInputDataSource(input, container, true);
        }

        checkConnected(false, null);
        setPartName(getEditorName());

        fireDataSourceChange();

        if (dataSourceContainer != null) {
            dataSourceContainer.acquire(this);
        }

        return true;
    }

    private void updateExecutionContext() {
        if (dataSourceContainer == null) {
            releaseExecutionContext();
        } else {
            // Get/open context
            final DBPDataSource dataSource = dataSourceContainer.getDataSource();
            if (dataSource == null) {
                releaseExecutionContext();
            } else if (curDataSource != dataSource) {
                releaseExecutionContext();
                curDataSource = dataSource;
                if (curDataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.EDITOR_SEPARATE_CONNECTION)) {
                    final OpenContextJob job = new OpenContextJob(dataSource);
                    job.addJobChangeListener(new JobChangeAdapter() {
                        @Override
                        public void done(IJobChangeEvent event) {
                            if (job.error != null) {
                                releaseExecutionContext();
                                DBUserInterface.getInstance().showError("Open context", "Can't open editor connection", job.error);
                            } else {
                                fireDataSourceChange();
                            }
                        }
                    });
                    job.schedule();
/*
                    try {
                        DBeaverUI.runInProgressDialog(new DBRRunnableWithProgress() {
                            @Override
                            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                monitor.beginTask("Open SQLEditor isolated connection", 1);
                                try {
                                    String title = "SQLEditor <" + getEditorInput().getPath().removeFileExtension().lastSegment() + ">";
                                    monitor.subTask("Open context " + title);
                                    executionContext = dataSource.openIsolatedContext(monitor, title);
                                } catch (DBException e) {
                                    throw new InvocationTargetException(e);
                                } finally {
                                    monitor.done();
                                }
                                ownContext = true;
                            }
                        });
                    } catch (InvocationTargetException e) {
                        releaseExecutionContext();
                        UIUtils.showErrorDialog(getSite().getShell(), "Open context", "Can't open editor connection", e);
                    }
*/
                } else {
                    executionContext = dataSource.getDefaultContext(false);
                }
            }
        }
        if (this.executionContext != null) {
            SQLScriptContext oldGlobalContext = globalScriptContext;
            this.globalScriptContext = new SQLScriptContext(
                    null,
                    executionContext,
                    EditorUtils.getLocalFileFromInput(getEditorInput()),
                    new OutputLogWriter());
            if (oldGlobalContext != null) {
                this.globalScriptContext.copyFrom(oldGlobalContext);
            }
        } else {
            this.globalScriptContext = null;
        }
    }

    private void releaseExecutionContext() {
        if (ownContext && executionContext != null && executionContext.isConnected()) {
            // Close context in separate job (otherwise it can block UI)
            new CloseContextJob(executionContext).schedule();
        }
        executionContext = null;
        ownContext = false;
        curDataSource = null;
    }

    private void releaseContainer() {
        releaseExecutionContext();

        if (dataSourceContainer != null) {
            dataSourceContainer.getPreferenceStore().removePropertyChangeListener(this);
            dataSourceContainer.getRegistry().removeDataSourceListener(this);
            dataSourceContainer.release(this);
            dataSourceContainer = null;
        }
    }

    private class OutputLogWriter extends Writer {
        @Override
        public void write(@NotNull final char[] cbuf, final int off, final int len) throws IOException {
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!outputViewer.isDisposed()) {
                        outputViewer.getOutputWriter().write(cbuf, off, len);
                        outputViewer.scrollToEnd();
                        if (!outputViewer.isVisible()) {
                            updateOutputViewerIcon(true);
                        }
                    }
                }
            });
        }

        @Override
        public void flush() throws IOException {
            outputViewer.getOutputWriter().flush();
        }

        @Override
        public void close() throws IOException {

        }
    }

    private class OpenContextJob extends AbstractJob {
        private final DBPDataSource dataSource;
        private Throwable error;
        OpenContextJob(DBPDataSource dataSource) {
            super("Open connection to " + dataSource.getContainer().getName());
            this.dataSource = dataSource;
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            monitor.beginTask("Open SQLEditor isolated connection", 1);
            try {
                String title = "SQLEditor <" + getEditorInput().getName() + ">";
                monitor.subTask("Open context " + title);
                executionContext = dataSource.openIsolatedContext(monitor, title);
            } catch (DBException e) {
                error = e;
                return Status.OK_STATUS;
            } finally {
                monitor.done();
            }
            ownContext = true;
            return Status.OK_STATUS;
        }
    }

    private class CloseContextJob extends AbstractJob {
        private final DBCExecutionContext context;
        CloseContextJob(DBCExecutionContext context) {
            super("Close context " + context.getContextName());
            this.context = context;
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            monitor.beginTask("Close SQLEditor isolated connection", 1);
            try {
                if (QMUtils.isTransactionActive(context)) {
                    DataSourceHandler.closeActiveTransaction(monitor, context, true);
                }

                monitor.subTask("Close context " + context.getContextName());
                context.close();

            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }

    @Override
    public boolean isDirty()
    {
        for (QueryProcessor queryProcessor : queryProcessors) {
            if (queryProcessor.isDirty()) {
                return true;
            }
        }
        if (ownContext && QMUtils.isTransactionActive(executionContext)) {
            return true;
        }
        if (isNonPersistentEditor()) {
            // Console is never dirty
            return false;
        }
        return super.isDirty();
    }

    @Nullable
    @Override
    public Object getAdapter(Class required)
    {
        if (required == IFindReplaceTarget.class) {
            return findReplaceTarget;
        }
        ResultSetViewer resultsView = getActiveResultSetViewer();
        if (resultsView != null) {
            if (required == ResultSetViewer.class) {
                return resultsView;
            }
            Object adapter = resultsView.getAdapter(required);
            if (adapter != null) {
                return adapter;
            }
        }
        return super.getAdapter(required);
    }

    private boolean checkConnected(boolean forceConnect, DBRProgressListener onFinish)
    {
        // Connect to datasource
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        boolean doConnect = dataSourceContainer != null &&
            (forceConnect || dataSourceContainer.getPreferenceStore().getBoolean(DBeaverPreferences.EDITOR_CONNECT_ON_ACTIVATE));
        if (doConnect) {
            if (!dataSourceContainer.isConnected()) {
                DataSourceHandler.connectToDataSource(null, dataSourceContainer, onFinish);
            }
        }
        return dataSourceContainer != null && dataSourceContainer.isConnected();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        setRangeIndicator(new DefaultRangeIndicator());

        sashForm = UIUtils.createPartDivider(
                this,
                parent,
                resultSetOrientation.getSashOrientation() | SWT.SMOOTH);
        sashForm.setSashWidth(5);
        UIUtils.setHelp(sashForm, IHelpContextIds.CTX_SQL_EDITOR);

        super.createPartControl(sashForm);

        editorControl = sashForm.getChildren()[0];

        getSite().setSelectionProvider(new DynamicSelectionProvider());

        createResultTabs();

        // Update controls
        onDataSourceChange();

        setAction(ITextEditorActionConstants.SHOW_INFORMATION, null);
        //toolTipAction.setEnabled(false);

        CoreFeatures.SQL_EDITOR_OPEN.use();

        // Start output reader
        new ServerOutputReader().schedule();
    }

    private void createResultTabs()
    {
        resultTabs = new CTabFolder(sashForm, SWT.TOP | SWT.FLAT);
        resultTabs.setLayoutData(new GridData(GridData.FILL_BOTH));
        resultTabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Object data = e.item.getData();
                if (data instanceof QueryResultsContainer) {
                    setActiveResultsContainer((QueryResultsContainer) data);
                }
            }
        });
        getTextViewer().getTextWidget().addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_PAGE_NEXT) {
                    ResultSetViewer viewer = getActiveResultSetViewer();
                    if (viewer != null && viewer.getActivePresentation().getControl().isVisible()) {
                        viewer.getActivePresentation().getControl().setFocus();
                        e.doit = false;
                        e.detail = SWT.TRAVERSE_NONE;
                    }
                }
            }
        });
        resultTabs.setSimple(true);

        //resultTabs.setMRUVisible(true);
        {
            ToolBar rsToolbar = new ToolBar(resultTabs, SWT.HORIZONTAL | SWT.RIGHT | SWT.WRAP);

            toolLogItem = new ToolItem(rsToolbar, SWT.CHECK);
            toolLogItem.setText("Log");
            toolLogItem.setToolTipText(ActionUtils.findCommandDescription(CoreCommands.CMD_SQL_SHOW_LOG, getSite(), false));
            toolLogItem.setImage(IMG_LOG);
            toolLogItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showExecutionLogPanel();
                }
            });

            toolOutputItem = new ToolItem(rsToolbar, SWT.CHECK);
            toolOutputItem.setText("Output");
            toolOutputItem.setToolTipText(ActionUtils.findCommandDescription(CoreCommands.CMD_SQL_SHOW_OUTPUT, getSite(), false));
            toolOutputItem.setImage(IMG_OUTPUT);
            toolOutputItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    toolOutputItem.setImage(IMG_OUTPUT);
                    showOutputPanel();
                }
            });

            resultTabs.setTopRight(rsToolbar);
        }

        resultTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button == 2) {
                    CTabItem item = resultTabs.getItem(new Point(e.x, e.y));
                    if (item != null && item.getShowClose()) {
                        item.dispose();
                    }
                }
            }
        });
        resultTabs.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                if (event.button != 1) {
                    return;
                }
                CTabItem selectedItem = resultTabs.getItem(new Point(event.getBounds().x, event.getBounds().y));
                if (selectedItem != null && selectedItem  == resultTabs.getSelection()) {
                    toggleEditorMaximize();
                }
            }
        });

        // Extra views
        //planView = new ExplainPlanViewer(this, resultTabs);
        logViewer = new SQLLogPanel(resultTabs, this);
        outputViewer = new SQLEditorOutputViewer(getSite(), resultTabs, SWT.NONE);

        // Create results tab
        createQueryProcessor(true);

        {
            MenuManager menuMgr = new MenuManager();
            Menu menu = menuMgr.createContextMenu(resultTabs);
            menuMgr.addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    manager.add(ActionUtils.makeCommandContribution(getSite(), CoreCommands.CMD_SQL_EDITOR_MAXIMIZE_PANEL));
                    if (resultTabs.getItemCount() > 1) {
                        manager.add(new Action("Close multiple results") {
                            @Override
                            public void run()
                            {
                                closeExtraResultTabs(null);
                            }
                        });
                    }
                    final CTabItem activeTab = resultTabs.getSelection();
                    if (activeTab != null && activeTab.getData() instanceof QueryResultsContainer) {
                        if (resultTabs.indexOf(activeTab) > 0) {
                            final QueryResultsContainer resultsContainer = (QueryResultsContainer) activeTab.getData();
                            manager.add(new Separator());
                            final boolean isPinned = resultsContainer.isPinned();
                            manager.add(new Action(isPinned ? "Unpin tab" : "Pin tab") {
                                @Override
                                public void run() {
                                    resultsContainer.setPinned(!isPinned);
                                }
                            });
                        }
                        manager.add(new Action("Set tab title") {
                            @Override
                            public void run()
                            {
                                EnterNameDialog dialog = new EnterNameDialog(resultTabs.getShell(), "Tab title", activeTab.getText());
                                if (dialog.open() == IDialogConstants.OK_ID) {
                                    activeTab.setText(dialog.getResult());
                                }
                            }
                        });
                    }
                    if (activeTab != null && activeTab.getShowClose()) {
                        manager.add(ActionUtils.makeCommandContribution(getSite(), CoreCommands.CMD_SQL_EDITOR_CLOSE_TAB));
                    }
                }
            });
            menuMgr.setRemoveAllWhenShown(true);
            resultTabs.setMenu(menu);
        }
    }

    private void setActiveResultsContainer(QueryResultsContainer data) {
        curResultsContainer = data;
        curQueryProcessor = curResultsContainer.queryProcessor;
        ResultSetViewer rsv = curResultsContainer.getResultSetController();
        if (rsv != null) {
            //rsv.getActivePresentation().getControl().setFocus();
        }
    }

    private void showExtraView(final ToolItem toolItem, String name, String toolTip, Image image, Control view) {
        for (CTabItem item : resultTabs.getItems()) {
            if (item.getData() == view) {
                if (resultTabs.getSelection() == item) {
                    item.dispose();
                    toolItem.setSelection(false);
                    return;
                } else {
                    resultTabs.setSelection(item);
                    return;
                }
            }
        }

        if (view == outputViewer) {
            updateOutputViewerIcon(false);
            outputViewer.resetNewOutput();
        }
        // Create new tab
        toolItem.setSelection(true);

        CTabItem item = new CTabItem(resultTabs, SWT.CLOSE);
        item.setControl(view);
        item.setText(name);
        item.setToolTipText(toolTip);
        item.setImage(image);
        item.setData(view);
        // De-select tool item on tab close
        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                toolItem.setSelection(false);
            }
        });
        resultTabs.setSelection(item);
    }

    public void closeActiveTab() {
        CTabItem tabItem = resultTabs.getSelection();
        if (tabItem != null && tabItem.getShowClose()) {
            tabItem.dispose();
        }
    }

    public void showOutputPanel() {
        if (sashForm.getMaximizedControl() != null) {
            sashForm.setMaximizedControl(null);
        }
        showExtraView(toolOutputItem, CoreMessages.editors_sql_output, "Database server output log", IMG_OUTPUT, outputViewer);
    }

    public void showExecutionLogPanel() {
        if (sashForm.getMaximizedControl() != null) {
            sashForm.setMaximizedControl(null);
        }
        showExtraView(toolLogItem, CoreMessages.editors_sql_execution_log, "SQL query execution log", IMG_LOG, logViewer);
    }

    public boolean hasMaximizedControl() {
        return sashForm.getMaximizedControl() != null;
    }

    public void toggleResultPanel() {
        if (sashForm.getMaximizedControl() == null) {
            sashForm.setMaximizedControl(editorControl);
        } else {
            sashForm.setMaximizedControl(null);
        }
    }

    public void toggleEditorMaximize()
    {
        if (sashForm.getMaximizedControl() == null) {
            sashForm.setMaximizedControl(resultTabs);
        } else {
            sashForm.setMaximizedControl(null);
        }
    }

    public void toggleActivePanel() {
        if (sashForm.getMaximizedControl() == null) {
            if (UIUtils.hasFocus(resultTabs)) {
                final Control editorControl = getEditorControl();
                if (editorControl != null) {
                    editorControl.setFocus();
                }
            } else {
                CTabItem selTab = resultTabs.getSelection();
                if (selTab != null) {
                    ResultSetViewer viewer = getActiveResultSetViewer();
                    if (viewer != null && viewer.getActivePresentation().getControl().isVisible()) {
                        viewer.getActivePresentation().getControl().setFocus();
                    } else {
                        selTab.getControl().setFocus();
                    }
                }
            }
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput editorInput)
        throws PartInitException
    {
        super.init(site, editorInput);

        updateResultSetOrientation();
    }

    private void updateResultSetOrientation() {
        try {
            resultSetOrientation = ResultSetOrientation.valueOf(getActivePreferenceStore().getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION));
        } catch (IllegalArgumentException e) {
            resultSetOrientation = ResultSetOrientation.HORIZONTAL;
        }
        if (sashForm != null) {
            sashForm.setOrientation(resultSetOrientation.getSashOrientation());
        }
    }

    @Override
    protected void doSetInput(IEditorInput editorInput) throws CoreException
    {
        // Check for file existence
        try {
            if (editorInput instanceof IFileEditorInput) {
                final IFile file = ((IFileEditorInput) editorInput).getFile();
                if (!file.exists()) {
                    file.create(new ByteArrayInputStream(new byte[]{}), true, new NullProgressMonitor());
                }
            }
        } catch (Exception e) {
            log.error("Error checking SQL file", e);
        }
        try {
            super.doSetInput(editorInput);
        } catch (Throwable e) {
            // Something bas may happend. E.g. OutOfMemory error in case of rtoo big input file.
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out, true));
            editorInput = new StringEditorInput("Error", CommonUtils.truncateString(out.toString(), 10000), true, GeneralUtils.UTF8_ENCODING);
            doSetInput(editorInput);
            log.error("Error loading input SQL file", e);
        }
        syntaxLoaded = false;
        setDataSourceContainer(EditorUtils.getInputDataSource(editorInput));
        setPartName(getEditorName());
        if (isNonPersistentEditor()) {
            setTitleImage(DBeaverIcons.getImage(UIIcon.SQL_CONSOLE));
        }
        editorImage = getTitleImage();
    }

    @Override
    public String getTitleToolTip() {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer == null) {
            return super.getTitleToolTip();
        }
        final IEditorInput editorInput = getEditorInput();
        String scriptPath;
        if (editorInput instanceof IFileEditorInput) {
            scriptPath = ((IFileEditorInput) editorInput).getFile().getFullPath().toString();
        } else if (editorInput instanceof IPathEditorInput) {
            scriptPath = ((IPathEditorInput) editorInput).getPath().toString();
        } else if (editorInput instanceof IURIEditorInput) {
            final URI uri = ((IURIEditorInput) editorInput).getURI();
            if ("file".equals(uri.getScheme())) {
                scriptPath = new File(uri).getAbsolutePath();
            } else {
                scriptPath = uri.toString();
            }
        } else if (editorInput instanceof INonPersistentEditorInput) {
            scriptPath = "SQL Console";
        } else {
            scriptPath = editorInput.getName();
            if (CommonUtils.isEmpty(scriptPath)) {
                scriptPath = "<not a file>";
            }
        }
        return
            "Script: " + scriptPath +
                " \nConnection: " + dataSourceContainer.getName() +
                " \nType: " + (dataSourceContainer.getDriver().getFullName()) +
                " \nURL: " + dataSourceContainer.getConnectionConfiguration().getUrl();
    }

    private String getEditorName() {
        final IFile file = EditorUtils.getFileFromInput(getEditorInput());
        String scriptName;
        if (file != null) {
            scriptName = file.getFullPath().removeFileExtension().lastSegment();
        } else {
            File localFile = EditorUtils.getLocalFileFromInput(getEditorInput());
            if (localFile != null) {
                return localFile.getName();
            } else {
                scriptName = getEditorInput().getName();
            }
        }

        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        DBPPreferenceStore preferenceStore = getActivePreferenceStore();
        String pattern = preferenceStore.getString(DBeaverPreferences.SCRIPT_TITLE_PATTERN);
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_CONNECTION_NAME, dataSourceContainer == null ? "none" : dataSourceContainer.getName());
        vars.put(VAR_FILE_NAME, scriptName);
        vars.put(VAR_FILE_EXT,
            file == null ? "" : file.getFullPath().getFileExtension());
        vars.put(VAR_DRIVER_NAME, dataSourceContainer == null ? "?" : dataSourceContainer.getDriver().getFullName());
        return GeneralUtils.replaceVariables(pattern, new GeneralUtils.MapResolver(vars));
    }

    @Override
    public void setFocus()
    {
        super.setFocus();
    }

    public void explainQueryPlan()
    {
        final SQLScriptElement scriptElement = extractActiveQuery();
        if (scriptElement == null) {
            setStatus(CoreMessages.editors_sql_status_empty_query_string, DBPMessageType.ERROR);
            return;
        }
        if (!(scriptElement instanceof SQLQuery)) {
            setStatus("Can't explain plan for command", DBPMessageType.ERROR);
            return;
        }
        explainQueryPlan((SQLQuery) scriptElement);
    }

    private void explainQueryPlan(SQLQuery sqlQuery)
    {
        // 1. Determine whether planner supports plan extraction
        DBCQueryPlanner planner = DBUtils.getAdapter(DBCQueryPlanner.class, getDataSource());
        if (planner == null) {
            DBUserInterface.getInstance().showError("Execution plan", "Execution plan explain isn't supported by current datasource");
            return;
        }
        DBCPlanStyle planStyle = planner.getPlanStyle();
        if (planStyle == DBCPlanStyle.QUERY) {
            explainPlanFromQuery(planner, sqlQuery);
            return;
        }

        ExplainPlanViewer planView = null;
        for (CTabItem item : resultTabs.getItems()) {
            if (item.getData() instanceof ExplainPlanViewer) {
                ExplainPlanViewer pv = (ExplainPlanViewer) item.getData();
                if (pv.getQuery() != null && pv.getQuery().equals(sqlQuery)) {
                    resultTabs.setSelection(item);
                    planView = pv;
                    break;
                }
            }
        }

        if (planView == null) {
            planView = new ExplainPlanViewer(this, resultTabs);

            final CTabItem item = new CTabItem(resultTabs, SWT.CLOSE);
            item.setControl(planView.getControl());
            item.setText("Exec. Plan");
            item.setToolTipText("Execution plan for\n" + sqlQuery.getText());
            item.setImage(IMG_EXPLAIN_PLAN);
            item.setData(planView);
            UIUtils.disposeControlOnItemDispose(item);
            resultTabs.setSelection(item);
        }

        try {
            planView.explainQueryPlan(getExecutionContext(), sqlQuery);
        } catch (DBCException e) {
            DBUserInterface.getInstance().showError(
                    CoreMessages.editors_sql_error_execution_plan_title,
                CoreMessages.editors_sql_error_execution_plan_message,
                e);
        }
    }

    private void explainPlanFromQuery(final DBCQueryPlanner planner, final SQLQuery sqlQuery) {
        final String[] planQueryString = new String[1];
        DBRRunnableWithProgress queryObtainTask = new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Prepare plan query")) {
                    DBCPlan plan = planner.planQueryExecution(session, sqlQuery.getText());
                    planQueryString[0] = plan.getPlanQueryString();
                } catch (Exception e) {
                    log.error(e);
                }
            }
        };
        if (RuntimeUtils.runTask(queryObtainTask, "Retrieve plan query", 5000) && !CommonUtils.isEmpty(planQueryString[0])) {
            SQLQuery planQuery = new SQLQuery(getDataSource(), planQueryString[0]);
            processQueries(Collections.<SQLScriptElement>singletonList(planQuery), true, false, true, null);
        }
    }

    public void processSQL(boolean newTab, boolean script) {
        processSQL(newTab, script, null, null);
    }

    public void processSQL(boolean newTab, boolean script, SQLQueryTransformer transformer, @Nullable SQLQueryListener queryListener)
    {
        IDocument document = getDocument();
        if (document == null) {
            setStatus(CoreMessages.editors_sql_status_cant_obtain_document, DBPMessageType.ERROR);
            return;
        }
        List<SQLScriptElement> elements;
        if (script) {
            // Execute all SQL statements consequently
            ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
            if (selection.getLength() > 1) {
                elements = extractScriptQueries(selection.getOffset(), selection.getLength(), true, false);
            } else {
                elements = extractScriptQueries(0, document.getLength(), true, false);
            }
        } else {
            // Execute statement under cursor or selected text (if selection present)
            SQLScriptElement sqlQuery = extractActiveQuery();
            if (sqlQuery == null) {
                setStatus(CoreMessages.editors_sql_status_empty_query_string, DBPMessageType.ERROR);
                return;
            } else {
                elements = Collections.singletonList(sqlQuery);
            }
        }
        try {
            if (transformer != null) {
                DBPDataSource dataSource = getDataSource();
                if (dataSource instanceof SQLDataSource) {
                    List<SQLScriptElement> xQueries = new ArrayList<>(elements.size());
                    for (int i = 0; i < elements.size(); i++) {
                        SQLScriptElement element = elements.get(i);
                        if (element instanceof SQLQuery) {
                            SQLQuery query = transformer.transformQuery((SQLDataSource) dataSource, (SQLQuery) element);
                            if (query != null) {
                                xQueries.add(query);
                            }
                        } else {
                            xQueries.add(element);
                        }
                    }
                    elements = xQueries;
                }
            }
        }
        catch (DBException e) {
            DBUserInterface.getInstance().showError("Bad query", "Can't execute query", e);
            return;
        }
        processQueries(elements, newTab, false, true, queryListener);
    }

    public void exportDataFromQuery()
    {
        SQLScriptElement sqlQuery = extractActiveQuery();
        if (sqlQuery instanceof SQLQuery) {
            processQueries(Collections.singletonList(sqlQuery), false, true, true, null);
        } else {
            DBUserInterface.getInstance().showError(
                    "Extract data",
                    "Can't extract data from control command");
        }
    }

    private void processQueries(@NotNull final List<SQLScriptElement> queries, final boolean newTab, final boolean export, final boolean checkSession, @Nullable final SQLQueryListener queryListener)
    {
        if (queries.isEmpty()) {
            // Nothing to process
            return;
        }
        final DBPDataSourceContainer container = getDataSourceContainer();
        if (checkSession) {
            try {
                DBRProgressListener connectListener = new DBRProgressListener() {
                    @Override
                    public void onTaskFinished(IStatus status) {
                        if (!status.isOK() || container == null || !container.isConnected()) {
                            DBUserInterface.getInstance().showError(
                                    CoreMessages.editors_sql_error_cant_obtain_session,
                                null,
                                status);
                            return;
                        }
                        // Make a small pause to let all UI connection listeners to finish
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // it's ok
                        }
                        DBeaverUI.syncExec(new Runnable() {
                            @Override
                            public void run() {
                                processQueries(queries, newTab, export, false, queryListener);
                            }
                        });
                    }
                };
                if (!checkSession(connectListener)) {
                    return;
                }
            } catch (DBException ex) {
                ResultSetViewer viewer = getActiveResultSetViewer();
                if (viewer != null) {
                    viewer.setStatus(ex.getMessage(), DBPMessageType.ERROR);
                }
                DBUserInterface.getInstance().showError(
                        CoreMessages.editors_sql_error_cant_obtain_session,
                    ex.getMessage());
                return;
            }
        }
        if (dataSourceContainer == null) {
            return;
        }

        final boolean isSingleQuery = (queries.size() == 1);
        if (isSingleQuery && queries.get(0) instanceof SQLQuery) {
            SQLQuery query = (SQLQuery) queries.get(0);
            if (query.isDeleteUpdateDangerous()) {
                String targetName = "multiple tables";
                if (query.getSingleSource() != null) {
                    targetName = query.getSingleSource().getEntityName();
                }
                if (ConfirmationDialog.showConfirmDialogEx(
                    getSite().getShell(),
                    DBeaverPreferences.CONFIRM_DANGER_SQL,
                    ConfirmationDialog.CONFIRM,
                    ConfirmationDialog.WARNING,
                    query.getType().name(),
                    targetName) != IDialogConstants.OK_ID)
                {
                    return;
                }
            }
        }


        if (sashForm.getMaximizedControl() != null) {
            sashForm.setMaximizedControl(null);
        }

        // Save editor
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE) && isDirty()) {
            doSave(new NullProgressMonitor());
        }

        if (!newTab || !isSingleQuery) {
            // We don't need new tab or we are executing a script - so close all extra tabs
            closeExtraResultTabs(null);
        }

        if (newTab) {
            // Execute each query in a new tab
            for (int i = 0; i < queries.size(); i++) {
                SQLScriptElement query = queries.get(i);
                QueryProcessor queryProcessor = (i == 0 && !isSingleQuery ? curQueryProcessor : createQueryProcessor(queries.size() == 1));
                queryProcessor.processQueries(
                        Collections.singletonList(query),
                        true,
                        export,
                        getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR), queryListener);
            }
        } else {
            // Use current tab.
            // If current tab was pinned then use first tab
            final QueryResultsContainer firstResults = curQueryProcessor.getFirstResults();
            if (firstResults.isPinned()) {
                curQueryProcessor = queryProcessors.get(0);
            }
            closeExtraResultTabs(curQueryProcessor);
            if (firstResults.tabItem != null) {
                // Do not switch tab if Output tab is active
                CTabItem selectedTab = resultTabs.getSelection();
                if (selectedTab == null || selectedTab.getData() != outputViewer) {
                    resultTabs.setSelection(firstResults.tabItem);
                }
            }
            curQueryProcessor.processQueries(queries, false, export, false, queryListener);
        }
    }

    private void setStatus(String status, DBPMessageType messageType)
    {
        ResultSetViewer resultsView = getActiveResultSetViewer();
        if (resultsView != null) {
            resultsView.setStatus(status, messageType);
        }
    }

    private void closeExtraResultTabs(@Nullable QueryProcessor queryProcessor)
    {
        // Close all tabs except first one
        for (int i = resultTabs.getItemCount() - 1; i > 0; i--) {
            CTabItem item = resultTabs.getItem(i);
            if (item.getData() instanceof QueryResultsContainer && item.getShowClose()) {
                QueryResultsContainer resultsProvider = (QueryResultsContainer)item.getData();
                if (queryProcessor != null && queryProcessor != resultsProvider.queryProcessor) {
                    continue;
                }
                if (queryProcessor != null && queryProcessor.resultContainers.size() < 2) {
                    // Do not remove first tab for this processor
                    continue;
                }
                item.dispose();
            }
        }
    }

    private boolean checkSession(DBRProgressListener onFinish)
        throws DBException
    {
        DBPDataSourceContainer ds = getDataSourceContainer();
        if (ds == null) {
            throw new DBException("No active connection");
        }
        if (!ds.isConnected()) {
            boolean doConnect = ds.getPreferenceStore().getBoolean(DBeaverPreferences.EDITOR_CONNECT_ON_EXECUTE);
            if (doConnect) {
                return checkConnected(true, onFinish);
            } else {
                throw new DBException("Disconnected from database");
            }
        }
        return true;
    }

    /**
     * Handles datasource change action in UI
     */
    private void fireDataSourceChange()
    {
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                onDataSourceChange();
            }
        });
    }

    private void onDataSourceChange()
    {
        updateExecutionContext();

        if (sashForm == null || sashForm.isDisposed()) {
            reloadSyntaxRules();
            return;
        }

        DatabaseEditorUtils.setPartBackground(this, sashForm);


        DBCExecutionContext executionContext = getExecutionContext();
        if (syntaxLoaded && lastExecutionContext == executionContext) {
            return;
        }
        if (curResultsContainer != null) {
            ResultSetViewer rsv = curResultsContainer.getResultSetController();
            if (rsv != null) {
                if (executionContext == null) {
                    rsv.setStatus(CoreMessages.editors_sql_status_not_connected_to_database);
                } else {
                    rsv.setStatus(CoreMessages.editors_sql_staus_connected_to + executionContext.getDataSource().getContainer().getName() + "'"); //$NON-NLS-2$
                }
            }
        }

        // Update command states
        SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);
        SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXPLAIN);

        reloadSyntaxRules();

        if (getDataSourceContainer() == null) {
            sashForm.setMaximizedControl(editorControl);
        } else {
            sashForm.setMaximizedControl(null);
        }

        lastExecutionContext = executionContext;
        syntaxLoaded = true;
    }

    @Override
    public void beforeConnect()
    {
    }

    @Override
    public void beforeDisconnect()
    {
        closeAllJobs();
    }

    @Override
    public void dispose()
    {
        // Release ds container
        releaseContainer();
        closeAllJobs();

        final IEditorInput editorInput = getEditorInput();
        IFile sqlFile = EditorUtils.getFileFromInput(editorInput);

        logViewer = null;
        outputViewer = null;

        queryProcessors.clear();
        curResultsContainer = null;
        curQueryProcessor = null;

        super.dispose();

        if (sqlFile != null && !PlatformUI.getWorkbench().isClosing()) {
            deleteFileIfEmpty(sqlFile);
        }
    }

    private void deleteFileIfEmpty(IFile sqlFile) {
        if (sqlFile == null || !sqlFile.exists()) {
            return;
        }
        if (!getActivePreferenceStore().getBoolean(DBeaverPreferences.SCRIPT_DELETE_EMPTY)) {
            return;
        }
        File osFile = sqlFile.getLocation().toFile();
        if (!osFile.exists() || osFile.length() != 0) {
            // Not empty
            return;
        }
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            IFileState[] fileHistory = sqlFile.getHistory(monitor);
            if (!ArrayUtils.isEmpty(fileHistory)) {
                for (IFileState historyItem : fileHistory) {
                    try (InputStream contents = historyItem.getContents()) {
                        int cValue = contents.read();
                        if (cValue != -1) {
                            // At least once there was some content saved
                            return;
                        }
                    }
                }
            }
            // This file is empty and never (at least during this session) had any contents.
            // Drop it.
            log.debug("Delete empty SQL script '" + sqlFile.getFullPath().toOSString() + "'");
            sqlFile.delete(true, monitor);
        } catch (Exception e) {
            log.error("Can't delete empty script file", e); //$NON-NLS-1$
        }
    }

    private void closeAllJobs()
    {
        for (QueryProcessor queryProcessor : queryProcessors) {
            queryProcessor.closeJob();
        }
    }

    private int getTotalQueryRunning() {
        int jobsRunning = 0;
        for (QueryProcessor queryProcessor : queryProcessors) {
            jobsRunning += queryProcessor.curJobRunning.get();
        }
        return jobsRunning;
    }

    @Override
    public void handleDataSourceEvent(final DBPEvent event)
    {
        final boolean dsEvent = event.getObject() == getDataSourceContainer();
        final boolean objectEvent = event.getObject().getDataSource() == getDataSource();
        if (dsEvent || objectEvent) {
            DBeaverUI.asyncExec(
                new Runnable() {
                    @Override
                    public void run() {
                        switch (event.getAction()) {
                            case OBJECT_REMOVE:
                                if (dsEvent) {
                                    setDataSourceContainer(null);
                                }
                                break;
                            case OBJECT_SELECT:
                                if (objectEvent) {
                                    // Active schema was changed? Update title and tooltip
                                    firePropertyChange(IWorkbenchPartConstants.PROP_TITLE);
                                }
                                break;
                            default:
                                break;
                        }
                        onDataSourceChange();
                    }
                }
            );
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        monitor.beginTask("Save data changes...", 1);
        try {
            monitor.subTask("Save '" + getPartName() + "' changes...");
            SaveJob saveJob = new SaveJob();
            saveJob.schedule();

            // Wait until job finished
            UIUtils.waitJobCompletion(saveJob);
            if (!saveJob.success) {
                monitor.setCanceled(true);
                return;
            }
        } finally {
            monitor.done();
        }

        super.doSave(monitor);
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return true;
    }

    @Override
    public void doSaveAs()
    {
        saveToExternalFile();
    }

    @Override
    public int promptToSaveOnClose()
    {
        int jobsRunning = getTotalQueryRunning();
        if (jobsRunning > 0) {
            log.warn("There are " + jobsRunning + " SQL job(s) still running in the editor");
//            MessageBox messageBox = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.OK);
//            messageBox.setMessage(CoreMessages.editors_sql_save_on_close_message);
//            messageBox.setText(CoreMessages.editors_sql_save_on_close_text);
//            messageBox.open();
//            return ISaveablePart2.CANCEL;
        }

        for (QueryProcessor queryProcessor : queryProcessors) {
            for (QueryResultsContainer resultsProvider : queryProcessor.getResultContainers()) {
                ResultSetViewer rsv = resultsProvider.getResultSetController();
                if (rsv != null && rsv.isDirty()) {
                    return rsv.promptToSaveOnClose();
                }
            }
        }

        // End transaction
        if (!DataSourceHandler.checkAndCloseActiveTransaction(new DBCExecutionContext[] {executionContext})) {
            return ISaveablePart2.CANCEL;
        }

        if (isNonPersistentEditor()) {
            return ISaveablePart2.NO;
        }
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE)) {
            return ISaveablePart2.YES;
        }
        return ISaveablePart2.DEFAULT;
    }

    protected void afterSaveToFile(File saveFile) {
        try {
            IFileStore fileStore = EFS.getStore(saveFile.toURI());
            IEditorInput input = new FileStoreEditorInput(fileStore);

            EditorUtils.setInputDataSource(input, getDataSourceContainer(), false);

            init(getEditorSite(), input);
        } catch (CoreException e) {
            DBUserInterface.getInstance().showError("File save", "Can't open SQL editor from external file", e);
        }
    }

    @Nullable
    private ResultSetViewer getActiveResultSetViewer()
    {
        if (curResultsContainer != null) {
            return curResultsContainer.getResultSetController();
        }
        return null;
    }

    private void showScriptPositionRuler(boolean show)
    {
        IColumnSupport columnSupport = (IColumnSupport) getAdapter(IColumnSupport.class);
        if (columnSupport != null) {
            RulerColumnDescriptor positionColumn = RulerColumnRegistry.getDefault().getColumnDescriptor(ScriptPositionColumn.ID);
            columnSupport.setColumnVisible(positionColumn, show);
        }
    }

    private void showStatementInEditor(final SQLQuery query, final boolean select)
    {
        DBeaverUI.runUIJob("Select SQL query in editor", new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                if (isDisposed()) {
                    return;
                }
                if (select) {
                    selectAndReveal(query.getOffset(), query.getLength());
                    setStatus(query.getText(), DBPMessageType.INFORMATION);
                } else {
                    getSourceViewer().revealRange(query.getOffset(), query.getLength());
                }
            }
        });
    }

    @Override
    public void reloadSyntaxRules() {
        super.reloadSyntaxRules();
        if (outputViewer != null) {
            outputViewer.refreshStyles();
        }
    }

    private QueryProcessor createQueryProcessor(boolean setSelection)
    {
        final QueryProcessor queryProcessor = new QueryProcessor();
        curQueryProcessor = queryProcessor;
        curResultsContainer = queryProcessor.getFirstResults();
        if (setSelection && curResultsContainer.tabItem != null) {
            resultTabs.setSelection(curResultsContainer.tabItem);
        }

        return queryProcessor;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        switch (event.getProperty()) {
            case ModelPreferences.SCRIPT_STATEMENT_DELIMITER:
            case ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER:
            case ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK:
            case ModelPreferences.SQL_PARAMETERS_ENABLED:
            case ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK:
            case ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED:
            case ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX:
                reloadSyntaxRules();
                break;
            case SQLPreferenceConstants.RESULT_SET_ORIENTATION:
                updateResultSetOrientation();
                break;
        }
    }

    public enum ResultSetOrientation {
        HORIZONTAL(SWT.VERTICAL, "Horizontal", "Results are below the editor", true),
        VERTICAL(SWT.HORIZONTAL, "Vertical", "Results are to the right from editor", true),
        DETACHED(SWT.VERTICAL, "Detached", "Results are in separate view", false);

        private final int sashOrientation;
        private final String label;
        private final String description;
        private final boolean supported;

        ResultSetOrientation(int sashOrientation, String label, String description, boolean supported) {
            this.sashOrientation = sashOrientation;
            this.label = label;
            this.description = description;
            this.supported = supported;
        }

        public int getSashOrientation() {
            return sashOrientation;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public boolean isSupported() {
            return supported;
        }
    }

    public static class ResultSetOrientationMenuContributor extends CompoundContributionItem
    {
        @Override
        protected IContributionItem[] getContributionItems() {
            IEditorPart activeEditor = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (!(activeEditor instanceof SQLEditorBase)) {
                return new IContributionItem[0];
            }
            final DBPPreferenceStore preferenceStore = ((SQLEditorBase) activeEditor).getActivePreferenceStore();
            String curPresentation = preferenceStore.getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION);
            ResultSetOrientation[] orientations = ResultSetOrientation.values();
            List<IContributionItem> items = new ArrayList<>(orientations.length);
            for (final ResultSetOrientation orientation : orientations) {
                Action action = new Action(orientation.getLabel(), Action.AS_RADIO_BUTTON) {
                    @Override
                    public void run() {
                        preferenceStore.setValue(SQLPreferenceConstants.RESULT_SET_ORIENTATION, orientation.name());
                        PrefUtils.savePreferenceStore(preferenceStore);
                    }
                };
                action.setDescription(orientation.getDescription());
                if (!orientation.isSupported()) {
                    action.setEnabled(false);
                }
                if (orientation.name().equals(curPresentation)) {
                    action.setChecked(true);
                }
                items.add(new ActionContributionItem(action));
            }
            return items.toArray(new IContributionItem[items.size()]);
        }
    }

    public class QueryProcessor implements SQLResultsConsumer {

        private SQLQueryJob curJob;
        private AtomicInteger curJobRunning = new AtomicInteger(0);
        private final List<QueryResultsContainer> resultContainers = new ArrayList<>();
        private DBDDataReceiver curDataReceiver = null;

        QueryProcessor() {
            // Create first (default) results provider
            queryProcessors.add(this);
            createResultsProvider(0);
        }

        private QueryResultsContainer createResultsProvider(int resultSetNumber) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultSetNumber);
            resultContainers.add(resultsProvider);
            return resultsProvider;
        }

        private QueryResultsContainer createResultsProvider(DBSDataContainer dataContainer) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultContainers.size(), dataContainer);
            resultContainers.add(resultsProvider);
            return resultsProvider;
        }

        @NotNull
        QueryResultsContainer getFirstResults()
        {
            return resultContainers.get(0);
        }
        @Nullable
        QueryResultsContainer getResults(SQLQuery query) {
            for (QueryResultsContainer provider : resultContainers) {
                if (provider.query == query) {
                    return provider;
                }
            }
            return null;
        }

        List<QueryResultsContainer> getResultContainers() {
            return resultContainers;
        }

        private void closeJob()
        {
            final SQLQueryJob job = curJob;
            if (job != null) {
                if (job.getState() == Job.RUNNING) {
                    job.cancel();
                }
                curJob.closeJob();
                curJob = null;
            }
        }

        void processQueries(final List<SQLScriptElement> queries, final boolean fetchResults, boolean export, boolean closeTabOnError, SQLQueryListener queryListener)
        {
            if (queries.isEmpty()) {
                // Nothing to process
                return;
            }
            if (curJobRunning.get() > 0) {
                DBUserInterface.getInstance().showError(
                        CoreMessages.editors_sql_error_cant_execute_query_title,
                    CoreMessages.editors_sql_error_cant_execute_query_message);
                return;
            }
            final DBCExecutionContext executionContext = getExecutionContext();
            if (executionContext == null) {
                DBUserInterface.getInstance().showError(
                        CoreMessages.editors_sql_error_cant_execute_query_title,
                    CoreMessages.editors_sql_status_not_connected_to_database);
                return;
            }
            final boolean isSingleQuery = (queries.size() == 1);

            // Prepare execution job
            {
                showScriptPositionRuler(true);
                QueryResultsContainer resultsContainer = getFirstResults();

                SQLEditorQueryListener listener = new SQLEditorQueryListener(this, closeTabOnError);
                if (queryListener != null) {
                    listener.setExtListener(queryListener);
                }
                File localFile = EditorUtils.getLocalFileFromInput(getEditorInput());
                final SQLQueryJob job = new SQLQueryJob(
                    getSite(),
                    isSingleQuery ? CoreMessages.editors_sql_job_execute_query : CoreMessages.editors_sql_job_execute_script,
                    executionContext,
                    resultsContainer,
                    queries,
                    new SQLScriptContext(globalScriptContext, executionContext, localFile, new OutputLogWriter()),
                    this,
                    listener);

                if (export || isSingleQuery) {
                    resultsContainer.query = queries.get(0);
                }
                if (export) {
                    // Assign current job from active query and open wizard
                    resultsContainer.lastGoodQuery = null;
                    curJob = job;
                    ActiveWizardDialog dialog = new ActiveWizardDialog(
                        getSite().getWorkbenchWindow(),
                        new DataTransferWizard(
                            new IDataTransferProducer[] {
                                new DatabaseTransferProducer(resultsContainer, null)},
                            null),
                        new StructuredSelection(this));
                    dialog.open();
                } else if (isSingleQuery) {
                    closeJob();
                    curJob = job;
                    ResultSetViewer rsv = resultsContainer.getResultSetController();
                    if (rsv != null) {
                        rsv.resetDataFilter(false);
                        rsv.resetHistory();
                        rsv.refresh();
                    }
                } else {
                    if (fetchResults) {
                        job.setFetchResultSets(true);
                    }
                    job.schedule();
                    curJob = job;
                }
            }
        }

        public boolean isDirty() {
            for (QueryResultsContainer resultsProvider : resultContainers) {
                ResultSetViewer rsv = resultsProvider.getResultSetController();
                if (rsv != null && rsv.isDirty()) {
                    return true;
                }
            }
            return false;
        }

        void removeResults(QueryResultsContainer resultsContainer) {
            if (resultContainers.size() > 1) {
                resultContainers.remove(resultsContainer);
            } else {
                queryProcessors.remove(this);
                if (curQueryProcessor == this) {
                    if (queryProcessors.isEmpty()) {
                        curQueryProcessor = null;
                        curResultsContainer = null;
                    } else {
                        curQueryProcessor = queryProcessors.get(0);
                        curResultsContainer = curQueryProcessor.getFirstResults();
                    }
                }
            }
        }

        @Nullable
        @Override
        public DBDDataReceiver getDataReceiver(final SQLQuery statement, final int resultSetNumber) {
            if (curDataReceiver != null) {
                return curDataReceiver;
            }
            final boolean isStatsResult = (statement != null && statement.getData() == SQLQueryJob.STATS_RESULTS);
//            if (isStatsResult) {
//                // Maybe it was already open
//                for (QueryResultsProvider provider : resultContainers) {
//                    if (provider.query != null && provider.query.getData() == SQLQueryJob.STATS_RESULTS) {
//                        resultSetNumber = provider.resultSetNumber;
//                        break;
//                    }
//                }
//            }
            if (resultSetNumber >= resultContainers.size() && !isDisposed()) {
                // Open new results processor in UI thread
                DBeaverUI.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        createResultsProvider(resultSetNumber);
                    }
                });
            }
            if (resultSetNumber >= resultContainers.size()) {
                // Editor seems to be disposed - no data receiver
                return null;
            }
            final QueryResultsContainer resultsProvider = resultContainers.get(resultSetNumber);
            // Open new results processor in UI thread
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (statement != null && !resultTabs.isDisposed()) {
                        resultsProvider.query = statement;
                        resultsProvider.lastGoodQuery = statement;
                        String tabName = null;
                        String toolTip = CommonUtils.truncateString(statement.getText(), 1000);
                        // Special statements (not real statements) have their name in data
                        if (isStatsResult) {
                            tabName = "Statistics";
                            int queryIndex = queryProcessors.indexOf(QueryProcessor.this);
                            if (queryIndex > 0) {
                                tabName += " - " + (queryIndex + 1);
                            }
                        }
                        resultsProvider.updateResultsName(tabName, toolTip);
                    }
                }
            });
            ResultSetViewer rsv = resultsProvider.getResultSetController();
            return rsv == null ? null : rsv.getDataReceiver();
        }

    }

    public class QueryResultsContainer implements DBSDataContainer, IResultSetContainer, IResultSetListener, IDataSourceContainerProvider {

        private final QueryProcessor queryProcessor;
        private final CTabItem tabItem;
        private final ResultSetViewer viewer;
        private final int resultSetNumber;
        private SQLScriptElement query = null;
        private SQLScriptElement lastGoodQuery = null;
        // Data container and filter are non-null only in case of associations navigation
        private DBSDataContainer dataContainer;

        private QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber)
        {
            this.queryProcessor = queryProcessor;
            this.resultSetNumber = resultSetNumber;

            boolean detachedViewer = false;
            SQLResultsView sqlView = null;
            if (detachedViewer) {
                try {
                    sqlView = (SQLResultsView) getSite().getPage().showView(SQLResultsView.VIEW_ID, null, IWorkbenchPage.VIEW_CREATE);
                } catch (Throwable e) {
                    DBUserInterface.getInstance().showError("Detached results", "Can't open results view", e);
                }
            }

            if (sqlView != null) {
                // Detached results viewer
                sqlView.setContainer(this);
                this.viewer = sqlView.getViewer();
                this.tabItem = null;
            } else {
                // Embedded results viewer
                this.viewer = new ResultSetViewer(resultTabs, getSite(), this);
                this.viewer.addListener(this);

                int tabCount = resultTabs.getItemCount();
                int tabIndex = 0;
                for (int i = tabCount; i > 0; i--) {
                    if (resultTabs.getItem(i - 1).getData() instanceof QueryResultsContainer) {
                        tabIndex = i;
                        break;
                    }
                }
                tabItem = new CTabItem(resultTabs, SWT.NONE, tabIndex);
                int queryIndex = queryProcessors.indexOf(queryProcessor);
                String tabName = getResultsTabName(resultSetNumber, queryIndex, null);
                tabItem.setText(tabName);
                tabItem.setImage(IMG_DATA_GRID);
                tabItem.setData(this);
                if (queryIndex > 0 || resultSetNumber > 0) {
                    tabItem.setShowClose(true);
                }
                tabItem.setControl(viewer.getControl());
                UIUtils.disposeControlOnItemDispose(tabItem);
            }

            viewer.getControl().addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    QueryResultsContainer.this.queryProcessor.removeResults(QueryResultsContainer.this);
                    if (QueryResultsContainer.this == curResultsContainer) {
                        curResultsContainer = null;
                    }
                }
            });
        }

        public QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber, DBSDataContainer dataContainer) {
            this(queryProcessor, resultSetNumber);
            this.dataContainer = dataContainer;
            updateResultsName(getResultsTabName(resultSetNumber, 0, dataContainer.getName()), null);
        }

        void updateResultsName(String resultSetName, String toolTip) {
            if (tabItem != null && !tabItem.isDisposed()) {
                if (!CommonUtils.isEmpty(resultSetName)) {
                    tabItem.setText(resultSetName);
                }
                tabItem.setToolTipText(toolTip);
            }
        }

        boolean isPinned() {
            return tabItem != null && resultTabs.indexOf(tabItem) > 0 && !tabItem.getShowClose();
        }

        void setPinned(boolean pinned) {
            tabItem.setShowClose(!pinned);
            tabItem.setImage(pinned ? IMG_DATA_GRID_LOCKED : IMG_DATA_GRID);
        }

        @Override
        public DBCExecutionContext getExecutionContext() {
            return SQLEditor.this.getExecutionContext();
        }

        @Nullable
        @Override
        public ResultSetViewer getResultSetController()
        {
            return viewer;
        }

        @Nullable
        @Override
        public DBSDataContainer getDataContainer()
        {
            return this;
        }

        @Override
        public boolean isReadyToRun()
        {
            return queryProcessor.curJob == null || queryProcessor.curJobRunning.get() == 0;
        }

        @Override
        public void openNewContainer(DBRProgressMonitor monitor, DBSDataContainer dataContainer, DBDDataFilter newFilter) {
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    QueryResultsContainer resultsProvider = queryProcessor.createResultsProvider(dataContainer);
                    resultsProvider.tabItem.getParent().setSelection(resultsProvider.tabItem);
                    setActiveResultsContainer(resultsProvider);
                    resultsProvider.viewer.refreshWithFilter(newFilter);
                }
            });
        }

        @Override
        public int getSupportedFeatures()
        {
            if (dataContainer != null) {
                return dataContainer.getSupportedFeatures();
            }
            int features = DATA_SELECT;
            features |= DATA_COUNT;

            if (getQueryResultCounts() <= 1) {
                features |= DATA_FILTER;
            }
            return features;
        }

        @NotNull
        @Override
        public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException
        {
            if (dataContainer != null) {
                return dataContainer.readData(source, session, dataReceiver, dataFilter, firstRow, maxRows, flags);
            }
            final SQLQueryJob job = queryProcessor.curJob;
            if (job == null) {
                throw new DBCException("No active query - can't read data");
            }
            if (this.query instanceof SQLQuery) {
                SQLQuery query = (SQLQuery) this.query;
                if (query.getResultsMaxRows() >= 0) {
                    firstRow = query.getResultsOffset();
                    maxRows = query.getResultsMaxRows();
                }
            }
            try {
                if (dataReceiver != viewer.getDataReceiver()) {
                    // Some custom receiver. Probably data export
                    queryProcessor.curDataReceiver = dataReceiver;
                } else {
                    queryProcessor.curDataReceiver = null;
                }
                // Count number of results for this query. If > 1 then we will refresh them all at once
                int resultCounts = getQueryResultCounts();

                if (resultCounts <= 1 && resultSetNumber > 0) {
                    job.setFetchResultSetNumber(resultSetNumber);
                } else {
                    job.setFetchResultSetNumber(-1);
                }
                job.setResultSetLimit(firstRow, maxRows);
                job.setDataFilter(dataFilter);

                job.extractData(session, this.query, resultCounts > 1 ? 0 : resultSetNumber);

                lastGoodQuery = job.getLastGoodQuery();

                return job.getStatistics();
            } finally {
                // Nullify custom data receiver
                queryProcessor.curDataReceiver = null;
            }
        }

        private int getQueryResultCounts() {
            int resultCounts = 0;
            for (QueryResultsContainer qrc : queryProcessor.resultContainers) {
                if (qrc.query == query) {
                    resultCounts++;
                }
            }
            return resultCounts;
        }

        @Override
        public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter)
            throws DBCException
        {
            if (dataContainer != null) {
                return dataContainer.countData(source, session, dataFilter);
            }
            DBPDataSource dataSource = getDataSource();
            if (!(dataSource instanceof SQLDataSource)) {
                throw new DBCException("Query transform is not supported by datasource");
            }
            if (!(query instanceof SQLQuery)) {
                throw new DBCException("Can't count rows for control command");
            }
            try {
                SQLQuery countQuery = new SQLQueryTransformerCount().transformQuery((SQLDataSource) dataSource, (SQLQuery) query);
                try (DBCStatement dbStatement = DBUtils.makeStatement(source, session, DBCStatementType.QUERY, countQuery, 0, 0)) {
                    if (dbStatement.executeStatement()) {
                        try (DBCResultSet rs = dbStatement.openResultSet()) {
                            if (rs.nextRow()) {
                                Object countValue = rs.getAttributeValue(0);
                                if (countValue instanceof Number) {
                                    return ((Number) countValue).longValue();
                                } else {
                                    throw new DBCException("Unexpected row count value: " + countValue);
                                }
                            } else {
                                throw new DBCException("Row count result is empty");
                            }
                        }
                    } else {
                        throw new DBCException("Row count query didn't return any value");
                    }
                }
            } catch (DBException e) {
                throw new DBCException("Error executing row count", e);
            }
        }

        @Nullable
        @Override
        public String getDescription()
        {
            if (dataContainer != null) {
                return dataContainer.getDescription();
            } else {
                return CoreMessages.editors_sql_description;
            }
        }

        @Nullable
        @Override
        public DBSObject getParentObject()
        {
            return getDataSourceContainer();
        }

        @Nullable
        @Override
        public DBPDataSource getDataSource()
        {
            return SQLEditor.this.getDataSource();
        }

        @Override
        public boolean isPersisted()
        {
            if (dataContainer != null) {
                return dataContainer.isPersisted();
            } else {
                return true;
            }
        }

        @NotNull
        @Override
        public String getName()
        {
            if (dataContainer != null) {
                return dataContainer.getName();
            }
            String name = lastGoodQuery != null ?
                    lastGoodQuery.getOriginalText() :
                    (query == null ? null : query.getOriginalText());
            if (name == null) {
                name = "SQL";
            }
            return name;
        }

        @Nullable
        @Override
        public DBPDataSourceContainer getDataSourceContainer() {
            return SQLEditor.this.getDataSourceContainer();
        }

        @Override
        public String toString() {
            if (dataContainer != null) {
                return dataContainer.toString();
            }
            return query == null ?
                "SQL Query / " + SQLEditor.this.getEditorInput().getName() :
                query.getOriginalText();
        }

        @Override
        public void handleResultSetLoad() {

        }

        @Override
        public void handleResultSetChange() {
            updateDirtyFlag();
        }
    }

    private String getResultsTabName(int resultSetNumber, int queryIndex, String name) {
        String tabName = name;
        if (CommonUtils.isEmpty(tabName)) {
            tabName = CoreMessages.editors_sql_data_grid;
        }
        if (resultSetNumber > 0) {
            tabName += " - " + (resultSetNumber + 1);
        } else if (queryIndex > 0) {
            tabName += " - " + (queryIndex + 1);
        }
        return tabName;
    }

    private class SQLEditorQueryListener implements SQLQueryListener {
        private final QueryProcessor queryProcessor;
        private boolean scriptMode;
        private long lastUIUpdateTime;
        private final ITextSelection originalSelection = (ITextSelection) getSelectionProvider().getSelection();
        private int topOffset, visibleLength;
        private boolean closeTabOnError;
        private SQLQueryListener extListener;

        private SQLEditorQueryListener(QueryProcessor queryProcessor, boolean closeTabOnError) {
            this.queryProcessor = queryProcessor;
            this.closeTabOnError = closeTabOnError;
        }

        public SQLQueryListener getExtListener() {
            return extListener;
        }

        public void setExtListener(SQLQueryListener extListener) {
            this.extListener = extListener;
        }

        @Override
        public void onStartScript() {
            try {
                lastUIUpdateTime = -1;
                scriptMode = true;
                DBeaverUI.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }
                        sashForm.setMaximizedControl(editorControl);
                    }
                });
            } finally {
                if (extListener != null) extListener.onStartScript();
            }
        }

        @Override
        public void onStartQuery(DBCSession session, final SQLQuery query) {
            try {
                boolean isInExecute = getTotalQueryRunning() > 0;
                if (!isInExecute) {
                    setTitleImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT_EXECUTE));
                }
                queryProcessor.curJobRunning.incrementAndGet();
                synchronized (runningQueries) {
                    runningQueries.add(query);
                }
                if (lastUIUpdateTime < 0 || System.currentTimeMillis() - lastUIUpdateTime > SCRIPT_UI_UPDATE_PERIOD) {
                    DBeaverUI.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            TextViewer textViewer = getTextViewer();
                            if (textViewer != null) {
                                topOffset = textViewer.getTopIndexStartOffset();
                                visibleLength = textViewer.getBottomIndexEndOffset() - topOffset;
                            }
                        }
                    });
                    if (scriptMode) {
                        showStatementInEditor(query, false);
                    }
                    lastUIUpdateTime = System.currentTimeMillis();
                }
            } finally {
                if (extListener != null) extListener.onStartQuery(session, query);
            }
        }

        @Override
        public void onEndQuery(final DBCSession session, final SQLQueryResult result) {
            try {
                synchronized (runningQueries) {
                    runningQueries.remove(result.getStatement());
                }
                queryProcessor.curJobRunning.decrementAndGet();
                if (getTotalQueryRunning() <= 0) {
                    setTitleImage(editorImage);
                }

                if (isDisposed()) {
                    return;
                }
                DBeaverUI.runUIJob("Process SQL query result", new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        // Finish query
                        processQueryResult(session, result);
                        // Update dirty flag
                        updateDirtyFlag();
                    }
                });
            } finally {
                if (extListener != null) extListener.onEndQuery(session, result);
            }
        }

        private void processQueryResult(DBCSession session, SQLQueryResult result) {
            dumpServerOutput(result);
            if (!scriptMode) {
                runPostExecuteActions(result);
            }
            SQLQuery query = result.getStatement();
            Throwable error = result.getError();
            if (error != null) {
                setStatus(GeneralUtils.getFirstMessage(error), DBPMessageType.ERROR);
                if (!scrollCursorToError(session.getProgressMonitor(), query, error)) {
                    int errorQueryOffset = query.getOffset();
                    int errorQueryLength = query.getLength();
                    if (errorQueryOffset >= 0 && errorQueryLength > 0) {
                        if (scriptMode) {
                            getSelectionProvider().setSelection(new TextSelection(errorQueryOffset, errorQueryLength));
                        } else {
                            getSelectionProvider().setSelection(originalSelection);
                        }
                    }
                }
            } else if (!scriptMode && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE)) {
                getSelectionProvider().setSelection(originalSelection);
            }
            // Get results window (it is possible that it was closed till that moment
            {
                for (QueryResultsContainer cr : queryProcessor.resultContainers) {
                    cr.viewer.updateFiltersText(false);
                }
                // Set tab name only if we have just one resultset
                // If query produced multiple results - leave their names as is
                if (scriptMode || queryProcessor.getResultContainers().size() == 1) {
                    QueryResultsContainer results = queryProcessor.getResults(query);
                    if (results != null) {
                        int queryIndex = queryProcessors.indexOf(queryProcessor);
                        String resultSetName = getResultsTabName(results.resultSetNumber, queryIndex, result.getResultSetName());
                        results.updateResultsName(resultSetName, null);
                    }
                }
            }
            // Close tab on error
            if (closeTabOnError && error != null) {
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        CTabItem tabItem = queryProcessor.getFirstResults().tabItem;
                        if (tabItem != null && tabItem.getShowClose()) {
                            tabItem.dispose();
                        }
                    }
                });
            }
            // Beep
            if (dataSourceContainer != null && !scriptMode && dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.BEEP_ON_QUERY_END)) {
                Display.getCurrent().beep();
            }
            // Notify agent
            if (result.getQueryTime() > DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT) * 1000) {
                DBeaverUI.notifyAgent(
                        "Query completed [" + getEditorInput().getName() + "]" + GeneralUtils.getDefaultLineSeparator() +
                                CommonUtils.truncateString(query.getText(), 200), !result.hasError() ? IStatus.INFO : IStatus.ERROR);
            }
        }

        @Override
        public void onEndScript(final DBCStatistics statistics, final boolean hasErrors) {
            try {
                if (isDisposed()) {
                    return;
                }
                runPostExecuteActions(null);
                DBeaverUI.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            // Editor closed
                            return;
                        }
                        sashForm.setMaximizedControl(null);
                        if (!hasErrors) {
                            getSelectionProvider().setSelection(originalSelection);
                        }
                        QueryResultsContainer results = queryProcessor.getFirstResults();
                        ResultSetViewer viewer = results.getResultSetController();
                        if (viewer != null) {
                            viewer.getModel().setStatistics(statistics);
                            viewer.updateStatusMessage();
                        }
                    }
                });
            } finally {
                if (extListener != null) extListener.onEndScript(statistics, hasErrors);
            }

        }
    }

    public void updateDirtyFlag() {
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
    }

    private class FindReplaceTarget extends DynamicFindReplaceTarget {
        private boolean lastFocusInEditor = true;
        @Override
        public IFindReplaceTarget getTarget() {
            ResultSetViewer rsv = getActiveResultSetViewer();
            TextViewer textViewer = getTextViewer();
            boolean focusInEditor = textViewer != null && textViewer.getTextWidget().isFocusControl();
            if (!focusInEditor) {
                if (rsv != null && rsv.getActivePresentation().getControl().isFocusControl()) {
                    focusInEditor = false;
                } else {
                    focusInEditor = lastFocusInEditor;
                }
            }
            lastFocusInEditor = focusInEditor;
            if (!focusInEditor && rsv != null) {
                IFindReplaceTarget nested = rsv.getAdapter(IFindReplaceTarget.class);
                if (nested != null) {
                    return nested;
                }
            } else if (textViewer != null) {
                return textViewer.getFindReplaceTarget();
            }
            return null;
        }
    }

    private class DynamicSelectionProvider extends CompositeSelectionProvider {
        private boolean lastFocusInEditor = true;
        @Override
        public ISelectionProvider getProvider() {
            ResultSetViewer rsv = getActiveResultSetViewer();
            TextViewer textViewer = getTextViewer();
            boolean focusInEditor = textViewer != null && textViewer.getTextWidget().isFocusControl();
            if (!focusInEditor) {
                if (rsv != null && rsv.getActivePresentation().getControl().isFocusControl()) {
                    focusInEditor = false;
                } else {
                    focusInEditor = lastFocusInEditor;
                }
            }
            lastFocusInEditor = focusInEditor;
            if (!focusInEditor && rsv != null) {
                return rsv;
            } else if (textViewer != null) {
                return textViewer.getSelectionProvider();
            } else {
                return null;
            }
        }
    }

    private void dumpServerOutput(@Nullable SQLQueryResult result) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            final DBPDataSource dataSource = executionContext.getDataSource();
            // Dump server output
            DBCServerOutputReader outputReader = DBUtils.getAdapter(DBCServerOutputReader.class, dataSource);
            if (outputReader == null && result != null) {
                outputReader = new DefaultServerOutputReader(result);
            }
            if (outputReader != null && outputReader.isServerOutputEnabled()) {
                synchronized (serverOutputs) {
                    serverOutputs.add(new ServerOutputInfo(outputReader, executionContext));
                }
            }
        }
    }

    private void runPostExecuteActions(@Nullable SQLQueryResult result) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            final DBPDataSource dataSource = executionContext.getDataSource();
            // Refresh active object
            if (result == null || !result.hasError() && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)) {
                final DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
                if (objectSelector != null) {
                    new AbstractJob("Refresh default object") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Refresh default object")) {
                                objectSelector.refreshDefaultObject(session);
                            } catch (Exception e) {
                                log.error(e);
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            }
        }
    }

    private void updateOutputViewerIcon(boolean alert) {
        Image image = alert ? IMG_OUTPUT_ALERT : IMG_OUTPUT;
        CTabItem outputItem = UIUtils.getTabItem(resultTabs, outputViewer);
        if (outputItem != null && outputItem != resultTabs.getSelection()) {
            outputItem.setImage(image);
        } else {
            toolOutputItem.setImage(image);
        }
    }

    private class SaveJob extends AbstractJob {
        private transient Boolean success = null;
        SaveJob() {
            super("Save '" + getPartName() + "' data changes...");
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                for (QueryProcessor queryProcessor : queryProcessors) {
                    for (QueryResultsContainer resultsProvider : queryProcessor.getResultContainers()) {
                        ResultSetViewer rsv = resultsProvider.getResultSetController();
                        if (rsv != null && rsv.isDirty()) {
                            rsv.doSave(monitor);
                        }
                    }
                }
                success = true;
                return Status.OK_STATUS;
            } catch (Throwable e) {
                success = false;
                log.error(e);
                return GeneralUtils.makeExceptionStatus(e);
            } finally {
                if (success == null) {
                    success = true;
                }
            }
        }
    }

    private static class ServerOutputInfo {
        private final DBCServerOutputReader outputReader;
        private final DBCExecutionContext executionContext;

        ServerOutputInfo(DBCServerOutputReader outputReader, DBCExecutionContext executionContext) {
            this.outputReader = outputReader;
            this.executionContext = executionContext;
        }
    }

    private final List<ServerOutputInfo> serverOutputs = new ArrayList<>();

    private class ServerOutputReader extends AbstractJob {

        ServerOutputReader() {
            super("Dump server output");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            dumpOutput(monitor);

            if (!DBeaverCore.isClosing()) {
                schedule(200);
            }

            return Status.OK_STATUS;
        }

        private void dumpOutput(DBRProgressMonitor monitor) {
            List<ServerOutputInfo> outputs;
            synchronized (serverOutputs) {
                outputs = new ArrayList<>(serverOutputs);
                serverOutputs.clear();
            }

            for (ServerOutputInfo info : outputs) {
                final StringWriter dump = new StringWriter();
                try {
                    info.outputReader.readServerOutput(monitor, info.executionContext, new PrintWriter(dump, true));
                    final String dumpString = dump.toString();
                    if (!dumpString.isEmpty()) {
                        DBeaverUI.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (outputViewer.isDisposed()) {
                                    return;
                                }
                                try {
                                    IOUtils.copyText(new StringReader(dumpString), outputViewer.getOutputWriter());
                                } catch (IOException e) {
                                    log.error(e);
                                }
                                if (outputViewer.isHasNewOutput()) {
                                    outputViewer.scrollToEnd();
                                    updateOutputViewerIcon(true);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }
}
