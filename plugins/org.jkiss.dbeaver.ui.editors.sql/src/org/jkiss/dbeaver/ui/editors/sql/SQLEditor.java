/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.impl.DefaultServerOutputReader;
import org.jkiss.dbeaver.model.impl.sql.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.sql.SQLResultsConsumer;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.*;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.INonPersistentEditorInput;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.execute.SQLQueryJob;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogPanel;
import org.jkiss.dbeaver.ui.editors.sql.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationPanelDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationRegistry;
import org.jkiss.dbeaver.ui.editors.text.ScriptPositionColumn;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Executor
 */
public class SQLEditor extends SQLEditorBase implements
    IDataSourceContainerProviderEx,
    DBPEventListener,
    ISaveablePart2,
    DBPDataSourceTask,
    DBPDataSourceHandler,
    ISmartTransactionManager
{
    private static final long SCRIPT_UI_UPDATE_PERIOD = 100;
    private static final int MAX_PARALLEL_QUERIES_NO_WARN = 1;

    private static final int SQL_EDITOR_CONTROL_INDEX = 1;
    private static final int EXTRA_CONTROL_INDEX = 0;

    private static final String PANEL_ITEM_PREFIX = "SQLPanelToggle:";

    private static final String EMBEDDED_BINDING_PREFIX = "-- CONNECTION: ";
    private static final Pattern EMBEDDED_BINDING_PREFIX_PATTERN = Pattern.compile("--\\s*CONNECTION:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private static Image IMG_DATA_GRID = DBeaverIcons.getImage(UIIcon.SQL_PAGE_DATA_GRID);
    private static Image IMG_DATA_GRID_LOCKED = DBeaverIcons.getImage(UIIcon.SQL_PAGE_DATA_GRID_LOCKED);
    private static Image IMG_EXPLAIN_PLAN = DBeaverIcons.getImage(UIIcon.SQL_PAGE_EXPLAIN_PLAN);
    private static Image IMG_LOG = DBeaverIcons.getImage(UIIcon.SQL_PAGE_LOG);
    private static Image IMG_OUTPUT = DBeaverIcons.getImage(UIIcon.SQL_PAGE_OUTPUT);
    private static Image IMG_OUTPUT_ALERT = DBeaverIcons.getImage(UIIcon.SQL_PAGE_OUTPUT_ALERT);

    private static final String TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.editors.sql.toolbar.side";
    private static final String TOOLBAR_GROUP_TOP = "top";
    private static final String TOOLBAR_GROUP_ADDITIONS = IWorkbenchActionConstants.MB_ADDITIONS;
    private static final String TOOLBAR_GROUP_PANELS = "panelToggles";

    public static final String VAR_CONNECTION_NAME = "connectionName";
    public static final String VAR_FILE_NAME = "fileName";
    public static final String VAR_FILE_EXT = "fileExt";
    public static final String VAR_DRIVER_NAME = "driverName";

    public static final String DEFAULT_TITLE_PATTERN = "<${" + VAR_CONNECTION_NAME + "}> ${" + VAR_FILE_NAME + "}";

    private ResultSetOrientation resultSetOrientation = ResultSetOrientation.HORIZONTAL;
    private CustomSashForm resultsSash;
    private Composite sqlEditorPanel;
    @Nullable
    private CustomSashForm presentationSash;
    private CTabFolder resultTabs;
    private CTabItem activeResultsTab;

    private SQLLogPanel logViewer;
    private SQLEditorOutputConsoleViewer outputViewer;

    private volatile QueryProcessor curQueryProcessor;
    private final List<QueryProcessor> queryProcessors = new ArrayList<>();

    private DBPDataSourceContainer dataSourceContainer;
    private DBPDataSource curDataSource;
    private volatile DBCExecutionContext executionContext;
    private volatile DBCExecutionContext lastExecutionContext;
    private SQLScriptContext globalScriptContext;
    private volatile boolean syntaxLoaded = false;
    private final FindReplaceTarget findReplaceTarget = new FindReplaceTarget();
    private final List<SQLQuery> runningQueries = new ArrayList<>();
    private QueryResultsContainer curResultsContainer;
    private Image editorImage;
    private VerticalFolder sideToolBar;

    private SQLPresentationDescriptor extraPresentationDescriptor;
    private SQLEditorPresentation extraPresentation;
    private Map<SQLPresentationPanelDescriptor, SQLEditorPresentationPanel> extraPresentationPanels = new HashMap<>();
    private SQLEditorPresentationPanel extraPresentationCurrentPanel;
    private VerticalFolder presentationSwitchFolder;

    private final List<SQLEditorListener> listeners = new ArrayList<>();

    private DisposeListener resultTabDisposeListener = new DisposeListener() {
        @Override
        public void widgetDisposed(DisposeEvent e) {
            if (resultTabs.getItemCount() == 0) {
                if (resultsSash.getMaximizedControl() == null) {
                    // Hide results
                    toggleResultPanel();
                }
            }
        }
    };
    private VerticalButton switchPresentationSQLButton;
    private VerticalButton switchPresentationExtraButton;

    public SQLEditor()
    {
        super();

        this.extraPresentationDescriptor = SQLPresentationRegistry.getInstance().getPresentation(this);
    }

    @Override
    protected String[] getKeyBindingContexts() {
        return new String[]{TEXT_EDITOR_CONTEXT, SQLEditorContributions.SQL_EDITOR_CONTEXT, SQLEditorContributions.SQL_EDITOR_SCRIPT_CONTEXT};
    }

    @Override
    public DBPDataSource getDataSource() {
        DBPDataSourceContainer container = getDataSourceContainer();
        return container == null ? null : container.getDataSource();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        if (executionContext != null) {
            return executionContext;
        }
        if (dataSourceContainer != null && !SQLEditorUtils.isOpenSeparateConnection(dataSourceContainer)) {
            return DBUtils.getDefaultContext(getDataSource(), false);
        }
        return null;
    }

    @Nullable
    public DBPProject getProject()
    {
        IFile file = EditorUtils.getFileFromInput(getEditorInput());
        return file == null ?
            DBWorkbench.getPlatform().getWorkspace().getActiveProject() : DBWorkbench.getPlatform().getWorkspace().getProject(file.getProject());
    }

    @Nullable
    @Override
    public int[] getCurrentLines()
    {
        synchronized (runningQueries) {
            IDocument document = getDocument();
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
            DBPDataSourceContainer savedContainer = EditorUtils.getInputDataSource(input);
            if (savedContainer != container) {
                EditorUtils.setInputDataSource(input, new SQLNavigatorContext(container, getExecutionContext()));
            }
            IFile file = EditorUtils.getFileFromInput(input);
            if (file != null) {
                DBNUtils.refreshNavigatorResource(file, container);
            } else {
                // FIXME: this is a hack. We can't fire event on resource change so editor's state won't be updated in UI.
                // FIXME: To update main toolbar and other controls we hade and show this editor
                IWorkbenchPage page = getSite().getPage();
                for (IEditorReference er : page.getEditorReferences()) {
                    if (er.getEditor(false) == this) {
                        page.hideEditor(er);
                        page.showEditor(er);
                        break;
                    }
                }
                //page.activate(this);
            }
        }

        checkConnected(false, status -> UIUtils.asyncExec(() -> {
            if (!status.isOK()) {
                DBWorkbench.getPlatformUI().showError("Can't connect to database", "Error connecting to datasource", status);
            }
            setFocus();
        }));
        setPartName(getEditorName());

        fireDataSourceChange();

        if (dataSourceContainer != null) {
            dataSourceContainer.acquire(this);
        }

        if (SQLEditorBase.isWriteEmbeddedBinding()) {
            // Patch connection reference
            UIUtils.syncExec(this::embedDataSourceAssociation);
        }

        return true;
    }

    private void updateDataSourceContainer() {
        DBPDataSourceContainer inputDataSource = null;
        if (SQLEditorBase.isReadEmbeddedBinding()) {
            // Try to get datasource from contents (always, no matter what )
            inputDataSource = getDataSourceFromContent();
        }
        if (inputDataSource == null) {
            inputDataSource = EditorUtils.getInputDataSource(getEditorInput());
        }
        if (inputDataSource == null) {
            // No datasource. Try to get one from active part
            IWorkbenchPart activePart = getSite().getWorkbenchWindow().getActivePage().getActivePart();
            if (activePart != this && activePart instanceof IDataSourceContainerProvider) {
                inputDataSource = ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
            }
        }
        setDataSourceContainer(inputDataSource);
    }

    private void updateExecutionContext(Runnable onSuccess) {
        if (dataSourceContainer == null) {
            releaseExecutionContext();
        } else {
            // Get/open context
            final DBPDataSource dataSource = dataSourceContainer.getDataSource();
            if (dataSource == null) {
                releaseExecutionContext();
            } else if (curDataSource != dataSource) {
                // Datasource was changed or instance was changed (PG)
                releaseExecutionContext();
                curDataSource = dataSource;
                DBPDataSourceContainer container = dataSource.getContainer();
                if (SQLEditorUtils.isOpenSeparateConnection(container)) {
                    initSeparateConnection(dataSource, onSuccess);
                } else {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }
            }
        }
    }

    private void initSeparateConnection(@NotNull DBPDataSource dataSource, Runnable onSuccess) {
        DBSInstance dsInstance = dataSource.getDefaultInstance();
        String[] contextDefaults = isRestoreActiveSchemaFromScript() ?
            EditorUtils.getInputContextDefaults(dataSource.getContainer(), getEditorInput()) : null;
        if (!ArrayUtils.isEmpty(contextDefaults) && contextDefaults[0] != null) {
            DBSInstance selectedInstance = DBUtils.findObject(dataSource.getAvailableInstances(), contextDefaults[0]);
            if (selectedInstance != null) {
                dsInstance = selectedInstance;
            }
        }
        if (dsInstance != null) {
            final OpenContextJob job = new OpenContextJob(dsInstance, onSuccess);
            job.schedule();
        }
    }

    private void releaseExecutionContext() {
        if (executionContext != null && executionContext.isConnected()) {
            // Close context in separate job (otherwise it can block UI)
            new CloseContextJob(executionContext).schedule();
        }
        executionContext = null;
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

    private DBPDataSourceContainer getDataSourceFromContent() {

        DBPProject project = getProject();
        IDocument document = getDocument();
        if (document == null || document.getNumberOfLines() == 0) {
            return null;
        }

        try {
            IRegion region = document.getLineInformation(0);
            String line = document.get(region.getOffset(), region.getLength());
            Matcher matcher = EMBEDDED_BINDING_PREFIX_PATTERN.matcher(line);
            if (matcher.matches()) {
                String connSpec = matcher.group(1).trim();
                if (!CommonUtils.isEmpty(connSpec)) {
                    final DBPDataSourceContainer dataSource = DataSourceUtils.getDataSourceBySpec(project, connSpec, null, true, false);
                    if (dataSource != null) {
                        return dataSource;
                    }
                }
            }

        } catch (Throwable e) {
            log.debug("Error extracting datasource info from script's content", e);
        }

        return null;
    }

    private void embedDataSourceAssociation() {
        if (getDataSourceFromContent() == dataSourceContainer) {
            return;
        }
        IDocument document = getDocument();

        try {

            int totalLines = document.getNumberOfLines();
            IRegion region = null;
            if (totalLines > 0) {
                region = document.getLineInformation(0);
                String line = document.get(region.getOffset(), region.getLength());
                Matcher matcher = EMBEDDED_BINDING_PREFIX_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    // Update existing association
                    region = null;
                }
            }

            if (dataSourceContainer == null) {
                if (region == null) {
                    return;
                }
                // Remove connection association
                document.replace(region.getOffset(), region.getLength(), "");
            } else {
                SQLScriptBindingType bindingType = SQLScriptBindingType.valueOf(DBWorkbench.getPlatform().getPreferenceStore().getString(SQLPreferenceConstants.SCRIPT_BIND_COMMENT_TYPE));

                StringBuilder assocSpecLine = new StringBuilder(EMBEDDED_BINDING_PREFIX);
                bindingType.appendSpec(dataSourceContainer, assocSpecLine);

                if (region != null) {
                    // Remove connection association
                    document.replace(region.getOffset(), region.getLength(), assocSpecLine.toString());
                } else {
                    document.replace(0, 0, assocSpecLine.toString());
                }
            }

        } catch (Throwable e) {
            log.debug("Error extracting datasource info from script's content", e);
        }

        UIUtils.asyncExec(() -> getTextViewer().refresh());
    }

    public void addListener(SQLEditorListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(SQLEditorListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public boolean isActiveTask() {
        return getTotalQueryRunning() > 0;
    }

    @Override
    public boolean isSmartAutoCommit() {
        return getActivePreferenceStore().getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT);
    }

    @Override
    public void setSmartAutoCommit(boolean smartAutoCommit) {
        getActivePreferenceStore().setValue(ModelPreferences.TRANSACTIONS_SMART_COMMIT, smartAutoCommit);
        try {
            getActivePreferenceStore().save();
        } catch (IOException e) {
            log.error("Error saving smart auto-commit option", e);
        }
    }

    public void refreshActions() {
        // Redraw toolbar to refresh action sets
        sideToolBar.redraw();
    }

    private class OutputLogWriter extends Writer {
        @Override
        public void write(@NotNull final char[] cbuf, final int off, final int len) {
            UIUtils.syncExec(() -> {
                if (!outputViewer.isDisposed()) {
                    outputViewer.getOutputWriter().write(cbuf, off, len);
                    outputViewer.scrollToEnd();
                    if (!outputViewer.isVisible()) {
                        updateOutputViewerIcon(true);
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
        private final DBSInstance instance;
        private final Runnable onSuccess;
        private Throwable error;
        OpenContextJob(DBSInstance instance, Runnable onSuccess) {
            super("Open connection to " + instance.getDataSource().getContainer().getName());
            this.instance = instance;
            this.onSuccess = onSuccess;
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            monitor.beginTask("Open SQLEditor isolated connection", 1);
            try {
                String title = "SQLEditor <" + getEditorInput().getName() + ">";
                monitor.subTask("Open context " + title);
                DBCExecutionContext newContext = instance.openIsolatedContext(monitor, title, instance.getDefaultContext(monitor, false));
                // Set context defaults
                String[] contextDefaultNames = isRestoreActiveSchemaFromScript() ?
                    EditorUtils.getInputContextDefaults(instance.getDataSource().getContainer(), getEditorInput()) : null;
                if (contextDefaultNames != null && contextDefaultNames.length > 1 &&
                    (!CommonUtils.isEmpty(contextDefaultNames[0]) || !CommonUtils.isEmpty(contextDefaultNames[1])))
                {
                    try {
                        DBExecUtils.setExecutionContextDefaults(monitor, newContext.getDataSource(), newContext, contextDefaultNames[0], null, contextDefaultNames[1]);
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError("New connection default", "Error setting default catalog/schema for new connection", e);
                    }
                }
                SQLEditor.this.executionContext = newContext;
                // Needed to update main toolbar
                DBUtils.fireObjectSelect(instance, true);
            } catch (DBException e) {
                error = e;
            } finally {
                monitor.done();
            }
            updateContext();
            return Status.OK_STATUS;
        }

        private void updateContext() {
            if (error != null) {
                releaseExecutionContext();
                DBWorkbench.getPlatformUI().showError("Open context", "Can't open editor connection", error);
            } else {
                if (onSuccess != null) {
                    onSuccess.run();
                }
                fireDataSourceChange();
            }
        }
    }

    private boolean isRestoreActiveSchemaFromScript() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA) &&
            getActivePreferenceStore().getBoolean(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION);
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
                    UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
                    if (serviceConnections != null) {
                        serviceConnections.closeActiveTransaction(monitor, context, false);
                    }
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
            if (queryProcessor.isDirty() || queryProcessor.curJobRunning.get() > 0) {
                return true;
            }
        }
        if (executionContext != null && QMUtils.isTransactionActive(executionContext)) {
            return true;
        }
        if (isNonPersistentEditor()) {
            // Console is never dirty
            return false;
        }
        if (extraPresentation instanceof ISaveablePart && ((ISaveablePart) extraPresentation).isDirty()) {
            return true;
        }
        return super.isDirty();
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> required)
    {
        if (required == INavigatorModelView.class) {
            return null;
        }

        if (resultTabs != null && !resultTabs.isDisposed()) {
            if (required == IFindReplaceTarget.class) {
                return required.cast(findReplaceTarget);
            }
            CTabItem activeResultsTab = getActiveResultsTab();
            if (activeResultsTab != null && UIUtils.isUIThread()) {
                Object tabControl = activeResultsTab.getData();
                if (tabControl instanceof QueryResultsContainer) {
                    tabControl = ((QueryResultsContainer) tabControl).viewer;
                }
                if (tabControl instanceof IAdaptable) {
                    T adapter = ((IAdaptable) tabControl).getAdapter(required);
                    if (adapter != null) {
                        return adapter;
                    }
                }
                if (tabControl instanceof ResultSetViewer && (required == IResultSetController.class || required == ResultSetViewer.class)) {
                    return required.cast(tabControl);
                }
            }
        }

        return super.getAdapter(required);
    }

    private boolean checkConnected(boolean forceConnect, DBRProgressListener onFinish)
    {
        // Connect to datasource
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        boolean doConnect = dataSourceContainer != null &&
            (forceConnect || dataSourceContainer.getPreferenceStore().getBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE));
        if (doConnect) {
            if (!dataSourceContainer.isConnected()) {
                UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
                if (serviceConnections != null) {
                    serviceConnections.connectDataSource(dataSourceContainer, onFinish);
                }
            }
        }
        return dataSourceContainer != null && dataSourceContainer.isConnected();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        setRangeIndicator(new DefaultRangeIndicator());

        // divides editor area and results/panels area
        resultsSash = UIUtils.createPartDivider(
                this,
                parent,
                resultSetOrientation.getSashOrientation() | SWT.SMOOTH);
        CSSUtils.setCSSClass(resultsSash, DBStyles.COLORED_BY_CONNECTION_TYPE);
        resultsSash.setSashWidth(5);

        UIUtils.setHelp(resultsSash, IHelpContextIds.CTX_SQL_EDITOR);

        Composite editorContainer;
        sqlEditorPanel = UIUtils.createPlaceholder(resultsSash, 3, 0);

        // Create left vertical toolbar
        createControlsBar(sqlEditorPanel);

        // Create editor presentations sash
        Composite pPlaceholder = null;
        if (extraPresentationDescriptor != null) {
            presentationSash = UIUtils.createPartDivider(
                    this,
                sqlEditorPanel,
                    ((resultSetOrientation.getSashOrientation() == SWT.VERTICAL) ? SWT.HORIZONTAL : SWT.VERTICAL) | SWT.SMOOTH);
            presentationSash.setSashWidth(5);
            presentationSash.setLayoutData(new GridData(GridData.FILL_BOTH));
            editorContainer = presentationSash;

            pPlaceholder = new Composite(presentationSash, SWT.NONE);
            pPlaceholder.setLayout(new FillLayout());
        } else {
            editorContainer = sqlEditorPanel;
        }

        super.createPartControl(editorContainer);
        getEditorControlWrapper().setLayoutData(new GridData(GridData.FILL_BOTH));

        // Create right vertical toolbar
        createPresentationSwitchBar(sqlEditorPanel);

        if (pPlaceholder != null) {
            switch (extraPresentationDescriptor.getActivationType()) {
                case HIDDEN:
                    presentationSash.setMaximizedControl(presentationSash.getChildren()[SQL_EDITOR_CONTROL_INDEX]);
                    break;
                case MAXIMIZED:
                case VISIBLE:
                    extraPresentation.createPresentation(pPlaceholder, this);
                    if (extraPresentationDescriptor.getActivationType() == SQLEditorPresentation.ActivationType.MAXIMIZED) {
                        if (presentationSash.getChildren()[EXTRA_CONTROL_INDEX] != null) {
                            presentationSash.setMaximizedControl(pPlaceholder);
                        }
                    }
                    break;
            }
        }

        getSite().setSelectionProvider(new DynamicSelectionProvider());

        DBPProject project = getProject();
        if (project != null && project.isRegistryLoaded()) {
            createResultTabs();
        } else {
            UIExecutionQueue.queueExec(this::createResultTabs);
        }

        setAction(ITextEditorActionConstants.SHOW_INFORMATION, null);
        //toolTipAction.setEnabled(false);

/*
        resultsSash.setSashBorders(new boolean[]{true, true});
        if (presentationSash != null) {
            presentationSash.setSashBorders(new boolean[]{true, true});
        }
*/

        SQLEditorFeatures.SQL_EDITOR_OPEN.use();

        // Start output reader
        new ServerOutputReader().schedule();

        updateExecutionContext(null);

        // Update controls
        UIExecutionQueue.queueExec(this::onDataSourceChange);
    }

    private void createControlsBar(Composite sqlEditorPanel) {

        sideToolBar = new VerticalFolder(sqlEditorPanel, SWT.LEFT);
        ((GridLayout)sideToolBar.getLayout()).marginTop = 3;
        ((GridLayout)sideToolBar.getLayout()).marginBottom = 10;
        ((GridLayout)sideToolBar.getLayout()).verticalSpacing = 3;
        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.PUSH, getSite(), SQLEditorCommands.CMD_EXECUTE_STATEMENT, false);
        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.PUSH, getSite(), SQLEditorCommands.CMD_EXECUTE_STATEMENT_NEW, false);
        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.PUSH, getSite(), SQLEditorCommands.CMD_EXECUTE_SCRIPT, false);
        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.PUSH, getSite(), SQLEditorCommands.CMD_EXECUTE_SCRIPT_NEW, false);
        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.PUSH, getSite(), SQLEditorCommands.CMD_EXPLAIN_PLAN, false);

        UIUtils.createEmptyLabel(sideToolBar, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));


        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.CHECK, new ShowPreferencesAction(), false);

        Label label = new Label(sideToolBar, SWT.NONE);
        label.setImage(DBeaverIcons.getImage(UIIcon.SEPARATOR_H));

        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.CHECK, getSite(), SQLEditorCommands.CMD_SQL_SHOW_OUTPUT, false);
        VerticalButton.create(sideToolBar, SWT.LEFT | SWT.CHECK, getSite(), SQLEditorCommands.CMD_SQL_SHOW_LOG, false);

