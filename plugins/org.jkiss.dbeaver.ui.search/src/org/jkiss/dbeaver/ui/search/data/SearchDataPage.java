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
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;
import org.jkiss.dbeaver.ui.search.AbstractSearchPage;
import org.jkiss.dbeaver.ui.search.internal.UISearchMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

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

    private SearchDataParams params = new SearchDataParams();
    private Set<String> searchHistory = new LinkedHashSet<>();

    private static final Map<Class<? extends AbstractSearchPage>, String> searchStateCache = new IdentityHashMap<>();
    private DatabaseObjectsSelectorPanel selectorPanel;

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
            Group databasesGroup = UIUtils.createControlGroup(optionsGroup, "Databases", 1, GridData.FILL_BOTH, 0);
            databasesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

            selectorPanel = new DatabaseObjectsSelectorPanel(
                databasesGroup,
                true,
                new RunnableContextDelegate(container.getRunnableContext())) {
                @Override
                protected boolean isDatabaseObjectVisible(DBSObject obj) {
                    if (obj instanceof DBSDataContainer && obj instanceof DBSEntity) {
                        if ((((DBSDataContainer) obj).getSupportedFeatures() & DBSDataContainer.DATA_SEARCH) == 0) {
                            return false;
                        }
                    }
                    return super.isDatabaseObjectVisible(obj);
                }

                @Override
                protected void onSelectionChange(Object element) {
                    updateEnablement();
                }
            };
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

            final Button caseCheckbox = UIUtils.createCheckbox(optionsGroup2, UISearchMessages.dialog_search_objects_case_sensitive, "Case sensitive search", params.caseSensitive, 2);
            caseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.caseSensitive = caseCheckbox.getSelection();
                }
            });

            final Button fastSearchCheckbox = UIUtils.createCheckbox(optionsGroup2, "Fast search (indexed)", "Search only in indexed columns", params.fastSearch, 2);
            fastSearchCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.fastSearch = fastSearchCheckbox.getSelection();
                }
            });


            final Button searchNumbersCheckbox = UIUtils.createCheckbox(optionsGroup2, "Search in numbers", "Search in numeric columns (search value must be a number)", params.searchNumbers, 2);
            searchNumbersCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.searchNumbers = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchLOBCheckbox = UIUtils.createCheckbox(optionsGroup2, "Search in LOBs", "Search in BLOB/CLOB/binary columns", params.searchLOBs, 2);
            searchLOBCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.searchLOBs = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchForeignCheckbox = UIUtils.createCheckbox(optionsGroup2, "Search in foreign objects", "Search in foreign tables or DB links. Searching in such tables may cause performance issues.", params.searchForeignObjects, 2);
            searchForeignCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.searchForeignObjects = searchForeignCheckbox.getSelection();
                }
            });
        }
        UIUtils.asyncExec(this::restoreCheckedNodes);

        if (!params.selectedNodes.isEmpty()) {
            selectorPanel.setSelection(params.selectedNodes);
        }

        selectorPanel.setEnabled(true);
    }

    @Override
    public SearchDataQuery createQuery() throws DBException {
        params.sources = getCheckedSources();

        // Save search query
        if (!searchHistory.contains(params.searchString)) {
            searchHistory.add(params.searchString);
            searchText.add(params.searchString);
        }

        return SearchDataQuery.createQuery(params);

    }

    @Override
    public void loadState(DBPPreferenceStore store) {
        params.searchString = store.getString(PROP_MASK);
        params.caseSensitive = store.getBoolean(PROP_CASE_SENSITIVE);
        params.fastSearch = store.getBoolean(PROP_FAST_SEARCH);
        params.searchNumbers = store.getString(PROP_SEARCH_NUMBERS) == null || store.getBoolean(PROP_SEARCH_NUMBERS);
        params.searchLOBs = store.getBoolean(PROP_SEARCH_LOBS);
        params.searchForeignObjects = store.getBoolean(PROP_SEARCH_FOREIGN);
        params.maxResults = store.getInt(PROP_SAMPLE_ROWS);
        for (int i = 0; ; i++) {
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
    public void saveState(DBPPreferenceStore store) {
        store.setValue(PROP_MASK, params.searchString);
        store.setValue(PROP_CASE_SENSITIVE, params.caseSensitive);
        store.setValue(PROP_SAMPLE_ROWS, params.maxResults);
        store.setValue(PROP_FAST_SEARCH, params.fastSearch);
        store.setValue(PROP_SEARCH_NUMBERS, params.searchNumbers);
        store.setValue(PROP_SEARCH_LOBS, params.searchLOBs);
        store.setValue(PROP_SEARCH_FOREIGN, params.searchForeignObjects);
        saveTreeState();

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

    private List<DBSDataContainer> getCheckedSources() {
        List<DBSDataContainer> result = new ArrayList<>();
        for (DBNNode node : selectorPanel.getCheckedNodes()) {
            if (node instanceof DBNDatabaseNode) {
                DBSObject object = ((DBNDatabaseNode) node).getObject();
                if (object instanceof DBSDataContainer && object.getDataSource() != null) {
                    result.add((DBSDataContainer) object);
                }
            }
        }
        return result;
    }

    protected void updateEnablement() {
        container.setPerformActionEnabled(selectorPanel.hasCheckedNodes());
    }

    protected void saveTreeState() {
        // Object sources
        StringBuilder sourcesString = new StringBuilder();
        for (DBNNode node : selectorPanel.getCheckedNodes()) {
            if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSDataContainer) {
                if (sourcesString.length() > 0) {
                    sourcesString.append("|"); //$NON-NLS-1$
                }
                sourcesString.append(node.getNodeItemPath());
            }
        }
        searchStateCache.put(getClass(), sourcesString.toString());
    }

    protected List<DBNNode> loadTreeState(DBRProgressMonitor monitor) {
        final String sources = searchStateCache.get(getClass());
        return loadTreeState(monitor, NavigatorUtils.getSelectedProject(), sources);
    }

    private void restoreCheckedNodes() {
        final List<DBNNode> checkedNodes = new ArrayList<>();
        try {
            container.getRunnableContext().run(false, true, monitor -> {
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
            selectorPanel.checkNodes(checkedNodes, false);
            updateEnablement();
        }
    }

}
