/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.model.impl.DefaultServerOutputReader;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.sql.SQLQueryTransformer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLResultsConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.INonPersistentEditorInput;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogPanel;
import org.jkiss.dbeaver.ui.editors.text.ScriptPositionColumn;
import org.jkiss.dbeaver.ui.views.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
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
    public static final String DEFAULT_PATTERN = "<${" + VAR_CONNECTION_NAME + "}> ${" + VAR_FILE_NAME + "}";
    public static final String VAR_FILE_EXT = "fileExt";
    public static final String VAR_DRIVER_NAME = "driverName";

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
    private volatile boolean syntaxLoaded = false;
    private volatile boolean ownContext = false;
    private final FindReplaceTarget findReplaceTarget = new FindReplaceTarget();
    private final List<SQLQuery> runningQueries = new ArrayList<>();
    private QueryResultsContainer curResultsContainer;
    private Image editorImage;

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
            onDataSourceChange();
            return true;
        }
        // Release ds container
        releaseContainer();
        closeAllJobs();

        dataSourceContainer = container;
        if (dataSourceContainer != null) {
            dataSourceContainer.getPreferenceStore().addPropertyChangeListener(this);
        }
        IEditorInput input = getEditorInput();
        if (input != null) {
            EditorUtils.setInputDataSource(input, container, true);
        }

        checkConnected(false, null);
        setPartName(getEditorName());

        onDataSourceChange();

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
                                UIUtils.showErrorDialog(getSite().getShell(), "Open context", "Can't open editor connection", job.error);
                            } else {
                                DBeaverUI.syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        onDataSourceChange();
                                    }
                                });
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
    }

    private void releaseExecutionContext() {
        if (ownContext && executionContext != null) {
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
            dataSourceContainer.release(this);
            dataSourceContainer = null;
        }
    }

    private class OpenContextJob extends AbstractJob {
        private final DBPDataSource dataSource;
        private Throwable error;
        protected OpenContextJob(DBPDataSource dataSource) {
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
        protected CloseContextJob(DBCExecutionContext context) {
            super("Close context " + context.getContextName());
            this.context = context;
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            monitor.beginTask("Close SQLEditor isolated connection", 1);
            try {
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
        if (!isNonPersistentEditor() && super.isDirty()) {
            return true;
        }
        for (QueryProcessor queryProcessor : queryProcessors) {
            if (queryProcessor.isDirty()) {
                return true;
            }
        }
        return false;
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

        sashForm = UIUtils.createPartDivider(this, parent, SWT.VERTICAL | SWT.SMOOTH);
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
                    curResultsContainer = (QueryResultsContainer) data;
                    curQueryProcessor = curResultsContainer.queryProcessor;
                    ResultSetViewer rsv = curResultsContainer.getResultSetController();
                    if (rsv != null) {
                        //rsv.getActivePresentation().getControl().setFocus();
                    }
                } else if (data == outputViewer) {
                    ((CTabItem) e.item).setImage(IMG_OUTPUT);
                    outputViewer.resetNewOutput();
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

/*
            ToolItem planItem = new ToolItem(rsToolbar, SWT.NONE);
            planItem.setText("Plan");
            planItem.setImage(IMG_EXPLAIN_PLAN);
            planItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (curResultsContainer != null && curResultsContainer.query != null) {
                        explainQueryPlan(curResultsContainer.query);
                    } else {
                        UIUtils.showErrorDialog(
                            sashForm.getShell(),
                            CoreMessages.editors_sql_error_execution_plan_title,
                            "Select tab with SQL query results");
                    }
                }
            });
*/

            final ToolItem logItem = new ToolItem(rsToolbar, SWT.CHECK);
            logItem.setText("Log");
            logItem.setToolTipText("Query execution log");
            logItem.setImage(IMG_LOG);
            logItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showExtraView(logItem, CoreMessages.editors_sql_execution_log, "SQL query execution log", IMG_LOG, logViewer);
                }
            });

            toolOutputItem = new ToolItem(rsToolbar, SWT.CHECK);
            toolOutputItem.setText("Output");
            toolOutputItem.setImage(IMG_OUTPUT);
            toolOutputItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    toolOutputItem.setImage(IMG_OUTPUT);
                    showExtraView(toolOutputItem, CoreMessages.editors_sql_output, "Database server output log", IMG_OUTPUT, outputViewer);
                }
            });

            resultTabs.setTopRight(rsToolbar);
        }
        //resultTabs.getItem(0).addListener();

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
                    if (sashForm.getMaximizedControl() == null) {
                        manager.add(new Action("Maximize results") {
                            @Override
                            public void run()
                            {
                                toggleEditorMaximize();
                            }
                        });
                    } else {
                        manager.add(new Action("Normalize results") {
                            @Override
                            public void run()
                            {
                                toggleEditorMaximize();
                            }
                        });
                    }
                    if (resultTabs.getItemCount() > 3) {
                        manager.add(new Action("Close multiple results") {
                            @Override
                            public void run()
                            {
                                closeExtraResultTabs(null);
                            }
                        });
                    }
                    final CTabItem activeTab = resultTabs.getSelection();
                    if (activeTab != null && resultTabs.indexOf(activeTab) > 0 && activeTab.getData() instanceof QueryResultsContainer) {
                        final QueryResultsContainer resultsContainer = (QueryResultsContainer)activeTab.getData();
                        manager.add(new Separator());
                        final boolean isPinned = resultsContainer.isPinned();
                        manager.add(new Action(isPinned ? "Unpin tab" : "Pin tab") {
                            @Override
                            public void run()
                            {
                                resultsContainer.setPinned(!isPinned);
                            }
                        });
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
                }
            });
            menuMgr.setRemoveAllWhenShown(true);
            resultTabs.setMenu(menu);
        }
    }

    private void showExtraView(final ToolItem toolItem, String name, String toolTip, Image image, Control view) {
        if (toolItem.getSelection()) {
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
        } else {
            for (CTabItem item : resultTabs.getItems()) {
                if (item.getData() == view) {
                    item.dispose();
                }
            }
        }
    }

    private void toggleEditorMaximize()
    {
        if (sashForm.getMaximizedControl() == null) {
            sashForm.setMaximizedControl(resultTabs);
        } else {
            sashForm.setMaximizedControl(null);
        }
    }

    public void toggleResultPanel() {
        if (sashForm.getMaximizedControl() == null) {
            sashForm.setMaximizedControl(editorControl);
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

        final DBPDataSourceContainer dsContainer = EditorUtils.getInputDataSource(editorInput);
        if (dsContainer != null) {
            dsContainer.getRegistry().addDataSourceListener(this);
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
        final SQLQuery sqlQuery = extractActiveQuery();
        if (sqlQuery == null) {
            setStatus(CoreMessages.editors_sql_status_empty_query_string, DBPMessageType.ERROR);
            return;
        }
        explainQueryPlan(sqlQuery);
    }

    public void explainQueryPlan(SQLQuery sqlQuery)
    {
//        final CTabItem planItem = UIUtils.getTabItem(resultTabs, planView);
//        if (planItem != null) {
//            resultTabs.setSelection(planItem);
//        }
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

            CTabItem item = new CTabItem(resultTabs, SWT.CLOSE);
            item.setControl(planView.getControl());
            item.setText("Exec. Plan");
            item.setToolTipText("Execution plan for\n" + sqlQuery.getQuery());
            item.setImage(IMG_EXPLAIN_PLAN);
            item.setData(planView);
            resultTabs.setSelection(item);
        }

        try {
            planView.explainQueryPlan(getExecutionContext(), sqlQuery);
        } catch (DBCException e) {
            UIUtils.showErrorDialog(
                sashForm.getShell(),
                CoreMessages.editors_sql_error_execution_plan_title,
                CoreMessages.editors_sql_error_execution_plan_message,
                e);
        }
    }

    public void processSQL(boolean newTab, boolean script) {
        processSQL(newTab, script, null);
    }

    public void processSQL(boolean newTab, boolean script, SQLQueryTransformer transformer)
    {
        IDocument document = getDocument();
        if (document == null) {
            setStatus(CoreMessages.editors_sql_status_cant_obtain_document, DBPMessageType.ERROR);
            return;
        }
        List<SQLQuery> queries;
        if (script) {
            // Execute all SQL statements consequently
            ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
            if (selection.getLength() > 1) {
                queries = extractScriptQueries(selection.getOffset(), selection.getLength());
            } else {
                queries = extractScriptQueries(0, document.getLength());
            }
        } else {
            // Execute statement under cursor or selected text (if selection present)
            SQLQuery sqlQuery = extractActiveQuery();
            if (sqlQuery == null) {
                setStatus(CoreMessages.editors_sql_status_empty_query_string, DBPMessageType.ERROR);
                return;
            } else {
                queries = Collections.singletonList(sqlQuery);
            }
        }
        try {
            if (transformer != null) {
                DBPDataSource dataSource = getDataSource();
                if (dataSource instanceof SQLDataSource) {
                    List<SQLQuery> xQueries = new ArrayList<>(queries.size());
                    for (int i = 0; i < queries.size(); i++) {
                        SQLQuery query = transformer.transformQuery((SQLDataSource)dataSource, queries.get(i));
                        if (query != null) {
                            xQueries.add(query);
                        }
                    }
                    queries = xQueries;
                }
            }
        }
        catch (DBException e) {
            UIUtils.showErrorDialog(getSite().getShell(), "Bad query", "Can't execute query", e);
            return;
        }
        processQueries(queries, newTab, false, true);
    }

    public void exportDataFromQuery()
    {
        SQLQuery sqlQuery = extractActiveQuery();
        if (sqlQuery != null) {
            processQueries(Collections.singletonList(sqlQuery), false, true, true);
        }
    }

    private void processQueries(@NotNull final List<SQLQuery> queries, final boolean newTab, final boolean export, final boolean checkSession)
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
                            UIUtils.showErrorDialog(
                                getSite().getShell(),
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
                                processQueries(queries, newTab, export, false);
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
                UIUtils.showErrorDialog(
                    getSite().getShell(),
                    CoreMessages.editors_sql_error_cant_obtain_session,
                    ex.getMessage());
                return;
            }
        }

        if (sashForm.getMaximizedControl() != null) {
            sashForm.setMaximizedControl(null);
        }

        // Save editor
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE) && isDirty()) {
            doSave(new NullProgressMonitor());
        }

        final boolean isSingleQuery = (queries.size() == 1);

        if (!newTab || !isSingleQuery) {
            // We don't need new tab or we are executing a script - so close all extra tabs
            closeExtraResultTabs(null);
        }

        if (newTab) {
            // Execute each query in a new tab
            for (int i = 0; i < queries.size(); i++) {
                SQLQuery query = queries.get(i);
                QueryProcessor queryProcessor = (i == 0 && !isSingleQuery ? curQueryProcessor : createQueryProcessor(queries.size() == 1));
                queryProcessor.processQueries(Collections.singletonList(query), true, export);
            }
        } else {
            // Use current tab.
            // If current tab was pinned then use first tab
            if (curQueryProcessor.getFirstResults().isPinned()) {
                curQueryProcessor = queryProcessors.get(0);
            }
            closeExtraResultTabs(curQueryProcessor);
            resultTabs.setSelection(curQueryProcessor.getFirstResults().tabItem);
            curQueryProcessor.processQueries(queries, false, export);
        }
    }

    private List<SQLQuery> extractScriptQueries(int startOffset, int length)
    {
        List<SQLQuery> queryList = new ArrayList<>();

        IDocument document = getDocument();
        if (document == null) {
            return queryList;
        }

        this.startScriptEvaluation();
        try {
            for (int queryOffset = startOffset; ; ) {
                SQLQuery query = parseQuery(document, queryOffset, startOffset + length, queryOffset);
                if (query == null) {
                    break;
                }
                queryList.add(query);
                queryOffset = query.getOffset() + query.getLength() + 1;
            }
        }
        finally {
            this.endScriptEvaluation();
        }

        if (getActivePreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)) {
            // Parse parameters
            for (SQLQuery query : queryList) {
                query.setParameters(parseParameters(getDocument(), query));
            }
        }
        return queryList;
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

    private void onDataSourceChange()
    {
        updateExecutionContext();

        if (sashForm == null || sashForm.isDisposed()) {
            return;
        }

        DatabaseEditorUtils.setPartBackground(this, sashForm);


        DBCExecutionContext executionContext = getExecutionContext();
        if (syntaxLoaded && lastExecutionContext == executionContext) {
            return;
        }
        for (QueryProcessor queryProcessor : queryProcessors) {
            for (QueryResultsContainer resultsProvider : queryProcessor.getResultContainers()) {
                ResultSetViewer rsv = resultsProvider.getResultSetController();
                if (rsv != null) {
                    if (executionContext == null) {
                        rsv.setStatus(CoreMessages.editors_sql_status_not_connected_to_database);
                    } else {
                        rsv.setStatus(CoreMessages.editors_sql_staus_connected_to + executionContext.getDataSource().getContainer().getName() + "'"); //$NON-NLS-2$
                    }
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

        final DBPDataSourceContainer dsContainer = EditorUtils.getInputDataSource(editorInput);
        if (dsContainer != null) {
            dsContainer.getRegistry().removeDataSourceListener(this);
        }

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

    @Override
    public void handleDataSourceEvent(final DBPEvent event)
    {
        if (event.getObject() == getDataSourceContainer()) {
            DBeaverUI.asyncExec(
                new Runnable() {
                    @Override
                    public void run() {
                        switch (event.getAction()) {
                            case OBJECT_REMOVE:
                                getSite().getWorkbenchWindow().getActivePage().closeEditor(SQLEditor.this, false);
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
            Display display = Display.getCurrent();
            while (saveJob.finished == null) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            display.update();
            if (!saveJob.finished) {
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
        int jobsRunning = 0;
        for (QueryProcessor queryProcessor : queryProcessors) {
            jobsRunning += queryProcessor.curJobRunning.get();
        }
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
            UIUtils.showErrorDialog(getSite().getShell(), "File save", "Can't open SQL editor from external file", e);
        }
    }

    @Nullable
    public ResultSetViewer getActiveResultSetViewer()
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
                if (select) {
                    selectAndReveal(query.getOffset(), query.getLength());
                    setStatus(query.getQuery(), DBPMessageType.INFORMATION);
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
        if (setSelection) {
            resultTabs.setSelection(curResultsContainer.tabItem);
        }

        return queryProcessor;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (event.getProperty().equals(ModelPreferences.SCRIPT_STATEMENT_DELIMITER) ||
            event.getProperty().equals(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER) ||
            event.getProperty().equals(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK) ||
            event.getProperty().equals(ModelPreferences.SQL_PARAMETERS_ENABLED) ||
            event.getProperty().equals(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK) ||
            event.getProperty().equals(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED) ||
            event.getProperty().equals(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX))
        {
            reloadSyntaxRules();
        }
    }

    public class QueryProcessor implements SQLResultsConsumer {

        private SQLQueryJob curJob;
        private AtomicInteger curJobRunning = new AtomicInteger(0);
        private final List<QueryResultsContainer> resultContainers = new ArrayList<>();
        private DBDDataReceiver curDataReceiver = null;

        public QueryProcessor() {
            // Create first (default) results provider
            queryProcessors.add(this);
            createResultsProvider(0);
        }

        private QueryResultsContainer createResultsProvider(int resultSetNumber) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultSetNumber);
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
                curJob.close();
                curJob = null;
            }
        }

        void processQueries(final List<SQLQuery> queries, final boolean fetchResults, boolean export)
        {
            if (queries.isEmpty()) {
                // Nothing to process
                return;
            }
            if (curJobRunning.get() > 0) {
                UIUtils.showErrorDialog(
                    getSite().getShell(),
                    CoreMessages.editors_sql_error_cant_execute_query_title,
                    CoreMessages.editors_sql_error_cant_execute_query_message);
                return;
            }
            final DBCExecutionContext executionContext = getExecutionContext();
            if (executionContext == null) {
                UIUtils.showErrorDialog(
                    getSite().getShell(),
                    CoreMessages.editors_sql_error_cant_execute_query_title,
                    CoreMessages.editors_sql_status_not_connected_to_database);
                return;
            }
            final boolean isSingleQuery = (queries.size() == 1);

            // Prepare execution job
            {
                showScriptPositionRuler(true);
                QueryResultsContainer resultsContainer = getFirstResults();

                SQLQueryListener listener = new SQLEditorQueryListener(this);
                final SQLQueryJob job = new SQLQueryJob(
                    getSite(),
                    isSingleQuery ? CoreMessages.editors_sql_job_execute_query : CoreMessages.editors_sql_job_execute_script,
                    executionContext,
                    resultsContainer,
                    queries,
                    this,
                    listener);

                if (export) {
                    // Assign current job from active query and open wizard
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
                        if (!resultsProvider.tabItem.isDisposed()) {
                            resultsProvider.tabItem.setToolTipText(CommonUtils.truncateString(statement.getQuery(), 1000));
                            // Special statements (not real statements) have their name in data
                            if (isStatsResult) {
                                String tabName = "Statistics";
                                int queryIndex = queryProcessors.indexOf(QueryProcessor.this);
                                if (queryIndex > 0) {
                                    tabName += " - " + (queryIndex + 1);
                                }
                                resultsProvider.tabItem.setText(tabName);
                                if (!CommonUtils.isEmpty(statement.getQuery())) {
                                    resultsProvider.tabItem.setToolTipText(statement.getQuery());
                                }
                            }
                        }
                    }
                }
            });
            ResultSetViewer rsv = resultsProvider.getResultSetController();
            return rsv == null ? null : rsv.getDataReceiver();
        }

    }

    public class QueryResultsContainer implements DBSDataContainer, IResultSetContainer, IDataSourceContainerProvider {

        private final QueryProcessor queryProcessor;
        private final CTabItem tabItem;
        private final ResultSetViewer viewer;
        private final int resultSetNumber;
        private SQLQuery query = null;
        private SQLQuery lastGoodQuery = null;

        private QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber)
        {
            this.queryProcessor = queryProcessor;
            this.resultSetNumber = resultSetNumber;
            viewer = new ResultSetViewer(resultTabs, getSite(), this);

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
            tabItem.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    QueryResultsContainer.this.queryProcessor.removeResults(QueryResultsContainer.this);
                    if (QueryResultsContainer.this == curResultsContainer) {
                        curResultsContainer = null;
                    }
                }
            });
        }

        public boolean isPinned() {
            return resultTabs.indexOf(tabItem) > 0 && !tabItem.getShowClose();
        }

        public void setPinned(boolean pinned) {
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
        public int getSupportedFeatures()
        {
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
            final SQLQueryJob job = queryProcessor.curJob;
            if (job != null) {
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

                job.extractData(session, query, resultCounts > 1 ? 0 : resultSetNumber);

                lastGoodQuery = job.getLastGoodQuery();

                return job.getStatistics();
            } else {
                throw new DBCException("No active query - can't read data");
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
            DBPDataSource dataSource = getDataSource();
            if (!(dataSource instanceof SQLDataSource)) {
                throw new DBCException("Query transform is not supported by datasource");
            }
            try {
                SQLQuery countQuery = new SQLQueryTransformerCount().transformQuery((SQLDataSource) dataSource, query);
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
            return CoreMessages.editors_sql_description;
        }

        @Nullable
        @Override
        public DBSObject getParentObject()
        {
            return getDataSourceContainer();
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource()
        {
            return SQLEditor.this.getDataSource();
        }

        @Override
        public boolean isPersisted()
        {
            return true;
        }

        @NotNull
        @Override
        public String getName()
        {
            String name = lastGoodQuery == null ? null : lastGoodQuery.getOriginalQuery();
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
            return "SQL Query / " + SQLEditor.this.getEditorInput().getName() + ": " + query;
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

        private SQLEditorQueryListener(QueryProcessor queryProcessor) {
            this.queryProcessor = queryProcessor;
        }

        @Override
        public void onStartScript() {
            lastUIUpdateTime = -1;
            scriptMode = true;
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    sashForm.setMaximizedControl(editorControl);
                }
            });
        }

        @Override
        public void onStartQuery(DBCSession session, final SQLQuery query) {
            setTitleImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT_EXECUTE));
            queryProcessor.curJobRunning.incrementAndGet();
            synchronized (runningQueries) {
                runningQueries.add(query);
            }
            if (lastUIUpdateTime < 0 || System.currentTimeMillis() - lastUIUpdateTime > SCRIPT_UI_UPDATE_PERIOD) {
                DBeaverUI.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        topOffset = getTextViewer().getTopIndexStartOffset();
                        visibleLength = getTextViewer().getBottomIndexEndOffset() - topOffset;
                    }
                });
                showStatementInEditor(query, false);
                lastUIUpdateTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onEndQuery(final DBCSession session, final SQLQueryResult result) {
            setTitleImage(editorImage);
            synchronized (runningQueries) {
                runningQueries.remove(result.getStatement());
            }
            queryProcessor.curJobRunning.decrementAndGet();

            if (isDisposed()) {
                return;
            }
            DBeaverUI.runUIJob("Process SQL query result", new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    processQueryResult(session, result);
                    if (!result.hasError() && topOffset >= 0) {
                        final TextViewer textViewer = getTextViewer();
                        if (textViewer != null) {
                            //textViewer.revealRange(topOffset, visibleLength);
                        }
                    }
                }
            });
        }

        private void processQueryResult(DBCSession session, SQLQueryResult result) {
            if (!scriptMode) {
                runPostExecuteActions(result);
            }
            Throwable error = result.getError();
            if (error != null) {
                setStatus(GeneralUtils.getFirstMessage(error), DBPMessageType.ERROR);
                if (!scrollCursorToError(session, result, error)) {
                    getSelectionProvider().setSelection(originalSelection);
                }
            } else if (!scriptMode && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE)) {
                getSelectionProvider().setSelection(originalSelection);
            }
            // Get results window (it is possible that it was closed till that moment
            SQLQuery query = result.getStatement();
            {
                for (QueryResultsContainer cr : queryProcessor.resultContainers) {
                    cr.viewer.updateFiltersText(false);
                }
                // Set tab name only if we have just one resultset
                // If query produced multiple results - leave their names as is
                if (scriptMode || queryProcessor.getResultContainers().size() == 1) {
                    QueryResultsContainer results = queryProcessor.getResults(query);
                    if (results != null) {
                        CTabItem tabItem = results.tabItem;
                        if (!tabItem.isDisposed()) {
                            int queryIndex = queryProcessors.indexOf(queryProcessor);
                            String resultSetName = getResultsTabName(results.resultSetNumber, queryIndex, result.getResultSetName());
                            if (!CommonUtils.isEmpty(resultSetName)) {
                                tabItem.setText(resultSetName);
                            }
                        }
                    }
                }
            }

            if (dataSourceContainer != null && !scriptMode && dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.BEEP_ON_QUERY_END)) {
                Display.getCurrent().beep();
            }
            if (result.getQueryTime() > DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT) * 1000) {
                DBeaverUI.notifyAgent(
                        "Query completed [" + getEditorInput().getName() + "]" + GeneralUtils.getDefaultLineSeparator() +
                                CommonUtils.truncateString(query.getQuery(), 200), !result.hasError() ? IStatus.INFO : IStatus.ERROR);
            }
        }

        @Override
        public void onEndScript(final DBCStatistics statistics, final boolean hasErrors) {
            if (isDisposed()) {
                return;
            }
            runPostExecuteActions(null);
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
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
        }
    }

    private boolean scrollCursorToError(@NotNull DBCSession session, @NotNull SQLQueryResult result, @NotNull Throwable error) {
        DBCExecutionContext context = getExecutionContext();
        if (context == null) {
            return false;
        }
        try {
            boolean scrolled = false;
            DBPErrorAssistant errorAssistant = DBUtils.getAdapter(DBPErrorAssistant.class, context.getDataSource());
            if (errorAssistant != null) {
                SQLQuery query = result.getStatement();
                DBPErrorAssistant.ErrorPosition[] positions = errorAssistant.getErrorPosition(session, query.getQuery(), error);
                if (positions != null && positions.length > 0) {
                    int queryStartOffset = query.getOffset();
                    int queryLength = query.getLength();

                    DBPErrorAssistant.ErrorPosition pos = positions[0];
                    if (pos.line < 0) {
                        if (pos.position >= 0) {
                            // Only position
                            getSelectionProvider().setSelection(new TextSelection(queryStartOffset + pos.position, 1));
                            scrolled = true;
                        }
                    } else {
                        // Line + position
                        Document document = getDocument();
                        if (document != null) {
                            int startLine = document.getLineOfOffset(queryStartOffset);
                            int errorOffset = document.getLineOffset(startLine + pos.line);
                            int errorLength;
                            if (pos.position >= 0) {
                                errorOffset += pos.position;
                                errorLength = 1;
                            } else {
                                errorLength = document.getLineLength(startLine + pos.line);
                            }
                            if (errorOffset < queryStartOffset) errorOffset = queryStartOffset;
                            if (errorLength > queryLength) errorLength = queryLength;
                            getSelectionProvider().setSelection(new TextSelection(errorOffset, errorLength));
                            scrolled = true;
                        }
                    }
                }
            }
            return scrolled;
//            if (!scrolled) {
//                // Can't position on error - let's just select entire problem query
//                showStatementInEditor(result.getStatement(), true);
//            }
        } catch (Exception e) {
            log.warn("Error positioning on query error", e);
            return false;
        }
    }

    private class FindReplaceTarget extends DynamicFindReplaceTarget {
        private boolean lastFocusInEditor = true;
        @Override
        public IFindReplaceTarget getTarget() {
            ResultSetViewer rsv = getActiveResultSetViewer();
            boolean focusInEditor = getTextViewer().getTextWidget().isFocusControl();
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
            }
            return getTextViewer().getFindReplaceTarget();
        }
    }

    private class DynamicSelectionProvider extends CompositeSelectionProvider {
        private boolean lastFocusInEditor = true;
        @Override
        public ISelectionProvider getProvider() {
            ResultSetViewer rsv = getActiveResultSetViewer();
            boolean focusInEditor = getTextViewer().getTextWidget().isFocusControl();
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
            }
            return getTextViewer().getSelectionProvider();
        }
    }

    private void runPostExecuteActions(@Nullable SQLQueryResult result) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            final DBPDataSource dataSource = executionContext.getDataSource();
            // Dump server output
            DBCServerOutputReader outputReader = DBUtils.getAdapter(DBCServerOutputReader.class, dataSource);
            if (outputReader == null && result != null) {
                outputReader = new DefaultServerOutputReader(result);
            }
            if (outputReader != null && outputReader.isServerOutputEnabled()) {
                dumpServerOutput(executionContext, outputReader);
            }
            // Refresh active object
            if (result == null || !result.hasError() && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)) {
                final DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
                if (objectSelector != null) {
                    new AbstractJob("Refresh default object") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            try (DBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Refresh default object")) {
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

    private void dumpServerOutput(@NotNull final DBCExecutionContext executionContext, @NotNull final DBCServerOutputReader outputReader) {
        new AbstractJob("Dump server output") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                final StringWriter dump = new StringWriter();
                try {
                    outputReader.readServerOutput(monitor, executionContext, new PrintWriter(dump, true));
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
                                    CTabItem outputItem = UIUtils.getTabItem(resultTabs, outputViewer);
                                    if (outputItem != null && outputItem != resultTabs.getSelection()) {
                                        outputItem.setImage(IMG_OUTPUT_ALERT);
                                    } else {
                                        toolOutputItem.setImage(IMG_OUTPUT_ALERT);
                                    }
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private class SaveJob extends AbstractJob {
        private transient Boolean finished = null;

        public SaveJob() {
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
                if (ownContext && executionContext != null) {
                    if (DataSourceHandler.isContextTransactionAffected(executionContext)) {
                        DataSourceHandler.closeActiveTransaction(monitor, executionContext, true);
                    }
                }
                finished = true;
                return Status.OK_STATUS;
            } catch (Throwable e) {
                finished = false;
                log.error(e);
                return GeneralUtils.makeExceptionStatus(e);
            } finally {
                if (finished == null) {
                    finished = true;
                }
            }
        }
    }
}
