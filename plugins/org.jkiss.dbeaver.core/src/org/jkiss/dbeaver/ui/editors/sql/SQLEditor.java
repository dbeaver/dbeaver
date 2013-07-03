/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.rules.IToken;
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
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDataSourceContainerProviderEx;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryResult;
import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceConnectHandler;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogPanel;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLCommentToken;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLDelimiterToken;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.views.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SQL Executor
 */
public class SQLEditor extends SQLEditorBase
    implements IResourceChangeListener, IDataSourceContainerProviderEx,
    DBPEventListener, ISaveablePart2, ResultSetProvider, DBPDataSourceUser, DBPDataSourceHandler
{

    private static final long SCRIPT_UI_UPDATE_PERIOD = 100;

    private SashForm sashForm;
    private Control editorControl;
    private CTabFolder resultTabs;

    private ExplainPlanViewer planView;

    //private volatile SQLQueryJob curJob;
    private volatile DataContainer curDataContainer;
    private final List<DataContainer> dataContainers = new ArrayList<DataContainer>();

    private DBSDataSourceContainer dataSourceContainer;
    private final DynamicFindReplaceTarget findReplaceTarget = new DynamicFindReplaceTarget();

    private static Image imgDataGrid;
    private static Image imgExplainPlan;
    private static Image imgLog;
    private CompositeSelectionProvider selectionProvider;

    static {
        imgDataGrid = DBeaverActivator.getImageDescriptor("/icons/sql/page_data_grid.png").createImage(); //$NON-NLS-1$
        imgExplainPlan = DBeaverActivator.getImageDescriptor("/icons/sql/page_explain_plan.png").createImage(); //$NON-NLS-1$
        imgLog = DBeaverActivator.getImageDescriptor("/icons/sql/page_error.png").createImage(); //$NON-NLS-1$
    }

    public SQLEditor()
    {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public DBPDataSource getDataSource()
    {
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        return dataSourceContainer == null ? null : dataSourceContainer.getDataSource();
    }

    public IProject getProject()
    {
        IFile file = ContentUtils.convertPathToWorkspaceFile(getEditorInput().getPath());
        return file == null ? null : file.getProject();
    }

    @Override
    public DBSDataSourceContainer getDataSourceContainer()
    {
        return dataSourceContainer;
    }

    @Override
    public boolean setDataSourceContainer(DBSDataSourceContainer container)
    {
        if (container == dataSourceContainer) {
            return true;
        }
        // Acquire ds container
        if (dataSourceContainer != null) {
            dataSourceContainer.release(this);
            dataSourceContainer = null;
        }

        closeAllJobs();

        dataSourceContainer = container;
        IPathEditorInput input = getEditorInput();
        if (input == null) {
            return false;
        }
        IFile file = ContentUtils.convertPathToWorkspaceFile(input.getPath());
        if (file == null || !file.exists()) {
            log.warn("File '" + input.getPath() + "' doesn't exists");
            return false;
        }
        SQLEditorInput.setScriptDataSource(file, container, true);
        checkConnected();

        onDataSourceChange();

        if (container != null) {
            container.acquire(this);
        }
        return true;
    }

    @Override
    public boolean isDirty()
    {
        if (super.isDirty()) {
            return true;
        }
        for (DataContainer dataContainer : dataContainers) {
            if (dataContainer.viewer.isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getAdapter(Class required)
    {
/*
        // Return find/replace target of RSV only if it is in focus
        if (required == IFindReplaceTarget.class) {
            return
                UIUtils.hasFocus(resultsView.getControl()) ?
                    resultsView.getAdapter(required) :
                    super.getAdapter(required);
        }
*/
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
                DataSourceConnectHandler.execute(null, dataSourceContainer, null);
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
        UIUtils.setHelp(sashForm, IHelpContextIds.CTX_SQL_EDITOR);

        super.createPartControl(sashForm);

        editorControl = sashForm.getChildren()[0];

        selectionProvider = new CompositeSelectionProvider();
        getSite().setSelectionProvider(selectionProvider);

        createResultTabs();

        // Find/replace target activation
        getViewer().getTextWidget().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e)
            {
                findReplaceTarget.setTarget(getViewer().getFindReplaceTarget());
            }
        });
        // By default use editor's target
        findReplaceTarget.setTarget(getViewer().getFindReplaceTarget());

        // Check connection
        checkConnected();

        // Update controls
        onDataSourceChange();
    }

    private void createResultTabs()
    {
        resultTabs = new CTabFolder(sashForm, SWT.TOP | SWT.FLAT);
        resultTabs.setLayoutData(new GridData(GridData.FILL_BOTH));
        resultTabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Object data = e.item.getData();
                if (data instanceof DataContainer) {
                    curDataContainer = (DataContainer) data;
                    curDataContainer.viewer.getSpreadsheet().setFocus();
                }
            }
        });
        resultTabs.setSimple(true);
        //resultTabs.getItem(0).addListener();

        planView = new ExplainPlanViewer(this, resultTabs, this);
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
                                closeExtraResultTabs();
                            }
                        });
                    }
                }
            });
            menuMgr.setRemoveAllWhenShown(true);
            resultTabs.setMenu(menu);
        }

        // Create tabs
        createDataContainer(true);

        CTabItem item = new CTabItem(resultTabs, SWT.NONE);
        item.setControl(planView.getControl());
        item.setText(CoreMessages.editors_sql_explain_plan);
        item.setImage(imgExplainPlan);
        item.setData(planView);

        item = new CTabItem(resultTabs, SWT.NONE);
        item.setControl(logViewer);
        item.setText(CoreMessages.editors_sql_execution_log);
        item.setImage(imgLog);
        item.setData(logViewer);

        selectionProvider.trackProvider(getTextViewer().getTextWidget(), getTextViewer());
        selectionProvider.trackProvider(planView.getViewer().getControl(), planView.getViewer());
    }

    private void toggleEditorMaximize()
    {
        if (sashForm.getMaximizedControl() == null) {
            sashForm.setMaximizedControl(resultTabs);
        } else {
            sashForm.setMaximizedControl(null);
        }
    }

    private DataContainer createDataContainer(boolean setSelection)
    {
        boolean firstResultSet = dataContainers.isEmpty();
        final DataContainer dataContainer = new DataContainer();
        dataContainers.add(dataContainer);

        curDataContainer = dataContainer;

        final ResultSetViewer resultsView = new ResultSetViewer(resultTabs, getSite(), dataContainer);

        selectionProvider.trackProvider(resultsView.getSpreadsheet(), resultsView);

        // Find/replace target activation
        resultsView.getSpreadsheet().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e)
            {
                findReplaceTarget.setTarget(resultsView.getFindReplaceTarget());
            }
        });

        int tabIndex = dataContainers.size() - 1;
        final CTabItem item = new CTabItem(resultTabs, SWT.NONE, tabIndex);
        String tabName = CoreMessages.editors_sql_data_grid;
        if (!firstResultSet) {
            tabName += " [" + (getTabIndex(item) + 1) + "]";
        }
        item.setText(tabName);
        item.setImage(imgDataGrid);
        item.setData(dataContainer);
        if (!firstResultSet) {
            item.setShowClose(true);
        }
        item.setControl(resultsView.getControl());
        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                DataContainer dataContainer = (DataContainer) item.getData();
                if (dataContainer != null) {
                    dataContainers.remove(dataContainer);
                }
                if (dataContainer == curDataContainer) {
                    if (dataContainers.isEmpty()) {
                        curDataContainer = null;
                    } else {
                        curDataContainer = dataContainers.get(0);
                    }
                }
            }
        });

        if (setSelection) {
            resultTabs.setSelection(item);
        }

        dataContainer.init(item, resultsView);

        return dataContainer;
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

        // Acquire ds container
        final DBSDataSourceContainer dsContainer = getDataSourceContainer();
        if (dsContainer != null) {
            dsContainer.acquire(this);
        }
    }

    @Override
    protected void doSetInput(IEditorInput editorInput) throws CoreException
    {
        if (!(editorInput instanceof IPathEditorInput)) {
            throw new PartInitException("Invalid Input: Must be " + IPathEditorInput.class.getSimpleName());
        }
        IPathEditorInput input = (IPathEditorInput)editorInput;
        IFile file = ContentUtils.convertPathToWorkspaceFile(input.getPath());
        if (file == null || !file.exists()) {
            throw new PartInitException("File '" + input.getPath() + "' doesn't exists");
        }
        dataSourceContainer = SQLEditorInput.getScriptDataSource(file);

        super.doSetInput(input);
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event)
    {
/*
    	IPath path = getEditorInput().getPath();
    	if (path != null) {
	        final IResourceDelta delta = event.getDelta() == null ? null : event.getDelta().findMember(path);
	        if (delta != null) {
	            final int kind = delta.getKind();
	        }
    	}
*/
    }

    @Override
    public void setFocus()
    {
        super.setFocus();
    }

    public void explainQueryPlan()
    {
        final SQLStatementInfo sqlQuery = extractActiveQuery();
        if (sqlQuery == null) {
            setStatus(CoreMessages.editors_sql_status_empty_query_string, true);
            return;
        }
        for (CTabItem item : resultTabs.getItems()) {
            if (item.getData() == planView) {
                resultTabs.setSelection(item);
                break;
            }
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

    public void processSQL(boolean newTab, boolean script)
    {
        IDocument document = getDocument();
        if (document == null) {
            setStatus(CoreMessages.editors_sql_status_cant_obtain_document, true);
            return;
        }
        if (script) {
            // Execute all SQL statements consequently
            List<SQLStatementInfo> statementInfos;
            ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
            if (selection.getLength() > 1) {
                statementInfos = extractScriptQueries(selection.getOffset(), selection.getLength());
            } else {
                statementInfos = extractScriptQueries(0, document.getLength());
            }
            processQueries(statementInfos, newTab, false);
        } else {
            // Execute statement under cursor or selected text (if selection present)
            SQLStatementInfo sqlQuery = extractActiveQuery();
            if (sqlQuery == null) {
                setStatus(CoreMessages.editors_sql_status_empty_query_string, true);
            } else {
                processQueries(Collections.singletonList(sqlQuery), newTab, false);
            }
        }
    }

    public void exportDataFromQuery()
    {
        SQLStatementInfo sqlQuery = extractActiveQuery();
        if (sqlQuery != null) {
            processQueries(Collections.singletonList(sqlQuery), false, true);
        }
    }

    private void processQueries(final List<SQLStatementInfo> queries, final boolean newTab, final boolean export)
    {
        if (queries.isEmpty()) {
            // Nothing to process
            return;
        }
        try {
            checkSession();
        } catch (DBException ex) {
            getResultSetViewer().setStatus(ex.getMessage(), true);
            UIUtils.showErrorDialog(
                getSite().getShell(),
                CoreMessages.editors_sql_error_cant_obtain_session,
                ex.getMessage());
            return;
        }

        final boolean isSingleQuery = (queries.size() == 1);

        if (newTab && !isSingleQuery) {
            // If we execute a script with newTabs - close all previously opened ones
            closeExtraResultTabs();
        }

        if (newTab) {
            // Execute each query in a new tab
            for (int i = 0; i < queries.size(); i++) {
                SQLStatementInfo query = queries.get(i);
                DataContainer dataContainer = (i == 0 && !isSingleQuery ? curDataContainer : createDataContainer(queries.size() == 1));
                dataContainer.processQueries(Collections.singletonList(query), true, export);
            }
        } else {
            // Use current tab
            resultTabs.setSelection(curDataContainer.tabItem);
            curDataContainer.processQueries(queries, false, export);
        }
    }

    private List<SQLStatementInfo> extractScriptQueries(int startOffset, int length)
    {
        IDocument document = getDocument();
/*
        {
            Collection<? extends Position> selectedPositions = syntaxManager.getPositions(startOffset, length);
            for (Position position : selectedPositions) {
                try {
                    String query = document.get(position.getOffset(), position.getContentLength());
                    System.out.println(query);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
*/
        SQLSyntaxManager syntaxManager = getSyntaxManager();

        List<SQLStatementInfo> queryList = new ArrayList<SQLStatementInfo>();
        syntaxManager.setRange(document, startOffset, length);
        int statementStart = startOffset;
        boolean hasValuableTokens = false;
        for (;;) {
            IToken token = syntaxManager.nextToken();
            if (token.isEOF() || token instanceof SQLDelimiterToken) {
                int tokenOffset = syntaxManager.getTokenOffset();
                if (tokenOffset >= document.getLength()) {
                    tokenOffset = document.getLength();
                }
                try {
                    while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(statementStart))) {
                        statementStart++;
                    }
                    if (hasValuableTokens) {
                        int queryLength = tokenOffset - statementStart;
                        String query = document.get(statementStart, queryLength);
                        query = query.trim();
                        if (query.length() > 0) {
                            SQLStatementInfo statementInfo = new SQLStatementInfo(query);
                            statementInfo.setOffset(statementStart);
                            statementInfo.setLength(queryLength);
                            queryList.add(statementInfo);
                        }
                    }
                    hasValuableTokens = false;
                } catch (BadLocationException ex) {
                    log.error("Error extracting script query", ex); //$NON-NLS-1$
                }
                statementStart = tokenOffset + 1;
            }
            if (token.isEOF()) {
                break;
            }
            if (!token.isWhitespace() && !(token instanceof SQLCommentToken)) {
                hasValuableTokens = true;
            }
        }
        // Parse parameters
        for (SQLStatementInfo statementInfo : queryList) {
            statementInfo.parseParameters(getDocument(), getSyntaxManager());
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

    private void closeExtraResultTabs()
    {
        // Close all tabs except first one
        for (int i = resultTabs.getItemCount() - 1; i > 0; i--) {
            if (resultTabs.getItem(i).getData() instanceof DataContainer) {
                resultTabs.getItem(i).dispose();
            }
        }
    }

    private int getTabIndex(CTabItem tab)
    {
        CTabItem[] items = resultTabs.getItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i] == tab) {
                return i;
            }
        }
        log.warn("Bad tab: " + tab);
        return 0;
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
        if (sashForm == null || sashForm.isDisposed()) {
            return;
        }
        DatabaseEditorUtils.setPartBackground(this, sashForm);

        for (DataContainer dataContainer : dataContainers) {
            if (getDataSource() == null) {
                dataContainer.viewer.setStatus(CoreMessages.editors_sql_status_not_connected_to_database);
            } else {
                dataContainer.viewer.setStatus(CoreMessages.editors_sql_staus_connected_to + getDataSource().getContainer().getName() + "'"); //$NON-NLS-2$
            }
        }
        if (planView != null) {
            //resultsView.refresh();
            // Refresh plan view
            planView.refresh();
        }

        // Update command states
        SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);
        SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXPLAIN);

        reloadSyntaxRules();
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
/*
        IFile fileToDelete = null;
        // If it is close then delete it
        final IDocument document = getDocument();
        if (document != null) {
            if (document.get().trim().isEmpty()) {
                fileToDelete = ContentUtils.getFileFromEditorInput(getEditorInput());
            }
        }
*/

        // Acquire ds container
        final DBSDataSourceContainer dsContainer = getDataSourceContainer();
        if (dsContainer != null) {
            dsContainer.release(this);
        }

        closeAllJobs();

        IProject project = getProject();
        if (project != null) {
            final DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
            if (dataSourceRegistry != null) {
                dataSourceRegistry.removeDataSourceListener(this);
            }
        }

        planView = null;
        dataContainers.clear();
        curDataContainer = null;
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        super.dispose();

