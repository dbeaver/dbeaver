/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetails;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionDetailsProvider;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.autorefresh.AutoRefreshControl;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.plan.ExplainPlanViewer;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * SessionManagerViewer
 */
public class SessionManagerViewer<SESSION_TYPE extends DBAServerSession>
{
    private static final Log log = Log.getLog(SessionManagerViewer.class);

    private IWorkbenchPart workbenchPart;
    private final DBAServerSessionManager<SESSION_TYPE> sessionManager;
    private SessionListControl sessionTable;
    //private Text sessionInfo;
    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;

    private Font boldFont;
    private PropertyTreeViewer sessionProps;
    private DBAServerSession curSession;
    private AutoRefreshControl refreshControl;
    private final SashForm sashMain;
    private final SashForm sashDetails;

    private IDialogSettings settings;

    private CTabFolder previewFolder;
    private final CTabItem detailsItem;

    private final DBCQueryPlanner planner;
    private ExplainPlanViewer planViewer;
    private Object selectedPlanElement;
    private final CTabFolder detailsFolder;

    protected SessionManagerViewer(IWorkbenchPart part, Composite parent, final DBAServerSessionManager<SESSION_TYPE> sessionManager) {
        this.workbenchPart = part;
        this.sessionManager = sessionManager;
        this.subSite = new SubEditorSite(workbenchPart.getSite());
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());

        planner = DBUtils.getAdapter(DBCQueryPlanner.class, sessionManager.getDataSource());

        Composite composite = UIUtils.createPlaceholder(parent, 1);

        sashMain = UIUtils.createPartDivider(workbenchPart, composite, SWT.VERTICAL | SWT.SMOOTH);
        sashMain.setLayoutData(new GridData(GridData.FILL_BOTH));

        refreshControl = new AutoRefreshControl(sashMain, sessionManager.getClass().getSimpleName(), monitor -> UIUtils.syncExec(this::refreshSessions));

        {
            sessionTable = new SessionListControl(sashMain, workbenchPart.getSite(), sessionManager);
            sessionTable.getItemsViewer().addSelectionChangedListener(event -> onSessionSelect(getSelectedSession()));
            sessionTable.addDisposeListener(e -> saveSettings(settings));

            sessionTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            sessionTable.createProgressPanel(composite);
        }

