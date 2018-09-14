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
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ISearchExecutor;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.autorefresh.AutoRefreshControl;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.ui.views.plan.PlanNodesTree;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;
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

    private TabFolder previewFolder;
    private TabItem sqlViewItem;
    private TabItem sqlPlanItem;
    private final TabItem detailsItem;

    private final DBCQueryPlanner planner;
    private PlanNodesTree planTree;
    private Object selectedPlanElement;

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
                previewFolder = new TabFolder(sashDetails, SWT.TOP);
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

                sqlViewItem = new TabItem(previewFolder, SWT.NONE);
                sqlViewItem.setText("SQL");
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
                TabFolder detailsFolder = new TabFolder(sashDetails, SWT.TOP);
                sessionProps = new PropertyTreeViewer(detailsFolder, SWT.NONE);

                detailsItem = new TabItem(detailsFolder, SWT.NONE);
                detailsItem.setText("Details");
                detailsItem.setImage(DBeaverIcons.getImage(UIIcon.PROPERTIES));
                detailsItem.setControl(sessionProps.getControl());

                detailsFolder.setSelection(detailsItem);
            }

            sashMain.setWeights(new int[]{50, 50});
        }

        sashMain.setWeights(new int[]{70, 30});
    }

    private void updatePreview() {
        if (previewFolder.getSelectionIndex() == 0) {
            // Show SQL
            detailsItem.setText("Session Details");
            updateSQL();
            if (curSession == null) {
                sessionProps.clearProperties();
            } else {
                PropertyCollector propCollector = new PropertyCollector(curSession, true);
                propCollector.collectProperties();
                sessionProps.loadProperties(propCollector);
            }
        } else if (planTree != null) {
            // Show execution plan
            planTree.clearListData();
            String sqlText = curSession == null ? "" : CommonUtils.notEmpty(curSession.getActiveQuery());
            if (!CommonUtils.isEmpty(sqlText)) {
                DBPDataSource dataSource = sessionManager.getDataSource();
                planTree.init(DBUtils.getDefaultContext(dataSource, false), planner, sqlText);
                planTree.loadData();
            }
        }
    }

    private void createPlannerTab(TabFolder previewFolder) {
        planTree = new PlanNodesTree(previewFolder, SWT.SHEET, workbenchPart.getSite());
        planTree.substituteProgressPanel(getSessionListControl());
        planTree.getItemsViewer().addSelectionChangedListener(event -> showPlanNode());

        sqlPlanItem = new TabItem(previewFolder, SWT.NONE);
        sqlPlanItem.setText("Execution Plan");
        sqlPlanItem.setImage(DBeaverIcons.getImage(UIIcon.SQL_PAGE_EXPLAIN_PLAN));
        sqlPlanItem.setControl(planTree);
    }

    private void showPlanNode()
    {
        detailsItem.setText("Plan Details");

        ISelection selection = planTree.getItemsViewer().getSelection();
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

    public void refreshSessions()
    {
        sessionTable.loadData();
        onSessionSelect(null);

        refreshControl.scheduleAutoRefresh(false);
    }

    public void alterSession(final SESSION_TYPE session, Map<String, Object> options) {
        sessionTable.createAlterService(session, options).schedule();
    }

    private void updateSQL() {
        String text = curSession == null ? "" : CommonUtils.notEmpty(curSession.getActiveQuery());
        StringEditorInput sqlInput = new StringEditorInput(sessionTable.getShell().getText(), text, true, GeneralUtils.getDefaultFileEncoding());
        if (sqlViewer.getSite() == null) {
            try {
                sqlViewer.init(subSite, sqlInput);
            } catch (PartInitException e) {
                DBUserInterface.getInstance().showError(sessionTable.getShell().getText(), null, e);
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
        protected void fillCustomActions(IContributionManager contributionManager) {
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


}