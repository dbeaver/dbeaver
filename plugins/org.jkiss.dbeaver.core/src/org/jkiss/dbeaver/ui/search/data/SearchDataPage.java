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
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.dbeaver.ui.search.AbstractSearchPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class SearchDataPage extends AbstractSearchPage {

    private static final String PROP_MASK = "search.data.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search.data.case-sensitive"; //$NON-NLS-1$
    private static final String PROP_SAMPLE_ROWS = "search.data.sample-rows"; //$NON-NLS-1$
    private static final String PROP_FAST_SEARCH = "search.data.fast-search"; //$NON-NLS-1$
    private static final String PROP_SEARCH_NUMBERS = "search.data.search-numbers"; //$NON-NLS-1$
    private static final String PROP_SEARCH_LOBS = "search.data.search-lobs"; //$NON-NLS-1$
    private static final String PROP_SEARCH_FOREIGN = "search.data.search-foreign"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search.data.history"; //$NON-NLS-1$

    private Combo searchText;
    private DatabaseNavigatorTree dataSourceTree;

    private SearchDataParams params = new SearchDataParams();
    private Set<String> searchHistory = new LinkedHashSet<>();
    private DatabaseObjectsTreeManager checkboxTreeManager;

    private static final Map<Class<? extends AbstractSearchPage>, String> searchStateCache = new IdentityHashMap<>();

    public SearchDataPage() {
		super("Database objects search");
    }

	@Override
	public void createControl(Composite parent) {
        super.createControl(parent);
        initializeDialogUnits(parent);

        Composite searchGroup = new Composite(parent, SWT.NONE);
        searchGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        searchGroup.setLayout(new GridLayout(3, false));
        setControl(searchGroup);
        UIUtils.createControlLabel(searchGroup, "String");
        searchText = new Combo(searchGroup, SWT.DROP_DOWN);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (params.searchString != null) {
            searchText.setText(params.searchString);
        }
        for (String history : searchHistory) {
            searchText.add(history);
        }
        searchText.addModifyListener(e -> {
            params.searchString = searchText.getText();
            updateEnablement();
        });

        Composite optionsGroup = new SashForm(searchGroup, SWT.NONE);
        GridLayout layout = new GridLayout(2, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        optionsGroup.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        optionsGroup.setLayoutData(gd);

        {
            final DBeaverCore core = DBeaverCore.getInstance();

            Group databasesGroup = UIUtils.createControlGroup(optionsGroup, "Databases", 1, GridData.FILL_BOTH, 0);
            gd = new GridData(GridData.FILL_BOTH);
            //gd.heightHint = 300;
            databasesGroup.setLayoutData(gd);
            final DBNProject projectNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());
            DBNNode rootNode = projectNode == null ? core.getNavigatorModel().getRoot() : projectNode.getDatabases();
            dataSourceTree = new DatabaseNavigatorTree(databasesGroup, rootNode, SWT.SINGLE | SWT.CHECK);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            dataSourceTree.setLayoutData(gd);
            final CheckboxTreeViewer viewer = (CheckboxTreeViewer) dataSourceTree.getViewer();
            viewer.addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof TreeNodeSpecial) {
                        return true;
                    }
                    if (element instanceof DBNNode) {
                        if (element instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                            Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                            return folderItemsClass != null &&
                                (DBSObjectContainer.class.isAssignableFrom(folderItemsClass) ||
                                    DBSEntity.class.isAssignableFrom(folderItemsClass));
                        }
                        if (element instanceof DBNLocalFolder ||
                            element instanceof DBNProjectDatabases ||
                            element instanceof DBNDataSource)
                        {
                            return true;
                        }
                        if (element instanceof DBSWrapper) {
                            DBSObject obj = ((DBSWrapper) element).getObject();
                            if (obj instanceof DBSObjectContainer) return true;
                            if (obj instanceof DBSDataContainer && obj instanceof DBSEntity) {
                                if ((((DBSDataContainer)obj).getSupportedFeatures() & DBSDataContainer.DATA_SEARCH) != 0) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            });
            checkboxTreeManager = new DatabaseObjectsTreeManager(new RunnableContextDelegate(container.getRunnableContext()), viewer,
                new Class[]{DBSDataContainer.class});
            viewer.addCheckStateListener(event -> updateEnablement());
        }
        {
            //new Label(searchGroup, SWT.NONE);
            Composite optionsGroup2 = UIUtils.createControlGroup(optionsGroup, "Settings", 2, GridData.FILL_HORIZONTAL, 0);
            optionsGroup2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));

            if (params.maxResults <= 0) {
                params.maxResults = 10;
            }

            final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(optionsGroup2, "Sample rows", "Maximum number of rows to search. Don't set to a big number, this might greatly reduce search performance.", params.maxResults, 1, Integer.MAX_VALUE);
            maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            maxResultsSpinner.addModifyListener(e -> params.maxResults = maxResultsSpinner.getSelection());

            final Button caseCheckbox = UIUtils.createCheckbox(optionsGroup2, CoreMessages.dialog_search_objects_case_sensitive, "Case sensitive search", params.caseSensitive, 2);
            caseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.caseSensitive = caseCheckbox.getSelection();
                }
            });

            final Button fastSearchCheckbox = UIUtils.createCheckbox(optionsGroup2, "Fast search (indexed)", "Search only in indexed columns", params.fastSearch, 2);
            fastSearchCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.fastSearch = fastSearchCheckbox.getSelection();
                }
            });


            final Button searchNumbersCheckbox = UIUtils.createCheckbox(optionsGroup2, "Search in numbers", "Search in numeric columns (search value must be a number)", params.searchNumbers, 2);
            searchNumbersCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.searchNumbers = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchLOBCheckbox = UIUtils.createCheckbox(optionsGroup2, "Search in LOBs", "Search in BLOB/CLOB/binary columns", params.searchLOBs, 2);
            searchLOBCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.searchLOBs = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchForeignCheckbox = UIUtils.createCheckbox(optionsGroup2, "Search in foreign objects", "Search in foreign tables or DB links. Searching in such tables may cause performance issues.", params.searchForeignObjects, 2);
            searchForeignCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.searchForeignObjects = searchForeignCheckbox.getSelection();
                }
            });
        }
        UIUtils.asyncExec(this::restoreCheckedNodes);

        if (!params.selectedNodes.isEmpty()) {
            dataSourceTree.getViewer().setSelection(
                new StructuredSelection(params.selectedNodes), true);
        }

        dataSourceTree.setEnabled(true);
    }

    @Override
    public SearchDataQuery createQuery() throws DBException
    {
        params.sources = getCheckedSources();

        // Save search query
        if (!searchHistory.contains(params.searchString)) {
            searchHistory.add(params.searchString);
            searchText.add(params.searchString);
        }

        return SearchDataQuery.createQuery(params);

    }

    @Override
    public void loadState(DBPPreferenceStore store)
    {
        params.searchString = store.getString(PROP_MASK);
        params.caseSensitive = store.getBoolean(PROP_CASE_SENSITIVE);
        params.fastSearch = store.getBoolean(PROP_FAST_SEARCH);
        params.searchNumbers = store.getString(PROP_SEARCH_NUMBERS) == null || store.getBoolean(PROP_SEARCH_NUMBERS);
        params.searchLOBs = store.getBoolean(PROP_SEARCH_LOBS);
        params.searchForeignObjects = store.getBoolean(PROP_SEARCH_FOREIGN);
        params.maxResults = store.getInt(PROP_SAMPLE_ROWS);
        for (int i = 0; ;i++) {
            String history = store.getString(PROP_HISTORY + "." + i); //$NON-NLS-1$
            if (CommonUtils.isEmpty(history)) {
                break;
            }
            searchHistory.add(history);
        }

        params.selectedNodes.clear();
        ISelection selection = container.getSelection();
        if (selection instanceof IStructuredSelection) {
            for (Object selItem : ((IStructuredSelection) selection).toArray()) {
                if (selItem instanceof DBNNode) {
                    params.selectedNodes.add((DBNNode) selItem);
                }
            }
        }
    }

    @Override
    public void saveState(DBPPreferenceStore store)
    {
        store.setValue(PROP_MASK, params.searchString);
        store.setValue(PROP_CASE_SENSITIVE, params.caseSensitive);
        store.setValue(PROP_SAMPLE_ROWS, params.maxResults);
        store.setValue(PROP_FAST_SEARCH, params.fastSearch);
        store.setValue(PROP_SEARCH_NUMBERS, params.searchNumbers);
        store.setValue(PROP_SEARCH_LOBS, params.searchLOBs);
        store.setValue(PROP_SEARCH_FOREIGN, params.searchForeignObjects);
        saveTreeState(dataSourceTree);

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

    private List<DBSDataContainer> getCheckedSources()
    {
        List<DBSDataContainer> result = new ArrayList<>();
        for (Object sel : ((CheckboxTreeViewer)dataSourceTree.getViewer()).getCheckedElements()) {
            if (sel instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) sel).getObject();
                if (object instanceof DBSDataContainer && object.getDataSource() != null) {
                    result.add((DBSDataContainer) object);
                }
            }
        }
        return result;
    }

    protected void updateEnablement()
    {
        boolean enabled = false;
        if (!ArrayUtils.isEmpty(((CheckboxTreeViewer)dataSourceTree.getViewer()).getCheckedElements())) {
            enabled = true;
        }
        container.setPerformActionEnabled(enabled);
    }

    protected void saveTreeState(DatabaseNavigatorTree tree)
    {
        // Object sources
        StringBuilder sourcesString = new StringBuilder();
        for (Object obj : ((CheckboxTreeViewer) tree.getViewer()).getCheckedElements()) {
            DBNNode node = (DBNNode) obj;
            if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSDataContainer) {
                if (sourcesString.length() > 0) {
                    sourcesString.append("|"); //$NON-NLS-1$
                }
                sourcesString.append(node.getNodeItemPath());
            }
        }
        searchStateCache.put(getClass(), sourcesString.toString());
    }

    protected List<DBNNode> loadTreeState(DBRProgressMonitor monitor)
    {
        final String sources = searchStateCache.get(getClass());
        return loadTreeState(monitor, sources);
    }

    private void restoreCheckedNodes() {
        final List<DBNNode> checkedNodes = new ArrayList<>();
        try {
            container.getRunnableContext().run(true, true, monitor -> {
                monitor.beginTask("Load database nodes", 1);
                try {
                    monitor.subTask("Load tree state");
                    checkedNodes.addAll(
                        loadTreeState(new DefaultProgressMonitor(monitor)));
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Data sources load", "Error loading settings", e.getTargetException());
        } catch (InterruptedException e) {
            // Ignore
        }

        if (!checkedNodes.isEmpty()) {
            boolean first = true;
            for (DBNNode node : checkedNodes) {
                ((CheckboxTreeViewer) dataSourceTree.getViewer()).setChecked(node, true);
                if (first) {
                    DBNDataSource dsNode = DBNDataSource.getDataSourceNode(node);
                    if (dsNode != null) {
                        dataSourceTree.getViewer().reveal(dsNode);
                    }
                    first = false;
                }
            }
            checkboxTreeManager.updateCheckStates();
            updateEnablement();
        }
    }

}
