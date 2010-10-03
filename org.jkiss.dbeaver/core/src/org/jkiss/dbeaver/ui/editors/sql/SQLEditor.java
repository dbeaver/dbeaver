/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;


import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;
import org.jkiss.dbeaver.runtime.sql.SQLQueryJob;
import org.jkiss.dbeaver.runtime.sql.SQLQueryResult;
import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceConnectHandler;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogViewer;
import org.jkiss.dbeaver.ui.editors.sql.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLDelimiterToken;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLSymbolInserter;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * SQL Executor
 */
public class SQLEditor extends BaseTextEditor
    implements
        IResourceChangeListener, IDataSourceProvider, IDataSourceContainerProvider, DBPEventListener, ISaveablePart2, ResultSetProvider
{
    static final Log log = LogFactory.getLog(SQLEditor.class);

    static final int PAGE_INDEX_RESULTSET = 0;
    static final int PAGE_INDEX_PLAN = 1;
    static final int PAGE_INDEX_LOG = 2;

    static final String ACTION_CONTENT_ASSIST_PROPOSAL = "ContentAssistProposal";
    static final String ACTION_CONTENT_ASSIST_TIP = "ContentAssistTip";
    static final String ACTION_CONTENT_FORMAT_PROPOSAL = "ContentFormatProposal";
    //private static final String ACTION_DEFINE_FOLDING_REGION = "DefineFoldingRegion";

    private SashForm sashForm;
    private Control editorControl;
    private CTabFolder resultTabs;
    private ResultSetViewer resultsView;

    private ExplainPlanViewer planView;
    private SQLLogViewer logViewer;
    private SQLSyntaxManager syntaxManager;

    private ProjectionSupport projectionSupport;

    private ProjectionAnnotationModel annotationModel;
    private Map<Annotation, Position> curAnnotations;

    private SQLSymbolInserter symbolInserter = null;

    private SQLQueryJob curJob;
    private boolean curJobRunning;
    private DataContainer dataContainer;

    private static Image imgDataGrid;
    private static Image imgExplainPlan;
    private static Image imgLog;
    private IAnnotationAccess annotationAccess;

    static {
        imgDataGrid = DBeaverActivator.getImageDescriptor("/icons/sql/page_data_grid.png").createImage();
        imgExplainPlan = DBeaverActivator.getImageDescriptor("/icons/sql/page_explain_plan.png").createImage();
        imgLog = DBeaverActivator.getImageDescriptor("/icons/sql/page_error.png").createImage();
    }

    public SQLEditor()
    {
        super();
        setDocumentProvider(new SQLDocumentProvider());
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        DataSourceRegistry.getDefault().addDataSourceListener(this);
        dataContainer = new DataContainer();

        setSourceViewerConfiguration(new SQLEditorSourceViewerConfiguration(this));
    }

    public DBPDataSource getDataSource()
    {
        return getDataSourceContainer().getDataSource();
    }

    public DBSDataSourceContainer getDataSourceContainer()
    {
        return getEditorInput().getDataSourceContainer();
    }

    public void setDataSourceContainer(DBSDataSourceContainer container)
    {
        if (container == getDataSourceContainer()) {
            return;
        }
        closeSession();
        getEditorInput().setDataSourceContainer(container);
        checkConnected();

        onDataSourceChange();
    }

    public ISourceViewer getSV() {
        return getSourceViewer();
    }

    public SQLSyntaxManager getSyntaxManager()
    {
        return syntaxManager;
    }

    public ResultSetViewer getResultsView()
    {
        return resultsView;
    }

    public IDocument getDocument()
    {
        IDocumentProvider provider = getDocumentProvider();
        return provider == null ? null : provider.getDocument(getEditorInput());
    }

    public ProjectionAnnotationModel getAnnotationModel()
    {
        return annotationModel;
    }

    public boolean isDirty()
    {
        return super.isDirty();
    }

    public void close(boolean save)
    {
        super.close(save);
    }

    private boolean checkConnected()
    {
        // Connect to datasource
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            if (!dataSourceContainer.isConnected()) {
                DataSourceConnectHandler.execute(dataSourceContainer);
            }
        }
        setPartName(getEditorInput().getName());
        return dataSourceContainer != null && dataSourceContainer.isConnected();
    }

    public void createPartControl(Composite parent)
    {
        syntaxManager = new SQLSyntaxManager(this);
        setRangeIndicator(new DefaultRangeIndicator());

        sashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
        sashForm.setSashWidth(10);

        super.createPartControl(sashForm);

        editorControl = sashForm.getChildren()[0];

        {
            resultTabs = new CTabFolder(sashForm, SWT.TOP | SWT.FLAT);
            resultTabs.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultTabs.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    resultTabs.indexOf((CTabItem) e.item);
                    //pageChange(newPageIndex);
                }
            });
            resultTabs.setSimple(true);

            resultsView = new ResultSetViewer(resultTabs, getSite(), this);

            planView = new ExplainPlanViewer(this, resultTabs);
            logViewer = new SQLLogViewer(resultTabs);

            // Create tabs
            CTabItem item = new CTabItem(resultTabs, SWT.NONE, 0);
            item.setControl(resultsView.getControl());
            item.setText("Data Grid");
            item.setImage(imgDataGrid);

            item = new CTabItem(resultTabs, SWT.NONE, 1);
            item.setControl(planView.getControl());
            item.setText("Explain Plan");
            item.setImage(imgExplainPlan);

            item = new CTabItem(resultTabs, SWT.NONE, 2);
            item.setControl(logViewer.getControl());
            item.setText("Execute Log");
            item.setImage(imgLog);

            resultTabs.setSelection(0);
        }

        ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
        projectionSupport = new ProjectionSupport(
            viewer,
            getAnnotationAccess(),
            getSharedColors());
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
        projectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
        projectionSupport.install();

        viewer.doOperation(ProjectionViewer.TOGGLE);

        annotationModel = viewer.getProjectionAnnotationModel();

        // Symbol inserter
        {
            symbolInserter = new SQLSymbolInserter(this);

            IPreferenceStore preferenceStore = DBeaverCore.getInstance().getGlobalPreferenceStore();
            boolean closeSingleQuotes = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
            boolean closeDoubleQuotes = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
            boolean closeBrackets = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);

            symbolInserter.setCloseSingleQuotesEnabled(closeSingleQuotes);
            symbolInserter.setCloseDoubleQuotesEnabled(closeDoubleQuotes);
            symbolInserter.setCloseBracketsEnabled(closeBrackets);

            ISourceViewer sourceViewer = getSourceViewer();
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(symbolInserter);
            }
        }

        // Check connection
        checkConnected();

        // Update controls
        onDataSourceChange();
    }

    private ISharedTextColors getSharedColors()
    {
        return DBeaverCore.getInstance().getSharedTextColors();
    }

    protected void createActions()
    {
        super.createActions();

        ResourceBundle bundle = DBeaverCore.getInstance().getPlugin().getResourceBundle();

        IAction a = new TextOperationAction(bundle, "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS);
        a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction(ACTION_CONTENT_ASSIST_PROPOSAL, a);

        a = new TextOperationAction(bundle, "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
        a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
        setAction(ACTION_CONTENT_ASSIST_TIP, a);

        a = new TextOperationAction(bundle, "ContentFormatProposal.", this, ISourceViewer.FORMAT);
        setAction(ACTION_CONTENT_FORMAT_PROPOSAL, a);

/*
        // Add the task action to the Edit pulldown menu (bookmark action is  'free')
        ResourceAction ra = new AddTaskAction(bundle, "AddTask.", this);
        ra.setHelpContextId(ITextEditorHelpContextIds.ADD_TASK_ACTION);
        ra.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
        setAction(IDEActionFactory.ADD_TASK.getId(), ra);
*/
    }

    public void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);

        menu.add(new Separator("content"));
        addAction(menu, ACTION_CONTENT_ASSIST_PROPOSAL);
        addAction(menu, ACTION_CONTENT_ASSIST_TIP);
        addAction(menu, ACTION_CONTENT_FORMAT_PROPOSAL);
        //addAction(menu, ACTION_DEFINE_FOLDING_REGION);
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    protected ISourceViewer createSourceViewer(Composite parent,
        IVerticalRuler ruler, int styles)
    {
        OverviewRuler overviewRuler = new OverviewRuler(
            getAnnotationAccess(),
            VERTICAL_RULER_WIDTH,
            getSharedColors());

        return new SQLEditorSourceViewer(
            parent,
            ruler,
            overviewRuler,
            true,
            styles);
    }

    private IAnnotationAccess getAnnotationAccess()
    {
        if (annotationAccess == null) {
            annotationAccess = new SQLMarkerAnnotationAccess();
        }
        return annotationAccess;
    }

/*
    protected void adjustHighlightRange(int offset, int length)
    {
        ISourceViewer viewer = getSourceViewer();
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            extension.exposeModelRange(new Region(offset, length));
        }
    }
*/

    public Object getAdapter(Class required)
    {
        if (projectionSupport != null) {
            Object adapter = projectionSupport.getAdapter(
                getSourceViewer(), required);
            if (adapter != null)
                return adapter;
        }

        return super.getAdapter(required);
    }

    public SQLEditorInput getEditorInput()
    {
        return (SQLEditorInput) super.getEditorInput();
    }

    public void init(IEditorSite site, IEditorInput editorInput)
        throws PartInitException
    {
        if (!(editorInput instanceof SQLEditorInput)) {
            throw new PartInitException("Invalid Input: Must be SQLEditorInput");
        }
        super.init(site, editorInput);
    }

    public void resourceChanged(final IResourceChangeEvent event)
    {
    }

    public void explainQueryPlan()
    {
        final SQLStatementInfo sqlQuery = extractActiveQuery();
        if (sqlQuery == null) {
            setStatus("Empty query string", true);
            return;
        }
        resultTabs.setSelection(PAGE_INDEX_PLAN);
        try {
            planView.explainQueryPlan(sqlQuery.getQuery());
        } catch (DBCException e) {
            DBeaverUtils.showErrorDialog(
                sashForm.getShell(),
                "Execution plan",
                "Could not explain execution plan",
                e);
        }
    }

    public void processSQL(boolean script)
    {
        IDocument document = getDocument();
        if (document == null) {
            setStatus("Can't obtain editor's document", true);
            return;
        }
        resultTabs.setSelection(PAGE_INDEX_RESULTSET);
        if (script) {
            // Execute all SQL statements consequently
            List<SQLStatementInfo> statementInfos;
            ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
            if (selection.getLength() > 1) {
                statementInfos = extractScriptQueries(selection.getOffset(), selection.getLength());
            } else {
                statementInfos = extractScriptQueries(0, document.getLength());
            }
            processQuery(statementInfos);
        } else {
            // Execute statement under cursor or selected text (if selection present)
            SQLStatementInfo sqlQuery = extractActiveQuery();
            if (sqlQuery == null) {
                setStatus("Empty query string", true);
            } else {
                processQuery(Collections.singletonList(sqlQuery));
            }
        }
    }

    private SQLStatementInfo extractActiveQuery()
    {
        SQLStatementInfo sqlQuery;
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        String selText = selection.getText().trim();
        selText = selText.trim();
        if (selText.endsWith(syntaxManager.getStatementDelimiter())) {
            selText = selText.substring(0, selText.length() - syntaxManager.getStatementDelimiter().length());
        }
        if (!CommonUtils.isEmpty(selText)) {
            sqlQuery = new SQLStatementInfo(selText);
            sqlQuery.setOffset(selection.getOffset());
            sqlQuery.setLength(selection.getLength());
        } else {
            sqlQuery = extractQueryAtPos(selection.getOffset());
        }
        // Check query do not ends with delimiter
        // (this may occur if user selected statement including delimiter)
        if (sqlQuery == null || CommonUtils.isEmpty(sqlQuery.getQuery())) {
            return null;
        }
        return sqlQuery;
    }

    private static boolean isEmptyLine(IDocument document, int line)
        throws BadLocationException
    {
        IRegion region = document.getLineInformation(line);
        if (region == null || region.getLength() == 0) {
            return true;
        }
        String str = document.get(region.getOffset(), region.getLength());
        return str.trim().length() == 0;
    }

    public SQLStatementInfo extractQueryAtPos(int currentPos)
    {
        IDocument document = getDocument();
        if (document.getLength() == 0) {
            return null;
        }
        // Extract part of document between empty lines
        int startPos = 0;
        int endPos = document.getLength();
        try {
            int currentLine = document.getLineOfOffset(currentPos);
            int lineOffset = document.getLineOffset(currentLine);
            int linesCount = document.getNumberOfLines();
            int firstLine = currentLine, lastLine = currentLine;
            while (firstLine > 0) {
                if (isEmptyLine(document, firstLine)) {
                    break;
                }
                firstLine--;
            }
            while (lastLine < linesCount) {
                if (isEmptyLine(document, lastLine)) {
                    break;
                }
                lastLine++;
            }
            if (lastLine >= linesCount) {
                lastLine = linesCount - 1;
            }
            startPos = document.getLineOffset(firstLine);
            endPos = document.getLineOffset(lastLine) + document.getLineLength(lastLine);
            //String lastDelimiter = document.getLineDelimiter(lastLine);
            //if (lastDelimiter != null) {
            //    endPos += lastDelimiter.length();
            //}

            // Move currentPos at line begin
            currentPos = lineOffset;
        }
        catch (BadLocationException e) {
            log.warn(e);
        }

        // Parse range
        syntaxManager.setRange(document, startPos, endPos - startPos);
        int statementStart = startPos;
        for (;;) {
            IToken token = syntaxManager.nextToken();
            int tokenOffset = syntaxManager.getTokenOffset();
            if (token.isEOF() ||
                (token instanceof SQLDelimiterToken && tokenOffset >= currentPos)||
                tokenOffset > endPos) 
            {
                // get position before last token start
                if (tokenOffset > endPos) {
                    tokenOffset = endPos;
                }

                if (tokenOffset >= document.getLength()) {
                    // Sometimes (e.g. when comment finishing script text)
                    // last token offset is beyon document range
                    tokenOffset = document.getLength();
                }
                assert (tokenOffset >= currentPos);
                try {
                    // remove leading spaces
                    while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(statementStart))) {
                        statementStart++;
                    }
                    // remove trailing spaces
                    while (statementStart < tokenOffset && Character.isWhitespace(document.getChar(tokenOffset - 1))) {
                        tokenOffset--;
                    }
                    String queryText = document.get(statementStart, tokenOffset - statementStart);
                    queryText = queryText.trim();
                    if (queryText.endsWith(syntaxManager.getStatementDelimiter())) {
                        queryText = queryText.substring(0, queryText.length() - syntaxManager.getStatementDelimiter().length()); 
                    }
                    // make script line
                    SQLStatementInfo statementInfo = new SQLStatementInfo(queryText.trim());
                    statementInfo.setOffset(statementStart);
                    statementInfo.setLength(tokenOffset - statementStart);
                    return statementInfo;
                } catch (BadLocationException ex) {
                    log.warn("Can't extract query", ex);
                    return null;
                }
            }
            if (token instanceof SQLDelimiterToken) {
                statementStart = tokenOffset + syntaxManager.getTokenLength();
            }
            if (token.isEOF()) {
                return null;
            }
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

        List<SQLStatementInfo> queryList = new ArrayList<SQLStatementInfo>();
        syntaxManager.setRange(document, startOffset, length);
        int statementStart = startOffset;
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
                    int queryLength = tokenOffset - statementStart;
                    String query = document.get(statementStart, queryLength);
                    query = query.trim();
                    if (query.length() > 0) {
                        SQLStatementInfo statementInfo = new SQLStatementInfo(query);
                        statementInfo.setOffset(statementStart);
                        statementInfo.setLength(queryLength);
                        queryList.add(statementInfo);
                    }
                } catch (BadLocationException ex) {
                    log.error("Error extracting script query", ex);
                }
                statementStart = tokenOffset + 1;
            }
            if (token.isEOF()) {
                break;
            }
        }
        return queryList;
    }

    private void setStatus(String status, boolean error)
    {
        resultsView.setStatus(status, error);
    }

    private void processQuery(final List<SQLStatementInfo> queries)
    {
        if (queries.isEmpty()) {
            // Nothing to process
            return;
        }
        if (curJobRunning) {
            DBeaverUtils.showErrorDialog(
                getSite().getShell(),
                "Can't execute query",
                "Can't execute more than one query in one editor simultaneously");
            return;
        }
        try {
            checkSession();
        } catch (DBException ex) {
            this.setStatus(ex.getMessage(), true);
            DBeaverUtils.showErrorDialog(
                getSite().getShell(),
                "Can't obtain session",
                ex.getMessage());
            return;
        }

        // Prepare execution job
        {
            final ITextSelection originalSelection = (ITextSelection) getSelectionProvider().getSelection();
            final boolean isSingleQuery = (queries.size() == 1);
            final SQLQueryJob job = new SQLQueryJob(
                isSingleQuery ? "Execute query" : "Execute script",
                getDataSource(),
                queries,
                resultsView.getDataReceiver());
            job.addQueryListener(new ISQLQueryListener() {
                public void onStartJob()
                {
                    curJobRunning = true;
                    if (!isSingleQuery) {
                        asyncExec(new Runnable() {
                            public void run()
                            {
                                sashForm.setMaximizedControl(editorControl);
                            }
                        });
                    }
                }
                public void onStartQuery(final SQLStatementInfo query)
                {
                    asyncExec(new Runnable() {
                        public void run()
                        {
                            selectAndReveal(query.getOffset(), query.getLength());
                            setStatus(query.getQuery(), false);
                        }
                    });
                }

                public void onEndQuery(final SQLQueryResult result)
                {
                    if (isDisposed()) {
                        return;
                    }
                    asyncExec(new Runnable() {
                        public void run()
                        {
                            if (result.getError() == null) {
                                if (result.getRowCount() != null) {
                                    // No status message for selected rows - this info is set by RS viewer itself
/*
                                    status = result.getRowCount() + " row(s) fetched";
                                    if (result.getRowOffset() != null) {
                                        status += " (" + result.getRowOffset() + " - " + (result.getRowOffset() + result.getRowCount()) + ")";
                                    }
*/
                                } else if (result.getUpdateCount() != null) {
                                    if (result.getUpdateCount() == 0) {
                                        setStatus("No rows updated", false);
                                    } else {
                                        setStatus(String.valueOf(result.getUpdateCount()) + " row(s) updated", false);
                                    }
                                } else {
                                    setStatus("Statement executed", false);
                                }
                                resultsView.setExecutionTime(result.getQueryTime());
                            } else {
                                setStatus(result.getError().getMessage(), true);
                            }
                            if (queries.size() < 2) {
                                getSelectionProvider().setSelection(originalSelection);
                            }
                        }
                    });
                }
                public void onEndJob(final boolean hasErrors)
                {
                    curJobRunning = false;

                    if (isDisposed()) {
                        return;
                    }
                    asyncExec(new Runnable() {
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
            if (isSingleQuery) {
                curJob = job;
                resultsView.refresh();
            } else {
                job.schedule();
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

    private void closeSession()
    {
        if (curJob != null) {
            curJob = null;
        }
    }

    private void onDataSourceChange()
    {
        if (getDataSource() == null) {
            resultsView.setStatus("Not connected to database");
        } else {
            resultsView.setStatus("Connected to '" + getDataSource().getContainer().getName() + "'");
        }
        // Refresh plan view
        planView.refresh();

        // Update command states
        SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);
        SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXPLAIN);


        // Refresh syntax
        if (syntaxManager != null) {
            syntaxManager.changeDataSource(getDataSource());
        }
        ProjectionViewer projectionViewer = (ProjectionViewer)getSourceViewer();
        IDocument document = getDocument();
        if (projectionViewer != null && document != null && document.getLength() > 0) {
            // Refresh viewer
            //projectionViewer.getTextWidget().redraw();
            try {
                projectionViewer.reinitializeProjection();
            } catch (BadLocationException ex) {
                log.warn("Error refreshing projection", ex);
            }
        }
    }

    public void dispose()
    {
        closeSession();

        DataSourceRegistry.getDefault().removeDataSourceListener(this);
        if (syntaxManager != null) {
            syntaxManager.dispose();
            syntaxManager = null;
        }
        if (planView != null) {
            planView.dispose();
            planView = null;
        }
        if (resultsView != null) {
            resultsView.dispose();
            resultsView = null;
        }
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        super.dispose();
    }

    public void handleDataSourceEvent(final DBPEvent event)
    {
        if (event.getObject() == getDataSourceContainer()) {
            getSite().getShell().getDisplay().asyncExec(
                new Runnable() {
                    public void run() {
                        switch (event.getAction()) {
                            case OBJECT_UPDATE:
                                if (event.getEnabled() != null) {
                                    if (!event.getEnabled()) {
                                        closeSession();
                                    }
                                }
                                break;
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

    public synchronized void updateFoldingStructure(int offset, int length, List<Position> positions)
    {
        if (curAnnotations == null) {
            curAnnotations = new HashMap<Annotation, Position>();
        }
        List<Annotation> deletedAnnotations = new ArrayList<Annotation>();
        Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();

        // Delete all annotations if specified range
        for (Map.Entry<Annotation,Position> entry : curAnnotations.entrySet()) {
            int entryOffset = entry.getValue().getOffset();
            if (entryOffset >= offset && entryOffset < offset + length) {
                deletedAnnotations.add(entry.getKey());
            }
        }
        for (Annotation annotation : deletedAnnotations) {
            curAnnotations.remove(annotation);
        }

        // Add new annotations
        for (Position position : positions) {
            ProjectionAnnotation annotation = new ProjectionAnnotation();
            newAnnotations.put(annotation, position);
        }

        // Modify annotation set
        annotationModel.modifyAnnotations(
            deletedAnnotations.toArray(new Annotation[deletedAnnotations.size()]),
            newAnnotations,
            null);

        // Update current annotations
        curAnnotations.putAll(newAnnotations);
    }

    private void asyncExec(Runnable runnable)
    {
        getSite().getShell().getDisplay().asyncExec(runnable);
    }

    public boolean isDisposed()
    {
        return
            getSourceViewer() == null ||
            getSourceViewer().getTextWidget() == null ||
            getSourceViewer().getTextWidget().isDisposed();
    }

    public int promptToSaveOnClose()
    {
        if (curJobRunning) {
            MessageBox messageBox = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.OK);
            messageBox.setMessage("Editor can't be closed while SQL query is being executed");
            messageBox.setText("Query is being executed");
            messageBox.open();
            return ISaveablePart2.CANCEL;
        }
        return ISaveablePart2.YES;
    }

    public void loadFromExternalFile()
    {
        FileDialog fileDialog = new FileDialog(getSite().getShell(), SWT.OPEN);
        fileDialog.setFilterExtensions(new String[] { "*.sql", "*.txt", "*.*"});
        String fileName = fileDialog.open();
        if (CommonUtils.isEmpty(fileName)) {
            return;
        }
        final File loadFile = new File(fileName);
        if (!loadFile.exists()) {
            MessageBox aMessageBox = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.OK);
            aMessageBox.setText("File doesn't exists");
            aMessageBox.setMessage("The file "+ loadFile.getAbsolutePath() + " doesn't exists.");
            aMessageBox.open();
            return;
        }

        String newContent = null;
        try {
            Reader reader = new InputStreamReader(
                new FileInputStream(loadFile),
                ContentUtils.DEFAULT_FILE_CHARSET);
            try {
                StringWriter buffer = new StringWriter();
                IOUtils.copyText(reader, buffer, 10000);
                newContent = buffer.toString();
            }
            finally {
                reader.close();
            }
        }
        catch (IOException e) {
            DBeaverUtils.showErrorDialog(
                getSite().getShell(),
                "Can't load file",
                "Can't load file '" + loadFile.getAbsolutePath() + "' - " + e.getMessage());
        }
        if (newContent != null) {
            getDocument().set(newContent);
        }
    }

    public void saveToExternalFile()
    {
        FileDialog fileDialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        fileDialog.setFilterExtensions(new String[] { "*.sql", "*.txt", "*.*"});
        fileDialog.setOverwrite(true);
        String fileName = fileDialog.open();
        if (CommonUtils.isEmpty(fileName)) {
            return;
        }
        final File saveFile = new File(fileName);

        DBeaverCore.getInstance().runAndWait(new DBRRunnableWithProgress() {
            public void run(final DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                getSite().getShell().getDisplay().syncExec(new Runnable() {
                    public void run()
                    {
                        doSave(monitor.getNestedMonitor());
                    }
                });

                try {
                    ContentUtils.saveContentToFile(getEditorInput().getFile().getContents(), saveFile, monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }

    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    public boolean isReadyToRun()
    {
        return curJob != null && !curJobRunning;
    }

    private class DataContainer implements DBSDataContainer {

        public int getSupportedFeatures()
        {
            return 0;
        }

        public long readData(DBCExecutionContext context, DBDDataReceiver dataReceiver, long firstRow, long maxRows) throws DBException
        {
            if (curJob != null) {
                curJob.setDataReciever(dataReceiver);
                curJob.setResultSetLimit(firstRow, maxRows);
                return curJob.extractData(context);
            } else {
                return 0;
            }
        }

        public long readDataCount(DBCExecutionContext context) throws DBException
        {
            throw new DBException("Not Implemented");
        }

        public long insertData(DBCExecutionContext context, List<DBDColumnValue> columns, DBDDataReceiver keysReceiver) throws DBException
        {
            throw new DBException("Not Implemented");
        }

        public long updateData(DBCExecutionContext context, List<DBDColumnValue> keyColumns, List<DBDColumnValue> updateColumns, DBDDataReceiver keysReceiver) throws DBException
        {
            throw new DBException("Not Implemented");
        }

        public long deleteData(DBCExecutionContext context, List<DBDColumnValue> keyColumns) throws DBException
        {
            throw new DBException("Not Implemented");
        }

        public String getObjectId()
        {
            return getName();
        }

        public String getDescription()
        {
            return "SQL Editor";
        }

        public DBSObject getParentObject()
        {
            return getDataSourceContainer();
        }

        public DBPDataSource getDataSource()
        {
            return SQLEditor.this.getDataSource();
        }

        public String getName()
        {
            return curJob == null ? null :
                curJob.getLastQuery() == null ? null : curJob.getLastQuery().getQuery();
        }
    }

}