        {
            sashDetails = UIUtils.createPartDivider(workbenchPart, sashMain, SWT.HORIZONTAL | SWT.SMOOTH);
            sashDetails.setLayoutData(new GridData(GridData.FILL_BOTH));

            {
                previewFolder = new CTabFolder(sashDetails, SWT.TOP);
                sqlViewer = new SQLEditorBase() {
                    @Override
                    public DBCExecutionContext getExecutionContext() {
                        return sessionManager.getDataSource().getDefaultInstance().getDefaultContext(false);
                    }

                    @Override
                    public boolean isFoldingEnabled() {
                        return false;
                    }
                };
                updateSQL();
                sqlViewer.createPartControl(previewFolder);
                Object text = sqlViewer.getAdapter(Control.class);
                if (text instanceof StyledText) {
                    ((StyledText) text).setWordWrap(true);
                }

                sqlViewer.reloadSyntaxRules();

                parent.addDisposeListener(e -> sqlViewer.dispose());

                CTabItem sqlViewItem = new CTabItem(previewFolder, SWT.NONE);
                sqlViewItem.setText(CoreMessages.viewer_view_item_sql);
                sqlViewItem.setImage(DBeaverIcons.getImage(UIIcon.SQL_TEXT));
                sqlViewItem.setControl(sqlViewer.getEditorControlWrapper());

                previewFolder.setSelection(sqlViewItem);

                if (planner != null) {
                    createPlannerTab(previewFolder);
                }

                previewFolder.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updatePreview();
                    }
                });
            }

            {
                detailsFolder = new CTabFolder(sashDetails, SWT.TOP);
                sessionProps = new PropertyTreeViewer(detailsFolder, SWT.NONE);

                detailsItem = new CTabItem(detailsFolder, SWT.NONE);
                detailsItem.setText("Details");
                detailsItem.setImage(DBeaverIcons.getImage(UIIcon.PROPERTIES));
                detailsItem.setControl(sessionProps.getControl());

                if (sessionManager instanceof DBAServerSessionDetailsProvider) {
                    List<DBAServerSessionDetails> sessionDetails = ((DBAServerSessionDetailsProvider) sessionManager).getSessionDetails();
                    if (sessionDetails != null) {
                        for (DBAServerSessionDetails detailsInfo : sessionDetails) {
                            CTabItem extDetailsItem = new CTabItem(detailsFolder, SWT.NONE);
                            extDetailsItem.setData(detailsInfo);
                            extDetailsItem.setText(detailsInfo.getDetailsTitle());
                            if (detailsInfo.getDetailsIcon() != null) {
                                extDetailsItem.setImage(DBeaverIcons.getImage(detailsInfo.getDetailsIcon()));
                            }
                            if (detailsInfo.getDetailsTooltip() != null) {
                                extDetailsItem.setToolTipText(detailsInfo.getDetailsTooltip());
                            }

                            DetailsListControl detailsProps = new DetailsListControl(detailsFolder, workbenchPart.getSite(), detailsInfo);
                            extDetailsItem.setControl(detailsProps);
                        }
                    }
                }

                detailsFolder.setSelection(detailsItem);
                detailsFolder.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        CTabItem item = detailsFolder.getItem(detailsFolder.getSelectionIndex());
                        Object data = item.getData();
                        if (data instanceof DBAServerSessionDetails) {
                            DetailsListControl detailsViewer = (DetailsListControl) item.getControl();
                            detailsViewer.loadData();
                        }
                    }
                });
            }

            sashMain.setWeights(new int[]{500, 500});
        }

        sashMain.setWeights(new int[]{700, 300});
    }

    private void updatePreview() {
        if (previewFolder.getSelectionIndex() == 0) {
            // Show SQL
            detailsItem.setText(CoreMessages.viewer_details_item_session_details);
            updateSQL();
            if (curSession == null) {
                sessionProps.clearProperties();
            } else {
                PropertyCollector propCollector = new PropertyCollector(curSession, true);
                propCollector.collectProperties();
                sessionProps.loadProperties(propCollector);
            }
        } else if (planViewer != null) {
            // Show execution plan
            String sqlText = curSession == null ? "" : CommonUtils.notEmpty(curSession.getActiveQuery());
            if (!CommonUtils.isEmpty(sqlText)) {
                planViewer.explainQueryPlan(new SQLQuery(sessionManager.getDataSource(), sqlText));
            }
        }
        if (detailsFolder.getSelectionIndex() > 0) {
            CTabItem detailsItem = detailsFolder.getItem(detailsFolder.getSelectionIndex());
            Object data = detailsItem.getData();
            if (data instanceof DBAServerSessionDetails) {
                DetailsListControl detailsListControl = (DetailsListControl) detailsItem.getControl();
                detailsListControl.loadData();
            }
        }
    }

    private void createPlannerTab(CTabFolder previewFolder) {
        planViewer = new ExplainPlanViewer(workbenchPart, sqlViewer, previewFolder);

//        planTree = new PlanNodesTree(previewFolder, SWT.SHEET, workbenchPart.getSite());
//        planTree.substituteProgressPanel(getSessionListControl());
        planViewer.addSelectionChangedListener(event -> showPlanNode());

        CTabItem sqlPlanItem = new CTabItem(previewFolder, SWT.NONE);
        sqlPlanItem.setText(CoreMessages.viewer_sql_plan_item_execution_plan);
        sqlPlanItem.setImage(DBeaverIcons.getImage(UIIcon.SQL_PAGE_EXPLAIN_PLAN));
        sqlPlanItem.setControl(planViewer.getControl());
    }

    private void showPlanNode()
    {
        detailsItem.setText("Plan Details");

        ISelection selection = planViewer.getSelection();
        if (selection.isEmpty()) {
            sessionProps.clearProperties();
        } else if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element != selectedPlanElement) {
                PropertyCollector propertySource = new PropertyCollector(element, true);
                propertySource.collectProperties();
                sessionProps.loadProperties(propertySource);
                selectedPlanElement = element;
            }
        }
    }

    public DatabaseObjectListControl getSessionListControl() {
        return sessionTable;
    }

    public void dispose()
    {
        sessionTable.disposeControl();
        UIUtils.dispose(boldFont);
    }

    protected void onSessionSelect(DBAServerSession session)
    {
        if (curSession == session && selectedPlanElement == null) {
            return;
        }
        selectedPlanElement = null;
        previewFolder.setSelection(0);
        curSession = session;
        updatePreview();
    }

    protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
    {

    }

    public DBAServerSession getSelectedSession()
    {
        ISelection selection = sessionTable.getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            return (DBAServerSession)((IStructuredSelection) selection).getFirstElement();
        } else {
            return null;
        }
    }

    public List<DBAServerSession> getSelectedSessions()
    {
        ISelection selection = sessionTable.getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            List<DBAServerSession> sessions = new ArrayList<>();
            for (Object item : ((IStructuredSelection) selection).toArray()) {
                if (item instanceof DBAServerSession) {
                    sessions.add((DBAServerSession) item);
                }
            }
            return sessions;
        } else {
            return Collections.emptyList();
        }
    }

    public void refreshSessions()
    {
        sessionTable.loadData();
        onSessionSelect(null);

        refreshControl.scheduleAutoRefresh(false);
    }

    public void alterSessions(final List<SESSION_TYPE> sessions, Map<String, Object> options) {
        sessionTable.createAlterService(sessions, options).schedule();
    }

    private void updateSQL() {
        String text = curSession == null ? "" : CommonUtils.notEmpty(curSession.getActiveQuery());
        StringEditorInput sqlInput = new StringEditorInput(sessionTable.getShell().getText(), text, true, GeneralUtils.getDefaultFileEncoding());
        if (sqlViewer.getSite() == null) {
            try {
                sqlViewer.init(subSite, sqlInput);
            } catch (PartInitException e) {
                DBWorkbench.getPlatformUI().showError(sessionTable.getShell().getText(), null, e);
            }
        } else {
            sqlViewer.setInput(sqlInput);
        }
        if (sqlViewer.getTextViewer() != null) {
            sqlViewer.reloadSyntaxRules();
        }
    }

    public Composite getControl() {
        return sessionTable.getControl();
    }

    public Map<String, Object> getSessionOptions() {
        return null;
    }

    void loadSettings(AbstractSessionEditor sessionEditor) {
        //$NON-NLS-1$
        settings = UIUtils.getDialogSettings("DBeaver." + sessionEditor.getClass().getSimpleName());
        loadSettings(settings);
    }

    protected void loadSettings(IDialogSettings settings) {
        int mainSashRatio = CommonUtils.toInt(settings.get("MainSashRatio"), 0);
        if (mainSashRatio > 0) {
            sashMain.setWeights(new int[] { mainSashRatio, 1000 - mainSashRatio });
        }
        int detailsSashRatio = CommonUtils.toInt(settings.get("DetailsSashRatio"), 0);
        if (detailsSashRatio > 0) {
            sashDetails.setWeights(new int[] { detailsSashRatio, 1000 - detailsSashRatio });
        }
    }

    protected void saveSettings(IDialogSettings settings) {
        settings.put("MainSashRatio", sashMain.getWeights()[0]);
        settings.put("DetailsSashRatio", sashDetails.getWeights()[0]);
    }

    protected boolean sessionMatches(SESSION_TYPE element, Pattern searchPattern) {
        String activeQuery = element.getActiveQuery();
        if (activeQuery != null && searchPattern.matcher(activeQuery).find()) {
            return true;
        }
        return false;
    }

    private class SessionListControl extends SessionTable<SESSION_TYPE> {

        private SessionSearcher searcher;

        SessionListControl(Composite sash, IWorkbenchSite site, DBAServerSessionManager<SESSION_TYPE> sessionManager)
        {
            super(sash, SWT.SHEET, site, sessionManager);
            searcher = new SessionSearcher();
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            contributeToToolbar(getSessionManager(), contributionManager);
            refreshControl.populateRefreshButton(contributionManager);
            contributionManager.add(new Action("Refresh sessions", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH)) {
                @Override
                public void run()
                {
                    refreshSessions();
                }
            });
        }

        @Override
        protected final Map<String, Object> getSessionOptions() {
            return SessionManagerViewer.this.getSessionOptions();
        }

        @Override
        protected ISearchExecutor getSearchRunner()
        {
            return searcher;
        }

        private class SessionSearcher implements ISearchExecutor {

            @Override
            public boolean performSearch(String searchString, int options) {
                try {
                    SearchFilter searchFilter = new SearchFilter(
                        searchString,
                        (options & SEARCH_CASE_SENSITIVE) != 0);
                    getItemsViewer().setFilters(searchFilter);
                    return true;
                } catch (PatternSyntaxException e) {
                    log.error(e.getMessage());
                    return false;
                }
            }

            @Override
            public void cancelSearch() {
                getItemsViewer().setFilters();
            }
        }

        private class SearchFilter extends ViewerFilter {
            final Pattern pattern;

            public SearchFilter(String searchString, boolean caseSensitiveSearch) throws PatternSyntaxException {
                pattern = Pattern.compile(SQLUtils.makeLikePattern(searchString), caseSensitiveSearch ? 0 : Pattern.CASE_INSENSITIVE);
            }

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                try {
                    if (element instanceof DBAServerSession) {
                        boolean matches = false;
                        for (DBPPropertyDescriptor property : getAllProperties()) {
                            if (property instanceof ObjectPropertyDescriptor) {
                                Object value = ((ObjectPropertyDescriptor) property).readValue(element, null);
                                if (value != null && pattern.matcher(CommonUtils.toString(value)).find()) {
                                    matches = true;
                                    break;
                                }
                            }
                        }
                        return matches;
                    }
                    return false;
                } catch (Exception e) {
                    log.error(e);
                    return false;
                }
            }
        }
    }

    private class DetailsListControl extends DatabaseObjectListControl<DBPObject> {

        private DBAServerSessionDetails sessionDetails;

        protected DetailsListControl(Composite parent, IWorkbenchSite site, DBAServerSessionDetails sessionDetails) {
            super(parent, SWT.SHEET, site, new ListContentProvider());
            this.sessionDetails = sessionDetails;
        }

        @Override
        protected String getListConfigId(List<Class<?>> classList) {
            return "SessionDetails/" + sessionManager.getDataSource().getContainer().getDriver().getId() + "/" + sessionDetails.getDetailsTitle();
        }

        @Override
        protected Class<?>[] getListBaseTypes(Collection<DBPObject> items) {
            return new Class[] { sessionDetails.getDetailsType() };
        }

        @Override
        protected LoadingJob<Collection<DBPObject>> createLoadService() {
            return LoadingJob.createService(
                new SessionDetailsLoadService(sessionDetails),
                new ObjectsLoadVisualizer());
        }
    }

    private class SessionDetailsLoadService extends DatabaseLoadService<Collection<DBPObject>> {

        private DBAServerSessionDetails sessionDetails;

        public SessionDetailsLoadService(DBAServerSessionDetails sessionDetails) {
            super("Load session details " + sessionDetails.getDetailsTitle(), sessionManager.getDataSource());
            this.sessionDetails = sessionDetails;
        }

        @Override
        public Collection<DBPObject> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            if (curSession == null) {
                return Collections.emptyList();
            }
            try {
                DBCExecutionContext context = DBUtils.getDefaultContext(sessionManager.getDataSource(), false);
                try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Load session details (" + sessionDetails.getDetailsTitle() + ")")) {
                    List<? extends DBPObject> sessionDetails = this.sessionDetails.getSessionDetails(session, curSession);
                    List<DBPObject> result = new ArrayList<>();
                    if (sessionDetails != null) {
                        result.addAll(sessionDetails);
                    }
                    return result;
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }

    }

}