/*
        sideToolBar.add(new GroupMarker(TOOLBAR_GROUP_PANELS));
        final IMenuService menuService = getSite().getService(IMenuService.class);
        if (menuService != null) {
            int prevSize = sideToolBar.getSize();
            menuService.populateContributionManager(sideToolBar, TOOLBAR_CONTRIBUTION_ID);
            if (prevSize != sideToolBar.getSize()) {
                // Something was populated
                sideToolBar.insertBefore(TOOLBAR_GROUP_ADDITIONS, new ToolbarSeparatorContribution(false));
            }
        }
*/

        sideToolBar.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING));
    }

    private void createPresentationSwitchBar(Composite sqlEditorPanel) {
        if (extraPresentationDescriptor == null) {
            return;
        }

        presentationSwitchFolder = new VerticalFolder(sqlEditorPanel, SWT.RIGHT);
        presentationSwitchFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        switchPresentationSQLButton = new VerticalButton(presentationSwitchFolder, SWT.RIGHT | SWT.CHECK);
        switchPresentationSQLButton.setText(SQLEditorMessages.editors_sql_description);
        switchPresentationSQLButton.setImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT));

        switchPresentationExtraButton = new VerticalButton(presentationSwitchFolder, SWT.RIGHT | SWT.CHECK);
        switchPresentationExtraButton.setData(extraPresentationDescriptor);
        switchPresentationExtraButton.setText(extraPresentationDescriptor.getLabel());
        switchPresentationExtraButton.setImage(DBeaverIcons.getImage(extraPresentationDescriptor.getIcon()));
        String toolTip = ActionUtils.findCommandDescription(extraPresentationDescriptor.getToggleCommandId(), getSite(), false);
        if (CommonUtils.isEmpty(toolTip)) {
            toolTip = extraPresentationDescriptor.getDescription();
        }
        if (!CommonUtils.isEmpty(toolTip)) {
            switchPresentationExtraButton.setToolTipText(toolTip);
        }

        switchPresentationSQLButton.setChecked(true);

        // We use single switch handler. It must be provided by presentation itself
        // Presentation switch may require some additional action so we can't just switch visible controls
        SelectionListener switchListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (((VerticalButton)e.item).isChecked() || presentationSwitchFolder.getSelection() == e.item) {
                    return;
                }
                String toggleCommandId = extraPresentationDescriptor.getToggleCommandId();
                ActionUtils.runCommand(toggleCommandId, getSite());
            }
        };
        switchPresentationSQLButton.addSelectionListener(switchListener);
        switchPresentationExtraButton.addSelectionListener(switchListener);

        // Stretch
        UIUtils.createEmptyLabel(presentationSwitchFolder, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));
        VerticalButton.create(presentationSwitchFolder, SWT.RIGHT | SWT.CHECK, getSite(), SQLEditorCommands.CMD_TOGGLE_LAYOUT, false);

    }

    /**
     * Sets focus in current editor.
     * This function is called on drag-n-drop and some other operations
     */
    @Override
    public boolean validateEditorInputState() {
        boolean res = super.validateEditorInputState();
        if (res) {
            StyledText textWidget = getViewer().getTextWidget();
            if (textWidget != null && !textWidget.isDisposed()) {
                textWidget.setFocus();
            }
        }
        return res;
    }

    private void createResultTabs()
    {
        resultTabs = new CTabFolder(resultsSash, SWT.TOP | SWT.FLAT);
        CSSUtils.setCSSClass(resultTabs, DBStyles.COLORED_BY_CONNECTION_TYPE);
        new TabFolderReorder(resultTabs);
        resultTabs.setLayoutData(new GridData(GridData.FILL_BOTH));
        resultTabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (extraPresentationCurrentPanel != null) {
                    extraPresentationCurrentPanel.deactivatePanel();
                }
                extraPresentationCurrentPanel = null;
                Object data = e.item.getData();
                if (data instanceof QueryResultsContainer) {
                    setActiveResultsContainer((QueryResultsContainer) data);
                } else if (data instanceof SQLEditorPresentationPanel) {
                    extraPresentationCurrentPanel = ((SQLEditorPresentationPanel) data);
                    extraPresentationCurrentPanel.activatePanel();
                } else if (data instanceof ExplainPlanViewer) {
                    SQLQuery planQuery = ((ExplainPlanViewer) data).getQuery();
                    if (planQuery != null) {
                        getSelectionProvider().setSelection(new TextSelection(planQuery.getOffset(), 0));
                    }
                }
            }
        });
        this.resultTabs.addListener(SWT.Resize, event -> {
            if (!resultsSash.isDisposed()) {
                int[] weights = resultsSash.getWeights();
                IPreferenceStore prefs = getPreferenceStore();
                if (prefs != null && weights.length == 2) {
                    prefs.setValue(SQLPreferenceConstants.RESULTS_PANEL_RATIO, weights[0] + "-" + weights[1]);
                }
            }
        });
        String resultsPanelRatio = getPreferenceStore().getString(SQLPreferenceConstants.RESULTS_PANEL_RATIO);
        if (!CommonUtils.isEmpty(resultsPanelRatio)) {
            String[] weights = resultsPanelRatio.split("-");
            if (weights.length > 1) {
                resultsSash.setWeights(new int[] {
                    Integer.parseInt(weights[0]),
                    Integer.parseInt(weights[1]),
                });
            }
        }


        getTextViewer().getTextWidget().addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_PAGE_NEXT) {
                ResultSetViewer viewer = getActiveResultSetViewer();
                if (viewer != null && viewer.getActivePresentation().getControl().isVisible()) {
                    viewer.getActivePresentation().getControl().setFocus();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });
        resultTabs.setSimple(true);

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
        resultTabs.addListener(SWT.MouseDoubleClick, event -> {
            if (event.button != 1) {
                return;
            }
            CTabItem selectedItem = resultTabs.getItem(new Point(event.getBounds().x, event.getBounds().y));
            if (selectedItem != null && selectedItem  == resultTabs.getSelection()) {
                toggleEditorMaximize();
            }
        });

        // Extra views
        //planView = new ExplainPlanViewer(this, resultTabs);
        logViewer = new SQLLogPanel(resultTabs, this);
        outputViewer = new SQLEditorOutputConsoleViewer(getSite(), resultTabs, SWT.NONE);

        // Create results tab
        createQueryProcessor(true, true);

        {
            resultTabs.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(MouseEvent e) {
                    activeResultsTab = resultTabs.getItem(new Point(e.x, e.y));
                }
            });
            MenuManager menuMgr = new MenuManager();
            Menu menu = menuMgr.createContextMenu(resultTabs);
            menuMgr.addMenuListener(manager -> {
                manager.add(ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_SQL_EDITOR_MAXIMIZE_PANEL));
                if (resultTabs.getItemCount() > 1) {
                    manager.add(new Action("Close multiple results") {
                        @Override
                        public void run()
                        {
                            closeExtraResultTabs(null, false);
                        }
                    });
                    int pinnedTabsCount = 0;
                    for (CTabItem item : resultTabs.getItems()) {
                        if (item.getData() instanceof QueryResultsContainer) {
                            if (((QueryResultsContainer) item.getData()).isPinned()) {
                                pinnedTabsCount++;
                            }
                        }
                    }
                    if (pinnedTabsCount > 1) {
                        manager.add(new Action("Unpin all tabs") {
                            @Override
                            public void run()
                            {
                                for (CTabItem item : resultTabs.getItems()) {
                                    if (item.getData() instanceof QueryResultsContainer) {
                                        if (((QueryResultsContainer) item.getData()).isPinned()) {
                                            ((QueryResultsContainer) item.getData()).setPinned(false);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
                final CTabItem activeTab = getActiveResultsTab();
                if (activeTab != null && activeTab.getData() instanceof QueryResultsContainer) {
                    {
                        final QueryResultsContainer resultsContainer = (QueryResultsContainer) activeTab.getData();
                        if (resultsContainer.getResultSetController().hasData()) {
                            manager.add(new Separator());
                            final boolean isPinned = resultsContainer.isPinned();
                            manager.add(new Action(isPinned ? "Unpin tab" : "Pin tab") {
                                @Override
                                public void run() {
                                    resultsContainer.setPinned(!isPinned);
                                }
                            });
                        }
                    }
                    manager.add(new Action("Set tab title") {
                        @Override
                        public void run()
                        {
                            EnterNameDialog dialog = new EnterNameDialog(resultTabs.getShell(), "Tab title", activeTab.getText());
                            if (dialog.open() == IDialogConstants.OK_ID) {
                                if (activeTab.getData() instanceof QueryResultsContainer) {
                                    final QueryResultsContainer resultsContainer = (QueryResultsContainer) activeTab.getData();
                                    resultsContainer.setTabName(dialog.getResult());
                                }
                            }
                        }
                    });
                }
                if (activeTab != null && activeTab.getShowClose()) {
                    manager.add(ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_SQL_EDITOR_CLOSE_TAB));
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

    /////////////////////////////////////////////////////////////
    // Panels

    private void showExtraView(final String commandId, String name, String toolTip, Image image, Control view) {
        VerticalButton viewItem = getViewToolItem(commandId);
        if (viewItem == null) {
            log.warn("Tool item for command " + commandId + " not found");
            return;
        }
        for (CTabItem item : resultTabs.getItems()) {
            if (item.getData() == view) {
                // Close tab if it is already open
                viewItem.setChecked(false);
                viewItem.redraw();
                item.dispose();
                return;
            }
        }

        if (view == outputViewer.getControl()) {
            updateOutputViewerIcon(false);
            outputViewer.resetNewOutput();
        }
        // Create new tab
        viewItem.setChecked(true);

        CTabItem item = new CTabItem(resultTabs, SWT.CLOSE);
        item.setControl(view);
        item.setText(name);
        item.setToolTipText(toolTip);
        item.setImage(image);
        item.setData(view);
        // De-select tool item on tab close
        item.addDisposeListener(e -> {
            if (!viewItem.isDisposed()) {
                viewItem.setChecked(false);
                viewItem.redraw();
            }
            resultTabDisposeListener.widgetDisposed(e);
        });
        resultTabs.setSelection(item);
        viewItem.redraw();
    }

    private VerticalButton getViewToolItem(String commandId) {
        VerticalButton viewItem = null;
        for (VerticalButton item : sideToolBar.getItems()) {
            if (commandId.equals(item.getCommandId())) {
                viewItem = item;
                break;
            }
        }
        return viewItem;
    }

    private CTabItem getActiveResultsTab() {
        return activeResultsTab == null || activeResultsTab.isDisposed() ?
            (resultTabs == null ? null : resultTabs.getSelection()) : activeResultsTab;
    }

    public void closeActiveTab() {
        CTabItem tabItem = getActiveResultsTab();
        if (tabItem != null && tabItem.getShowClose()) {
            tabItem.dispose();
            activeResultsTab = null;
        }
    }

    public void showOutputPanel() {
        if (resultsSash.getMaximizedControl() != null) {
            resultsSash.setMaximizedControl(null);
        }
        showExtraView(SQLEditorCommands.CMD_SQL_SHOW_OUTPUT, SQLEditorMessages.editors_sql_output, SQLEditorMessages.editors_sql_output_tip, IMG_OUTPUT, outputViewer.getControl());
    }

    public void showExecutionLogPanel() {
        if (resultsSash.getMaximizedControl() != null) {
            resultsSash.setMaximizedControl(null);
        }
        showExtraView(SQLEditorCommands.CMD_SQL_SHOW_LOG, SQLEditorMessages.editors_sql_execution_log, SQLEditorMessages.editors_sql_execution_log_tip, IMG_LOG, logViewer);
    }

    public <T> T getExtraPresentationPanel(Class<T> panelClass) {
        for (CTabItem tabItem : resultTabs.getItems()) {
            if (tabItem.getData() instanceof SQLEditorPresentationPanel && tabItem.getData().getClass() == panelClass) {
                return panelClass.cast(tabItem.getData());
            }
        }
        return null;
    }

    public boolean showPresentationPanel(SQLEditorPresentationPanel panel) {
        for (CTabItem item : resultTabs.getItems()) {
            if (item.getData() == panel) {
                resultTabs.setSelection(item);
                return true;
            }
        }
        return false;
    }

    public SQLEditorPresentationPanel showPresentationPanel(String panelID) {
        for (VerticalButton cItem : sideToolBar.getItems()) {
            IAction action = cItem.getAction();
            if (action != null) {
                if (action instanceof PresentationPanelToggleAction && ((PresentationPanelToggleAction) action).panel.getId().equals(panelID)) {
                    action.run();
                    return extraPresentationCurrentPanel;
                }
            }
        }
        return null;
    }

    public boolean hasMaximizedControl() {
        return resultsSash.getMaximizedControl() != null;
    }

    public SQLEditorPresentation getExtraPresentation() {
        return extraPresentation;
    }

    public SQLEditorPresentation.ActivationType getExtraPresentationState() {
        if (extraPresentation == null) {
            return SQLEditorPresentation.ActivationType.HIDDEN;
        }
        Control maximizedControl = presentationSash.getMaximizedControl();
        if (maximizedControl == getExtraPresentationControl()) {
            return SQLEditorPresentation.ActivationType.MAXIMIZED;
        } else if (maximizedControl == getEditorControlWrapper()) {
            return SQLEditorPresentation.ActivationType.HIDDEN;
        } else {
            return SQLEditorPresentation.ActivationType.VISIBLE;
        }
    }

    public void showExtraPresentation(boolean show, boolean maximize) {
        if (extraPresentationDescriptor == null) {
            return;
        }
        resultsSash.setRedraw(false);
        try {
            if (!show) {
                //boolean epHasFocus = UIUtils.hasFocus(getExtraPresentationControl());
                presentationSash.setMaximizedControl(presentationSash.getChildren()[SQL_EDITOR_CONTROL_INDEX]);
                //if (epHasFocus) {
                    getEditorControlWrapper().setFocus();
                //}
            } else {
                if (extraPresentation == null) {
                    // Lazy activation
                    try {
                        extraPresentation = extraPresentationDescriptor.createPresentation();
                        extraPresentation.createPresentation((Composite) getExtraPresentationControl(), this);
                    } catch (DBException e) {
                        log.error("Error creating presentation", e);
                    }
                }
                if (maximize) {
                    presentationSash.setMaximizedControl(getExtraPresentationControl());
                    getExtraPresentationControl().setFocus();
                } else {
                    presentationSash.setMaximizedControl(null);
                }
            }

            // Show presentation panels
            boolean sideBarChanged = false;
            if (getExtraPresentationState() == SQLEditorPresentation.ActivationType.HIDDEN) {
                // Remove all presentation panel toggles
                for (SQLPresentationPanelDescriptor panelDescriptor : extraPresentationDescriptor.getPanels()) {
                    for (Control vb : presentationSwitchFolder.getChildren()) {
                        if (vb instanceof Label || vb.getData() instanceof SQLPresentationPanelDescriptor) {
                            vb.dispose();
                            sideBarChanged = true;
                        }
                    }
                }
                // Close all panels
                for (CTabItem tabItem : resultTabs.getItems()) {
                    if (tabItem.getData() instanceof SQLEditorPresentationPanel) {
                        tabItem.dispose();
                    }
                }
                extraPresentationCurrentPanel = null;
            } else {
                // Check and add presentation panel toggles
                UIUtils.createEmptyLabel(presentationSwitchFolder, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));
                for (SQLPresentationPanelDescriptor panelDescriptor : extraPresentationDescriptor.getPanels()) {
                    sideBarChanged = true;
                    PresentationPanelToggleAction toggleAction = new PresentationPanelToggleAction(panelDescriptor);
                    VerticalButton panelButton = new VerticalButton(presentationSwitchFolder, SWT.RIGHT);
                    panelButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
                    panelButton.setAction(toggleAction, true);
                    panelButton.setData(panelDescriptor);
                    if (panelDescriptor.isAutoActivate()) {
                        //panelButton.setChecked(true);
                        toggleAction.run();
                    }
                }
            }

            boolean isExtra = getExtraPresentationState() == SQLEditorPresentation.ActivationType.MAXIMIZED;
            switchPresentationSQLButton.setChecked(!isExtra);
            switchPresentationExtraButton.setChecked(isExtra);
            presentationSwitchFolder.redraw();

            if (sideBarChanged) {
                sideToolBar.getParent().layout(true, true);
            }
        } finally {
            resultsSash.setRedraw(true);
        }
    }

    private Control getExtraPresentationControl() {
        return presentationSash.getChildren()[EXTRA_CONTROL_INDEX];
    }

    public void toggleResultPanel() {
        if (resultsSash.getMaximizedControl() == null) {
            resultsSash.setMaximizedControl(sqlEditorPanel);
            switchFocus(false);
        } else {
            // Show both editor and results
            // Check for existing query processors (maybe all result tabs were closed)
            if (resultTabs.getItemCount() == 0) {
                createQueryProcessor(true, true);
            }

            resultsSash.setMaximizedControl(null);
            switchFocus(true);
        }
    }

    public void toggleEditorMaximize()
    {
        if (resultsSash.getMaximizedControl() == null) {
            resultsSash.setMaximizedControl(resultTabs);
            switchFocus(true);
        } else {
            resultsSash.setMaximizedControl(null);
            switchFocus(false);
        }
    }

    private void switchFocus(boolean results) {
        if (results) {
            ResultSetViewer activeRS = getActiveResultSetViewer();
            if (activeRS != null && activeRS.getActivePresentation() != null) {
                activeRS.getActivePresentation().getControl().setFocus();
            } else {
                CTabItem activeTab = resultTabs.getSelection();
                if (activeTab != null && activeTab.getControl() != null) {
                    activeTab.getControl().setFocus();
                }
            }
        } else {
            getEditorControlWrapper().setFocus();
        }
    }

    public void toggleActivePanel() {
        if (resultsSash.getMaximizedControl() == null) {
            if (UIUtils.hasFocus(resultTabs)) {
                switchFocus(false);
            } else {
                switchFocus(true);
            }
        }
    }

    private void updateResultSetOrientation() {
        try {
            resultSetOrientation = ResultSetOrientation.valueOf(DBWorkbench.getPlatform().getPreferenceStore().getString(SQLPreferenceConstants.RESULT_SET_ORIENTATION));
        } catch (IllegalArgumentException e) {
            resultSetOrientation = ResultSetOrientation.HORIZONTAL;
        }
        if (resultsSash != null) {
            resultsSash.setOrientation(resultSetOrientation.getSashOrientation());
        }
    }

    private class PresentationPanelToggleAction extends Action {
        private SQLPresentationPanelDescriptor panel;
        private CTabItem tabItem;

        public PresentationPanelToggleAction(SQLPresentationPanelDescriptor panel) {
            super(panel.getLabel(), Action.AS_CHECK_BOX);
            setId(PANEL_ITEM_PREFIX + panel.getId());
            if (panel.getIcon() != null) {
                setImageDescriptor(DBeaverIcons.getImageDescriptor(panel.getIcon()));
            }
            if (panel.getDescription() != null) {
                setToolTipText(panel.getDescription());
            }
            this.panel = panel;
        }

        @Override
        public void run() {
            setChecked(!isChecked());
            SQLEditorPresentationPanel panelInstance = extraPresentationPanels.get(panel);
            if (panelInstance != null && !isChecked()) {
                // Hide panel
                for (CTabItem tabItem : resultTabs.getItems()) {
                    if (tabItem.getData() == panelInstance) {
                        tabItem.dispose();
                        return;
                    }
                }
            }
            if (panelInstance == null) {
                Control panelControl;
                try {
                    panelInstance = panel.createPanel();
                    panelControl = panelInstance.createPanel(resultTabs, SQLEditor.this, extraPresentation);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Panel opening error", "Can't create panel " + panel.getLabel(), e);
                    return;
                }
                extraPresentationPanels.put(panel, panelInstance);
                tabItem = new CTabItem(resultTabs, SWT.CLOSE);
                tabItem.setControl(panelControl);
                tabItem.setText(panel.getLabel());
                tabItem.setToolTipText(panel.getDescription());
                tabItem.setImage(DBeaverIcons.getImage(panel.getIcon()));
                tabItem.setData(panelInstance);

                // De-select tool item on tab close
                tabItem.addDisposeListener(e -> {
                    PresentationPanelToggleAction.this.setChecked(false);
                    panelControl.dispose();
                    extraPresentationPanels.remove(panel);
                    extraPresentationCurrentPanel = null;
                    resultTabDisposeListener.widgetDisposed(e);
                });
                extraPresentationCurrentPanel = panelInstance;
                resultTabs.setSelection(tabItem);
            } else {
                for (CTabItem tabItem : resultTabs.getItems()) {
                    if (tabItem.getData() == panelInstance) {
                        resultTabs.setSelection(tabItem);
                        break;
                    }
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////
    // Initialization

    @Override
    public void init(IEditorSite site, IEditorInput editorInput)
        throws PartInitException
    {
        super.init(site, editorInput);

        updateResultSetOrientation();

        this.globalScriptContext = new SQLScriptContext(
            null,
            this,
            EditorUtils.getLocalFileFromInput(getEditorInput()),
            new OutputLogWriter(),
            new SQLEditorParametersProvider(getSite()));
    }

    @Override
    protected void doSetInput(IEditorInput editorInput)
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
            // Something bad may happen. E.g. OutOfMemory error in case of too big input file.
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out, true));
            editorInput = new StringEditorInput("Error", CommonUtils.truncateString(out.toString(), 10000), true, GeneralUtils.UTF8_ENCODING);
            doSetInput(editorInput);
            log.error("Error loading input SQL file", e);
        }
        syntaxLoaded = false;

        Runnable inputinitializer = () -> {
            DBPDataSourceContainer oldDataSource = SQLEditor.this.getDataSourceContainer();
            DBPDataSourceContainer newDataSource = EditorUtils.getInputDataSource(SQLEditor.this.getEditorInput());

            if (oldDataSource != newDataSource) {
                SQLEditor.this.dataSourceContainer = null;
                SQLEditor.this.updateDataSourceContainer();
            } else {
                SQLEditor.this.reloadSyntaxRules();
            }
        };
        if (isNonPersistentEditor()) {
            inputinitializer.run();
        } else {
            // Run in queue - for app startup
            UIExecutionQueue.queueExec(inputinitializer);
        }

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
                scriptName = localFile.getName();
            } else {
                scriptName = getEditorInput().getName();
            }
        }

        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        DBPPreferenceStore preferenceStore = getActivePreferenceStore();
        String pattern = preferenceStore.getString(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN);
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

    public void loadQueryPlan() {
        DBCQueryPlanner planner = GeneralUtils.adapt(getDataSource(), DBCQueryPlanner.class);
        ExplainPlanViewer planView = getPlanView(null, planner);

        if (planView != null) {
            if (!planView.loadQueryPlan(planner, planView)) {
                closeActiveTab();
            }
        }

    }

    public void explainQueryPlan() {
        // Notify listeners
        synchronized (listeners) {
            for (SQLEditorListener listener : listeners) {
                listener.beforeQueryPlanExplain();
            }
        }

        final SQLScriptElement scriptElement = extractActiveQuery();
        if (scriptElement == null) {
            setStatus(SQLEditorMessages.editors_sql_status_empty_query_string, DBPMessageType.ERROR);
            return;
        }
        if (!(scriptElement instanceof SQLQuery)) {
            setStatus("Can't explain plan for command", DBPMessageType.ERROR);
            return;
        }
        explainQueryPlan((SQLQuery) scriptElement);
    }

    private void explainQueryPlan(SQLQuery sqlQuery) {
        DBCQueryPlanner planner = GeneralUtils.adapt(getDataSource(), DBCQueryPlanner.class);

        DBCPlanStyle planStyle = planner.getPlanStyle();
        if (planStyle == DBCPlanStyle.QUERY) {
            explainPlanFromQuery(planner, sqlQuery);
        } else if (planStyle == DBCPlanStyle.OUTPUT) {
            explainPlanFromQuery(planner, sqlQuery);
            showOutputPanel();
        } else {
            ExplainPlanViewer planView = getPlanView(sqlQuery, planner);

            if (planView != null) {
                planView.explainQueryPlan(sqlQuery, planner);
            }
        }
    }

    private ExplainPlanViewer getPlanView(SQLQuery sqlQuery, DBCQueryPlanner planner) {

        // 1. Determine whether planner supports plan extraction

        if (planner == null) {
            DBWorkbench.getPlatformUI().showError("Execution plan", "Execution plan explain isn't supported by current datasource");
            return null;
        }
        // Transform query parameters
        if (sqlQuery != null) {
            if (!transformQueryWithParameters(sqlQuery)) {
                return null;
            }
        }

        ExplainPlanViewer planView = null;

        if (sqlQuery != null) {
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
        }

        if (planView == null) {
            int maxPlanNumber = 0;
            for (CTabItem item : resultTabs.getItems()) {
                if (item.getData() instanceof ExplainPlanViewer) {
                    maxPlanNumber = Math.max(maxPlanNumber, ((ExplainPlanViewer) item.getData()).getPlanNumber());
                }
            }
            maxPlanNumber++;

            planView = new ExplainPlanViewer(this, this, resultTabs, maxPlanNumber);
            final CTabItem item = new CTabItem(resultTabs, SWT.CLOSE);
            item.setControl(planView.getControl());
            item.setText(SQLEditorMessages.editors_sql_error_execution_plan_title + " - " + maxPlanNumber);
            if (sqlQuery != null) {
                item.setToolTipText(sqlQuery.getText());
            }
            item.setImage(IMG_EXPLAIN_PLAN);
            item.setData(planView);
            item.addDisposeListener(resultTabDisposeListener);
            UIUtils.disposeControlOnItemDispose(item);
            resultTabs.setSelection(item);
        }
        return planView;
    }

    private void explainPlanFromQuery(final DBCQueryPlanner planner, final SQLQuery sqlQuery) {
        final String[] planQueryString = new String[1];
        DBRRunnableWithProgress queryObtainTask = monitor -> {
            DBCQueryPlannerConfiguration configuration = ExplainPlanViewer.makeExplainPlanConfiguration(monitor, planner);
            if (configuration == null) {
                return;
            }
            try (DBCSession session = getExecutionContext().openSession(monitor, DBCExecutionPurpose.UTIL, "Prepare plan query")) {
                DBCPlan plan = planner.planQueryExecution(session, sqlQuery.getText(), configuration);
                planQueryString[0] = plan.getPlanQueryString();
            } catch (Exception e) {
                log.error(e);
            }
        };
        if (RuntimeUtils.runTask(queryObtainTask, "Retrieve plan query", 5000) && !CommonUtils.isEmpty(planQueryString[0])) {
            SQLQuery planQuery = new SQLQuery(getDataSource(), planQueryString[0]);
            processQueries(Collections.singletonList(planQuery), false, true, false, true, null);
        }
    }

    public void processSQL(boolean newTab, boolean script) {
        processSQL(newTab, script, null, null);
    }

    public boolean processSQL(boolean newTab, boolean script, SQLQueryTransformer transformer, @Nullable SQLQueryListener queryListener)
    {
        IDocument document = getDocument();
        if (document == null) {
            setStatus(SQLEditorMessages.editors_sql_status_cant_obtain_document, DBPMessageType.ERROR);
            return false;
        }

        // Notify listeners
        synchronized (listeners) {
            for (SQLEditorListener listener : listeners) {
                listener.beforeQueryExecute(script, newTab);
            }
        }

        List<SQLScriptElement> elements;
        if (script) {
            // Execute all SQL statements consequently
            ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
            if (selection.getLength() > 1) {
                elements = extractScriptQueries(selection.getOffset(), selection.getLength(), true, false, true);
            } else {
                elements = extractScriptQueries(0, document.getLength(), true, false, true);
            }
        } else {
            // Execute statement under cursor or selected text (if selection present)
            SQLScriptElement sqlQuery = extractActiveQuery();
            if (sqlQuery == null) {
                ResultSetViewer activeViewer = getActiveResultSetViewer();
                if (activeViewer != null) {
                    activeViewer.setStatus(SQLEditorMessages.editors_sql_status_empty_query_string, DBPMessageType.ERROR);
                }
                return false;
            } else {
                elements = Collections.singletonList(sqlQuery);
            }
        }
        try {
            if (transformer != null) {
                DBPDataSource dataSource = getDataSource();
                if (dataSource != null) {
                    List<SQLScriptElement> xQueries = new ArrayList<>(elements.size());
                    for (SQLScriptElement element : elements) {
                        if (element instanceof SQLQuery) {
                            SQLQuery query = transformer.transformQuery(dataSource, getSyntaxManager(), (SQLQuery) element);
                            if (!CommonUtils.isEmpty(query.getParameters())) {
                                query.setParameters(parseQueryParameters(query));
                            }
                            xQueries.add(query);
                        } else {
                            xQueries.add(element);
                        }
                    }
                    elements = xQueries;
                }
            }
        }
        catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Bad query", "Can't execute query", e);
            return false;
        }
        if (!CommonUtils.isEmpty(elements)) {
            return processQueries(elements, script, newTab, false, true, queryListener);
        } else {
            return false;
        }
    }

    public void exportDataFromQuery()
    {
        List<SQLScriptElement> elements;
        ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
        if (selection.getLength() > 1) {
            elements = extractScriptQueries(selection.getOffset(), selection.getLength(), true, false, true);
        } else {
            elements = new ArrayList<>();
            elements.add(extractActiveQuery());
        }

        if (!elements.isEmpty()) {
            processQueries(elements, false, false, true, true, null);
        } else {
            DBWorkbench.getPlatformUI().showError(
                    "Extract data",
                    "Choose one or more queries to export from");
        }
    }

    private boolean processQueries(@NotNull final List<SQLScriptElement> queries, final boolean forceScript, final boolean newTab, final boolean export, final boolean checkSession, @Nullable final SQLQueryListener queryListener)
    {
        if (queries.isEmpty()) {
            // Nothing to process
            return false;
        }

        final DBPDataSourceContainer container = getDataSourceContainer();
        if (checkSession) {
            try {
                DBRProgressListener connectListener = status -> {
                    if (!status.isOK() || container == null || !container.isConnected()) {
                        DBWorkbench.getPlatformUI().showError(
                            SQLEditorMessages.editors_sql_error_cant_obtain_session,
                            null,
                            status);
                        return;
                    }
                    updateExecutionContext(() -> UIUtils.syncExec(() ->
                        processQueries(queries, forceScript, newTab, export, false, queryListener)));
                };
                if (!checkSession(connectListener)) {
                    return false;
                }
            } catch (DBException ex) {
                ResultSetViewer viewer = getActiveResultSetViewer();
                if (viewer != null) {
                    viewer.setStatus(ex.getMessage(), DBPMessageType.ERROR);
                }
                DBWorkbench.getPlatformUI().showError(
                    SQLEditorMessages.editors_sql_error_cant_obtain_session,
                    ex.getMessage());
                return false;
            }
        }
        if (dataSourceContainer == null) {
            return false;
        }
        if (!dataSourceContainer.hasModifyPermission(DBPDataSourcePermission.PERMISSION_EXECUTE_SCRIPTS)) {
            DBWorkbench.getPlatformUI().showError(
                SQLEditorMessages.editors_sql_error_cant_execute_query_title,
                "Query execution was restricted by connection configuration");
            return false;
        }

        SQLScriptContext scriptContext = createScriptContext();

        final boolean isSingleQuery = !forceScript && (queries.size() == 1);
        if (isSingleQuery && queries.get(0) instanceof SQLQuery) {
            SQLQuery query = (SQLQuery) queries.get(0);
            if (query.isDeleteUpdateDangerous()) {
                String targetName = "multiple tables";
                if (query.getSingleSource() != null) {
                    targetName = query.getSingleSource().getEntityName();
                }
                if (ConfirmationDialog.showConfirmDialogEx(
                    ResourceBundle.getBundle(SQLEditorMessages.BUNDLE_NAME),
                    getSite().getShell(),
                    SQLPreferenceConstants.CONFIRM_DANGER_SQL,
                    ConfirmationDialog.CONFIRM,
                    ConfirmationDialog.WARNING,
                    query.getType().name(),
                    targetName) != IDialogConstants.OK_ID)
                {
                    return false;
                }
            }
        } else if (newTab && queries.size() > MAX_PARALLEL_QUERIES_NO_WARN) {
            if (ConfirmationDialog.showConfirmDialogEx(
                ResourceBundle.getBundle(SQLEditorMessages.BUNDLE_NAME),
                getSite().getShell(),
                SQLPreferenceConstants.CONFIRM_MASS_PARALLEL_SQL,
                ConfirmationDialog.CONFIRM,
                ConfirmationDialog.WARNING,
                queries.size()) != IDialogConstants.OK_ID)
            {
                return false;
            }
        }


        if (resultsSash.getMaximizedControl() != null) {
            resultsSash.setMaximizedControl(null);
        }

        // Save editor
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE) && isDirty()) {
            doSave(new NullProgressMonitor());
        }
        boolean extraTabsClosed = false;
        if (!export) {
            if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE)) {
                outputViewer.clearOutput();
            }

            if (!newTab || !isSingleQuery) {
                // We don't need new tab or we are executing a script - so close all extra tabs
                int tabsClosed = closeExtraResultTabs(null, true);
                if (tabsClosed == IDialogConstants.CANCEL_ID) {
                    return false;
                }
                extraTabsClosed = tabsClosed == IDialogConstants.YES_ID;
            }
        }

        if (queryProcessors.isEmpty()) {
            // If all tabs were closed
            createQueryProcessor(true, true);
        }

        if (newTab) {
            // Execute each query in a new tab
            for (int i = 0; i < queries.size(); i++) {
                SQLScriptElement query = queries.get(i);
                QueryProcessor queryProcessor;
                if (i == 0 && (extraTabsClosed || !curQueryProcessor.getFirstResults().hasData())) {
                    queryProcessor = curQueryProcessor;
                } else {
                    queryProcessor = createQueryProcessor(queries.size() == 1, false);
                }
                queryProcessor.processQueries(
                    scriptContext,
                    Collections.singletonList(query),
                    false,
                    true,
                    export,
                    getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR), queryListener);
            }
        } else {
            if (!export) {
                // Use current tab.
                // If current tab was pinned then use first tab
                QueryResultsContainer firstResults = curQueryProcessor.getFirstResults();
                CTabItem tabItem = firstResults.getTabItem();
                if (firstResults.isPinned()) {
                    curQueryProcessor = queryProcessors.get(0);
                    firstResults = curQueryProcessor.getFirstResults();
                    if (firstResults.isPinned()) {
                        // The very first tab is also pinned
                        // Well, let's create a new tab
                        curQueryProcessor = createQueryProcessor(true, true);
                        // Make new tab the default
                        firstResults = curQueryProcessor.getFirstResults();
                        if (firstResults.isPinned()) {
                            tabItem.setShowClose(false);
                        }
                    }
                }
                if (!extraTabsClosed) {
                    if (closeExtraResultTabs(curQueryProcessor, true) == IDialogConstants.CANCEL_ID) {
                        return false;
                    }
                }
                if (tabItem != null) {
                    // Do not switch tab if Output tab is active
                    CTabItem selectedTab = resultTabs.getSelection();
                    if (selectedTab == null || selectedTab.getData() != outputViewer.getControl()) {
                        resultTabs.setSelection(tabItem);
                    }
                }
            }
            return curQueryProcessor.processQueries(scriptContext, queries, forceScript, false, export, false, queryListener);
        }
        return true;
    }

    @NotNull
    private SQLScriptContext createScriptContext() {
        File localFile = EditorUtils.getLocalFileFromInput(getEditorInput());
        return new SQLScriptContext(globalScriptContext, SQLEditor.this, localFile, new OutputLogWriter(), new SQLEditorParametersProvider(getSite()));
    }

    private void setStatus(String status, DBPMessageType messageType)
    {
        ResultSetViewer resultsView = getActiveResultSetViewer();
        if (resultsView != null) {
            resultsView.setStatus(status, messageType);
        }
    }

    private int closeExtraResultTabs(@Nullable QueryProcessor queryProcessor, boolean confirmClose)
    {
        // Close all tabs except first one
        List<CTabItem> tabsToClose = new ArrayList<>();
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
                tabsToClose.add(item);
            } else if (item.getData() instanceof ExplainPlanViewer) {
                tabsToClose.add(item);
            }
        }

        if (tabsToClose.size() > 1) {
            int confirmResult = IDialogConstants.YES_ID;
            if (confirmClose) {
                confirmResult = ConfirmationDialog.showConfirmDialog(
                    ResourceBundle.getBundle(SQLEditorMessages.BUNDLE_NAME),
                    getSite().getShell(),
                    SQLPreferenceConstants.CONFIRM_RESULT_TABS_CLOSE,
                    ConfirmationDialog.QUESTION_WITH_CANCEL,
                    tabsToClose.size() + 1);
                if (confirmResult == IDialogConstants.CANCEL_ID) {
                    return IDialogConstants.CANCEL_ID;
                }
            }
            if (confirmResult == IDialogConstants.YES_ID) {
                for (CTabItem item : tabsToClose) {
                    item.dispose();
                }
            }
            return confirmResult;
        }
        return IDialogConstants.NO_ID;
    }

    public boolean transformQueryWithParameters(SQLQuery query) {
        return createScriptContext().fillQueryParameters(query, false);
    }

    private boolean checkSession(DBRProgressListener onFinish)
        throws DBException
    {
        DBPDataSourceContainer ds = getDataSourceContainer();
        if (ds == null) {
            throw new DBException("No active connection");
        }
        if (!ds.isConnected()) {
            boolean doConnect = ds.getPreferenceStore().getBoolean(SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE);
            if (doConnect) {
                return checkConnected(true, onFinish);
            } else {
                throw new DBException("Disconnected from database");
            }
        }
        DBPDataSource dataSource = ds.getDataSource();
        if (dataSource != null && SQLEditorUtils.isOpenSeparateConnection(ds) && executionContext == null) {
            initSeparateConnection(dataSource, () -> onFinish.onTaskFinished(Status.OK_STATUS));
            return executionContext != null;
        }
        return true;
    }

    /**
     * Handles datasource change action in UI
     */
    private void fireDataSourceChange()
    {
        updateExecutionContext(null);
        UIUtils.syncExec(this::onDataSourceChange);
    }

    private void onDataSourceChange()
    {
        if (resultsSash == null || resultsSash.isDisposed()) {
            reloadSyntaxRules();
            return;
        }

        DatabaseEditorUtils.setPartBackground(this, resultTabs);

        if (getSourceViewerConfiguration() instanceof SQLEditorSourceViewerConfiguration) {
            ((SQLEditorSourceViewerConfiguration) getSourceViewerConfiguration()).onDataSourceChange();
        }

        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            EditorUtils.setInputDataSource(getEditorInput(), new SQLNavigatorContext(executionContext));
        }
        if (syntaxLoaded && lastExecutionContext == executionContext) {
            return;
        }
        if (curResultsContainer != null) {
            ResultSetViewer rsv = curResultsContainer.getResultSetController();
            if (rsv != null) {
                if (executionContext == null) {
                    rsv.setStatus(ModelMessages.error_not_connected_to_database);
                } else {
                    rsv.setStatus(SQLEditorMessages.editors_sql_staus_connected_to + executionContext.getDataSource().getContainer().getName() + "'"); //$NON-NLS-2$
                }
            }
        }

        if (lastExecutionContext == null || executionContext == null || lastExecutionContext.getDataSource() != executionContext.getDataSource()) {
            // Update command states
            SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);
            SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXPLAIN);

            reloadSyntaxRules();
        }

        if (getDataSourceContainer() == null) {
            resultsSash.setMaximizedControl(sqlEditorPanel);
        } else {
            resultsSash.setMaximizedControl(null);
        }

        refreshActions();

        lastExecutionContext = executionContext;
        syntaxLoaded = true;

        loadActivePreferenceSettings();
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
        if (extraPresentation != null) {
            extraPresentation.dispose();
            extraPresentation = null;
        }
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
        SQLPreferenceConstants.EmptyScriptCloseBehavior emptyScriptCloseBehavior = SQLPreferenceConstants.EmptyScriptCloseBehavior.getByName(
            getActivePreferenceStore().getString(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY));
        if (emptyScriptCloseBehavior == SQLPreferenceConstants.EmptyScriptCloseBehavior.NOTHING) {
            return;
        }
        File osFile = sqlFile.getLocation().toFile();
        if (!osFile.exists() || osFile.length() != 0) {
            // Not empty
            return;
        }
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            if (emptyScriptCloseBehavior == SQLPreferenceConstants.EmptyScriptCloseBehavior.DELETE_NEW) {
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
            UIUtils.asyncExec(
                () -> {
                    switch (event.getAction()) {
                        case OBJECT_REMOVE:
                            if (dsEvent) {
                                setDataSourceContainer(null);
                            }
                            break;
                        case OBJECT_UPDATE:
                        case OBJECT_SELECT:
                            if (objectEvent) {
                                setPartName(getEditorName());
                                // Active schema was changed? Update title and tooltip
                                firePropertyChange(IWorkbenchPartConstants.PROP_TITLE);
                            }
                            break;
                        default:
                            break;
                    }
                    updateExecutionContext(null);
                    onDataSourceChange();
                }
            );
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (!EditorUtils.isInAutoSaveJob()) {
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
        }

        if (extraPresentation instanceof ISaveablePart) {
            ((ISaveablePart) extraPresentation).doSave(monitor);
        }
        super.doSave(monitor);

        updateDataSourceContainer();
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

            if (ConfirmationDialog.showConfirmDialog(
                ResourceBundle.getBundle(SQLEditorMessages.BUNDLE_NAME),
                null,
                SQLPreferenceConstants.CONFIRM_RUNNING_QUERY_CLOSE,
                ConfirmationDialog.QUESTION,
                jobsRunning) != IDialogConstants.YES_ID)
            {
                return ISaveablePart2.CANCEL;
            }
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
        if (executionContext != null) {
            UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
            if (serviceConnections != null && !serviceConnections.checkAndCloseActiveTransaction(new DBCExecutionContext[] {executionContext})) {
                return ISaveablePart2.CANCEL;
            }
        }

        // That's fine
        if (isNonPersistentEditor()) {
            return ISaveablePart2.NO;
        }

        // Cancel running jobs (if any) and close results tabs
        for (QueryProcessor queryProcessor : queryProcessors) {
            queryProcessor.cancelJob();
            // FIXME: it is a hack (to avoid asking "Save script?" because editor is marked as dirty while queries are running)
            // FIXME: make it better
            queryProcessor.curJobRunning.set(0);
        }
        updateDirtyFlag();

        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE)) {
            return ISaveablePart2.YES;
        }

        if (super.isDirty()) {
            return ISaveablePart2.DEFAULT;
        }
        return ISaveablePart2.YES;
    }

    protected void afterSaveToFile(File saveFile) {
        try {
            IFileStore fileStore = EFS.getStore(saveFile.toURI());
            IEditorInput input = new FileStoreEditorInput(fileStore);

            EditorUtils.setInputDataSource(input, new SQLNavigatorContext(getDataSourceContainer(), getExecutionContext()));

            setInput(input);
        } catch (CoreException e) {
            DBWorkbench.getPlatformUI().showError("File save", "Can't open SQL editor from external file", e);
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
        IColumnSupport columnSupport = getAdapter(IColumnSupport.class);
        if (columnSupport != null) {
            RulerColumnDescriptor positionColumn = RulerColumnRegistry.getDefault().getColumnDescriptor(ScriptPositionColumn.ID);
            columnSupport.setColumnVisible(positionColumn, show);
        }
    }

    private void showStatementInEditor(final SQLQuery query, final boolean select)
    {
        UIUtils.runUIJob("Select SQL query in editor", monitor -> {
            if (isDisposed()) {
                return;
            }
            if (select) {
                selectAndReveal(query.getOffset(), query.getLength());
                setStatus(query.getText(), DBPMessageType.INFORMATION);
            } else {
                getSourceViewer().revealRange(query.getOffset(), query.getLength());
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

    private QueryProcessor createQueryProcessor(boolean setSelection, boolean makeDefault)
    {
        final QueryProcessor queryProcessor = new QueryProcessor(makeDefault);
        curQueryProcessor = queryProcessor;
        curResultsContainer = queryProcessor.getFirstResults();
        if (setSelection) {
            CTabItem tabItem = curResultsContainer.getTabItem();
            if (tabItem != null) {
                resultTabs.setSelection(tabItem);
            }
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
            case ModelPreferences.SQL_VARIABLES_ENABLED:
            case ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX:
                reloadSyntaxRules();
                return;
            case SQLPreferenceConstants.RESULT_SET_ORIENTATION:
                updateResultSetOrientation();
                return;
            case SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION: {
                // Save current datasource (we want to keep it here)
                DBPDataSource dataSource = curDataSource;
                releaseExecutionContext();
                // Restore cur data source (as it is reset in releaseExecutionContext)
                curDataSource = dataSource;
                if (dataSource != null && SQLEditorUtils.isOpenSeparateConnection(dataSource.getContainer())) {
                    initSeparateConnection(dataSource, null);
                }
                return;
            }
        }
        super.preferenceChange(event);
    }

    public enum ResultSetOrientation {
        HORIZONTAL(SWT.VERTICAL, SQLEditorMessages.sql_editor_result_set_orientation_horizontal, SQLEditorMessages.sql_editor_result_set_orientation_horizontal_tip, true),
        VERTICAL(SWT.HORIZONTAL, SQLEditorMessages.sql_editor_result_set_orientation_vertical, SQLEditorMessages.sql_editor_result_set_orientation_vertical_tip, true),
        DETACHED(SWT.VERTICAL, SQLEditorMessages.sql_editor_result_set_orientation_detached, SQLEditorMessages.sql_editor_result_set_orientation_detached_tip, false);

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
            IEditorPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (!(activeEditor instanceof SQLEditorBase)) {
                return new IContributionItem[0];
            }
            final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
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
            return items.toArray(new IContributionItem[0]);
        }
    }

    public class QueryProcessor implements SQLResultsConsumer {

        private volatile SQLQueryJob curJob;
        private AtomicInteger curJobRunning = new AtomicInteger(0);
        private final List<QueryResultsContainer> resultContainers = new ArrayList<>();
        private volatile DBDDataReceiver curDataReceiver = null;

        QueryProcessor(boolean makeDefault) {
            // Create first (default) results provider
            if (makeDefault) {
                queryProcessors.add(0, this);
            } else {
                queryProcessors.add(this);
            }
            createResultsProvider(0, makeDefault);
        }

        private QueryResultsContainer createResultsProvider(int resultSetNumber, boolean makeDefault) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultSetNumber, makeDefault);
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
                curJob = null;
                if (job.isJobOpen()) {
                    RuntimeUtils.runTask(monitor -> {
                        job.closeJob();
                    }, "Close SQL job", 2000, true);
                }
            }
        }

        public void cancelJob() {
            for (QueryResultsContainer rc : resultContainers) {
                rc.viewer.cancelJobs();
            }
            final SQLQueryJob job = curJob;
            if (job != null) {
                if (job.getState() == Job.RUNNING) {
                    job.cancel();
                }
            }
        }

        boolean processQueries(SQLScriptContext scriptContext, final List<SQLScriptElement> queries, boolean forceScript, final boolean fetchResults, boolean export, boolean closeTabOnError, SQLQueryListener queryListener)
        {
            if (queries.isEmpty()) {
                // Nothing to process
                return false;
            }
            if (curJobRunning.get() > 0) {
                DBWorkbench.getPlatformUI().showError(
                        SQLEditorMessages.editors_sql_error_cant_execute_query_title,
                    SQLEditorMessages.editors_sql_error_cant_execute_query_message);
                return false;
            }
            final DBCExecutionContext executionContext = getExecutionContext();
            if (executionContext == null) {
                DBWorkbench.getPlatformUI().showError(
                        SQLEditorMessages.editors_sql_error_cant_execute_query_title,
                    ModelMessages.error_not_connected_to_database);
                return false;
            }
            final boolean isSingleQuery = !forceScript && (queries.size() == 1);

            // Prepare execution job
            {
                showScriptPositionRuler(true);
                QueryResultsContainer resultsContainer = getFirstResults();

                SQLEditorQueryListener listener = new SQLEditorQueryListener(this, closeTabOnError);
                if (queryListener != null) {
                    listener.setExtListener(queryListener);
                }

                if (export) {
                    List<IDataTransferProducer> producers = new ArrayList<>();
                    for (int i = 0; i < queries.size(); i++) {
                        SQLScriptElement element = queries.get(i);
                        if (element instanceof SQLControlCommand) {
                            try {
                                scriptContext.executeControlCommand((SQLControlCommand) element);
                            } catch (DBException e) {
                                DBWorkbench.getPlatformUI().showError("Command error", "Error processing control command", e);
                            }
                        } else {
                            SQLQuery query = (SQLQuery) element;
                            scriptContext.fillQueryParameters(query, false);

                            SQLQueryDataContainer dataContainer = new SQLQueryDataContainer(SQLEditor.this, query, scriptContext, log);
                            producers.add(new DatabaseTransferProducer(dataContainer, null));
                        }
                    }

                    DataTransferWizard.openWizard(
                        getSite().getWorkbenchWindow(),
                        producers,
                        null,
                        new StructuredSelection(this));
                } else {
                    final SQLQueryJob job = new SQLQueryJob(
                        getSite(),
                        isSingleQuery ? SQLEditorMessages.editors_sql_job_execute_query : SQLEditorMessages.editors_sql_job_execute_script,
                        executionContext,
                        resultsContainer,
                        queries,
                        scriptContext,
                        this,
                        listener);

                    if (isSingleQuery) {
                        resultsContainer.query = queries.get(0);

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
            return true;
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
            resultContainers.remove(resultsContainer);
            if (resultContainers.isEmpty()) {
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
                UIUtils.syncExec(() -> createResultsProvider(resultSetNumber, false));
            }
            if (resultSetNumber >= resultContainers.size()) {
                // Editor seems to be disposed - no data receiver
                return null;
            }
            final QueryResultsContainer resultsProvider = resultContainers.get(resultSetNumber);

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
                String finalTabName = tabName;
                UIUtils.asyncExec(() -> resultsProvider.updateResultsName(finalTabName, toolTip));
            }
            ResultSetViewer rsv = resultsProvider.getResultSetController();
            return rsv == null ? null : rsv.getDataReceiver();
        }
    }

    public class QueryResultsContainer implements DBSDataContainer, IResultSetContainer, IResultSetValueReflector, IResultSetListener, SQLQueryContainer, ISmartTransactionManager {

        private final QueryProcessor queryProcessor;
        private final ResultSetViewer viewer;
        private final int resultSetNumber;
        private SQLScriptElement query = null;
        private SQLScriptElement lastGoodQuery = null;
        // Data container and filter are non-null only in case of associations navigation
        private DBSDataContainer dataContainer;
        private CTabItem resultsTab;
        private String tabName;

        private QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber, boolean makeDefault)
        {
            this.queryProcessor = queryProcessor;
            this.resultSetNumber = resultSetNumber;

            boolean detachedViewer = false;
            SQLResultsView sqlView = null;
            if (detachedViewer) {
                try {
                    sqlView = (SQLResultsView) getSite().getPage().showView(SQLResultsView.VIEW_ID, null, IWorkbenchPage.VIEW_CREATE);
                } catch (Throwable e) {
                    DBWorkbench.getPlatformUI().showError("Detached results", "Can't open results view", e);
                }
            }

            if (sqlView != null) {
                // Detached results viewer
                sqlView.setContainer(this);
                this.viewer = sqlView.getViewer();
            } else {
                // Embedded results viewer
                this.viewer = new ResultSetViewer(resultTabs, getSite(), this);
                this.viewer.addListener(this);

                int tabCount = resultTabs.getItemCount();
                int tabIndex = 0;
                if (!makeDefault) {
                    for (int i = tabCount; i > 0; i--) {
                        if (resultTabs.getItem(i - 1).getData() instanceof QueryResultsContainer) {
                            tabIndex = i;
                            break;
                        }
                    }
                }
                resultsTab = new CTabItem(resultTabs, SWT.NONE, tabIndex);
                int queryIndex = queryProcessors.indexOf(queryProcessor);
                String tabName = getResultsTabName(resultSetNumber, queryIndex, null);
                resultsTab.setText(tabName);
                resultsTab.setImage(IMG_DATA_GRID);
                resultsTab.setData(this);
                resultsTab.setShowClose(true);
                CSSUtils.setCSSClass(resultsTab, DBStyles.COLORED_BY_CONNECTION_TYPE);

                resultsTab.setControl(viewer.getControl());
                resultsTab.addDisposeListener(resultTabDisposeListener);
                UIUtils.disposeControlOnItemDispose(resultsTab);
            }

            viewer.getControl().addDisposeListener(e -> {
                QueryResultsContainer.this.queryProcessor.removeResults(QueryResultsContainer.this);
                if (QueryResultsContainer.this == curResultsContainer) {
                    curResultsContainer = null;
                }
            });
        }

        QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber, DBSDataContainer dataContainer) {
            this(queryProcessor, resultSetNumber, false);
            this.dataContainer = dataContainer;
            updateResultsName(getResultsTabName(resultSetNumber, 0, dataContainer.getName()), null);
        }

        private CTabItem getTabItem() {
            return resultsTab;
        }

        void updateResultsName(String resultSetName, String toolTip) {
            if (resultTabs == null || resultTabs.isDisposed()) {
                return;
            }
            if (CommonUtils.isEmpty(resultSetName)) {
                resultSetName = tabName;
            }
            CTabItem tabItem = getTabItem();
            if (tabItem != null && !tabItem.isDisposed()) {
                if (!CommonUtils.isEmpty(resultSetName)) {
                    tabItem.setText(resultSetName);
                }
                tabItem.setToolTipText(toolTip);
            }
        }

        boolean isPinned() {
            CTabItem tabItem = getTabItem();
            return tabItem != null && !tabItem.isDisposed() && !tabItem.getShowClose();
        }

        void setPinned(boolean pinned) {
            CTabItem tabItem = getTabItem();
            if (tabItem != null) {
                tabItem.setShowClose(!pinned);
                tabItem.setImage(pinned ? IMG_DATA_GRID_LOCKED : IMG_DATA_GRID);
            }
        }

        @NotNull
        @Override
        public DBPProject getProject() {
            return SQLEditor.this.getProject();
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

        boolean hasData() {
            return viewer != null && viewer.hasData();
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
            return queryProcessor.curJob == null || queryProcessor.curJobRunning.get() <= 0;
        }

        @Override
        public void openNewContainer(DBRProgressMonitor monitor, @NotNull DBSDataContainer dataContainer, @NotNull DBDDataFilter newFilter) {
            UIUtils.syncExec(() -> {
                QueryResultsContainer resultsProvider = queryProcessor.createResultsProvider(dataContainer);
                CTabItem tabItem = resultsProvider.getTabItem();
                if (tabItem != null) {
                    tabItem.getParent().setSelection(tabItem);
                }
                setActiveResultsContainer(resultsProvider);
                resultsProvider.viewer.refreshWithFilter(newFilter);
            });
        }

        @Override
        public IResultSetDecorator createResultSetDecorator() {
            return new QueryResultsDecorator() {
                @Override
                public String getEmptyDataDescription() {
                    String execQuery = ActionUtils.findCommandDescription(SQLEditorCommands.CMD_EXECUTE_STATEMENT, getSite(), true);
                    String execScript = ActionUtils.findCommandDescription(SQLEditorCommands.CMD_EXECUTE_SCRIPT, getSite(), true);
                    return NLS.bind(ResultSetMessages.sql_editor_resultset_filter_panel_control_execute_to_see_reslut, execQuery, execScript);
                }
            };
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
        public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException
        {
            if (dataContainer != null) {
                return dataContainer.readData(source, session, dataReceiver, dataFilter, firstRow, maxRows, flags, fetchSize);
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
                job.setFetchSize(fetchSize);
                job.setFetchFlags(flags);

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
        public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter, long flags)
            throws DBCException
        {
            if (dataContainer != null) {
                return dataContainer.countData(source, session, dataFilter, DBSDataContainer.FLAG_NONE);
            }
            DBPDataSource dataSource = getDataSource();
            if (dataSource == null) {
                throw new DBCException("Query transform is not supported by datasource");
            }
            if (!(query instanceof SQLQuery)) {
                throw new DBCException("Can't count rows for control command");
            }
            try {
                SQLQuery countQuery = new SQLQueryTransformerCount().transformQuery(dataSource, getSyntaxManager(), (SQLQuery) query);
                if (!CommonUtils.isEmpty(countQuery.getParameters())) {
                    countQuery.setParameters(parseQueryParameters(countQuery));
                }

                try (DBCStatement dbStatement = DBUtils.makeStatement(source, session, DBCStatementType.QUERY, countQuery, 0, 0)) {
                    if (dbStatement.executeStatement()) {
                        try (DBCResultSet rs = dbStatement.openResultSet()) {
                            if (rs.nextRow()) {
                                List<DBCAttributeMetaData> resultAttrs = rs.getMeta().getAttributes();
                                Object countValue = null;
                                if (resultAttrs.size() == 1) {
                                    countValue = rs.getAttributeValue(0);
                                } else {
                                    // In some databases (Influx?) SELECT count(*) produces multiple columns. Try to find first one with 'count' in its name.
                                    for (int i = 0; i < resultAttrs.size(); i++) {
                                        DBCAttributeMetaData ma = resultAttrs.get(i);
                                        if (ma.getName().toLowerCase(Locale.ENGLISH).contains("count")) {
                                            countValue = rs.getAttributeValue(i);
                                            break;
                                        }
                                    }
                                }
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
                return SQLEditorMessages.editors_sql_description;
            }
        }

        @Nullable
        @Override
        public DBSObject getParentObject()
        {
            return getDataSource();
        }

        @Nullable
        @Override
        public DBPDataSource getDataSource()
        {
            return SQLEditor.this.getDataSource();
        }

        @Override
        public boolean isPersisted() {
            return dataContainer == null || dataContainer.isPersisted();
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

        @Override
        public void handleResultSetSelectionChange(SelectionChangedEvent event) {

        }

        @Override
        public SQLScriptElement getQuery() {
            return query;
        }

        @Override
        public Map<String, Object> getQueryParameters() {
            return globalScriptContext.getAllParameters();
        }

        @Override
        public boolean isSmartAutoCommit() {
            return SQLEditor.this.isSmartAutoCommit();
        }

        @Override
        public void setSmartAutoCommit(boolean smartAutoCommit) {
            SQLEditor.this.setSmartAutoCommit(smartAutoCommit);
        }

        public void setTabName(String tabName) {
            this.tabName = tabName;
            resultsTab.setText(tabName);
        }

        @Override
        public void insertCurrentCellValue(DBDAttributeBinding attributeBinding, Object cellValue, String stringValue) {
            StyledText textWidget = getTextViewer() == null ? null : getTextViewer().getTextWidget();
            if (textWidget != null) {
                String sqlValue;
                if (getDataSource() != null) {
                    sqlValue = SQLUtils.convertValueToSQL(getDataSource(), attributeBinding, cellValue);
                } else {
                    sqlValue = stringValue;
                }
                textWidget.insert(sqlValue);
                textWidget.setCaretOffset(textWidget.getCaretOffset() + sqlValue.length());
                textWidget.setFocus();
            }
        }
    }

    private String getResultsTabName(int resultSetNumber, int queryIndex, String name) {
        String tabName = name;
        if (CommonUtils.isEmpty(tabName)) {
            tabName = SQLEditorMessages.editors_sql_data_grid;
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
                UIUtils.asyncExec(() -> {
                    if (isDisposed()) {
                        return;
                    }
                    if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE)) {
                        resultsSash.setMaximizedControl(sqlEditorPanel);
                    }
                });
            } finally {
                if (extListener != null) extListener.onStartScript();
            }
        }

        @Override
        public void onStartQuery(DBCSession session, final SQLQuery query) {
            try {
                if (isSmartAutoCommit()) {
                    DBExecUtils.checkSmartAutoCommit(session, query.getText());
                }
                boolean isInExecute = getTotalQueryRunning() > 0;
                if (!isInExecute) {
                    UIUtils.asyncExec(() -> {
                        setTitleImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT_EXECUTE));
                        updateDirtyFlag();
                    });
                }
                queryProcessor.curJobRunning.incrementAndGet();
                synchronized (runningQueries) {
                    runningQueries.add(query);
                }
                if (lastUIUpdateTime < 0 || System.currentTimeMillis() - lastUIUpdateTime > SCRIPT_UI_UPDATE_PERIOD) {
                    UIUtils.asyncExec(() -> {
                        TextViewer textViewer = getTextViewer();
                        if (textViewer != null) {
                            topOffset = textViewer.getTopIndexStartOffset();
                            visibleLength = textViewer.getBottomIndexEndOffset() - topOffset;
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
        public void onEndQuery(final DBCSession session, final SQLQueryResult result, DBCStatistics statistics) {
            try {
                synchronized (runningQueries) {
                    runningQueries.remove(result.getStatement());
                }
                queryProcessor.curJobRunning.decrementAndGet();
                if (getTotalQueryRunning() <= 0) {
                    UIUtils.asyncExec(() -> {
                        setTitleImage(editorImage);
                        updateDirtyFlag();
                    });
                }

                if (isDisposed()) {
                    return;
                }
                UIUtils.runUIJob("Process SQL query result", monitor -> {
                    // Finish query
                    processQueryResult(monitor, result, statistics);
                    // Update dirty flag
                    updateDirtyFlag();
                    refreshActions();
                });
            } finally {
                if (extListener != null) extListener.onEndQuery(session, result, statistics);
            }
        }

        private void processQueryResult(DBRProgressMonitor monitor, SQLQueryResult result, DBCStatistics statistics) {
            dumpQueryServerOutput(result);
            if (!scriptMode) {
                runPostExecuteActions(result);
            }
            SQLQuery query = result.getStatement();
            Throwable error = result.getError();
            if (error != null) {
                setStatus(GeneralUtils.getFirstMessage(error), DBPMessageType.ERROR);
                if (!scrollCursorToError(monitor, query, error)) {
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
                // Set tab names by query results names
                if (scriptMode || queryProcessor.getResultContainers().size() > 0) {

                    int queryIndex = queryProcessors.indexOf(queryProcessor);
                    int resultsIndex = 0;
                    for (QueryResultsContainer results : queryProcessor.resultContainers) {
                        if (results.query != query) {
                            continue;
                        }
                        if (resultsIndex < result.getExecuteResults().size()) {
                            SQLQueryResult.ExecuteResult executeResult = result.getExecuteResults(resultsIndex, true);
                            String resultSetName = results.tabName;
                            if (CommonUtils.isEmpty(resultSetName)) {
                                resultSetName = getResultsTabName(results.resultSetNumber, queryIndex, executeResult.getResultSetName());
                                results.updateResultsName(resultSetName, null);
                            }
                            ResultSetViewer resultSetViewer = results.getResultSetController();
                            if (resultSetViewer != null) {
                                resultSetViewer.getModel().setStatistics(statistics);
                            }
                        }
                        resultsIndex++;
                    }
                }
            }
            // Close tab on error
            if (closeTabOnError && error != null) {
                CTabItem tabItem = queryProcessor.getFirstResults().getTabItem();
                if (tabItem != null && tabItem.getShowClose()) {
                    tabItem.dispose();
                }
            }
            // Beep
            if (dataSourceContainer != null && !scriptMode && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.BEEP_ON_QUERY_END)) {
                Display.getCurrent().beep();
            }
            // Notify agent
            if (result.getQueryTime() > DBWorkbench.getPlatformUI().getLongOperationTimeout() * 1000) {
                DBWorkbench.getPlatformUI().notifyAgent(
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
                UIUtils.asyncExec(() -> {
                    if (isDisposed()) {
                        // Editor closed
                        return;
                    }
                    resultsSash.setMaximizedControl(null);
                    if (!hasErrors) {
                        getSelectionProvider().setSelection(originalSelection);
                    }
                    QueryResultsContainer results = queryProcessor.getFirstResults();
                    ResultSetViewer viewer = results.getResultSetController();
                    if (viewer != null) {
                        viewer.getModel().setStatistics(statistics);
                        viewer.updateStatusMessage();
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

            CTabItem activeResultsTab = getActiveResultsTab();
            if (activeResultsTab != null && activeResultsTab.getData() instanceof StyledText) {
                StyledText styledText = (StyledText) activeResultsTab.getData();
                if (!focusInEditor) {
                    return new StyledTextFindReplaceTarget(styledText);
                }
            }

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

    private void dumpQueryServerOutput(@Nullable SQLQueryResult result) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            final DBPDataSource dataSource = executionContext.getDataSource();
            // Dump server output
            DBCServerOutputReader outputReader = DBUtils.getAdapter(DBCServerOutputReader.class, dataSource);
            if (outputReader == null && result != null) {
                outputReader = new DefaultServerOutputReader();
            }
            if (outputReader != null && outputReader.isServerOutputEnabled()) {
                synchronized (serverOutputs) {
                    serverOutputs.add(new ServerOutputInfo(outputReader, executionContext, result));
                }
            }
        }
    }

    private void runPostExecuteActions(@Nullable SQLQueryResult result) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            // Refresh active object
            if (result == null || !result.hasError() && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)) {
                DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults != null) {
                    new AbstractJob("Refresh default object") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            DBUtils.refreshContextDefaultsAndReflect(monitor, contextDefaults);
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            }
        }
    }

    private void updateOutputViewerIcon(boolean alert) {
        Image image = alert ? IMG_OUTPUT_ALERT : IMG_OUTPUT;
        CTabItem outputItem = UIUtils.getTabItem(resultTabs, outputViewer.getControl());
        if (outputItem != null && outputItem != resultTabs.getSelection()) {
            outputItem.setImage(image);
        } else {
            // TODO: make icon update. Can't call setImage because this will break contract f VerticalButton
/*
            VerticalButton viewItem = getViewToolItem(SQLEditorCommands.CMD_SQL_SHOW_OUTPUT);
            if (viewItem != null) {
                viewItem.setImage(image);
            }
*/
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
        private final SQLQueryResult result;

        ServerOutputInfo(DBCServerOutputReader outputReader, DBCExecutionContext executionContext, SQLQueryResult result) {
            this.outputReader = outputReader;
            this.executionContext = executionContext;
            this.result = result;
        }
    }

    private final List<ServerOutputInfo> serverOutputs = new ArrayList<>();

    private class ServerOutputReader extends AbstractJob {

        ServerOutputReader() {
            super("Dump server output");
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            if (!DBWorkbench.getPlatform().isShuttingDown() && resultsSash != null && !resultsSash.isDisposed()) {
                dumpOutput(monitor);

                schedule(200);
            }

            return Status.OK_STATUS;
        }

        private void dumpOutput(DBRProgressMonitor monitor) {
            if (outputViewer == null) {
                return;
            }

            List<ServerOutputInfo> outputs;
            synchronized (serverOutputs) {
                outputs = new ArrayList<>(serverOutputs);
                serverOutputs.clear();
            }

            PrintWriter outputWriter = outputViewer.getOutputWriter();

            if (!outputs.isEmpty()) {
                for (ServerOutputInfo info : outputs) {
                    try {
                        info.outputReader.readServerOutput(monitor, info.executionContext, info.result, null, outputWriter);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }

            {
                // Check running queries for async output
                DBCServerOutputReader outputReader = null;
                final DBCExecutionContext executionContext = getExecutionContext();
                if (executionContext != null) {
                    final DBPDataSource dataSource = executionContext.getDataSource();
                    // Dump server output
                    outputReader = DBUtils.getAdapter(DBCServerOutputReader.class, dataSource);
                }
                if (outputReader != null && outputReader.isAsyncOutputReadSupported()) {
                    for (QueryProcessor qp : queryProcessors) {
                        SQLQueryJob queryJob = qp.curJob;
                        if (queryJob != null) {
                            DBCStatement statement = queryJob.getCurrentStatement();
                            if (statement != null) {
                                try {
                                    outputReader.readServerOutput(monitor, executionContext, null, statement, outputWriter);
                                } catch (DBCException e) {
                                    log.error(e);
                                }
                            }
                        }
                    }
                }
            }
            outputWriter.flush();
            UIUtils.asyncExec(() -> {
                if (outputViewer!=null) {
                    if (outputViewer.getControl()!=null) {
                        if (!outputViewer.isDisposed() && outputViewer.isHasNewOutput()) {
                            outputViewer.scrollToEnd();
                            updateOutputViewerIcon(true);
                            outputViewer.resetNewOutput();
                        }
                    }
                }
            });
        }
    }
}
