/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.rulers.IColumnSupport;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.eclipse.ui.texteditor.rulers.RulerColumnRegistry;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DefaultServerOutputReader;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.sql.SQLQueryTransformer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLResultsConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.CompositeSelectionProvider;
import org.jkiss.dbeaver.ui.DynamicFindReplaceTarget;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogPanel;
import org.jkiss.dbeaver.ui.editors.text.ScriptPositionColumn;
import org.jkiss.dbeaver.ui.views.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
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
    IResultSetContainer,
    DBPDataSourceUser,
    DBPDataSourceHandler,
    DBPPreferenceListener
{
    private static final long SCRIPT_UI_UPDATE_PERIOD = 100;

    private static Image IMG_DATA_GRID = DBeaverActivator.getImageDescriptor("/icons/sql/page_data_grid.png").createImage(); //$NON-NLS-1$
    private static Image IMG_EXPLAIN_PLAN = DBeaverActivator.getImageDescriptor("/icons/sql/page_explain_plan.png").createImage(); //$NON-NLS-1$
    private static Image IMG_LOG = DBeaverActivator.getImageDescriptor("/icons/sql/page_error.png").createImage(); //$NON-NLS-1$
    private static Image IMG_OUTPUT = DBeaverActivator.getImageDescriptor("/icons/sql/page_output.png").createImage(); //$NON-NLS-1$
    private static Image IMG_OUTPUT_ALERT = DBeaverActivator.getImageDescriptor("/icons/sql/page_output_alert.png").createImage(); //$NON-NLS-1$

    private SashForm sashForm;
    private Control editorControl;
    private CTabFolder resultTabs;

    private ExplainPlanViewer planView;
    private SQLEditorOutputViewer outputViewer;

    private volatile QueryProcessor curQueryProcessor;
    private final List<QueryProcessor> queryProcessors = new ArrayList<QueryProcessor>();

    private DBSDataSourceContainer dataSourceContainer;
    private DBPDataSource curDataSource;
    private volatile DBCExecutionContext executionContext;
    private volatile boolean ownContext = false;
    private final FindReplaceTarget findReplaceTarget = new FindReplaceTarget();
    private final List<SQLQuery> runningQueries = new ArrayList<SQLQuery>();

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
        IFile file = EditorUtils.getFileFromEditorInput(getEditorInput());
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
            List<Integer> lines = new ArrayList<Integer>(runningQueries.size() * 2);
            for (SQLQuery statementInfo : runningQueries) {
                try {
                    int firstLine = document.getLineOfOffset(statementInfo.getOffset());
                    int lastLine = document.getLineOfOffset(statementInfo.getOffset() + statementInfo.getLength());
                    for (int k = firstLine; k <= lastLine; k++) {
                        lines.add(k);
                    }
                } catch (BadLocationException e) {
                    log.debug(e);
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
    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }

    @Override
    public boolean setDataSourceContainer(@Nullable DBSDataSourceContainer container)
    {
        if (container == dataSourceContainer) {
            return true;
        }
        // Release ds container
        releaseContainer();
        closeAllJobs();

        dataSourceContainer = container;
        if (dataSourceContainer != null) {
            dataSourceContainer.getPreferenceStore().addPropertyChangeListener(this);
        }
        IPathEditorInput input = getEditorInput();
        if (input == null) {
            return false;
        }
        IFile file = EditorUtils.getFileFromEditorInput(input);
        if (file == null || !file.exists()) {
            log.warn("File '" + input.getPath() + "' doesn't exists");
            return false;
        }
        SQLEditorInput.setScriptDataSource(file, container, true);
        checkConnected();

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
                if (getActivePreferenceStore().getBoolean(DBeaverPreferences.EDITOR_SEPARATE_CONNECTION)) {
                    final OpenContextJob job = new OpenContextJob(dataSource);
                    job.addJobChangeListener(new JobChangeAdapter() {
                        @Override
                        public void done(IJobChangeEvent event) {
                            if (job.error != null) {
                                releaseExecutionContext();
                                UIUtils.showErrorDialog(getSite().getShell(), "Open context", "Can't open editor connection", job.error);
                            } else {
                                UIUtils.runInUI(null, new Runnable() {
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
                String title = "SQLEditor <" + getEditorInput().getPath().removeFileExtension().lastSegment() + ">";
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

    private void releaseExecutionContext() {
        if (ownContext && executionContext != null) {
            executionContext.close();
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

    @Override
    public boolean isDirty()
    {
        if (super.isDirty()) {
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
        ResultSetViewer resultsView = getResultSetViewer();
        if (resultsView != null) {
            Object adapter = resultsView.getAdapter(required);
            if (adapter != null) {
                return adapter;
            }
        }
        return super.getAdapter(required);
    }

    private boolean checkConnected()
    {
        // Connect to datasource
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            if (!dataSourceContainer.isConnected()) {
                DataSourceHandler.connectToDataSource(null, dataSourceContainer, null);
            }
        }
        setPartName(getEditorInput().getName());
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

        final StyledText textWidget = getTextViewer().getTextWidget();

        // TODO: it is a hack. We change selection on each caret move to trigger toolbar elements update.
        // TODO: without this hack toolbar won't update Execute* commands.
        // TODO: Need some better solution or workaround.
        textWidget.addCaretListener(new CaretListener() {
            @Override
            public void caretMoved(CaretEvent event) {
                ((SQLEditorSourceViewer)getSourceViewer()).refreshTextSelection();
            }
        });

        createResultTabs();

        // Update controls
        onDataSourceChange();
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
                    QueryResultsContainer resultsProvider = (QueryResultsContainer) data;
                    curQueryProcessor = resultsProvider.queryProcessor;
                    ResultSetViewer rsv = resultsProvider.getResultSetViewer();
                    if (rsv != null) {
                        rsv.getActivePresentation().getControl().setFocus();
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
                    ResultSetViewer viewer = getResultSetViewer();
                    if (viewer != null && viewer.getActivePresentation().getControl().isVisible()) {
                        viewer.getActivePresentation().getControl().setFocus();
                        e.doit = false;
                        e.detail = SWT.TRAVERSE_NONE;
                    }
                }
            }
        });
        resultTabs.setSimple(true);
        //resultTabs.getItem(0).addListener();

        planView = new ExplainPlanViewer(this, resultTabs);
        final SQLLogPanel logViewer = new SQLLogPanel(resultTabs, this);

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

        // Create tabs
        createQueryProcessor(true);

        outputViewer = new SQLEditorOutputViewer(getSite(), resultTabs, SWT.NONE);

        CTabItem item = new CTabItem(resultTabs, SWT.NONE);
        item.setControl(planView.getControl());
        item.setText(CoreMessages.editors_sql_explain_plan);
        item.setImage(IMG_EXPLAIN_PLAN);
        item.setData(planView);

        item = new CTabItem(resultTabs, SWT.NONE);
        item.setControl(logViewer);
        item.setText(CoreMessages.editors_sql_execution_log);
        item.setImage(IMG_LOG);
        item.setData(logViewer);

        item = new CTabItem(resultTabs, SWT.NONE);
        item.setControl(outputViewer);
        item.setText(CoreMessages.editors_sql_output);
        item.setImage(IMG_OUTPUT);
        item.setData(outputViewer);

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
                }
            });
            menuMgr.setRemoveAllWhenShown(true);
            resultTabs.setMenu(menu);
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


    @Override
    public IPathEditorInput getEditorInput()
    {
        return (IPathEditorInput) super.getEditorInput();
    }

    @Override
    public void init(IEditorSite site, IEditorInput editorInput)
        throws PartInitException
    {
        super.init(site, editorInput);

        IProject project = getProject();
        if (project != null) {
            final DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
            if (dataSourceRegistry != null) {
                dataSourceRegistry.addDataSourceListener(this);
            }
        }
    }

    @Override
    protected void doSetInput(IEditorInput editorInput) throws CoreException
    {
        if (!(editorInput instanceof IPathEditorInput)) {
            throw new PartInitException("Invalid Input: Must be " + IPathEditorInput.class.getSimpleName());
        }
        IFile file = EditorUtils.getFileFromEditorInput(editorInput);
        if (file == null || !file.exists()) {
            throw new PartInitException("File '" + ((IPathEditorInput) editorInput).getPath() + "' doesn't exists");
        }
        super.doSetInput(editorInput);

        setDataSourceContainer(SQLEditorInput.getScriptDataSource(file));
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
            setStatus(CoreMessages.editors_sql_status_empty_query_string, true);
            return;
        }
        final CTabItem planItem = UIUtils.getTabItem(resultTabs, planView);
        if (planItem != null) {
            resultTabs.setSelection(planItem);
        }
        try {
            planView.explainQueryPlan(sqlQuery.getQuery());
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
            setStatus(CoreMessages.editors_sql_status_cant_obtain_document, true);
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
                setStatus(CoreMessages.editors_sql_status_empty_query_string, true);
                return;
            } else {
                queries = Collections.singletonList(sqlQuery);
            }
        }
        try {
            if (transformer != null) {
                for (SQLQuery query : queries) {
                    transformer.transformQuery(query);
                }
            }
        }
        catch (DBException e) {
            UIUtils.showErrorDialog(getSite().getShell(), "Bad query", "Can't execute query", e);
            return;
        }
        processQueries(queries, newTab, false);
    }

    public void exportDataFromQuery()
    {
        SQLQuery sqlQuery = extractActiveQuery();
        if (sqlQuery != null) {
            processQueries(Collections.singletonList(sqlQuery), false, true);
        }
    }

    private void processQueries(@NotNull final List<SQLQuery> queries, final boolean newTab, final boolean export)
    {
        if (queries.isEmpty()) {
            // Nothing to process
            return;
        }
        DBSDataSourceContainer container = getDataSourceContainer();
        if (container != null && container.isConnectionReadOnly()) {
/*
            // Allow only selects
            for (SQLQuery query : queries) {
                if (query.getType() != SQLQueryType.SELECT && query.getType() != SQLQueryType.UNKNOWN) {
                    UIUtils.showErrorDialog(
                        getSite().getShell(),
                        CoreMessages.editors_sql_error_cant_execute_query_title,
                        "Read-only connection '" + container.getName() + "' restricts execution of non-select statements. " +
                        "Query [" + query.getQuery() + "] is " + query.getType() + " and can't be processed");
                    return;
                }
            }
*/
        }
        try {
            checkSession();
        } catch (DBException ex) {
            ResultSetViewer viewer = getResultSetViewer();
            if (viewer != null) {
                viewer.setStatus(ex.getMessage(), true);
            }
            UIUtils.showErrorDialog(
                getSite().getShell(),
                CoreMessages.editors_sql_error_cant_obtain_session,
                ex.getMessage());
            return;
        }

        if (sashForm.getMaximizedControl() != null) {
            sashForm.setMaximizedControl(null);
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
            // Use current tab
            closeExtraResultTabs(curQueryProcessor);
            resultTabs.setSelection(curQueryProcessor.getFirstResults().tabItem);
            curQueryProcessor.processQueries(queries, false, export);
        }
    }

    private List<SQLQuery> extractScriptQueries(int startOffset, int length)
    {
        List<SQLQuery> queryList = new ArrayList<SQLQuery>();

        IDocument document = getDocument();
        if (document == null) {
            return queryList;
        }

        for (int queryOffset = startOffset;;) {
            SQLQuery query = parseQuery(document, queryOffset, startOffset + length, queryOffset);
            if (query == null) {
                break;
            }
            queryList.add(query);
            queryOffset = query.getOffset() + query.getLength() + 1;
        }

        if (getActivePreferenceStore().getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)) {
            // Parse parameters
            for (SQLQuery query : queryList) {
                query.setParameters(parseParameters(getDocument(), query));
            }
        }
        return queryList;
    }

    private void setStatus(String status, boolean error)
    {
        ResultSetViewer resultsView = getResultSetViewer();
        if (resultsView != null) {
            resultsView.setStatus(status, error);
        }
    }

    private void closeExtraResultTabs(@Nullable QueryProcessor queryProcessor)
    {
        // Close all tabs except first one
        for (int i = resultTabs.getItemCount() - 1; i > 0; i--) {
            CTabItem item = resultTabs.getItem(i);
            if (item.getData() instanceof QueryResultsContainer) {
                QueryResultsContainer resultsProvider = (QueryResultsContainer)item.getData();
                if (queryProcessor != null && queryProcessor != resultsProvider.queryProcessor) {
                    continue;
                }
                if (queryProcessor != null && queryProcessor.resultProviders.size() < 2) {
                    // Do not remove first tab for this processor
                    continue;
                }
                item.dispose();
            }
        }
    }

    private void checkSession()
        throws DBException
    {
        if (getDataSourceContainer() == null || !getDataSourceContainer().isConnected()) {
            throw new DBException("No active connection");
        }
    }

    private void onDataSourceChange()
    {
        updateExecutionContext();

        if (sashForm == null || sashForm.isDisposed()) {
            return;
        }

        DatabaseEditorUtils.setPartBackground(this, sashForm);

        DBCExecutionContext executionContext = getExecutionContext();
        for (QueryProcessor queryProcessor : queryProcessors) {
            for (QueryResultsContainer resultsProvider : queryProcessor.getResultProviders()) {
                ResultSetViewer rsv = resultsProvider.getResultSetViewer();
                if (rsv != null) {
                    if (executionContext == null) {
                        rsv.setStatus(CoreMessages.editors_sql_status_not_connected_to_database);
                    } else {
                        rsv.setStatus(CoreMessages.editors_sql_staus_connected_to + executionContext.getDataSource().getContainer().getName() + "'"); //$NON-NLS-2$
                    }
                    rsv.updateFiltersText();
                }
            }
        }
        if (planView != null) {
            // Refresh plan view
            planView.refresh(executionContext);
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
        // Acquire ds container
        releaseContainer();
        closeAllJobs();

        IProject project = getProject();
        if (project != null) {
            final DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
            if (dataSourceRegistry != null) {
                dataSourceRegistry.removeDataSourceListener(this);
            }
        }

        planView = null;
        queryProcessors.clear();
        curQueryProcessor = null;

        super.dispose();
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
            getSite().getShell().getDisplay().asyncExec(
                new Runnable() {
                    @Override
                    public void run() {
                        switch (event.getAction()) {
                            case OBJECT_REMOVE:
                                getSite().getWorkbenchWindow().getActivePage().closeEditor(SQLEditor.this, false);
                                break;
                        }
                        onDataSourceChange();
                    }
                }
            );
        }
    }

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        for (QueryProcessor queryProcessor : queryProcessors) {
            for (QueryResultsContainer resultsProvider : queryProcessor.getResultProviders()) {
                ResultSetViewer rsv = resultsProvider.getResultSetViewer();
                if (rsv != null && rsv.isDirty()) {
                    rsv.doSave(progressMonitor);
                }
            }
        }
        if (ownContext && executionContext != null) {
            if (DataSourceHandler.isContextTransactionAffected(executionContext)) {
                DataSourceHandler.closeActiveTransaction(new DefaultProgressMonitor(progressMonitor), executionContext, true);
            }
//            releaseExecutionContext();
        }
        super.doSave(progressMonitor);
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
            MessageBox messageBox = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.OK);
            messageBox.setMessage(CoreMessages.editors_sql_save_on_close_message);
            messageBox.setText(CoreMessages.editors_sql_save_on_close_text);
            messageBox.open();
            return ISaveablePart2.CANCEL;
        }

        for (QueryProcessor queryProcessor : queryProcessors) {
            for (QueryResultsContainer resultsProvider : queryProcessor.getResultProviders()) {
                ResultSetViewer rsv = resultsProvider.getResultSetViewer();
                if (rsv != null && rsv.isDirty()) {
                    return rsv.promptToSaveOnClose();
                }
            }
        }

        return ISaveablePart2.DEFAULT;
    }

    @Nullable
    @Override
    public ResultSetViewer getResultSetViewer()
    {
        if (resultTabs == null || resultTabs.isDisposed()) {
            return null;
        }
        CTabItem curTab = resultTabs.getSelection();
        if (curTab != null && !curTab.isDisposed() && curTab.getData() instanceof QueryResultsContainer) {
            return ((QueryResultsContainer)curTab.getData()).getResultSetViewer();
        }

        return curQueryProcessor == null ? null : curQueryProcessor.getCurrentResults().getResultSetViewer();
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer()
    {
        return curQueryProcessor == null ? null : curQueryProcessor.getCurrentResults();
    }

    @Override
    public boolean isReadyToRun()
    {
        if (resultTabs == null) {
            return false;
        }
        CTabItem curTab = resultTabs.getSelection();
        if (curTab != null && curTab.getData() instanceof QueryResultsContainer) {
            return ((QueryResultsContainer)curTab.getData()).isReadyToRun();
        }

        return curQueryProcessor != null &&
                curQueryProcessor.getFirstResults().isReadyToRun();
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
                    setStatus(query.getQuery(), false);
                } else {
                    getSourceViewer().revealRange(query.getOffset(), query.getLength());
                }
            }
        });
    }

    @Override
    public void reloadSyntaxRules() {
        super.reloadSyntaxRules();
        outputViewer.refreshStyles();
    }

    private QueryProcessor createQueryProcessor(boolean setSelection)
    {
        final QueryProcessor queryProcessor = new QueryProcessor();
        curQueryProcessor = queryProcessor;

        if (setSelection) {
            resultTabs.setSelection(queryProcessor.getFirstResults().tabItem);
        }

        return queryProcessor;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (event.getProperty().equals(ModelPreferences.SCRIPT_STATEMENT_DELIMITER) ||
            event.getProperty().equals(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER) ||
            event.getProperty().equals(ModelPreferences.SQL_PARAMETERS_ENABLED) ||
            event.getProperty().equals(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK) ||
            event.getProperty().equals(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED))
        {
            reloadSyntaxRules();
        }
    }

    public class QueryProcessor implements SQLResultsConsumer {

        private SQLQueryJob curJob;
        private AtomicInteger curJobRunning = new AtomicInteger(0);
        private final List<QueryResultsContainer> resultProviders = new ArrayList<QueryResultsContainer>();
        private DBDDataReceiver curDataReceiver = null;

        public QueryProcessor() {
            // Create first (default) results provider
            queryProcessors.add(this);
            createResultsProvider(0);
        }

        private QueryResultsContainer createResultsProvider(int resultSetNumber) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultSetNumber);
            resultProviders.add(resultsProvider);
            return resultsProvider;
        }
        @NotNull
        QueryResultsContainer getFirstResults()
        {
            return resultProviders.get(0);
        }
        @Nullable
        QueryResultsContainer getResults(SQLQuery query) {
            for (QueryResultsContainer provider : resultProviders) {
                if (provider.query == query) {
                    return provider;
                }
            }
            return null;
        }
        @NotNull
        QueryResultsContainer getCurrentResults()
        {
            if (!resultTabs.isDisposed()) {
                CTabItem curTab = resultTabs.getSelection();
                if (curTab != null && curTab.getData() instanceof QueryResultsContainer) {
                    return (QueryResultsContainer)curTab.getData();
                }
            }
            return getFirstResults();
        }

        List<QueryResultsContainer> getResultProviders() {
            return resultProviders;
        }

        private void closeJob()
        {
            final SQLQueryJob job = curJob;
            if (job != null) {
                if (job.getState() == Job.RUNNING) {
                    job.cancel();
                }
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
                    ResultSetViewer rsv = resultsContainer.getResultSetViewer();
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
                }
            }
        }

        public boolean isDirty() {
            for (QueryResultsContainer resultsProvider : resultProviders) {
                ResultSetViewer rsv = resultsProvider.getResultSetViewer();
                if (rsv != null && rsv.isDirty()) {
                    return true;
                }
            }
            return false;
        }

        void removeResults(QueryResultsContainer resultsProvider) {
            if (resultProviders.size() > 1) {
                resultProviders.remove(resultsProvider);
            } else {
                queryProcessors.remove(this);
                if (curQueryProcessor == this) {
                    if (queryProcessors.isEmpty()) {
                        curQueryProcessor = null;
                    } else {
                        curQueryProcessor = queryProcessors.get(0);
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
//                for (QueryResultsProvider provider : resultProviders) {
//                    if (provider.query != null && provider.query.getData() == SQLQueryJob.STATS_RESULTS) {
//                        resultSetNumber = provider.resultSetNumber;
//                        break;
//                    }
//                }
//            }
            if (resultSetNumber >= resultProviders.size() && !isDisposed()) {
                // Open new results processor in UI thread
                getSite().getShell().getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        createResultsProvider(resultSetNumber);
                    }
                });
            }
            if (resultSetNumber >= resultProviders.size()) {
                // Editor seems to be disposed - no data receiver
                return null;
            }
            final QueryResultsContainer resultsProvider = resultProviders.get(resultSetNumber);
            // Open new results processor in UI thread
            getSite().getShell().getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (statement != null) {
                        resultsProvider.query = statement;
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
            });
            ResultSetViewer rsv = resultsProvider.getResultSetViewer();
            return rsv == null ? null : rsv.getDataReceiver();
        }

    }

    public class QueryResultsContainer implements DBSDataContainer, IResultSetContainer {

        private final QueryProcessor queryProcessor;
        private final CTabItem tabItem;
        private final ResultSetViewer viewer;
        private final int resultSetNumber;
        private SQLQuery query = null;

        private QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber)
        {
            this.queryProcessor = queryProcessor;
            this.resultSetNumber = resultSetNumber;
            viewer = new ResultSetViewer(resultTabs, getSite(), this);

            //boolean firstResultSet = queryProcessors.isEmpty();
            int tabIndex = Math.max(resultTabs.getItemCount() - 3, 0);
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
                }
            });
        }

        @Override
        public DBCExecutionContext getExecutionContext() {
            return SQLEditor.this.getExecutionContext();
        }

        @Nullable
        @Override
        public ResultSetViewer getResultSetViewer()
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
            return queryProcessor.curJob != null && queryProcessor.curJobRunning.get() == 0;
        }

        @Override
        public int getSupportedFeatures()
        {
            int features = DATA_SELECT;

            if (resultSetNumber == 0) {
                features |= DATA_FILTER;
            }
            return features;
        }

        @NotNull
        @Override
        public DBCStatistics readData(@NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException
        {
            final SQLQueryJob job = queryProcessor.curJob;
            if (job != null) {
                if (dataReceiver != viewer.getDataReceiver()) {
                    // Some custom receiver. Probably data export
                    queryProcessor.curDataReceiver = dataReceiver;
                } else {
                    queryProcessor.curDataReceiver = null;
                }
                if (resultSetNumber > 0) {
                    job.setFetchResultSetNumber(resultSetNumber);
                } else {
                    job.setFetchResultSetNumber(-1);
                }
                job.setResultSetLimit(firstRow, maxRows);
                job.setDataFilter(dataFilter);
                job.extractData(session);
                return job.getStatistics();
            } else {
                DBCStatistics statistics = new DBCStatistics();
                statistics.addMessage("No active query - can't read data");
                return statistics;
            }
        }

        @Override
        public long countData(@NotNull DBCSession session, DBDDataFilter dataFilter)
        {
            return -1;
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
            final SQLQueryJob job = queryProcessor.curJob;
            String name = job == null ? null :
                    job.getLastQuery() == null ? null : CommonUtils.truncateString(job.getLastQuery().getQuery(), 200);
            if (name == null) {
                name = "SQL";
            }
            return name;
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

        private SQLEditorQueryListener(QueryProcessor queryProcessor) {
            this.queryProcessor = queryProcessor;
        }

        @Override
        public void onStartScript() {
            lastUIUpdateTime = -1;
            scriptMode = true;
            UIUtils.runInUI(null, new Runnable() {
                @Override
                public void run() {
                    sashForm.setMaximizedControl(editorControl);
                }
            });
        }

        @Override
        public void onStartQuery(final SQLQuery query) {
            queryProcessor.curJobRunning.incrementAndGet();
            synchronized (runningQueries) {
                runningQueries.add(query);
            }
            if (lastUIUpdateTime < 0 || System.currentTimeMillis() - lastUIUpdateTime > SCRIPT_UI_UPDATE_PERIOD) {
                showStatementInEditor(query, false);
                lastUIUpdateTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onEndQuery(final SQLQueryResult result) {
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
                    processQueryResult(result);
                }
            });
        }

        private void processQueryResult(SQLQueryResult result) {
            DBCExecutionContext executionContext = getExecutionContext();
            if (executionContext != null) {
                DBCServerOutputReader outputReader = DBUtils.getAdapter(DBCServerOutputReader.class, executionContext.getDataSource());
                if (outputReader == null) {
                    outputReader = new DefaultServerOutputReader(result);
                }
                if (outputReader.isServerOutputEnabled()) {
                    dumpServerOutput(executionContext, outputReader);
                }
            }
            Throwable error = result.getError();
            if (error != null) {
                setStatus(GeneralUtils.getFirstMessage(error), true);
                scrollCursorToError(result, error);
            } else if (!scriptMode && dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE)) {
                getSelectionProvider().setSelection(originalSelection);
            }
            // Get results window (it is possible that it was closed till that moment
            SQLQuery query = result.getStatement();
            QueryResultsContainer results = queryProcessor.getResults(query);
            if (results != null) {
                CTabItem tabItem = results.tabItem;
                if (!tabItem.isDisposed()) {
                    int queryIndex = queryProcessors.indexOf(queryProcessor);
                    String resultSetName = getResultsTabName(results.resultSetNumber, queryIndex, result.getResultSetName());
                    tabItem.setText(resultSetName);
                }
            }

            if (result.getQueryTime() > DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT) * 1000) {
                DBeaverUI.notifyAgent(
                        "Query completed [" + getEditorInput().getPath().lastSegment() + "]" + GeneralUtils.getDefaultLineSeparator() +
                                CommonUtils.truncateString(query == null ? "" : query.getQuery(), 200), !result.hasError() ? IStatus.INFO : IStatus.ERROR);
            }
        }

        @Override
        public void onEndScript(final DBCStatistics statistics, final boolean hasErrors) {
            if (isDisposed()) {
                return;
            }
            UIUtils.runInUI(null, new Runnable() {
                @Override
                public void run() {
                    if (!hasErrors) {
                        getSelectionProvider().setSelection(originalSelection);
                    }
                    sashForm.setMaximizedControl(null);
                    QueryResultsContainer results = queryProcessor.getFirstResults();
                    ResultSetViewer viewer = results.getResultSetViewer();
                    if (viewer != null) {
                        viewer.getModel().setStatistics(statistics);
                        viewer.updateStatusMessage();
                    }
                }
            });
        }
    }

    private void scrollCursorToError(@NotNull SQLQueryResult result, @NotNull Throwable error) {
        DBCExecutionContext context = getExecutionContext();
        if (context == null) {
            return;
        }
        try {
            boolean scrolled = false;
            DBPErrorAssistant errorAssistant = DBUtils.getAdapter(DBPErrorAssistant.class, context.getDataSource());
            if (errorAssistant != null) {
                DBPErrorAssistant.ErrorPosition[] positions = errorAssistant.getErrorPosition(error);
                if (positions != null && positions.length > 0) {
                    int queryStartOffset = result.getStatement().getOffset();

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
                            getSelectionProvider().setSelection(new TextSelection(errorOffset, errorLength));
                            scrolled = true;
                        }
                    }
                }
            }
            if (!scrolled) {
                // Can't position on error - let's just select entire problem query
                showStatementInEditor(result.getStatement(), true);
            }
        } catch (Exception e) {
            log.warn("Error positioning on query error", e);
        }
    }

    private class FindReplaceTarget extends DynamicFindReplaceTarget {
        private boolean lastFocusInEditor = true;
        @Override
        public IFindReplaceTarget getTarget() {
            ResultSetViewer rsv = getResultSetViewer();
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
                IFindReplaceTarget nested = (IFindReplaceTarget) rsv.getAdapter(IFindReplaceTarget.class);
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
            ResultSetViewer rsv = getResultSetViewer();
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

    private void dumpServerOutput(final DBCExecutionContext executionContext, final DBCServerOutputReader outputReader) {
        DBeaverUI.runUIJob("Dump server output", new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                if (outputViewer.isDisposed()) {
                    return;
                }
                try {
                    outputReader.readServerOutput(monitor, executionContext, outputViewer.getOutputWriter());
                    if (outputViewer.isHasNewOutput()) {
                        outputViewer.scrollToEnd();
                        CTabItem outputItem = UIUtils.getTabItem(resultTabs, outputViewer);
                        if (outputItem != null && outputItem != resultTabs.getSelection()) {
                            outputItem.setImage(IMG_OUTPUT_ALERT);
                        }
                    }
                } catch (DBCException e) {
                    log.error("Error dumping server output", e);
                }
            }
        });
    }

}