/*
        if (fileToDelete != null) {
            try {
                fileToDelete.delete(true, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Can't delete empty script file", e); //$NON-NLS-1$
            }
        }
*/
    }

    private void closeAllJobs()
    {
        for (DataContainer dataContainer : dataContainers) {
            dataContainer.closeJob();
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
        for (DataContainer dataContainer : dataContainers) {
            if (dataContainer.viewer.isDirty()) {
                dataContainer.viewer.doSave(progressMonitor);
            }
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
        for (DataContainer dataContainer : dataContainers) {
            jobsRunning += dataContainer.curJobRunning.get();
        }
        if (jobsRunning > 0) {
            MessageBox messageBox = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.OK);
            messageBox.setMessage(CoreMessages.editors_sql_save_on_close_message);
            messageBox.setText(CoreMessages.editors_sql_save_on_close_text);
            messageBox.open();
            return ISaveablePart2.CANCEL;
        }

        for (DataContainer dataContainer : dataContainers) {
            if (dataContainer.viewer.isDirty()) {
                return dataContainer.viewer.promptToSaveOnClose();
            }
        }
        return ISaveablePart2.YES;
    }

    @Override
    public ResultSetViewer getResultSetViewer()
    {
        return curDataContainer == null ? null : curDataContainer.viewer;
    }

    @Override
    public DBSDataContainer getDataContainer()
    {
        return curDataContainer;
    }

    @Override
    public boolean isReadyToRun()
    {
        return curDataContainer != null && curDataContainer.isReadyToRun();
    }

    private class DataContainer implements DBSDataContainer, ResultSetProvider {

        CTabItem tabItem;
        ResultSetViewer viewer;
        SQLQueryJob curJob;
        private AtomicInteger curJobRunning = new AtomicInteger(0);

        void init(CTabItem tabItem, ResultSetViewer viewer)
        {
            this.tabItem = tabItem;
            this.viewer = viewer;
        }

        @Override
        public int getSupportedFeatures()
        {
            int features = DATA_SELECT;

            final SQLQueryJob job = curJob;
            if (job != null && job.getLastResult() != null && job.getLastResult().hasResultSet()) {
                if (getDataSource().getInfo().supportsSubqueries()) {
                    features |= DATA_FILTER;
                }
            }
            return features;
        }

        @Override
        public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows) throws DBException
        {
            final SQLQueryJob job = curJob;
            if (job != null) {
                job.setResultSetLimit(firstRow, maxRows);
                job.setDataFilter(dataFilter);
                job.setDataReceiver(dataReceiver);
                return job.extractData(context);
            } else {
                log.warn("No active query - can't read data");
                return 0;
            }
        }

        @Override
        public long countData(DBCExecutionContext context, DBDDataFilter dataFilter) throws DBException
        {
            return -1;
        }

        @Override
        public String getDescription()
        {
            return CoreMessages.editors_sql_description;
        }

        @Override
        public DBSObject getParentObject()
        {
            return getDataSourceContainer();
        }

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

        @Override
        public String getName()
        {
            final SQLQueryJob job = curJob;
            String name = job == null ? null :
                job.getLastQuery() == null ? null : CommonUtils.truncateString(job.getLastQuery().getQuery(), 200);
            if (name == null) {
                name = "SQL";
            }
            return name;
        }

        private void closeJob()
        {
            final SQLQueryJob job = curJob;
            if (job != null) {
                job.cancel();
                curJob = null;
            }
        }

        void processQueries(final List<SQLStatementInfo> queries, final boolean fetchResults, boolean export)
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
            final boolean isSingleQuery = (queries.size() == 1);

            // Prepare execution job
            {
                final ITextSelection originalSelection = (ITextSelection) getSelectionProvider().getSelection();
                final SQLQueryJob job = new SQLQueryJob(
                    isSingleQuery ? CoreMessages.editors_sql_job_execute_query : CoreMessages.editors_sql_job_execute_script,
                    SQLEditor.this,
                    queries);
                job.setDataReceiver(viewer.getDataReceiver());
                job.addQueryListener(new ISQLQueryListener() {

                    private long lastUIUpdateTime = -1l;

                    @Override
                    public void onStartJob()
                    {
                        if (!isSingleQuery) {
                            UIUtils.runInUI(null, new Runnable() {
                                @Override
                                public void run()
                                {
                                    sashForm.setMaximizedControl(editorControl);
                                }
                            });
                        }
                    }

                    @Override
                    public void onStartQuery(final SQLStatementInfo query)
                    {
                        curJobRunning.incrementAndGet();
                        final long curTime = System.currentTimeMillis();
                        if (lastUIUpdateTime <= 0 || (curTime - lastUIUpdateTime >= SCRIPT_UI_UPDATE_PERIOD)) {
                            selectStatementInEditor(query);
                            lastUIUpdateTime = System.currentTimeMillis();
                        }
                    }

                    @Override
                    public void onEndQuery(final SQLQueryResult result)
                    {
                        curJobRunning.decrementAndGet();

                        if (isDisposed()) {
                            return;
                        }
                        if (result.hasError()) {
                            selectStatementInEditor(result.getStatement());
                        }
                        if (isSingleQuery) {
                            UIUtils.runInUI(null, new Runnable() {
                                @Override
                                public void run()
                                {
                                    processQueryResult(result);
                                }
                            });
                        }
                    }

                    private void selectStatementInEditor(final SQLStatementInfo query)
                    {
                        UIUtils.runInUI(null, new Runnable() {
                            @Override
                            public void run()
                            {
                                selectAndReveal(query.getOffset(), query.getLength());
                                setStatus(query.getQuery(), false);
                            }
                        });
                    }

                    private void processQueryResult(SQLQueryResult result)
                    {
                        if (result.hasError()) {
                            setStatus(result.getError().getMessage(), true);
                        }
                        if (queries.size() < 2) {
                            getSelectionProvider().setSelection(originalSelection);
                        }
                        if (!tabItem.isDisposed()) {
                            tabItem.setToolTipText(result.getStatement().getQuery());
                            if (!CommonUtils.isEmpty(result.getSourceEntity())) {
                                tabItem.setText(result.getSourceEntity());
                            } else {
                                int tabIndex = getTabIndex(tabItem);
                                tabItem.setText(
                                    CoreMessages.editors_sql_data_grid + (tabIndex == 0 ? "" : " [" + (tabIndex + 1) + "]"));
                            }
                        }

                        if (result.getQueryTime() > DBeaverCore.getGlobalPreferenceStore().getLong(PrefConstants.AGENT_LONG_OPERATION_TIMEOUT) * 1000) {
                            DBeaverUI.notifyAgent(
                                "Query completed [" + getEditorInput().getPath().lastSegment() + "]" + ContentUtils.getDefaultLineSeparator() +
                                    CommonUtils.truncateString(result.getStatement().getQuery(), 200), !result.hasError() ? IStatus.INFO : IStatus.ERROR);
                        }
                    }

                    @Override
                    public void onEndJob(final boolean hasErrors)
                    {
                        if (isDisposed()) {
                            return;
                        }
                        UIUtils.runInUI(null, new Runnable() {
                            @Override
                            public void run()
                            {
                                if (!hasErrors && queries.size() > 1) {
                                    getSelectionProvider().setSelection(originalSelection);
                                }
                                if (!isSingleQuery) {
                                    sashForm.setMaximizedControl(null);
                                }
                            }
                        });
                    }
                });

                if (export) {
                    // Assign current job from active query and open wizard
                    curJob = job;
                    ActiveWizardDialog dialog = new ActiveWizardDialog(
                        getSite().getWorkbenchWindow(),
                        new DataTransferWizard(
                            new IDataTransferProducer[] {
                                new DatabaseTransferProducer(getDataContainer(), null)},
                            null),
                        new StructuredSelection(this));
                    dialog.open();
                } else if (isSingleQuery) {
                    closeJob();
                    curJob = job;
                    viewer.setDataFilter(new DBDDataFilter(), false);
                    viewer.refresh();
                } else {
                    if (fetchResults) {
                        job.setFetchResultSets(true);
                    }
                    job.schedule();
                }
            }
        }

        @Override
        public ResultSetViewer getResultSetViewer()
        {
            return viewer;
        }

        @Override
        public DBSDataContainer getDataContainer()
        {
            return this;
        }

        @Override
        public boolean isReadyToRun()
        {
            return curJob != null && curJobRunning.get() == 0;
        }
    }

}
