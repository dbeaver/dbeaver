/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.search.AbstractSearchPage;
import org.jkiss.dbeaver.ui.views.navigator.database.CheckboxTreeManager;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;
import org.jkiss.utils.CommonUtils;

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
    private static final String PROP_HISTORY = "search.data.history"; //$NON-NLS-1$
    private static final String PROP_SOURCES = "search.data.object-source"; //$NON-NLS-1$

    private Combo searchText;
    private DatabaseNavigatorTree dataSourceTree;

    private SearchDataParams params = new SearchDataParams();
    private Set<String> searchHistory = new LinkedHashSet<String>();
    private CheckboxTreeManager checkboxTreeManager;
    private IPreferenceStore store;

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
        if (params.searchString != null) {
            searchText.setText(params.searchString);
        }
        for (String history : searchHistory) {
            searchText.add(history);
        }
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                params.searchString = searchText.getText();
                updateEnablement();
            }
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
            gd.heightHint = 300;
            databasesGroup.setLayoutData(gd);
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
            checkboxTreeManager = new CheckboxTreeManager(viewer,
                new Class[]{DBSDataContainer.class});
            viewer.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(CheckStateChangedEvent event) {
                    updateEnablement();
                }
            });
        }
        {
            //new Label(searchGroup, SWT.NONE);
            Composite optionsGroup2 = UIUtils.createControlGroup(optionsGroup, "Settings", 2, GridData.FILL_BOTH, 0);
            optionsGroup2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));

            if (params.maxResults <= 0) {
                params.maxResults = 10;
            }

            final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(optionsGroup2, "Sample rows", params.maxResults, 1, Integer.MAX_VALUE);
            maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            maxResultsSpinner.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    params.maxResults = maxResultsSpinner.getSelection();
                }
            });

            final Button caseCheckbox = UIUtils.createLabelCheckbox(optionsGroup2, CoreMessages.dialog_search_objects_case_sensitive, params.caseSensitive);
            caseCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            caseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.caseSensitive = caseCheckbox.getSelection();
                }
            });

            final Button fastSearchCheckbox = UIUtils.createLabelCheckbox(optionsGroup2, "Fast search (indexed)", params.fastSearch);
            fastSearchCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            fastSearchCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.fastSearch = fastSearchCheckbox.getSelection();
                }
            });


            final Button searchNumbersCheckbox = UIUtils.createLabelCheckbox(optionsGroup2, "Search in numbers", params.searchNumbers);
            searchNumbersCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            searchNumbersCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.searchNumbers = searchNumbersCheckbox.getSelection();
                }
            });

            final Button searchLOBCheckbox = UIUtils.createLabelCheckbox(optionsGroup2, "Search in LOBs", params.searchLOBs);
            searchLOBCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            searchLOBCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    params.searchLOBs = searchNumbersCheckbox.getSelection();
                }
            });
        }
        dataSourceTree.setEnabled(false);
        getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                List<DBNNode> checkedNodes = loadTreeState(store, PROP_SOURCES);

                if (!checkedNodes.isEmpty()) {
                    for (DBNNode node : checkedNodes) {
                        ((CheckboxTreeViewer) dataSourceTree.getViewer()).setChecked(node, true);
                    }
                    checkboxTreeManager.updateCheckStates();
                }
                updateEnablement();
                dataSourceTree.setEnabled(true);
            }
        });
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
    public void loadState(IPreferenceStore store)
    {
        params.searchString = store.getString(PROP_MASK);
        params.caseSensitive = store.getBoolean(PROP_CASE_SENSITIVE);
        params.fastSearch = store.getBoolean(PROP_FAST_SEARCH);
        params.searchNumbers = store.getString(PROP_SEARCH_NUMBERS) == null || store.getBoolean(PROP_SEARCH_NUMBERS);
        params.searchLOBs = store.getBoolean(PROP_SEARCH_LOBS);
        params.maxResults = store.getInt(PROP_SAMPLE_ROWS);
        for (int i = 0; ;i++) {
            String history = store.getString(PROP_HISTORY + "." + i); //$NON-NLS-1$
            if (CommonUtils.isEmpty(history)) {
                break;
            }
            searchHistory.add(history);
        }
        this.store = store;
    }

    @Override
    public void saveState(IPreferenceStore store)
    {
        store.setValue(PROP_MASK, params.searchString);
        store.setValue(PROP_CASE_SENSITIVE, params.caseSensitive);
        store.setValue(PROP_SAMPLE_ROWS, params.maxResults);
        store.setValue(PROP_FAST_SEARCH, params.fastSearch);
        store.setValue(PROP_SEARCH_NUMBERS, params.searchNumbers);
        store.setValue(PROP_SEARCH_LOBS, params.searchLOBs);
        saveTreeState(store, PROP_SOURCES, dataSourceTree);

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

    protected static void saveTreeState(IPreferenceStore store, String propName, DatabaseNavigatorTree tree)
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
        store.setValue(propName, sourcesString.toString());
    }

    protected List<DBSDataContainer> getCheckedSources()
    {
        List<DBSDataContainer> result = new ArrayList<DBSDataContainer>();
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
        if (!getCheckedSources().isEmpty()) {
            enabled = true;
        }
        container.setSearchEnabled(enabled);
    }

}
