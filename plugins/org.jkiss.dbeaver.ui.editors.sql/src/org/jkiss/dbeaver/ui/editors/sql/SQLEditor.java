/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.renderers.swt.HandledContributionItem;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.rulers.IColumnSupport;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.eclipse.ui.texteditor.rulers.RulerColumnRegistry;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
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
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI.UserChoiceResponse;
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
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddIn;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddInDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddInsRegistry;
import org.jkiss.dbeaver.ui.editors.sql.execute.SQLQueryJob;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorVariablesResolver;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogPanel;
import org.jkiss.dbeaver.ui.editors.sql.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationPanelDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationRegistry;
import org.jkiss.dbeaver.ui.editors.sql.scripts.ScriptsHandlerImpl;
import org.jkiss.dbeaver.ui.editors.sql.variables.AssignVariableAction;
import org.jkiss.dbeaver.ui.editors.sql.variables.SQLVariablesPanel;
import org.jkiss.dbeaver.ui.editors.text.ScriptPositionColumn;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL Executor
 */
public class SQLEditor extends SQLEditorBase implements
    IDataSourceContainerUpdate,
    DBPEventListener,
    ISaveablePart2,
    DBPDataSourceTask,
    DBPDataSourceAcquirer,
    IResultSetProvider,
    ISmartTransactionManager,
    IStatefulEditor
{
    private static final long SCRIPT_UI_UPDATE_PERIOD = 100;
    private static final int MAX_PARALLEL_QUERIES_NO_WARN = 1;

    private static final int QUERIES_COUNT_FOR_NO_FETCH_RESULT_SET_CONFIRMATION = 100;

    private static final int SQL_EDITOR_CONTROL_INDEX = 1;
    private static final int EXTRA_CONTROL_INDEX = 0;

    private static final String PANEL_ITEM_PREFIX = "SQLPanelToggle:";
    private static final String EMBEDDED_BINDING_PREFIX = "-- CONNECTION: ";
    private static final Pattern EMBEDDED_BINDING_PREFIX_PATTERN = Pattern.compile("--\\s*CONNECTION:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private static final Image IMG_DATA_GRID = DBeaverIcons.getImage(UIIcon.SQL_PAGE_DATA_GRID);
    private static final Image IMG_DATA_GRID_LOCKED = DBeaverIcons.getImage(UIIcon.SQL_PAGE_DATA_GRID_LOCKED);
    private static final Image IMG_EXPLAIN_PLAN = DBeaverIcons.getImage(UIIcon.SQL_PAGE_EXPLAIN_PLAN);
    private static final Image IMG_LOG = DBeaverIcons.getImage(UIIcon.SQL_PAGE_LOG);
    private static final Image IMG_VARIABLES = DBeaverIcons.getImage(UIIcon.SQL_VARIABLE);
    private static final Image IMG_OUTPUT = DBeaverIcons.getImage(UIIcon.SQL_PAGE_OUTPUT);
    private static final Image IMG_OUTPUT_ALERT = DBeaverIcons.getImage(UIIcon.SQL_PAGE_OUTPUT_ALERT);

    private static final String SIDE_TOP_TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.editors.sql.toolbar.side.top";
    private static final String SIDE_BOTTOM_TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.editors.sql.toolbar.side.bottom";

//    private static final String TOOLBAR_GROUP_PANELS = "panelToggles";

    public static final String VIEW_PART_PROP_NAME = "org.jkiss.dbeaver.ui.editors.sql.SQLEditor";



    public static final String DEFAULT_TITLE_PATTERN = "<${" + SQLPreferenceConstants.VAR_CONNECTION_NAME + "}> ${" + SQLPreferenceConstants.VAR_FILE_NAME + "}";
    public static final String DEFAULT_SCRIPT_FILE_NAME = "Script";
    private ResultSetOrientation resultSetOrientation = ResultSetOrientation.HORIZONTAL;
    private CustomSashForm resultsSash;
    private Composite sqlEditorPanel;
    @Nullable
    private Composite presentationStack;
    private SashForm sqlExtraPanelSash;
    private CTabFolder sqlExtraPanelFolder;
    private ToolBarManager sqlExtraPanelToolbar;

    private CTabFolder resultTabs;
    private TabFolderReorder resultTabsReorder;
    private CTabItem activeResultsTab;

    private SQLLogPanel logViewer;
    private SQLEditorOutputViewer outputViewer;
    private SQLVariablesPanel variablesViewer;

    private volatile QueryProcessor curQueryProcessor;
    private final List<QueryProcessor> queryProcessors = new ArrayList<>();

    private DBPDataSourceContainer dataSourceContainer;
    private DBPDataSource curDataSource;
    private volatile DBCExecutionContext executionContext;
    private volatile DBCExecutionContext lastExecutionContext;
    private volatile DBPContextProvider executionContextProvider;
    private SQLScriptContext globalScriptContext;
    private volatile boolean syntaxLoaded = false;
    private FindReplaceTarget findReplaceTarget = new FindReplaceTarget();
    private final List<SQLQuery> runningQueries = new ArrayList<>();
    private QueryResultsContainer curResultsContainer;
    private Image baseEditorImage;
    private Image editorImage;
    private Composite leftToolPanel;
    private ToolBarManager topBarMan;
    private ToolBarManager bottomBarMan;

    private SQLPresentationDescriptor extraPresentationDescriptor;
    private SQLEditorPresentation extraPresentation;
    private final Map<SQLPresentationPanelDescriptor, SQLEditorPresentationPanel> extraPresentationPanels = new HashMap<>();
    private SQLEditorPresentationPanel extraPresentationCurrentPanel;
    private VerticalFolder presentationSwitchFolder;

    private final List<SQLEditorListener> listeners = new ArrayList<>();
    private final List<ServerOutputInfo> serverOutputs = new ArrayList<>();
    private ScriptAutoSaveJob scriptAutoSavejob;
    private boolean isResultSetAutoFocusEnabled = true;
    private Boolean isDisableFetchResultSet = null;
    private boolean datasourceChanged;

    private final ArrayList<SQLEditorAddIn> addIns = new ArrayList<>();

    private static class ServerOutputInfo {
        private final DBCServerOutputReader outputReader;
        private final DBCExecutionContext executionContext;
        private final DBCExecutionResult result;

        ServerOutputInfo(DBCServerOutputReader outputReader, DBCExecutionContext executionContext, DBCExecutionResult result) {
            this.outputReader = outputReader;
            this.executionContext = executionContext;
            this.result = result;
        }
    }

    private final DisposeListener resultTabDisposeListener = new DisposeListener() {
        @Override
        public void widgetDisposed(DisposeEvent e) {
            Object data = e.widget.getData();
            if (data instanceof QueryResultsContainer) {
                QueryProcessor processor = ((QueryResultsContainer) data).queryProcessor;
                List<QueryResultsContainer> containers = processor.getResultContainers();
                for (int index = containers.indexOf(data) + 1; index < containers.size(); index++) {
                    QueryResultsContainer container = containers.get(index);
                    // Make sure that resultSetNumber equals to current loop index.
                    // This must be true for every container of this query processor
                    if (container.resultSetNumber == index) {
                        container.resultSetNumber--;
                    }
                }
            }
            if (resultTabs.getItemCount() == 0) {
                if (resultsSash.getMaximizedControl() == null) {
                    // Hide results
                    toggleResultPanel(false, true);
                }
            }
        }
    };
    private final IPropertyChangeListener themeChangeListener = e -> {
        final Font font = JFaceResources.getFont(UIFonts.DBEAVER_FONTS_MAIN_FONT);
        if (resultTabs != null) {
            resultTabs.setFont(font);
        }
        if (this.switchPresentationSQLButton != null) {
            this.switchPresentationSQLButton.setFont(font);
        }
        if (this.switchPresentationExtraButton != null) {
            this.switchPresentationExtraButton.setFont(font);
        }
    };
    private VerticalButton switchPresentationSQLButton;
    private VerticalButton switchPresentationExtraButton;

    public SQLEditor()
    {
        super();

        this.extraPresentationDescriptor = SQLPresentationRegistry.getInstance().getPresentation(this);
        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeChangeListener);
    }

    public void setResultSetAutoFocusEnabled(boolean value) {
        isResultSetAutoFocusEnabled = value;
    }

    @Override
    protected String[] getKeyBindingContexts() {
        return new String[]{
            TEXT_EDITOR_CONTEXT,
            SQLEditorContributions.SQL_EDITOR_CONTEXT,
            SQLEditorContributions.SQL_EDITOR_SCRIPT_CONTEXT,
            IResultSetController.RESULTS_CONTEXT_ID};
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
        if (executionContextProvider != null) {
            return executionContextProvider.getExecutionContext();
        }
        if (dataSourceContainer != null && !SQLEditorUtils.isOpenSeparateConnection(dataSourceContainer)) {
            return DBUtils.getDefaultContext(getDataSource(), false);
        }
        return null;
    }

    public SQLScriptContext getGlobalScriptContext() {
        return globalScriptContext;
    }

    @Nullable
    public DBPProject getProject()
    {
        IFile file = EditorUtils.getFileFromInput(getEditorInput());
        return file == null ?
            DBWorkbench.getPlatform().getWorkspace().getActiveProject() : DBPPlatformDesktop.getInstance().getWorkspace().getProject(file.getProject());
    }

    private boolean isProjectResourceEditable() {
        if (getEditorInput() instanceof IFileEditorInput) {
            DBPProject project = this.getProject();
            return project == null || project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);
        }
        return true;
    }

    @Override
    protected boolean isReadOnly() {
        return super.isReadOnly() || !this.isProjectResourceEditable();
    }

    @Override
    public boolean isEditable() {
        return super.isEditable() && this.isProjectResourceEditable();
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
                    // ignore - this may happen if SQL was edited after execution start
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
    public boolean setDataSourceContainer(@Nullable DBPDataSourceContainer container) {
        if (!datasourceChanged && curDataSource != null) {
            datasourceChanged = true;
        }
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
                // Container was changed. Reset context provider and update input settings
                DBCExecutionContext newExecutionContext = DBUtils.getDefaultContext(container, false);
                EditorUtils.setInputDataSource(input, new SQLNavigatorContext(container, newExecutionContext));
                this.executionContextProvider = null;
            } else {
                DBCExecutionContext iec = EditorUtils.getInputExecutionContext(input);
                if (iec != null) {
                    this.executionContextProvider = () -> iec;
                }
            }

            IFile file = EditorUtils.getFileFromInput(input);
            if (file != null) {
                DBNUtils.refreshNavigatorResource(dataSourceContainer.getProject(), file, container);
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
                DBWorkbench.getPlatformUI().showError(
                    "Can't connect to database", "Connection to '" + container.getName() + "' cannot be established.", status);
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
            if (activePart != this && activePart instanceof DBPDataSourceContainerProvider) {
                inputDataSource = ((DBPDataSourceContainerProvider) activePart).getDataSourceContainer();
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
                if (executionContextProvider == null) {
                    DBPDataSourceContainer container = dataSource.getContainer();
                    if (SQLEditorUtils.isOpenSeparateConnection(container)) {
                        initSeparateConnection(dataSource, onSuccess, true);
                    } else {
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    }
                }
            }
        }
        UIUtils.asyncExec(() -> fireDataSourceChanged(null));
    }

    private void initSeparateConnection(@NotNull DBPDataSource dataSource, Runnable onSuccess, boolean readDefaultsFromInstance) {
        DBSInstance dsInstance = dataSource.getDefaultInstance();
        String[] contextDefaults = isRestoreActiveSchemaFromScript() ?
            EditorUtils.getInputContextDefaults(dataSource.getContainer(), getEditorInput()) : null;
        if (!ArrayUtils.isEmpty(contextDefaults) && contextDefaults[0] != null) {
            DBSInstance selectedInstance = DBUtils.findObject(dataSource.getAvailableInstances(), contextDefaults[0]);
            if (selectedInstance != null) {
                dsInstance = selectedInstance;
            }
        }
        {
            final OpenContextJob job = new OpenContextJob(dsInstance, onSuccess, readDefaultsFromInstance);
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
        if (project == null || document == null || document.getNumberOfLines() == 0) {
            return null;
        }

        try {
            IRegion region = document.getLineInformation(0);
            String line = document.get(region.getOffset(), region.getLength());
            Matcher matcher = EMBEDDED_BINDING_PREFIX_PATTERN.matcher(line);
            if (matcher.matches()) {
                String connSpec = matcher.group(1).trim();
                if (!CommonUtils.isEmpty(connSpec)) {
                    final DBPDataSourceContainer dataSource = DataSourceUtils.getDataSourceBySpec(
                        project,
                        connSpec,
                        null,
                        true,
                        false);
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
        if (document == null) {
            log.error("Document is null");
            return;
        }

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
                assocSpecLine.append(GeneralUtils.getDefaultLineSeparator());

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

        UIUtils.asyncExec(() -> {
            TextViewer textViewer = getTextViewer();
            if (textViewer != null) {
                textViewer.refresh();
            }
        });
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
        DBPDataSourceContainer container = ((DBPDataSourceContainerProvider) this).getDataSourceContainer();
        if (container == null) {
            DBPDataSource dataSource = getDataSource();
            if (dataSource != null) {
                container = dataSource.getContainer();
            }
        }
        if (container != null) {
            DBPPreferenceStore preferenceStore = container.getPreferenceStore();
            // First check current data source settings
            if (preferenceStore.contains(ModelPreferences.TRANSACTIONS_SMART_COMMIT)) {
                return preferenceStore.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT);
            } else {
                return container.getConnectionConfiguration().getConnectionType().isSmartCommit();
            }
        }
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT);
    }

    @Override
    public boolean isFoldingEnabled() {
        return SQLEditorUtils.isSQLSyntaxParserEnabled(getEditorInput())
            && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.FOLDING_ENABLED);
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
        topBarMan.getControl().redraw();
        bottomBarMan.getControl().redraw();
    }

    private class OpenContextJob extends AbstractJob {
        private final DBSInstance instance;
        private final Runnable onSuccess;
        private Throwable error;
        private boolean readDefaultsFromInstance;

        OpenContextJob(DBSInstance instance, Runnable onSuccess, boolean readDefaultsFromInstance) {
            super("Open connection to " + instance.getDataSource().getContainer().getName());
            this.instance = instance;
            this.onSuccess = onSuccess;
            this.readDefaultsFromInstance = readDefaultsFromInstance;
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
                String[] contextDefaultNames = null;
                if (readDefaultsFromInstance && datasourceChanged) {
                    DBCExecutionContext defaultContext = DBUtils.getDefaultContext(instance, false);
                    if (defaultContext != null) {
                        DBCExecutionContextDefaults contextDefaultsDB = defaultContext.getContextDefaults();
                        if (contextDefaultsDB != null) {
                            contextDefaultNames = new String[2];
                            contextDefaultNames[0] = contextDefaultsDB.getDefaultCatalog() != null ?
                                contextDefaultsDB.getDefaultCatalog().getName() : null;
                            contextDefaultNames[1] = contextDefaultsDB.getDefaultSchema() != null ?
                                contextDefaultsDB.getDefaultSchema().getName() : null;
                        }
                    }
                }
                if (contextDefaultNames == null) {
                    contextDefaultNames = isRestoreActiveSchemaFromScript() && instance.getDataSource() != null ?
                        EditorUtils.getInputContextDefaults(instance.getDataSource().getContainer(), getEditorInput()) : null;
                }
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
                // FIXME: silly workaround. Command state update doesn't happen in some cases
                // FIXME: but it works after short pause. Seems to be a bug in E4 command framework
                new AbstractJob("Notify context change") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        DBUtils.fireObjectSelect(instance, true);
                        return Status.OK_STATUS;
                    }
                }.schedule(200);
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
        SeparateConnectionBehavior behavior = SeparateConnectionBehavior.parse(
            getActivePreferenceStore().getString(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION)
        );
        boolean isSeparateConnection;
        switch (behavior) {
            case ALWAYS:
                isSeparateConnection = true;
                break;
            case NEVER:
                isSeparateConnection = false;
                break;
            case DEFAULT:
            default:
                isSeparateConnection = this.getDataSourceContainer() == null
                    || !this.getDataSourceContainer().isForceUseSingleConnection();
                break;
        }
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA) && isSeparateConnection;
    }

    private static class CloseContextJob extends AbstractJob {
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
    public boolean isDirty() {
        for (QueryProcessor queryProcessor : queryProcessors) {
            if (queryProcessor.isDirty() || queryProcessor.curJobRunning.get() > 0) {
                return true;
            }
        }
        if (QMUtils.isTransactionActive(executionContext)) {
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
    public IResultSetController getResultSetController() {
        if (resultTabs != null && !resultTabs.isDisposed()) {
            CTabItem activeResultsTab = getActiveResultsTab();
            if (activeResultsTab != null && UIUtils.isUIThread()) {
                Object tabControl = activeResultsTab.getData();
                if (tabControl instanceof QueryResultsContainer) {
                    return ((QueryResultsContainer) tabControl).viewer;
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> required)
    {
        if (required == INavigatorModelView.class) {
            return null;
        }
        if (required == IResultSetController.class || required == ResultSetViewer.class) {
            return required.cast(getResultSetController());
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
    public void createPartControl(Composite parent) {
        setRangeIndicator(new DefaultRangeIndicator());

        // divides editor area and results/panels area
        resultsSash = UIUtils.createPartDivider(
                this,
                parent,
                resultSetOrientation.getSashOrientation() | SWT.SMOOTH);
        resultsSash.setShowBorders(true);
        CSSUtils.setCSSClass(resultsSash, DBStyles.COLORED_BY_CONNECTION_TYPE);
        resultsSash.setSashWidth(8);

        UIUtils.setHelp(resultsSash, IHelpContextIds.CTX_SQL_EDITOR);

        Composite editorContainer;
        sqlEditorPanel = UIUtils.createPlaceholder(resultsSash, 3, 0);

        // Create left vertical toolbar
        createControlsBar(sqlEditorPanel);

        sqlExtraPanelSash = new SashForm(sqlEditorPanel, SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.verticalIndent = 5;
        sqlExtraPanelSash.setLayoutData(gd);

        // Create editor presentations sash
        Composite pPlaceholder = null;
        StackLayout presentationStackLayout = null;
        if (extraPresentationDescriptor != null) {
            presentationStack = new Composite(sqlExtraPanelSash, SWT.NONE);
            presentationStack.setLayoutData(new GridData(GridData.FILL_BOTH));
            presentationStackLayout = new StackLayout();
            presentationStack.setLayout(presentationStackLayout);
            editorContainer = presentationStack;

            pPlaceholder = new Composite(presentationStack, SWT.NONE);
            pPlaceholder.setLayout(new FillLayout());
        } else {
            editorContainer = sqlExtraPanelSash;
        }

        super.createPartControl(editorContainer);
        getEditorControlWrapper().setLayoutData(new GridData(GridData.FILL_BOTH));

        sqlExtraPanelFolder = new CTabFolder(sqlExtraPanelSash, SWT.TOP | SWT.CLOSE | SWT.FLAT);
        sqlExtraPanelFolder.setSelection(0);
        sqlExtraPanelFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                CTabItem item = sqlExtraPanelFolder.getSelection();
                if (item != null) {
                    IActionContributor ac = (IActionContributor) item.getData("actionContributor");
                    updateExtraViewToolbar(ac);
                }
            }
        });

        sqlExtraPanelToolbar = new ToolBarManager();
        sqlExtraPanelToolbar.createControl(sqlExtraPanelFolder);
        sqlExtraPanelFolder.setTopRight(sqlExtraPanelToolbar.getControl());

        restoreSashRatio(sqlExtraPanelSash, SQLPreferenceConstants.EXTRA_PANEL_RATIO);
        sqlExtraPanelSash.setMaximizedControl(sqlExtraPanelSash.getChildren()[0]);
        this.addSashRatioSaveListener(sqlExtraPanelSash, SQLPreferenceConstants.EXTRA_PANEL_RATIO);

        // Create right vertical toolbar
        createPresentationSwitchBar(sqlEditorPanel);

        if (pPlaceholder != null) {
            switch (extraPresentationDescriptor.getActivationType()) {
                case HIDDEN:
                    presentationStackLayout.topControl = presentationStack.getChildren()[SQL_EDITOR_CONTROL_INDEX];
                    break;
                case MAXIMIZED:
                case VISIBLE:
                    extraPresentation.createPresentation(pPlaceholder, this);
                    if (extraPresentationDescriptor.getActivationType() == SQLEditorPresentation.ActivationType.MAXIMIZED) {
                        if (presentationStack.getChildren()[EXTRA_CONTROL_INDEX] != null) {
                            presentationStackLayout.topControl = pPlaceholder;
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

        SourceViewer viewer = getViewer();
        if (viewer != null) {
            StyledText textWidget = viewer.getTextWidget();
            if (textWidget != null) {
                textWidget.addModifyListener(this::onTextChange);
                textWidget.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        refreshActions();
                    }
                });
            }
        }

        // Start output reader
        new ServerOutputReader().schedule();

        updateExecutionContext(null);

        // Update controls
        UIExecutionQueue.queueExec(this::onDataSourceChange);
        themeChangeListener.propertyChange(null);
    }

    protected boolean isHideQueryText() {
        return false;
    }

    private void onTextChange(ModifyEvent e) {
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CHANGE)) {
            doScriptAutoSave();
        }
    }

    private void createControlsBar(Composite sqlEditorPanel) {
        leftToolPanel = new Composite(sqlEditorPanel, SWT.LEFT);
        GridLayout panelsLayout = new GridLayout(1, true);
        panelsLayout.marginHeight = 2;
        panelsLayout.marginWidth = 1;
        panelsLayout.marginTop = 1;
        panelsLayout.marginBottom = 7;
        panelsLayout.verticalSpacing = 1;
        leftToolPanel.setLayout(panelsLayout);
        leftToolPanel.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        ToolBar topBar = new ToolBar(leftToolPanel, SWT.VERTICAL | SWT.FLAT);
        topBar.setData(VIEW_PART_PROP_NAME, this);
        topBarMan = new ToolBarManager(topBar);

        final IMenuService menuService = getSite().getService(IMenuService.class);
        if (menuService != null) {
            menuService.populateContributionManager(topBarMan, SIDE_TOP_TOOLBAR_CONTRIBUTION_ID);
        }
        topBar.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, false));
        CSSUtils.setCSSClass(topBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
        topBarMan.update(true);
        topBar.pack();

        UIUtils.createEmptyLabel(leftToolPanel, 1, 1).setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

        bottomBarMan = new ToolBarManager(SWT.VERTICAL | SWT.FLAT);
        bottomBarMan.add(ActionUtils.makeActionContribution(new ShowPreferencesAction(), false));
        if (menuService != null) {
            menuService.populateContributionManager(bottomBarMan, SIDE_BOTTOM_TOOLBAR_CONTRIBUTION_ID);
        }

        ToolBar bottomBar = bottomBarMan.createControl(leftToolPanel);
        bottomBar.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, false));
        CSSUtils.setCSSClass(bottomBar, DBStyles.COLORED_BY_CONNECTION_TYPE);

        bottomBar.pack();
        bottomBarMan.update(true);
    }

    private void createPresentationSwitchBar(Composite sqlEditorPanel) {
        if (extraPresentationDescriptor == null) {
            return;
        }

        presentationSwitchFolder = new VerticalFolder(sqlEditorPanel, SWT.RIGHT);
        presentationSwitchFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        switchPresentationSQLButton = new VerticalButton(presentationSwitchFolder, SWT.RIGHT | SWT.CHECK);
        switchPresentationSQLButton.setText(SQLEditorMessages.editors_sql_description);
        switchPresentationSQLButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCRIPT));

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
        createToggleLayoutButton();

    }

    /**
     * Sets focus in current editor.
     * This function is called on drag-n-drop and some other operations
     */
    @Override
    public boolean validateEditorInputState() {
        boolean res = super.validateEditorInputState();
        if (res) {
            SourceViewer viewer = getViewer();
            if (viewer != null) {
                StyledText textWidget = viewer.getTextWidget();
                if (textWidget != null && !textWidget.isDisposed()) {
                    textWidget.setFocus();
                }
            }
        }
        return res;
    }

    private void createResultTabs() {
        resultTabs = new CTabFolder(resultsSash, SWT.TOP | SWT.FLAT);
        CSSUtils.setCSSClass(resultTabs, DBStyles.COLORED_BY_CONNECTION_TYPE);
        resultTabsReorder = new TabFolderReorder(resultTabs);
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
        this.addSashRatioSaveListener(resultsSash, SQLPreferenceConstants.RESULTS_PANEL_RATIO);
        this.resultTabs.addListener(TabFolderReorder.ITEM_MOVE_EVENT, event -> {
            CTabItem item = (CTabItem) event.item;
            if (item.getData() instanceof QueryResultsContainer) {
                ((QueryResultsContainer) item.getData()).resultsTab = item;
            }
        });
        restoreSashRatio(resultsSash, SQLPreferenceConstants.RESULTS_PANEL_RATIO);

        TextViewer textViewer = getTextViewer();
        if (textViewer != null) {
            textViewer.getTextWidget().addTraverseListener(e -> {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT && e.stateMask == SWT.MOD1) {
                    ResultSetViewer viewer = getActiveResultSetViewer();
                    if (viewer != null && viewer.getActivePresentation().getControl().isVisible()) {
                        viewer.getActivePresentation().getControl().setFocus();
                        e.detail = SWT.TRAVERSE_NONE;
                    }
                }
            });
        }
        resultTabs.setSimple(true);
        resultTabs.setFont(JFaceResources.getFont(UIFonts.DBEAVER_FONTS_MAIN_FONT));

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
        createExtraViewControls();

        // Create results tab
        createQueryProcessor(true, true);
        if (isHideQueryText()) {
            resultsSash.setMaximizedControl(resultTabs);
        } else {
            resultsSash.setMaximizedControl(sqlEditorPanel);
        }

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
                final CTabItem activeTab = getActiveResultsTab();
                if (activeTab != null && activeTab.getData() instanceof QueryResultsContainer) {
                    final QueryResultsContainer container = (QueryResultsContainer) activeTab.getData();
                    int pinnedTabsCount = 0;
                    int resultTabsCount = 0;

                    for (CTabItem item : resultTabs.getItems()) {
                        if (item.getData() instanceof QueryResultsContainer) {
                            resultTabsCount++;
                            if (((QueryResultsContainer) item.getData()).isPinned()) {
                                pinnedTabsCount++;
                            }
                        }
                    }

                    if (activeTab.getShowClose()) {
                        manager.add(ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_SQL_EDITOR_CLOSE_TAB));

                        if (resultTabsCount - pinnedTabsCount > 1) {
                            manager.add(new Action(SQLEditorMessages.action_result_tabs_close_all_tabs) {
                                @Override
                                public void run() {
                                    closeExtraResultTabs(null, false, false);
                                }
                            });

                            manager.add(new Action(SQLEditorMessages.action_result_tabs_close_query_tabs) {
                                @Override
                                public void run() {
                                    final QueryProcessor processor = ((QueryResultsContainer) activeTab.getData()).queryProcessor;
                                    final List<CTabItem> tabs = new ArrayList<>();
                                    for (QueryResultsContainer container : processor.getResultContainers()) {
                                        if (!container.isPinned() && container.queryProcessor == processor) {
                                            tabs.add(container.getTabItem());
                                        }
                                    }
                                    for (CTabItem tab : tabs) {
                                        tab.dispose();
                                    }
                                }
                            });

                            manager.add(new Action(SQLEditorMessages.action_result_tabs_close_other_tabs) {
                                @Override
                                public void run() {
                                    final List<CTabItem> tabs = new ArrayList<>();
                                    for (CTabItem tab : resultTabs.getItems()) {
                                        if (tab.getShowClose() && tab != activeTab && !isPinned(tab)) {
                                            tabs.add(tab);
                                        }
                                    }
                                    for (CTabItem tab : tabs) {
                                        tab.dispose();
                                    }
                                    setActiveResultsContainer((QueryResultsContainer) activeTab.getData());
                                }
                            });

                            if (resultTabs.indexOf(activeTab) - pinnedTabsCount > 0) {
                                manager.add(new Action(SQLEditorMessages.action_result_tabs_close_tabs_to_the_left) {
                                    @Override
                                    public void run() {
                                        final List<CTabItem> tabs = new ArrayList<>();
                                        for (int i = 0, last = resultTabs.indexOf(activeTab); i < last; i++) {
                                            CTabItem tab = resultTabs.getItem(i);
                                            if (!isPinned(tab)) {
                                                tabs.add(tab);
                                            }
                                        }
                                        for (CTabItem tab : tabs) {
                                            tab.dispose();
                                        }
                                    }
                                });
                            }

                            if (resultTabs.indexOf(activeTab) < resultTabsCount - pinnedTabsCount - 1) {
                                manager.add(new Action(SQLEditorMessages.action_result_tabs_close_tabs_to_the_right) {
                                    @Override
                                    public void run() {
                                        final List<CTabItem> tabs = new ArrayList<>();
                                        for (int i = resultTabs.indexOf(activeTab) + 1; i < resultTabs.getItemCount(); i++) {
                                            CTabItem tab = resultTabs.getItem(i);
                                            if (!isPinned(tab)) {
                                                tabs.add(tab);
                                            }
                                        }
                                        for (CTabItem tab : tabs) {
                                            tab.dispose();
                                        }
                                    }
                                });
                            }
                        }
                    }

                    if (container.hasData()) {
                        final boolean isPinned = container.isPinned();

                        manager.add(new Separator());
                        manager.add(ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_SQL_EDITOR_TOGGLE_TAB_PINNED));

                        if (isPinned && pinnedTabsCount > 1) {
                            manager.add(new Action(SQLEditorMessages.action_result_tabs_unpin_all_tabs) {
                                @Override
                                public void run() {
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

                        manager.add(new Action(SQLEditorMessages.action_result_tabs_set_name) {
                            @Override
                            public void run() {
                                EnterNameDialog dialog = new EnterNameDialog(resultTabs.getShell(), SQLEditorMessages.action_result_tabs_set_name_title, activeTab.getText());
                                if (dialog.open() == IDialogConstants.OK_ID) {
                                    container.setTabName(dialog.getResult());
                                }
                            }
                        });

                        manager.add(new Action(SQLEditorMessages.action_result_tabs_detach_tab) {
                            @Override
                            public void run() {
                                container.detach();
                            }
                        });

                        if (container.getQuery() != null) {
                            manager.add(new Separator());
                            AssignVariableAction action = new AssignVariableAction(SQLEditor.this, container.getQuery().getText());
                            action.setEditable(false);
                            manager.add(action);
                        }
                    }
                }
                manager.add(new Separator());
                manager.add(ActionUtils.makeCommandContribution(getSite(), SQLEditorCommands.CMD_SQL_EDITOR_MAXIMIZE_PANEL));
            });
            menuMgr.setRemoveAllWhenShown(true);
            resultTabs.setMenu(menu);
        }
    }

    private void addSashRatioSaveListener(SashForm sash, String prefId) {
        Control control = sash.getChildren()[0];
        control.addListener(SWT.Resize, event -> {
            if (!control.isDisposed()) {
                int[] weights = sash.getWeights();
                IPreferenceStore prefs = getPreferenceStore();
                if (prefs != null && weights.length == 2) {
                    prefs.setValue(prefId, weights[0] + "-" + weights[1]);
                }
            }
        });
    }

    private void restoreSashRatio(SashForm sash, String prefId) {
        String resultsPanelRatio = getPreferenceStore().getString(prefId);
        if (!CommonUtils.isEmpty(resultsPanelRatio)) {
            String[] weightsStr = resultsPanelRatio.split("-");
            if (weightsStr.length > 1) {
                int[] weights = {
                    CommonUtils.toInt(weightsStr[0]),
                    CommonUtils.toInt(weightsStr[1]),
                };
                // If weight of one of controls less than 5% of weight of another - restore default wqeights
                if (weights[1] < weights[0] / 15 || weights[0] < weights[1] / 15) {
                    log.debug("Restore default sash weights");
                } else {
                    sash.setWeights(weights);
                }
            }
        }
    }

    private void setActiveResultsContainer(QueryResultsContainer data) {
        curResultsContainer = data;
        curQueryProcessor = curResultsContainer.queryProcessor;
    }

    private boolean isPinned(CTabItem tabItem) {
        if (tabItem.getData() instanceof QueryResultsContainer) {
            return ((QueryResultsContainer) tabItem.getData()).isPinned();
        }
        return false;
    }

    /////////////////////////////////////////////////////////////
    // Panels

    public void toggleExtraPanelsLayout() {
        CTabItem outTab = getExtraViewTab(outputViewer);
        CTabItem logTab = getExtraViewTab(logViewer);
        CTabItem varTab = getExtraViewTab(variablesViewer);
        if (outTab != null) outTab.dispose();
        if (logTab != null) logTab.dispose();
        if (varTab != null) varTab.dispose();

        IPreferenceStore preferenceStore = getPreferenceStore();
        String epLocation = getExtraPanelsLocation();
        if (SQLPreferenceConstants.LOCATION_RESULTS.equals(epLocation)) {
            epLocation = SQLPreferenceConstants.LOCATION_RIGHT;
        } else {
            epLocation = SQLPreferenceConstants.LOCATION_RESULTS;
        }
        preferenceStore.setValue(SQLPreferenceConstants.EXTRA_PANEL_LOCATION, epLocation);

        createExtraViewControls();

        if (outTab != null) showOutputPanel();
        if (logTab != null) showExecutionLogPanel();
        if (varTab != null) showVariablesPanel();
    }

    public String getExtraPanelsLocation() {
        return getPreferenceStore().getString(SQLPreferenceConstants.EXTRA_PANEL_LOCATION);
    }

    private void createExtraViewControls() {
        if (logViewer != null) {
            logViewer.dispose();
            logViewer = null;
        }
        if (variablesViewer != null) {
            variablesViewer.dispose();
            variablesViewer = null;
        }
        if (outputViewer != null) {
            outputViewer.dispose();
            outputViewer = null;
        }
        if (sqlExtraPanelFolder != null) {
            for (CTabItem ti : sqlExtraPanelFolder.getItems()) {
                ti.dispose();
            }
        }

        //planView = new ExplainPlanViewer(this, resultTabs);
        CTabFolder folder = getFolderForExtraPanels();

        logViewer = new SQLLogPanel(folder, this);
        variablesViewer = new SQLVariablesPanel(folder, this);
        outputViewer = new SQLEditorOutputViewer(getSite(), folder, SWT.LEFT);
        outputViewer.setExecutionContext(executionContext);

        if (getFolderForExtraPanels() != sqlExtraPanelFolder) {
            sqlExtraPanelSash.setMaximizedControl(sqlExtraPanelSash.getChildren()[0]);
        }
    }

    public CTabFolder getResultTabsContainer() {
        return resultTabs;
    }

    private CTabFolder getFolderForExtraPanels() {
        CTabFolder folder = this.sqlExtraPanelFolder;
        String epLocation = getExtraPanelsLocation();
        if (SQLPreferenceConstants.LOCATION_RESULTS.equals(epLocation)) {
            folder = resultTabs;
        }
        return folder;
    }

    private CTabItem getExtraViewTab(Control control) {
        CTabFolder tabFolder = this.getFolderForExtraPanels();
        for (CTabItem item : tabFolder.getItems()) {
            if (item.getData() == control) {
                return item;
            }
        }
        return null;
    }

    private void showExtraView(final String commandId, String name, String toolTip, Image image, Control view, IActionContributor actionContributor) {
        ToolItem viewItem = getViewToolItem(commandId);
        if (viewItem == null) {
            log.warn("Tool item for command " + commandId + " not found");
            return;
        }
        CTabFolder tabFolder = this.getFolderForExtraPanels();
        CTabItem curItem = getExtraViewTab(view);
        if (curItem != null) {
            // Close tab if it is already open
            viewItem.setSelection(false);
            curItem.dispose();
            return;
        }

        boolean isTabsToTheRight = tabFolder == sqlExtraPanelFolder;

        if (isTabsToTheRight) {
            if (sqlExtraPanelSash.getMaximizedControl() != null) {
                sqlExtraPanelSash.setMaximizedControl(null);
            }
        } else {
            sqlExtraPanelSash.setMaximizedControl(sqlExtraPanelSash.getChildren()[0]);
            // Show results
            showResultsPanel(true);
        }

        if (view == outputViewer) {
            updateOutputViewerIcon(false);
            outputViewer.resetNewOutput();
        }
        // Create new tab
        viewItem.setSelection(true);

        CTabItem item = new CTabItem(tabFolder, SWT.CLOSE);
        item.setControl(view);
        item.setText(name);
        item.setToolTipText(toolTip);
        item.setImage(image);
        item.setData(view);
        item.setData("actionContributor", actionContributor);
        // De-select tool item on tab close
        item.addDisposeListener(e -> {
            if (!viewItem.isDisposed()) {
                viewItem.setSelection(false);
            }
            if (tabFolder.getItemCount() == 0) {
                sqlExtraPanelSash.setMaximizedControl(sqlExtraPanelSash.getChildren()[0]);
            }
        });
        tabFolder.setSelection(item);

        if (isTabsToTheRight) {
            updateExtraViewToolbar(actionContributor);
        }
    }

    private void updateExtraViewToolbar(IActionContributor actionContributor) {
        // Update toolbar
        sqlExtraPanelToolbar.removeAll();
        if (actionContributor != null) {
            actionContributor.contributeActions(sqlExtraPanelToolbar);
        }
        sqlExtraPanelToolbar.add(ActionUtils.makeCommandContribution(
            getSite(),
            "org.jkiss.dbeaver.ui.editors.sql.toggle.extraPanels",
            CommandContributionItem.STYLE_CHECK,
            UIIcon.ARROW_DOWN));
        sqlExtraPanelToolbar.update(true);
    }

    @Nullable
    private ToolItem getViewToolItem(@NotNull String commandId) {
        ToolItem viewItem = findViewItemByCommandId(topBarMan, commandId);
        if (viewItem == null) {
            viewItem = findViewItemByCommandId(bottomBarMan, commandId);
        }
        return viewItem;
    }
    
    @Nullable
    private ToolItem findViewItemByCommandId(@NotNull ToolBarManager toolbarManager, @NotNull String commandId) {
        for (ToolItem item : toolbarManager.getControl().getItems()) {
            Object data = item.getData();
            if (data instanceof CommandContributionItem) {
                ParameterizedCommand cmd = ((CommandContributionItem) data).getCommand(); 
                if (cmd != null && commandId.equals(cmd.getId())) {
                    return item;
                }
            } else if (data instanceof HandledContributionItem) {
                MHandledItem model = ((HandledContributionItem) data).getModel();
                if (model != null ) {
                    ParameterizedCommand cmd = model.getWbCommand();
                    if (cmd != null && commandId.equals(cmd.getId())) {
                        return item;
                    }
                }
            }
        }
        return null;
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
    
    /**
     * Toggle isPinned value of active tab container,
     * then move tab to left of all unpinned tabs if pinning,
     * or move tab to right of all pinned tabs if unpinning
     */
    public void toggleActiveTabPinned() {
        CTabItem activeTab = getActiveResultsTab();
        QueryResultsContainer container = (QueryResultsContainer) activeTab.getData();
        
        if (!container.hasData()) {
            return;
        }

        boolean isPinned = container.isPinned();

        container.setPinned(!isPinned);

        CTabItem currTabItem = activeTab;
        CTabItem nextTabItem;

        if (isPinned) {
            for (int i = resultTabs.indexOf(activeTab) + 1; i < resultTabs.getItemCount(); i++) {
                nextTabItem = resultTabs.getItem(i);
                if (nextTabItem.getShowClose()) {
                    break;
                }
                resultTabsReorder.swapTabs(currTabItem, nextTabItem);
                currTabItem = nextTabItem;
            }
        } else {
            for (int i = resultTabs.indexOf(activeTab) - 1; i >= 0; i--) {
                nextTabItem = resultTabs.getItem(i);
                if (!nextTabItem.getShowClose()) {
                    break;
                }
                resultTabsReorder.swapTabs(currTabItem, nextTabItem);
                currTabItem = nextTabItem;
            }
        }

    }

    /**
     * Return true if there is an active tab, and its container is pinned
     */
    public boolean isActiveTabPinned() {
        CTabItem tabItem = getActiveResultsTab();
        return tabItem != null && ((QueryResultsContainer) tabItem.getData()).isPinned();
    }

    public void showOutputPanel() {
        showExtraView(
            SQLEditorCommands.CMD_SQL_SHOW_OUTPUT,
            SQLEditorMessages.editors_sql_output,
            SQLEditorMessages.editors_sql_output_tip,
            IMG_OUTPUT,
            outputViewer,
            manager -> manager.add(new OutputAutoShowToggleAction()));
    }

    public void showExecutionLogPanel() {
        showExtraView(
            SQLEditorCommands.CMD_SQL_SHOW_LOG,
            SQLEditorMessages.editors_sql_execution_log,
            SQLEditorMessages.editors_sql_execution_log_tip,
            IMG_LOG,
            logViewer,
            null);
    }

    public void showVariablesPanel() {
        showExtraView(
            SQLEditorCommands.CMD_SQL_SHOW_VARIABLES,
            SQLEditorMessages.editors_sql_variables,
            SQLEditorMessages.editors_sql_variables_tip,
            IMG_VARIABLES,
            variablesViewer,
            null);
        UIUtils.asyncExec(() -> variablesViewer.refreshVariables());
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
                setResultTabSelection(item);
                return true;
            }
        }
        return false;
    }

    private void setResultTabSelection(CTabItem item) {
        if (isResultSetAutoFocusEnabled || !(item.getData() instanceof QueryResultsContainer) || resultTabs.getItemCount() == 1) {
            resultTabs.setSelection(item);
        }
    }

    public SQLEditorPresentationPanel showPresentationPanel(String panelID) {
        for (IContributionItem contributionItem : topBarMan.getItems()) {
            if (contributionItem instanceof ActionContributionItem) {
                IAction action = ((ActionContributionItem) contributionItem).getAction();
                if (action instanceof PresentationPanelToggleAction
                    && ((PresentationPanelToggleAction) action).panel.getId().equals(panelID)
                ) {
                    action.run();
                    return extraPresentationCurrentPanel;
                }
            }
        }
        for (IContributionItem contributionItem : bottomBarMan.getItems()) {
            if (contributionItem instanceof ActionContributionItem) {
                IAction action = ((ActionContributionItem) contributionItem).getAction();
                if (action instanceof PresentationPanelToggleAction
                    && ((PresentationPanelToggleAction) action).panel.getId().equals(panelID)
                ) {
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
        if (extraPresentation == null || presentationStack == null) {
            return SQLEditorPresentation.ActivationType.HIDDEN;
        }
        Control maximizedControl = ((StackLayout)presentationStack.getLayout()).topControl;
        if (maximizedControl == getExtraPresentationControl()) {
            return SQLEditorPresentation.ActivationType.MAXIMIZED;
        } else if (maximizedControl == getEditorControlWrapper()) {
            return SQLEditorPresentation.ActivationType.HIDDEN;
        } else {
            return SQLEditorPresentation.ActivationType.VISIBLE;
        }
    }

    public void showExtraPresentation(boolean show, boolean maximize) {
        if (extraPresentationDescriptor == null || presentationStack == null) {
            return;
        }
        resultsSash.setRedraw(false);
        try {
            StackLayout stackLayout = (StackLayout) presentationStack.getLayout();
            if (!show) {
                //boolean epHasFocus = UIUtils.hasFocus(getExtraPresentationControl());
                stackLayout.topControl = presentationStack.getChildren()[SQL_EDITOR_CONTROL_INDEX];
                //if (epHasFocus) {
                    getEditorControlWrapper().setFocus();
                //}
                // Set selection provider back to the editor
                getSite().setSelectionProvider(new DynamicSelectionProvider());
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
                getSite().setSelectionProvider(extraPresentation.getSelectionProvider());
                if (maximize) {
                    stackLayout.topControl = getExtraPresentationControl();
                    getExtraPresentationControl().setFocus();
                } else {
                    stackLayout.topControl = null;
                }
            }

            // Show presentation panels
            boolean sideBarChanged = false;
            if (getExtraPresentationState() == SQLEditorPresentation.ActivationType.HIDDEN) {
                // Remove all presentation panel toggles
                //for (SQLPresentationPanelDescriptor panelDescriptor : extraPresentationDescriptor.getPanels()) {
                    for (Control vb : presentationSwitchFolder.getChildren()) {
                        if (vb.getData() instanceof SQLPresentationPanelDescriptor) { // || vb instanceof Label
                            vb.dispose();
                            sideBarChanged = true;
                        }
                    }
                //}
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
                    removeToggleLayoutButton();
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
                    createToggleLayoutButton();
                }
            }

            boolean isExtra = getExtraPresentationState() == SQLEditorPresentation.ActivationType.MAXIMIZED;
            switchPresentationSQLButton.setChecked(!isExtra);
            switchPresentationExtraButton.setChecked(isExtra);
            presentationSwitchFolder.redraw();

            if (sideBarChanged) {
                topBarMan.update(true);
                bottomBarMan.update(true);
                topBarMan.getControl().getParent().layout(true);
                bottomBarMan.getControl().getParent().layout(true);
            }

            presentationStack.layout(true, true);
        } finally {
            resultsSash.setRedraw(true);
        }
    }

    private void createToggleLayoutButton() {
        VerticalButton.create(presentationSwitchFolder, SWT.RIGHT | SWT.CHECK, getSite(), SQLEditorCommands.CMD_TOGGLE_LAYOUT, false);
    }

    private void removeToggleLayoutButton() {
        for (VerticalButton vButton : presentationSwitchFolder.getItems()) {
            if (vButton.getCommandId() != null && vButton.getCommandId().equals(SQLEditorCommands.CMD_TOGGLE_LAYOUT)) {
                vButton.dispose();
            }
        }
    }

    private Control getExtraPresentationControl() {
        return presentationStack == null ? null : presentationStack.getChildren()[EXTRA_CONTROL_INDEX];
    }

    public void toggleResultPanel(boolean switchFocus, boolean createQueryProcessor) {
        if (isHideQueryText()) {
            return;
        }
        UIUtils.syncExec(() -> {
            if (resultsSash.getMaximizedControl() == null) {
                resultsSash.setMaximizedControl(sqlEditorPanel);
                switchFocus(false);
            } else {
                // Show both editor and results
                // Check for existing query processors (maybe all result tabs were closed)
                if (resultTabs.getItemCount() == 0 && createQueryProcessor) {
                    createQueryProcessor(true, true);
                }

                resultsSash.setMaximizedControl(null);

                if (switchFocus) {
                    switchFocus(true);
                }
            }
        });
    }

    /**
     * Toggles editor/results maximization
     */
    public void toggleEditorMaximize() {
        if (isHideQueryText()) {
            return;
        }
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
            if (!isHideQueryText() && resultsSash.getMaximizedControl() != null) {
                resultsSash.setMaximizedControl(null);
            }
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
                setResultTabSelection(tabItem);
            } else {
                for (CTabItem tabItem : resultTabs.getItems()) {
                    if (tabItem.getData() == panelInstance) {
                        setResultTabSelection(tabItem);
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

        SQLScriptContext parentContext = null;
        {
            DatabaseEditorContext parentEditorContext = EditorUtils.getEditorContext(editorInput);
            if (parentEditorContext instanceof SQLNavigatorContext) {
                parentContext = ((SQLNavigatorContext) parentEditorContext).getScriptContext();
            }
        }
        this.globalScriptContext = new SQLScriptContext(
            parentContext,
            this,
            EditorUtils.getLocalFileFromInput(getEditorInput()),
            new OutputLogWriter(),
            new SQLEditorParametersProvider(getSite()));

        this.globalScriptContext.addListener(new DBCScriptContextListener() {
            @Override
            public void variableChanged(ContextAction action, DBCScriptContext.VariableInfo variable) {
                saveContextVariables();
            }
            @Override
            public void parameterChanged(ContextAction action, String name, Object value) {
                saveContextVariables();
            }
            private void saveContextVariables() {
                new AbstractJob("Save variables") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        DBPDataSourceContainer ds = getDataSourceContainer();
                        if (ds != null) {
                            globalScriptContext.saveVariables(ds.getDriver(), null);
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule(200);
            }

        });

        // Initialize the add-ins and keep references for further cleanup on editor dispose
        for (SQLEditorAddInDescriptor addInDesc : SQLEditorAddInsRegistry.getInstance().getAddIns()) {
            try {
                SQLEditorAddIn addIn = addInDesc.createInstance();
                addIn.init(this);
                addIns.add(addIn);
            } catch (Throwable ex) {
                log.error("Error during SQL editor add-in initialilzation", ex); //$NON-NLS-1$
            }
        }
    }

    /**
     * Obtain the add-in instance by its concrete type
     */
    @Nullable
    public <T extends SQLEditorAddIn> T findAddIn(@NotNull Class<T> addInClass) {
        for (SQLEditorAddIn addIn : addIns) { // we are ok with brute-force until there are not many of add-ins
            if (addInClass.isInstance(addIn)) {
                @SuppressWarnings("unchecked")
                T concreteAddIn = (T) addIn;
                return concreteAddIn;
            }
        }
        return null;
    }

    @Override
    protected void doSetInput(IEditorInput editorInput) throws CoreException {
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

        IEditorInput finalEditorInput = editorInput;
        Runnable inputinitializer = () -> {
            DBPDataSourceContainer oldDataSource = SQLEditor.this.getDataSourceContainer();
            DBPDataSourceContainer newDataSource = EditorUtils.getInputDataSource(SQLEditor.this.getEditorInput());

            if (oldDataSource != newDataSource) {
                SQLEditor.this.dataSourceContainer = null;
                SQLEditor.this.updateDataSourceContainer();
            } else {
                SQLEditor.this.reloadSyntaxRules();
            }

            {
                DBPDataSourceContainer dataSource = EditorUtils.getInputDataSource(finalEditorInput);
                SQLEditorFeatures.SQL_EDITOR_OPEN.use(Map.of(
                    "driver", dataSource == null ? "" : dataSource.getDriver().getPreconfiguredId()
                ));
            }
        };
        if (isNonPersistentEditor()) {
            inputinitializer.run();
        } else {
            // Run in queue - for app startup
            UIExecutionQueue.queueExec(inputinitializer);
        }

        setPartName(getEditorName());
        if (isNonPersistentEditor() && isDetectTitleImageFromInput()) {
            setTitleImage(DBeaverIcons.getImage(UIIcon.SQL_CONSOLE));
        }
        baseEditorImage = getTitleImage();
        editorImage = new Image(Display.getCurrent(), baseEditorImage, SWT.IMAGE_COPY);
    }

    protected boolean isDetectTitleImageFromInput() {
        return true;
    }

    @Override
    public String getTitleToolTip() {
        if (!DBWorkbench.getPlatform().getApplication().isStandalone()) {
            // For Eclipse plugins return just title because it is used in main window title.
            return getTitle();
        }
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

        StringBuilder tip = new StringBuilder();
        tip
            .append(NLS.bind(SQLEditorMessages.sql_editor_title_tooltip_path, scriptPath))
            .append("\n").append(NLS.bind(SQLEditorMessages.sql_editor_title_tooltip_connecton, dataSourceContainer.getName()))
            .append("\n").append(NLS.bind(SQLEditorMessages.sql_editor_title_tooltip_type, dataSourceContainer.getDriver().getFullName()))
            .append("\n").append(NLS.bind(SQLEditorMessages.sql_editor_title_tooltip_url, dataSourceContainer.getConnectionConfiguration().getUrl()));

        SQLEditorVariablesResolver scriptNameResolver = new SQLEditorVariablesResolver(dataSourceContainer,
                dataSourceContainer.getConnectionConfiguration(),
                getExecutionContext(),
                scriptPath,
                null,
                getProject());
        if (scriptNameResolver.get(SQLPreferenceConstants.VAR_ACTIVE_DATABASE) != null) {
            tip.append("\n").append(NLS.bind(SQLEditorMessages.sql_editor_title_tooltip_database, scriptNameResolver.get(SQLPreferenceConstants.VAR_ACTIVE_DATABASE)));
        }
        if (scriptNameResolver.get(SQLPreferenceConstants.VAR_ACTIVE_SCHEMA) != null) {
            tip.append("\n").append(NLS.bind(SQLEditorMessages.sql_editor_title_tooltip_schema, scriptNameResolver.get(SQLPreferenceConstants.VAR_ACTIVE_SCHEMA)));
        }
        EditorUtils.appendProjectToolTip(tip, getProject());

        if (dataSourceContainer.getConnectionError() != null) {
            tip.append("\n\nConnection error:\n").append(dataSourceContainer.getConnectionError());
        }
        return tip.toString();
    }

    protected String getEditorName() {
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


        DBPPreferenceStore preferenceStore = getActivePreferenceStore();
        String pattern = preferenceStore.getString(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN);
        return GeneralUtils.replaceVariables(pattern, new SQLEditorVariablesResolver(
                dataSourceContainer,
                null,
                getExecutionContext(),
                scriptName,
                file,
                getProject()));
    }

    @Override
    public void setFocus() {
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
        showResultsPanel(false);
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

    private void showResultsPanel(boolean createQueryProcessor) {
        if (resultsSash.getMaximizedControl() != null) {
            toggleResultPanel(false, createQueryProcessor);
        }
        UIUtils.syncExec(() -> {
            if (resultsSash.isDownHidden()) {
                resultsSash.showDown();
            }
        });
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
                        setResultTabSelection(item);
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
                // Prepare query for tooltip
                String preparedText = sqlQuery.getText().replaceAll("[\n\r\t]{3,}", "");
                if (preparedText.length() > 300) {
                    item.setToolTipText(preparedText.substring(0, 300) + "...");
                } else {
                    item.setToolTipText(preparedText);
                }
            }
            item.setImage(IMG_EXPLAIN_PLAN);
            item.setData(planView);
            item.addDisposeListener(resultTabDisposeListener);
            UIUtils.disposeControlOnItemDispose(item);
            setResultTabSelection(item);
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
            processQueries(Collections.singletonList(planQuery), false, true, false, true, null, null);
        }
    }

    public void processSQL(boolean newTab, boolean script) {
        processSQL(newTab, script, null, null);
    }

    public boolean processSQL(boolean newTab, boolean script, SQLQueryTransformer transformer, @Nullable SQLQueryListener queryListener) {
        return processSQL(newTab, script, false, transformer, queryListener);
    }

    public boolean processSQL(boolean newTab, boolean script, boolean executeFromPosition) {
        return processSQL(newTab, script, executeFromPosition, null, null);
    }

    public boolean processSQL(boolean newTab, boolean script, boolean executeFromPosition, SQLQueryTransformer transformer,
        @Nullable SQLQueryListener queryListener
    ) {
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
            if (executeFromPosition) {
                // Get all queries from the current position
                ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
                elements = extractScriptQueries(selection.getOffset(), document.getLength(), true, false, true);
                // Replace first query with query under cursor for case if the cursor is in the middle of the query
                elements.remove(0);
                elements.add(0, extractActiveQuery());
            } else {
                // Execute all SQL statements consequently
                ITextSelection selection = (ITextSelection) getSelectionProvider().getSelection();
                if (selection.getLength() > 1) {
                    elements = extractScriptQueries(selection.getOffset(), selection.getLength(), true, false, true);
                } else {
                    elements = extractScriptQueries(0, document.getLength(), true, false, true);
                }
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
            return processQueries(elements, script, newTab, false, true, queryListener, null);
        } else {
            return false;
        }
    }

    public void exportDataFromQuery(@Nullable SQLScriptContext sqlScriptContext)
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
            processQueries(elements, false, false, true, true, null, sqlScriptContext);
        } else {
            DBWorkbench.getPlatformUI().showError(
                    "Extract data",
                    "Choose one or more queries to export from");
        }
    }

    public boolean processQueries(@NotNull final List<SQLScriptElement> queries, final boolean forceScript,
        boolean newTab, final boolean export, final boolean checkSession,
        @Nullable final SQLQueryListener queryListener, @Nullable final SQLScriptContext context
    ) {
        if (queries.isEmpty()) {
            // Nothing to process
            return false;
        }

        final DBPDataSourceContainer container = getDataSourceContainer();
        if (checkSession) {
            try {
                boolean finalNewTab = newTab;
                DBRProgressListener connectListener = status -> {
                    if (!status.isOK() || container == null || !container.isConnected()) {
                        DBWorkbench.getPlatformUI().showError(
                            SQLEditorMessages.editors_sql_error_cant_obtain_session,
                            null,
                            status);
                        return;
                    }
                    updateExecutionContext(() -> UIUtils.syncExec(() ->
                        processQueries(queries, forceScript, finalNewTab, export, false, queryListener, context)));
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

        SQLScriptContext scriptContext = context;
        if (scriptContext == null) {
            scriptContext = createScriptContext();
        }

        final boolean isSingleQuery = !forceScript && (queries.size() == 1);
        if (isSingleQuery && queries.get(0) instanceof SQLQuery) {
            SQLQuery query = (SQLQuery) queries.get(0);
            boolean isDropTable = query.isDropTableDangerous();
            if (query.isDeleteUpdateDangerous() || isDropTable) {
                String targetName = "multiple tables";
                if (query.getEntityMetadata(false) != null) {
                    targetName = query.getEntityMetadata(false).getEntityName();
                }
                if (ConfirmationDialog.confirmAction(
                    getSite().getShell(),
                    ConfirmationDialog.WARNING, isDropTable ? SQLPreferenceConstants.CONFIRM_DROP_SQL : SQLPreferenceConstants.CONFIRM_DANGER_SQL,
                    ConfirmationDialog.CONFIRM,
                    query.getType().name(),
                    targetName) != IDialogConstants.OK_ID)
                {
                    return false;
                }
            }
        } else if (newTab && queries.size() > MAX_PARALLEL_QUERIES_NO_WARN) {
            if (ConfirmationDialog.confirmAction(
                getSite().getShell(),
                ConfirmationDialog.WARNING, SQLPreferenceConstants.CONFIRM_MASS_PARALLEL_SQL,
                ConfirmationDialog.CONFIRM,
                queries.size()) != IDialogConstants.OK_ID)
            {
                return false;
            }
        }


        if (!isHideQueryText() && resultsSash.getMaximizedControl() != null) {
            resultsSash.setMaximizedControl(null);
        }

        // Save editor
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE) && isDirty()) {
            doSave(new NullProgressMonitor());
        }

        // Clear server console output
        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE)) {
            outputViewer.clearOutput();
        }

        boolean replaceCurrentTab = getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESULT_SET_REPLACE_CURRENT_TAB);

        if (!export) {
            // We only need to prompt user to close extra (unpinned) tabs if:
            // 1. The user is not executing query in a new tab
            // 2. The user is executing script that may open several result sets
            //    and replace current tab on single query execution option is not set
            if (isResultSetAutoFocusEnabled && !newTab && (!isSingleQuery || (isSingleQuery && !replaceCurrentTab))) {
                int tabsClosed = closeExtraResultTabs(null, true, false);
                if (tabsClosed == IDialogConstants.CANCEL_ID) {
                    return false;
                } else if (tabsClosed == IDialogConstants.NO_ID) {
                    newTab = true;
                }
            }

            // Create new query processor if:
            // 1. New tab is explicitly requested
            // 1. Or all tabs are closed and no query processors are present
            // 2. Or current query processor has pinned tabs
            // 3. Or current query processor has running jobs
            if (newTab || queryProcessors.isEmpty() || curQueryProcessor.hasPinnedTabs() || curQueryProcessor.getRunningJobs() > 0) {
                boolean foundSuitableTab = false;

                // Try to find suitable query processor among exiting ones if:
                // 1. New tab is not required
                // 2. The user is executing only single query
                if (!newTab && isSingleQuery) {
                    for (QueryProcessor processor : queryProcessors) {
                        if (!processor.hasPinnedTabs() && processor.getRunningJobs() == 0) {
                            foundSuitableTab = true;
                            curQueryProcessor = processor;
                            break;
                        }
                    }
                }

                // Just create a new query processor
                if (!foundSuitableTab) {
                    createQueryProcessor(true, false);
                }
            }

            // Close all extra tabs of this query processor
            // if the user is executing only single query
            if (!newTab && isSingleQuery && curQueryProcessor.getResultContainers().size() > 1) {
                closeExtraResultTabs(curQueryProcessor, false, true);
            }

            CTabItem tabItem = curQueryProcessor.getFirstResults().getTabItem();

            if (tabItem != null) {
                // Do not switch tab if Output tab is active
                CTabItem selectedTab = resultTabs.getSelection();
                if (selectedTab == null || selectedTab.getData() != outputViewer) {
                    setResultTabSelection(tabItem);
                }
            }
        }

        if (curQueryProcessor == null) {
            createQueryProcessor(true, true);
        }

        return curQueryProcessor.processQueries(
            scriptContext,
            queries,
            forceScript,
            false,
            export,
            !export && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR),
            queryListener);
    }

    public boolean isActiveQueryRunning() {
        return curQueryProcessor != null && curQueryProcessor.curJobRunning.get() > 0;
    }

    public void cancelActiveQuery() {
        if (isActiveQueryRunning()) {
            curQueryProcessor.cancelJob();
        }
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

    private int closeExtraResultTabs(@Nullable QueryProcessor queryProcessor, boolean confirmClose, boolean keepFirstTab) {
        List<CTabItem> tabsToClose = new ArrayList<>();
        for (CTabItem item : resultTabs.getItems()) {
            if (item.getData() instanceof QueryResultsContainer && item.getShowClose() && !isPinned(item)) {
                QueryResultsContainer resultsProvider = (QueryResultsContainer)item.getData();
                if (queryProcessor != null && queryProcessor != resultsProvider.queryProcessor) {
                    continue;
                }
                if (queryProcessor != null && queryProcessor.resultContainers.size() < 2 && keepFirstTab) {
                    // Do not remove first tab for this processor
                    continue;
                }
                tabsToClose.add(item);
            } else if (item.getData() instanceof ExplainPlanViewer) {
                tabsToClose.add(item);
            }
        }
        if (tabsToClose.size() > 1 || (tabsToClose.size() == 1 && keepFirstTab)) {
            int confirmResult = IDialogConstants.YES_ID;
            if (confirmClose) {
                confirmResult = ConfirmationDialog.confirmAction(
                    getSite().getShell(),
                    ConfirmationDialog.WARNING,
                    SQLPreferenceConstants.CONFIRM_RESULT_TABS_CLOSE,
                    ConfirmationDialog.QUESTION_WITH_CANCEL,
                    tabsToClose.size()
                );
                if (confirmResult == IDialogConstants.CANCEL_ID || confirmResult < 0) {
                    return IDialogConstants.CANCEL_ID;
                }
            }
            if (confirmResult == IDialogConstants.YES_ID) {
                for (int i = 0; i < tabsToClose.size(); i++) {
                    if (i == 0 && keepFirstTab) {
                        continue;
                    }
                    tabsToClose.get(i).dispose();
                }
            }
            return confirmResult;
        }
        // No need to close anything
        return IDialogConstants.IGNORE_ID;
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
        if (dataSource != null && executionContextProvider == null && SQLEditorUtils.isOpenSeparateConnection(ds) && executionContext == null) {
            initSeparateConnection(dataSource, () -> onFinish.onTaskFinished(Status.OK_STATUS), false);
            return executionContext != null;
        }
        return true;
    }

    /**
     * Handles datasource change action in UI
     */
    private void fireDataSourceChange() {
        updateExecutionContext(null);
        UIUtils.syncExec(this::onDataSourceChange);
    }

    protected void onDataSourceChange() {
        if (resultsSash == null || resultsSash.isDisposed()) {
            reloadSyntaxRules();
            return;
        }

        DBPDataSourceContainer dsContainer = getDataSourceContainer();

        if (resultTabs != null) {
            DatabaseEditorUtils.setPartBackground(this, resultTabs);
            Color bgColor = dsContainer == null ? null : UIUtils.getConnectionColor(dsContainer.getConnectionConfiguration());
            resultsSash.setBackground(bgColor);
            topBarMan.getControl().setBackground(bgColor);
            bottomBarMan.getControl().setBackground(bgColor);
        }

        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            EditorUtils.setInputDataSource(getEditorInput(), new SQLNavigatorContext(executionContext));
        }
        refreshActions();

        refreshEditorIconAndTitle();

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

        if (!isHideQueryText()) {
            if (dsContainer == null) {
                resultsSash.setMaximizedControl(sqlEditorPanel);
            } else {
                if (curQueryProcessor != null && curQueryProcessor.getFirstResults().hasData()) {
                    resultsSash.setMaximizedControl(null);
                }
            }
        }

        lastExecutionContext = executionContext;
        syntaxLoaded = true;

        loadActivePreferenceSettings();

        if (dsContainer != null) {
            globalScriptContext.loadVariables(dsContainer.getDriver(), null);
        } else {
            globalScriptContext.clearVariables();
        }

        if (outputViewer != null && executionContext != null) {
            outputViewer.setExecutionContext(executionContext);
        }
    }

    /**
     * Build and update icon and title
     */
    public void refreshEditorIconAndTitle() {
        DBPDataSourceContainer dsContainer = getDataSourceContainer();
        setPartName(getEditorName());

        // Update icon
        if (editorImage != null) {
            editorImage.dispose();
        }

        DBPImage bottomLeft;
        DBPImage bottomRight;

        if (executionContext == null) {
            if (dsContainer instanceof DBPStatefulObject && ((DBPStatefulObject) dsContainer).getObjectState() == DBSObjectState.INVALID) {
                bottomRight = DBIcon.OVER_ERROR;
            } else {
                bottomRight = null;
            }
        } else {
            bottomRight = DBIcon.OVER_SUCCESS;
        }

        if (SQLEditorUtils.isSQLSyntaxParserApplied(getEditorInput())) {
            bottomLeft = null;
        } else {
            bottomLeft = DBIcon.OVER_RED_LAMP;
        }

        if (bottomLeft != null || bottomRight != null) {
            DBPImage image = new DBIconComposite(new DBIconBinary(null, baseEditorImage), false, null, null, bottomLeft, bottomRight);
            editorImage = DBeaverIcons.getImage(image, false);
        } else {
            editorImage = new Image(Display.getCurrent(), baseEditorImage, SWT.IMAGE_COPY);
        }
        setTitleImage(editorImage);
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
    public void dispose() {
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

        { // Clean up the add-ins in reverse order
            ListIterator<SQLEditorAddIn> addInsIterator = addIns.listIterator(addIns.size());
            while (addInsIterator.hasPrevious()) {
                SQLEditorAddIn addIn = addInsIterator.previous();
                try {
                    addIn.cleanup(this);
                } catch (Throwable ex) {
                    log.error("Error during SQL editor add-in cleanup", ex); //$NON-NLS-1$
                }
            }
        }

        super.dispose();

        if (sqlFile != null && !PlatformUI.getWorkbench().isClosing()) {
            deleteFileIfEmpty(sqlFile);
        }

        PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChangeListener);
        UIUtils.dispose(editorImage);
        baseEditorImage = null;
        editorImage = null;
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

        if (!sqlFile.exists() || ResourceUtils.getFileLength(sqlFile) != 0) {
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
            if (sqlFile.exists()) {
                log.debug("Delete empty SQL script '" + sqlFile.getFullPath().toOSString() + "'");
                sqlFile.delete(true, monitor);
            }
        } catch (Exception e) {
            log.error("Error deleting empty script file", e); //$NON-NLS-1$
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
        final boolean objectEvent = event.getObject() != null && event.getObject().getDataSource() == getDataSource();
        final boolean registryEvent = getDataSourceContainer() != null && event.getData() == getDataSourceContainer().getRegistry(); 
        if (dsEvent || objectEvent || registryEvent) {
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
                UIUtils.waitJobCompletion(saveJob, monitor);
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

    private synchronized void doScriptAutoSave() {
        if (scriptAutoSavejob == null) {
            scriptAutoSavejob = new ScriptAutoSaveJob();
        } else {
            scriptAutoSavejob.cancel();
        }
        scriptAutoSavejob.schedule(1000);
    }

    @Override
    public int promptToSaveOnClose()
    {
        int jobsRunning = getTotalQueryRunning();
        if (jobsRunning > 0) {
            log.warn("There are " + jobsRunning + " SQL job(s) still running in the editor");

            if (ConfirmationDialog.confirmAction(
                null,
                ConfirmationDialog.WARNING,
                SQLPreferenceConstants.CONFIRM_RUNNING_QUERY_CLOSE,
                ConfirmationDialog.QUESTION,
                jobsRunning
            ) != IDialogConstants.YES_ID)
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

        // Cancel running jobs (if any) and close results tabs
        for (QueryProcessor queryProcessor : queryProcessors) {
            queryProcessor.cancelJob();
            // FIXME: it is a hack (to avoid asking "Save script?" because editor is marked as dirty while queries are running)
            // FIXME: make it better
            queryProcessor.curJobRunning.set(0);
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

        updateDirtyFlag();

        if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE)) {
            return ISaveablePart2.YES;
        }

        if (super.isDirty() || (extraPresentation instanceof ISaveablePart && ((ISaveablePart) extraPresentation).isDirty())) {
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

    @Override
    public void saveToExternalFile() {
        saveToExternalFile(getScriptDirectory());
    }

    @Nullable
    private String getScriptDirectory() {
        final File inputFile = EditorUtils.getLocalFileFromInput(getEditorInput());
        if (inputFile != null) {
            return inputFile.getParent();
        }
        final DBPWorkspaceDesktop workspace = DBPPlatformDesktop.getInstance().getWorkspace();
        final IFolder root = workspace.getResourceDefaultRoot(workspace.getActiveProject(), ScriptsHandlerImpl.class, false);
        if (root != null) {
            URI locationURI = root.getLocationURI();
            if (locationURI.getScheme().equals("file")) {
                return new File(locationURI).toString();
            }
        }
        return null;
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
                setResultTabSelection(tabItem);
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
            case ModelPreferences.SQL_CONTROL_COMMAND_PREFIX:
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
                    initSeparateConnection(dataSource, null, false);
                }
                return;
            }
            case SQLPreferenceConstants.SCRIPT_TITLE_PATTERN:
                setPartName(getEditorName());
                return;
        }

        UIUtils.asyncExec(() -> {
            topBarMan.update(true);
        });

        fireDataSourceChanged(event);
        super.preferenceChange(event);
    }

    private void fireDataSourceChanged(PreferenceChangeEvent event) {
        // Notify listeners
        synchronized (listeners) {
            for (SQLEditorListener listener : listeners) {
                try {
                    listener.onDataSourceChanged(event);
                } catch (Throwable ex) {
                    log.debug(ex);
                }
            }
        }
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

    public class QueryProcessor implements SQLResultsConsumer, ISmartTransactionManager {

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

        int getRunningJobs() {
            return curJobRunning.get();
        }

        private QueryResultsContainer createResultsProvider(int resultSetNumber, boolean makeDefault) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultSetNumber, getMaxResultsTabIndex() + 1, makeDefault);
            resultContainers.add(resultsProvider);
            return resultsProvider;
        }

        private QueryResultsContainer createResultsProvider(DBSDataContainer dataContainer) {
            QueryResultsContainer resultsProvider = new QueryResultsContainer(this, resultContainers.size(), getMaxResultsTabIndex(), dataContainer);
            resultContainers.add(resultsProvider);
            return resultsProvider;
        }

        public boolean hasPinnedTabs() {
            for (QueryResultsContainer container : resultContainers) {
                if (container.isPinned()) {
                    return true;
                }
            }
            return false;
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
                    List<IDataTransferProducer<?>> producers = new ArrayList<>();
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
                    boolean disableFetchCurrentResultSets;
                    if (queries.size() > QUERIES_COUNT_FOR_NO_FETCH_RESULT_SET_CONFIRMATION) {
                        if (isDisableFetchResultSet == null) {
                            UserChoiceResponse rs = DBWorkbench.getPlatformUI().showUserChoice(
                                SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_title,
                                SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_question,
                                List.of(
                                    SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_yes,
                                    SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_no
                                ),
                                List.of(SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_remember), 0, 0);
                            disableFetchCurrentResultSets = rs.choiceIndex == 0;
                            if (rs.forAllChoiceIndex != null) {
                                isDisableFetchResultSet = disableFetchCurrentResultSets;
                            }
                        } else {
                            disableFetchCurrentResultSets = isDisableFetchResultSet;
                        }
                    } else {
                        disableFetchCurrentResultSets = false;
                    }
                    final SQLQueryJob job = new SQLQueryJob(getSite(),
                        isSingleQuery ? SQLEditorMessages.editors_sql_job_execute_query
                            : SQLEditorMessages.editors_sql_job_execute_script,
                        executionContext, resultsContainer, queries, scriptContext, this, listener,
                        disableFetchCurrentResultSets);

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
                String queryText = CommonUtils.truncateString(statement.getText(), 1000);
                DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
                String toolTip =
                    "Connection: " + (dataSourceContainer == null ? "N/A" : dataSourceContainer.getName()) + GeneralUtils.getDefaultLineSeparator() +
                    "Time: " + new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT).format(new Date()) + GeneralUtils.getDefaultLineSeparator() +
                    "Query: " + (CommonUtils.isEmpty(queryText) ? "N/A" : queryText);
                // Special statements (not real statements) have their name in data
                if (isStatsResult) {
                    tabName = SQLEditorMessages.editors_sql_statistics;
                    int queryIndex = queryProcessors.indexOf(QueryProcessor.this);
                    tabName += " " + (queryIndex + 1);
                }
                String finalTabName = tabName;
                UIUtils.asyncExec(() -> resultsProvider.updateResultsName(finalTabName, toolTip));
            }
            ResultSetViewer rsv = resultsProvider.getResultSetController();
            return rsv == null ? null : rsv.getDataReceiver();
        }

        @Override
        public void releaseDataReceiver(int resultSetNumber) {
            if (resultContainers.size() > resultSetNumber) {
                final CTabItem tab = resultContainers.get(resultSetNumber).resultsTab;
                UIUtils.syncExec(tab::dispose);
            }
        }

        @Override
        public boolean isSmartAutoCommit() {
            return SQLEditor.this.isSmartAutoCommit();
        }

        @Override
        public void setSmartAutoCommit(boolean smartAutoCommit) {
            SQLEditor.this.setSmartAutoCommit(smartAutoCommit);
        }
    }

    public class QueryResultsContainer implements
        DBSDataContainer,
        IResultSetContainer,
        IResultSetValueReflector,
        IResultSetListener,
        IResultSetContainerExt,
        SQLQueryContainer,
        ISmartTransactionManager,
        IQueryExecuteController {

        private final QueryProcessor queryProcessor;
        private final ResultSetViewer viewer;
        private int resultSetNumber;
        private final int resultSetIndex;
        private SQLScriptElement query = null;
        private SQLScriptElement lastGoodQuery = null;
        // Data container and filter are non-null only in case of associations navigation
        private DBSDataContainer dataContainer;
        private CTabItem resultsTab;
        private String tabName;
        private boolean detached;

        private QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber, int resultSetIndex, boolean makeDefault) {
            this.queryProcessor = queryProcessor;
            this.resultSetNumber = resultSetNumber;
            this.resultSetIndex = resultSetIndex;

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
            resultsTab.setImage(IMG_DATA_GRID);
            resultsTab.setData(this);
            resultsTab.setShowClose(true);
            resultsTab.setText(getResultsTabName(resultSetNumber, getQueryIndex(), null));
            CSSUtils.setCSSClass(resultsTab, DBStyles.COLORED_BY_CONNECTION_TYPE);

            resultsTab.setControl(viewer.getControl());
            resultsTab.addDisposeListener(resultTabDisposeListener);
            UIUtils.disposeControlOnItemDispose(resultsTab);

            viewer.getControl().addDisposeListener(e -> {
                QueryResultsContainer.this.queryProcessor.removeResults(QueryResultsContainer.this);
                if (QueryResultsContainer.this == curResultsContainer) {
                    curResultsContainer = null;
                }
            });
        }

        QueryResultsContainer(QueryProcessor queryProcessor, int resultSetNumber, int resultSetIndex, DBSDataContainer dataContainer) {
            this(queryProcessor, resultSetNumber, resultSetIndex, false);
            this.dataContainer = dataContainer;
            updateResultsName(getResultsTabName(resultSetNumber, 0, dataContainer.getName()), null);
        }

        public void detach() {
            try {
                detached = true;
                getSite().getPage().openEditor(
                    new SQLResultsEditorInput(this),
                    SQLResultsEditor.class.getName(),
                    true,
                    IWorkbenchPage.MATCH_NONE
                );
            } catch (Throwable e) {
                DBWorkbench.getPlatformUI().showError("Detached results", "Can't open results view", e);
                detached = false;
            }

            if (detached) {
                resultsTab.dispose();
                resultsTab = null;
            }
        }

        private CTabItem getTabItem() {
            return resultsTab;
        }

        public int getResultSetIndex() {
            return resultSetIndex;
        }

        public int getQueryIndex() {
            return queryProcessors.indexOf(queryProcessor);
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
                if (toolTip != null) {
                    tabItem.setToolTipText(toolTip);
                }
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
            return createQueryResultsDecorator();
        }

        @Override
        public String[] getSupportedFeatures()
        {
            if (dataContainer != null) {
                return dataContainer.getSupportedFeatures();
            }
            List<String> features = new ArrayList<>(3);
            features.add(FEATURE_DATA_SELECT);
            if (query instanceof SQLQuery && ((SQLQuery) query).isModifiyng()) {
                features.add(FEATURE_DATA_MODIFIED_ON_REFRESH);
            }
            features.add(FEATURE_DATA_COUNT);

            if (getQueryResultCounts() <= 1) {
                features.add(FEATURE_DATA_FILTER);
            }
            return features.toArray(new String[0]);
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

                job.extractData(session, this.query, resultCounts > 1 ? 0 : resultSetNumber, !detached);

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
        public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @Nullable DBDDataFilter dataFilter, long flags)
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

                try (DBCStatement dbStatement = DBUtils.makeStatement(source, session, DBCStatementType.SCRIPT, countQuery, 0, 0)) {
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
                                if (countValue instanceof Map && ((Map<?, ?>) countValue).size() == 1) {
                                    // For document-based DBs
                                    Object singleValue = ((Map<?, ?>) countValue).values().iterator().next();
                                    if (singleValue instanceof Number) {
                                        countValue = singleValue;
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
        public void onModelPrepared() {
            notifyOnDataListeners(this);
        }

        @Override
        public SQLScriptContext getScriptContext() {
            return SQLEditor.this.getGlobalScriptContext();
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

        @Override
        public void forceDataReadCancel(Throwable error) {
            for (QueryProcessor processor : queryProcessors) {
                SQLQueryJob job = processor.curJob;
                if (job != null) {
                    SQLQueryResult currentQueryResult = job.getCurrentQueryResult();
                    if (currentQueryResult == null) {
                        currentQueryResult = new SQLQueryResult(new SQLQuery(null, ""));
                    }
                    currentQueryResult.setError(error);
                    job.notifyQueryExecutionEnd(currentQueryResult);
                }
            }
        }

        @Override
        public void handleExecuteResult(DBCExecutionResult result) {
            dumpQueryServerOutput(result);
        }

        @Override
        public void showCurrentError() {
            if (getLastQueryErrorPosition() > -1) {
                getSelectionProvider().setSelection(new TextSelection(getLastQueryErrorPosition(), 0));
                setFocus();
            }
        }
    }

    @NotNull
    protected QueryResultsDecorator createQueryResultsDecorator() {
        return new QueryResultsDecorator() {
            @Override
            public String getEmptyDataDescription() {
                String execQuery = ActionUtils.findCommandDescription(SQLEditorCommands.CMD_EXECUTE_STATEMENT, getSite(), true);
                String execScript = ActionUtils.findCommandDescription(SQLEditorCommands.CMD_EXECUTE_SCRIPT, getSite(), true);
                return NLS.bind(ResultSetMessages.sql_editor_resultset_filter_panel_control_execute_to_see_reslut, execQuery, execScript);
            }
        };
    }

    private int getMaxResultsTabIndex() {
        int maxIndex = 0;
        for (CTabItem tab : resultTabs.getItems()) {
            if (tab.getData() instanceof QueryResultsContainer) {
                maxIndex = Math.max(maxIndex, ((QueryResultsContainer) tab.getData()).getResultSetIndex());
            }
        }
        return maxIndex;
    }

    private String getResultsTabName(int resultSetNumber, int queryIndex, String name) {
        String tabName = name;
        if (CommonUtils.isEmpty(tabName)) {
            tabName = SQLEditorMessages.editors_sql_data_grid;
        }
        tabName += " " + (queryIndex + 1);
        if (resultSetNumber > 0) {
            tabName += " (" + (resultSetNumber + 1) + ")";
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
                    if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE)
                        && isResultSetAutoFocusEnabled
                        && !isHideQueryText()
                    ) {
                        resultsSash.setMaximizedControl(sqlEditorPanel);
                    }
                    clearProblems(null);
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
                    UIUtils.asyncExec(() -> {
                        setTitleImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT_EXECUTE));
                        updateDirtyFlag();
                        if (!scriptMode) {
                            clearProblems(query);
                        }
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
                        if (isDisposed()) {
                            return;
                        }
                        setTitleImage(editorImage);
                        updateDirtyFlag();
                    });
                }

                if (isDisposed()) {
                    return;
                }
                UIUtils.runUIJob("Process SQL query result", monitor -> {
                    if (isDisposed()) {
                        return;
                    }
                    // Finish query
                    processQueryResult(monitor, result, statistics);
                    // Update dirty flag
                    updateDirtyFlag();
                    refreshActions();
                });
            } finally {
                if (extListener != null) {
                    extListener.onEndQuery(session, result, statistics);
                }
            }
        }

        private void processQueryResult(DBRProgressMonitor monitor, SQLQueryResult result, DBCStatistics statistics) {
            dumpQueryServerOutput(result);
            if (!scriptMode) {
                runPostExecuteActions(result);
            }
            SQLQuery query = result.getStatement();
            Throwable error = result.getError();
            ISelectionProvider selectionProvider = getSelectionProvider();
            if (selectionProvider == null) {
                // Disposed?
                return;
            }
            if (error != null) {
                setStatus(GeneralUtils.getFirstMessage(error), DBPMessageType.ERROR);
                SQLQuery originalQuery = curResultsContainer.query instanceof SQLQuery ? (SQLQuery) curResultsContainer.query : null; // SQLQueryResult stores modified query
                if (!visualizeQueryErrors(monitor, query, error, originalQuery)) {
                    int errorQueryOffset = query.getOffset();
                    int errorQueryLength = query.getLength();
                    if (errorQueryOffset >= 0 && errorQueryLength > 0) {
                        if (!addProblem(GeneralUtils.getFirstMessage(error), new Position(errorQueryOffset, errorQueryLength))) {
                            if (scriptMode) {
                                selectionProvider.setSelection(new TextSelection(errorQueryOffset, errorQueryLength));
                            } else {
                                selectionProvider.setSelection(originalSelection);
                            }
                        }
                        setLastQueryErrorPosition(errorQueryOffset);
                    }
                }
            } else if (!scriptMode && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE)) {
                selectionProvider.setSelection(originalSelection);
            }
            notifyOnQueryResultListeners(curResultsContainer, result);
            // Get results window (it is possible that it was closed till that moment
            {
                for (QueryResultsContainer cr : queryProcessor.resultContainers) {
                    cr.viewer.updateFiltersText(false);
                }
                if (!result.hasError() && !queryProcessor.resultContainers.isEmpty()) {
                    if (activeResultsTab != null && !activeResultsTab.isDisposed()) {
                        setResultTabSelection(activeResultsTab);
                    } else {
                        setResultTabSelection(queryProcessor.resultContainers.get(0).resultsTab);
                    }
                }
                // Set tab names by query results names
                if (scriptMode || queryProcessor.getResultContainers().size() > 0) {

                    int queryIndex = queryProcessors.indexOf(queryProcessor);
                    int resultsIndex = 0;
                    for (QueryResultsContainer results : queryProcessor.resultContainers) {
                        if (results.query != query) {
                            // This happens when query results is statistics tab
                            // in that case we need to update tab selection and
                            // select new statistics tab
                            // see #16605
                            // But we need to avoid the result tab with the select statement
                            // because the statistics window can not be in focus in this case
                            if (query.getType() != SQLQueryType.SELECT) {
                                setResultTabSelection(results.resultsTab);
                            }
                            continue;
                        }
                        if (resultsIndex < result.getExecuteResults().size()) {
                            SQLQueryResult.ExecuteResult executeResult = result.getExecuteResults(resultsIndex, true);
                            String resultSetName = results.tabName;
                            if (CommonUtils.isEmpty(resultSetName)) {
                                resultSetName = getResultsTabName(results.resultSetNumber, queryIndex, executeResult.getResultSetName());
                                results.updateResultsName(resultSetName, null);
                                setResultTabSelection(results.resultsTab);
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
                    if (!isHideQueryText()) {
                        resultsSash.setMaximizedControl(null);
                        if (!hasErrors) {
                            getSelectionProvider().setSelection(originalSelection);
                        }
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

    @Override
    public void updateDirtyFlag() {
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
    }

    private class FindReplaceTarget extends DynamicFindReplaceTarget {
        private IFindReplaceTarget previousTarget = null;
        @Override
        public IFindReplaceTarget getTarget() {
            //getTarget determines current composite used for find/replace
            //We should update it, when we focus on the other panels or output view
            ResultSetViewer rsv = getActiveResultSetViewer();
            TextViewer textViewer = getTextViewer();
            final SQLEditorOutputConsoleViewer outputViewer = SQLEditor.this.outputViewer.getViewer();
            boolean focusInEditor = textViewer != null && textViewer.getTextWidget() != null && textViewer.getTextWidget().isFocusControl();
            if (!focusInEditor) {
                if (rsv == null && !outputViewer.getText().isFocusControl() && previousTarget != null) {
                    focusInEditor = textViewer != null && previousTarget.equals(textViewer.getFindReplaceTarget());
                }
            }
            if (!focusInEditor) {
                //Focus is on presentation we need to find a class for it
                if (rsv != null && rsv.getActivePresentation().getControl().isFocusControl()) {
                    previousTarget = rsv.getAdapter(IFindReplaceTarget.class);
                } else if (outputViewer.getControl().isFocusControl()) {
                    //Output viewer is just StyledText we use StyledTextFindReplace
                    previousTarget = new StyledTextFindReplaceTarget(outputViewer.getText());
                }
            } else {
                previousTarget = textViewer.getFindReplaceTarget();
            }
            return previousTarget;
        }
    }

    private class DynamicSelectionProvider extends CompositeSelectionProvider {
        private boolean lastFocusInEditor = true;
        @Override
        public ISelectionProvider getProvider() {
            if (extraPresentation != null && getExtraPresentationState() == SQLEditorPresentation.ActivationType.VISIBLE) {
                if (getExtraPresentationControl().isFocusControl()) {
                    ISelectionProvider selectionProvider = extraPresentation.getSelectionProvider();
                    if (selectionProvider != null) {
                        return selectionProvider;
                    }
                }
            }
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

    private void dumpQueryServerOutput(@Nullable DBCExecutionResult result) {
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
        showResultsPanel(true);

        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            // Refresh active object
            if (result == null || !result.hasError() && getActivePreferenceStore().getBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)) {
                DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults != null) {
                    new AbstractJob("Refresh default object") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            monitor.beginTask("Refresh default objects", 1);
                            try {
                                DBUtils.refreshContextDefaultsAndReflect(monitor, contextDefaults);
                            } finally {
                                monitor.done();
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
            ToolItem viewItem = getViewToolItem(SQLEditorCommands.CMD_SQL_SHOW_OUTPUT);
            if (viewItem != null) {
                viewItem.setImage(image);
            }
            // TODO: make icon update. Can't call setImage because this will break contract f VerticalButton
/*
            VerticalButton viewItem = getViewToolItem(SQLEditorCommands.CMD_SQL_SHOW_OUTPUT);
            if (viewItem != null) {
                viewItem.setImage(image);
            }
*/
        }
    }

    private class ScriptAutoSaveJob extends AbstractJob {
        ScriptAutoSaveJob() {
            super("Save '" + getPartName() + "' script");
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            if (EditorUtils.isInAutoSaveJob()) {
                return Status.CANCEL_STATUS;
            }
            monitor.beginTask("Auto-save SQL script", 1);
            try {
                UIUtils.asyncExec(() ->
                    SQLEditor.this.doTextEditorSave(monitor));
            } catch (Throwable e) {
                log.debug(e);
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
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
            monitor.beginTask("Save query processors", queryProcessors.size());
            try {
                for (QueryProcessor queryProcessor : queryProcessors) {
                    for (QueryResultsContainer resultsProvider : queryProcessor.getResultContainers()) {
                        ResultSetViewer rsv = resultsProvider.getResultSetController();
                        if (rsv != null && rsv.isDirty()) {
                            rsv.doSave(monitor);
                        }
                    }
                    monitor.worked(1);
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
                monitor.done();
            }
        }
    }

    private class OutputLogWriter implements DBCOutputWriter {
        @Override
        public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
            UIUtils.syncExec(() -> {
                if (!outputViewer.isDisposed()) {
                    outputViewer.println(severity, message);
                    outputViewer.getViewer().scrollToEnd();
                    if (!outputViewer.isVisible()) {
                        updateOutputViewerIcon(true);
                    }
                }
            });
        }

        @Override
        public void flush() {
            outputViewer.flush();
        }
    }

    private class ServerOutputReader extends AbstractJob {

        ServerOutputReader() {
            super("Dump server output");
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            if (!DBWorkbench.getPlatform().isShuttingDown() && resultsSash != null && !resultsSash.isDisposed()) {
                try {
                    dumpOutput(monitor);
                } catch (Exception e) {
                    log.debug(e);
                }
                schedule(200);
            }

            return Status.OK_STATUS;
        }

        private void dumpOutput(DBRProgressMonitor monitor) {
            SQLEditorOutputViewer currentOutputViewer = outputViewer;
            if (currentOutputViewer == null || currentOutputViewer.isDisposed()) {
                return;
            }

            List<ServerOutputInfo> outputs;
            synchronized (serverOutputs) {
                outputs = new ArrayList<>(serverOutputs);
                serverOutputs.clear();
            }

            List<PrintWriter> addInWriters = addIns.stream()
                .map(SQLEditorAddIn::getServerOutputConsumer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            final DBCOutputWriter outputWriter = new DBCOutputWriter() {
                @Override
                public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
                    currentOutputViewer.println(severity, message);

                    if (message != null) {
                        for (PrintWriter writer : addInWriters) {
                            writer.println(message);
                        }
                    }
                }

                @Override
                public void flush() {
                    currentOutputViewer.flush();

                    for (PrintWriter writer : addInWriters) {
                        writer.flush();
                    }
                }
            };

            if (!outputs.isEmpty()) {
                for (ServerOutputInfo info : outputs) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    try {
                        info.outputReader.readServerOutput(monitor, info.executionContext, info.result, null, outputWriter);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }

            if (!monitor.isCanceled()) {
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
                            try {
                                if (statement != null && !statement.isStatementClosed()) {
                                    outputReader.readServerOutput(monitor, executionContext, null, statement, outputWriter);
                                }
                            } catch (DBCException e) {
                                log.error(e);
                            }
                        }
                    }
                }
            }

            outputWriter.flush();
            if (currentOutputViewer == null || currentOutputViewer.isDisposed() || !currentOutputViewer.isHasNewOutput()) {
                return;
            }
            currentOutputViewer.resetNewOutput();
            // Show output log view if needed
            UIUtils.asyncExec(() -> {
                currentOutputViewer.getViewer().scrollToEnd();
                if (getActivePreferenceStore().getBoolean(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW)) {
                    if (!getViewToolItem(SQLEditorCommands.CMD_SQL_SHOW_OUTPUT).getSelection()) {
                        showOutputPanel();
                    }
                }
/*
                if (outputViewer!=null) {
                    if (outputViewer.getControl()!=null) {
                        if (!outputViewer.isDisposed()) {
                            outputViewer.scrollToEnd();
                            updateOutputViewerIcon(true);
                        }
                    }
                }
*/
            });
        }
    }

    private class OutputAutoShowToggleAction extends Action {
        OutputAutoShowToggleAction() {
            super(SQLEditorMessages.pref_page_sql_editor_label_auto_open_output_view, AS_CHECK_BOX);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SHOW_ALL_DETAILS));
            setChecked(getActivePreferenceStore().getBoolean(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW));
        }

        @Override
        public void run() {
            getActivePreferenceStore().setValue(SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW, isChecked());
            try {
                getActivePreferenceStore().save();
            } catch (IOException e) {
                log.error(e);
            }
        }

    }

    private void notifyOnDataListeners(@NotNull QueryResultsContainer container) {
        // Notify listeners
        synchronized (listeners) {
            for (SQLEditorListener listener : listeners) {
                try {
                    listener.onDataReceived(
                        getContextPrefStore(container),
                        container.getResultSetController().getModel(),
                        container.getQuery().getOriginalText()
                    );
                } catch (Throwable ex) {
                    log.error(ex);
                }
            }
        }
    }


    private void notifyOnQueryResultListeners(@NotNull QueryResultsContainer container, @NotNull SQLQueryResult result) {
        // Notify listeners
        synchronized (listeners) {
            for (SQLEditorListener listener : listeners) {
                try {
                    listener.onQueryResult(getContextPrefStore(container), result);
                } catch (Throwable ex) {
                    log.error(ex);
                }
            }
        }
    }

    @NotNull
    private DBPPreferenceStore getContextPrefStore(@NotNull QueryResultsContainer container) {
        DBCExecutionContext context = container.getExecutionContext();
        DBPPreferenceStore contextPrefStore = context != null
            ? context.getDataSource().getContainer().getPreferenceStore()
            : DBWorkbench.getPlatform().getPreferenceStore();
        return contextPrefStore;
    }
}
