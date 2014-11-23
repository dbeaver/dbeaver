/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ui.search.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.search.IObjectSearchContainer;
import org.jkiss.dbeaver.ui.search.IObjectSearchPage;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class SearchDataPage extends DialogPage implements IObjectSearchPage {

    static final Log log = LogFactory.getLog(SearchDataPage.class);

    private static final String PROP_MASK = "search.data.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search.data.case-sensitive"; //$NON-NLS-1$
    private static final String PROP_MAX_RESULT = "search.data.max-results"; //$NON-NLS-1$
    private static final String PROP_MATCH_INDEX = "search.data.match-index"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search.data.history"; //$NON-NLS-1$
    private static final String PROP_SOURCES = "search.data.object-source"; //$NON-NLS-1$

    private IObjectSearchContainer container;
    private Combo searchText;
    private DatabaseNavigatorTree dataSourceTree;

    private String searchString;
    private boolean caseSensitive;
    private boolean fastSearch; // Indexed
    private int maxResults;
    private int matchTypeIndex;
    private Set<String> searchHistory = new LinkedHashSet<String>();
    private List<DBNNode> sourceNodes = new ArrayList<DBNNode>();

    public SearchDataPage() {
		super("Database objects search");
    }

	@Override
	public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite searchGroup = new Composite(parent, SWT.NONE);
        searchGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        searchGroup.setLayout(new GridLayout(3, false));
        setControl(searchGroup);
        UIUtils.createControlLabel(searchGroup, "String");
        searchText = new Combo(searchGroup, SWT.DROP_DOWN);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (searchString != null) {
            searchText.setText(searchString);
        }
        for (String history : searchHistory) {
            searchText.add(history);
        }
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                searchString = searchText.getText();
                updateEnablement();
            }
        });

        {
            //new Label(searchGroup, SWT.NONE);
            Composite optionsGroup2 = UIUtils.createPlaceholder(searchGroup, 5, 5);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 3;
            optionsGroup2.setLayoutData(gd);

            if (maxResults <= 0) {
                maxResults = 100;
            }

            final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(optionsGroup2, CoreMessages.dialog_search_objects_spinner_max_results, maxResults, 1, 10000);
            maxResultsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            maxResultsSpinner.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    maxResults = maxResultsSpinner.getSelection();
                }
            });

            final Button caseCheckbox = UIUtils.createCheckbox(optionsGroup2, CoreMessages.dialog_search_objects_case_sensitive, caseSensitive);
            maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            caseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    caseSensitive = caseCheckbox.getSelection();
                }
            });

        }

        Composite optionsGroup = new Composite(searchGroup, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        optionsGroup.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        optionsGroup.setLayoutData(gd);

        {
            final DBeaverCore core = DBeaverCore.getInstance();

            Group databasesGroup = UIUtils.createControlGroup(optionsGroup, "Databases", 1, GridData.FILL_BOTH, 0);
            final DBNProject projectNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());
            DBNNode rootNode = projectNode == null ? core.getNavigatorModel().getRoot() : projectNode.getDatabases();
            dataSourceTree = new DatabaseNavigatorTree(databasesGroup, rootNode, SWT.SINGLE | SWT.CHECK);
            dataSourceTree.setLayoutData(new GridData(GridData.FILL_BOTH));
            final CheckboxTreeViewer viewer = (CheckboxTreeViewer) dataSourceTree.getViewer();
            viewer.addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof TreeLoadNode) {
                        return true;
                    }
                    if (element instanceof DBNNode) {
                        if (element instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                            Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                            return folderItemsClass != null && DBSObjectContainer.class.isAssignableFrom(folderItemsClass);
                        }
                        if (element instanceof DBNLocalFolder ||
                            element instanceof DBNProjectDatabases ||
                            element instanceof DBNDataSource ||
                            (element instanceof DBSWrapper && ((DBSWrapper) element).getObject() instanceof DBSObjectContainer)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            viewer.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(CheckStateChangedEvent event) {
                    updateEnablement();
                }
            });
        }

        if (!sourceNodes.isEmpty()) {
            dataSourceTree.getViewer().setSelection(
                new StructuredSelection(sourceNodes));
        } else {
            updateEnablement();
        }
    }

    private List<DBSObject> getSelectedSources()
    {
        List<DBSObject> result = new ArrayList<DBSObject>();
        for (Object sel : ((CheckboxTreeViewer)dataSourceTree.getViewer()).getCheckedElements()) {
            if (sel instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) sel).getObject();
                if (object != null && object.getDataSource() != null) {
                    result.add(object);
                }
            }
        }
        return result;
    }

    private void updateEnablement()
    {
        boolean enabled = false;
        if (!getSelectedSources().isEmpty()) {
            enabled = true;
        }
        container.setSearchEnabled(enabled);
    }

    @Override
    public void setSearchContainer(IObjectSearchContainer container)
    {
        this.container = container;
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if (visible) {
            updateEnablement();
        }
    }

    @Override
    public SearchDataQuery createQuery() throws DBException
    {
        List<DBSObject> selectedSources = getSelectedSources();

        String dataSearchString = searchString;

        // Save search query
        if (!searchHistory.contains(dataSearchString)) {
            searchHistory.add(dataSearchString);
            searchText.add(dataSearchString);
        }

        SearchDataParams params = new SearchDataParams();
        params.setSources(selectedSources);
        params.setSearchString(dataSearchString);
        params.setCaseSensitive(caseSensitive);
        params.setMaxResults(maxResults);
        return SearchDataQuery.createQuery(params);

    }

    @Override
    public void loadState(IPreferenceStore store)
    {
        searchString = store.getString(PROP_MASK);
        caseSensitive = store.getBoolean(PROP_CASE_SENSITIVE);
        maxResults = store.getInt(PROP_MAX_RESULT);
        matchTypeIndex = store.getInt(PROP_MATCH_INDEX);
        for (int i = 0; ;i++) {
            String history = store.getString(PROP_HISTORY + "." + i); //$NON-NLS-1$
            if (CommonUtils.isEmpty(history)) {
                break;
            }
            searchHistory.add(history);
        }
        {
            final String sources = store.getString(PROP_SOURCES);
            if (!CommonUtils.isEmpty(sources)) {
                try {
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                        {
                            StringTokenizer st = new StringTokenizer(sources, "|"); //$NON-NLS-1$
                            while (st.hasMoreTokens()) {
                                String nodePath = st.nextToken();
                                try {
                                    DBNNode node = DBNModel.getInstance().getNodeByPath(monitor, nodePath);
                                    if (node != null) {
                                        sourceNodes.add(node);
                                    }
                                } catch (DBException e) {
                                    log.error(e);
                                }
                            }
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void saveState(IPreferenceStore store)
    {
        store.setValue(PROP_MASK, searchString);
        store.setValue(PROP_CASE_SENSITIVE, caseSensitive);
        store.setValue(PROP_MAX_RESULT, maxResults);
        store.setValue(PROP_MATCH_INDEX, matchTypeIndex);
        {
            // Object sources
            StringBuilder sourcesString = new StringBuilder();
            IStructuredSelection ss = (IStructuredSelection) dataSourceTree.getViewer().getSelection();
            for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
                DBNNode node = (DBNNode) iter.next();
                if (sourcesString.length() > 0) {
                    sourcesString.append("|"); //$NON-NLS-1$
                }
                sourcesString.append(node.getNodeItemPath());
            }
            store.setValue(PROP_SOURCES, sourcesString.toString());
        }

        {
            // Search history
            int historyIndex = 0;
            for (String history : searchHistory) {
                if (historyIndex >= 20) {
                    break;
                }
                store.setValue(PROP_HISTORY + "." + historyIndex, history); //$NON-NLS-1$
                historyIndex++;
            }
        }
    }

}
