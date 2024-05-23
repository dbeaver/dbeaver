/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.dbeaver.ui.search.AbstractSearchPage;
import org.jkiss.dbeaver.ui.search.internal.UISearchMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SearchDataPage extends AbstractSearchPage {

    private static final String PROP_MASK = "search.data.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search.data.case-sensitive"; //$NON-NLS-1$
    private static final String PROP_SAMPLE_ROWS = "search.data.sample-rows"; //$NON-NLS-1$
    private static final String PROP_FAST_SEARCH = "search.data.fast-search"; //$NON-NLS-1$
    private static final String PROP_SEARCH_NUMBERS = "search.data.search-numbers"; //$NON-NLS-1$
    private static final String PROP_SEARCH_LOBS = "search.data.search-lobs"; //$NON-NLS-1$
    private static final String PROP_SEARCH_FOREIGN = "search.data.search-foreign"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search.data.history"; //$NON-NLS-1$

    private static final String PROP_SOURCES = "search.data.object-source"; //$NON-NLS-1$
    private static final String PROP_SHOW_CONNECTED = "search.data.show-connected-only"; //$NON-NLS-1$

    private Combo searchText;

    private SearchDataParams params = new SearchDataParams();
    private Set<String> searchHistory = new LinkedHashSet<>();

    private DatabaseNavigatorTree navigatorTree;

    private DBPProject currentProject;
    private boolean showConnected;

    public SearchDataPage() {
        super("Database objects search");
        currentProject = NavigatorUtils.getSelectedProject();
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);

        showConnected = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PROP_SHOW_CONNECTED);

        initializeDialogUnits(parent);

        Composite searchGroup = UIUtils.createComposite(parent, 1);
        searchGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        searchText = new Combo(searchGroup, SWT.DROP_DOWN);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.addEmptyTextHint(searchText, combo -> UISearchMessages.dialog_data_search_hint_text_string_to_search);
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

        SashForm optionsGroup = new SashForm(parent, SWT.NONE);
        optionsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group databasesGroup = UIUtils.createControlGroup(
                optionsGroup,
                UISearchMessages.dialog_data_search_control_group_databases,
                1,
                GridData.FILL_BOTH,
                0);
            databasesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

            DBPPlatform platform = DBWorkbench.getPlatform();
            final DBNProject projectNode = platform.getNavigatorModel().getRoot().getProjectNode(currentProject);
            DBNNode rootNode = projectNode == null ? platform.getNavigatorModel().getRoot() : projectNode.getDatabases();

            navigatorTree = new DatabaseNavigatorTree(databasesGroup, rootNode, SWT.MULTI);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            navigatorTree.setLayoutData(gd);

            TreeViewer treeViewer = navigatorTree.getViewer();

            treeViewer.addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof TreeNodeSpecial) {
                        return true;
                    }
                    if (showConnected) {
                        if (element instanceof DBNDataSource ds && ds.getDataSource() == null ||
                            element instanceof DBNLocalFolder lf && !lf.hasConnected()) {
                            return false;
                        }
                    }
                    if (element instanceof DBNNode) {
                        if (element instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                            Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                            return folderItemsClass != null
                                && (DBSObjectContainer.class.isAssignableFrom(folderItemsClass)
                                    || DBSEntity.class.isAssignableFrom(folderItemsClass));
                        }
                        if (element instanceof DBNProjectDatabases
                            || element instanceof DBNLocalFolder
                            || element instanceof DBNDataSource) {
                            return true;
                        }
                        if (element instanceof DBSWrapper) {
                            DBSObject object = ((DBSWrapper) element).getObject();
                            if (object instanceof DBSDataContainer && object instanceof DBSEntity
                                && !((DBSDataContainer) object).isFeatureSupported(DBSDataContainer.FEATURE_DATA_SEARCH)) {
                                return false;
                            }
                            return object instanceof DBSInstance
                                || object instanceof DBSObjectContainer
                                || (object instanceof DBSDataContainer && object instanceof DBSEntity);
                        }
                    }
                    return false;
                }
            });

            treeViewer.addSelectionChangedListener(event -> updateEnablement());

            treeViewer.addDoubleClickListener(event -> {
                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                for (Object node : selection.toArray()) {
                    if (node instanceof TreeNodeSpecial) {
                        ((TreeNodeSpecial) node).handleDefaultAction(navigatorTree);
                    }
                }
            });

            final Button showConnectedCheck = new Button(databasesGroup, SWT.CHECK);
            showConnectedCheck.setText(UINavigatorMessages.label_show_connected);
            showConnectedCheck.setSelection(showConnected);
            showConnectedCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showConnected = showConnectedCheck.getSelection();
                    treeViewer.refresh();
                    DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_SHOW_CONNECTED, showConnected);
                }
            });
        }

        {
            Composite optionsGroup2 = UIUtils.createControlGroup(
                optionsGroup,
                UISearchMessages.dialog_data_search_control_group_settings,
                2,
                GridData.FILL_HORIZONTAL,
                0);
            optionsGroup2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_BEGINNING
                | GridData.VERTICAL_ALIGN_BEGINNING));

            if (params.maxResults <= 0) {
                params.maxResults = 10;
            }

            final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(
                optionsGroup2,
                UISearchMessages.dialog_data_search_spinner_max_results,
                UISearchMessages.dialog_data_search_spinner_max_results_tip, params.maxResults,
                1,
                Integer.MAX_VALUE);
            maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            maxResultsSpinner.addModifyListener(e -> params.maxResults = maxResultsSpinner.getSelection());

            final Button caseCheckbox = UIUtils.createCheckbox(
                optionsGroup2,
                UISearchMessages.dialog_search_objects_case_sensitive,
                UISearchMessages.dialog_data_search_checkbox_case_sensitive_tip, params.caseSensitive,
                2);
            caseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.caseSensitive = caseCheckbox.getSelection();
                }
            });

            final Button fastSearchCheckbox = UIUtils.createCheckbox(
                optionsGroup2,
                UISearchMessages.dialog_data_search_checkbox_fast_search,
                UISearchMessages.dialog_data_search_checkbox_fast_search_tip, params.fastSearch,
                2);
            fastSearchCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.fastSearch = fastSearchCheckbox.getSelection();
                }
            });


            final Button searchNumbersCheckbox = UIUtils.createCheckbox(
                optionsGroup2,
                UISearchMessages.dialog_data_search_checkbox_search_in_numbers,
                UISearchMessages.dialog_data_search_checkbox_search_in_numbers_tip, params.searchNumbers,
                2);
            searchNumbersCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.searchNumbers = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchLOBCheckbox = UIUtils.createCheckbox(
                optionsGroup2,
                UISearchMessages.dialog_data_search_checkbox_search_in_lob,
                UISearchMessages.dialog_data_search_checkbox_search_in_lob_tip, params.searchLOBs,
                2);
            searchLOBCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.searchLOBs = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchForeignCheckbox = UIUtils.createCheckbox(
                optionsGroup2,
                UISearchMessages.dialog_data_search_checkbox_search_in_foreign_objects,
                UISearchMessages.dialog_data_search_checkbox_search_in_foreign_objects_tip,
                params.searchForeignObjects,
                2);
            searchForeignCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    params.searchForeignObjects = searchForeignCheckbox.getSelection();
                }
            });

            Control infoLabel = UIUtils.createInfoLabel(
                optionsGroup2,
                UISearchMessages.dialog_data_search_info_label_use_ctrl,
                GridData.FILL_BOTH,
                2);
            GridData gridData = new GridData(SWT.FILL, SWT.END, true, true);
            gridData.horizontalSpan = 2;
            infoLabel.setLayoutData(gridData);
        }
        UIUtils.asyncExec(this::restoreCheckedNodes);

        if (!params.selectedNodes.isEmpty()) {
            navigatorTree.getViewer().setSelection(new StructuredSelection(params.selectedNodes));
        }

        navigatorTree.setEnabled(true);

        setControl(searchGroup);
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
    public void saveState(@NotNull DBPPreferenceStore store) {
        store.setValue(PROP_MASK, params.searchString);
        store.setValue(PROP_CASE_SENSITIVE, params.caseSensitive);
        store.setValue(PROP_SAMPLE_ROWS, params.maxResults);
        store.setValue(PROP_FAST_SEARCH, params.fastSearch);
        store.setValue(PROP_SEARCH_NUMBERS, params.searchNumbers);
        store.setValue(PROP_SEARCH_LOBS, params.searchLOBs);
        store.setValue(PROP_SEARCH_FOREIGN, params.searchForeignObjects);
        saveTreeState(store);

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

    private Object[] getCheckedElements() {
        Object[] objects = ((IStructuredSelection) navigatorTree.getViewer().getSelection()).toArray();
        if (ArrayUtils.isEmpty(objects)) {
            return new Object[0];
        }
        return objects;
    }

    private List<DBSDataContainer> getCheckedSources() {
        List<DBSDataContainer> result = new ArrayList<>();
        Object[] elements = getCheckedElements();
        RuntimeUtils.runTask(monitor -> {
            for (Object node : elements) {
                if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    try {
                        List<DBSDataContainer> containers = DBUtils.getAllDataContainersFromParentContainer(monitor, object);
                        if (!CommonUtils.isEmpty(containers)) {
                            result.addAll(containers);
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            }
        }, "Loading all objects for search", 5000);
        return result;
    }

    protected void updateEnablement() {
        container.setPerformActionEnabled(hasCheckedNodes());
    }

    private boolean hasCheckedNodes() {
        for (Object element : getCheckedElements()) {
            if (element instanceof DBNNode) {
                return true;
            }
        }
        return false;
    }

    private void saveTreeState(@NotNull DBPPreferenceStore store) {
        // Object sources
        StringBuilder sourcesString = new StringBuilder();
        Object[] nodes = getCheckedElements();
        for (Object node : nodes) {
            if (node instanceof DBNDatabaseNode) {
                DBSObject object = ((DBNDatabaseNode) node).getObject();
                if (object instanceof DBSDataContainer || object instanceof DBSObjectContainer) {
                    if (sourcesString.length() > 0) {
                        sourcesString.append("|"); //$NON-NLS-1$
                    }
                    sourcesString.append(((DBNDatabaseNode) node).getNodeUri());
                }
            }
        }
        store.setValue(PROP_SOURCES, sourcesString.toString());
    }

    private List<DBNNode> loadTreeState(DBRProgressMonitor monitor) {
        return loadTreeState(
            monitor,
            NavigatorUtils.getSelectedProject(),
            DBWorkbench.getPlatform().getPreferenceStore().getString(PROP_SOURCES));
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
            navigatorTree.getViewer().setSelection(new StructuredSelection(checkedNodes));
            DBNDataSource node = DBNDataSource.getDataSourceNode(checkedNodes.get(0));
            if (node != null) {
                navigatorTree.getViewer().reveal(node);
            }
            updateEnablement();
        }
    }

}
