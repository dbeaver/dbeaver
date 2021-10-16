/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.search.metadata;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.dbeaver.ui.search.AbstractSearchPage;
import org.jkiss.dbeaver.ui.search.internal.UISearchMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

public class SearchMetadataPage extends AbstractSearchPage {
    private static final String PROP_MASK = "search.metadata.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search.metadata.case-sensitive"; //$NON-NLS-1$
    private static final String PROP_MAX_RESULT = "search.metadata.max-results"; //$NON-NLS-1$
    private static final String PROP_MATCH_INDEX = "search.metadata.match-index"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search.metadata.history"; //$NON-NLS-1$
    private static final String PROP_OBJECT_TYPE = "search.metadata.object-type"; //$NON-NLS-1$
    private static final String PROP_SOURCES = "search.metadata.object-source"; //$NON-NLS-1$
    private static final String PROP_SEARCH_IN_COMMENTS = "search.metadata.search-in-comments"; //$NON-NLS-1$
    private static final String PROP_SEARCH_IN_DEFINITIONS = "search.metadata.search-in-definitions"; //$NON-NLS-1$

    private Table typesTable;
    private Combo searchText;
    private DatabaseNavigatorTree dataSourceTree;
    private Button searchInCommentsCheckbox;
    private Button searchInDefinitionsCheckbox;

    private String nameMask;
    private boolean caseSensitive;
    private boolean searchInComments;
    private boolean searchInDefinitions;
    private int maxResults;
    private int matchTypeIndex;
    private Set<DBSObjectType> checkedTypes = new HashSet<>();
    private Set<String> searchHistory = new LinkedHashSet<>();
    private Set<String> savedTypeNames = new HashSet<>();
    private List<DBNNode> sourceNodes = new ArrayList<>();
    private DBPProject currentProject;

    public SearchMetadataPage() {
        super("Database objects search");
        currentProject = NavigatorUtils.getSelectedProject();
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);

        initializeDialogUnits(parent);

        Composite searchGroup = UIUtils.createComposite(parent, 1);
        searchGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        setControl(searchGroup);

        searchText = new Combo(searchGroup, SWT.DROP_DOWN);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.addEmptyTextHint(searchText, combo -> UISearchMessages.dialog_search_objects_label_object_name);
        if (nameMask != null) {
            searchText.setText(nameMask);
        }
        for (String history : searchHistory) {
            searchText.add(history);
        }
        searchText.addModifyListener(e -> {
            nameMask = searchText.getText();
            updateEnablement();
        });

        Composite optionsGroup = new SashForm(searchGroup, 2);
        optionsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group sourceGroup = UIUtils.createControlGroup(optionsGroup, UISearchMessages.dialog_search_objects_group_objects_source, 1, GridData.FILL_BOTH, 0);
            DBPPlatform platform = DBWorkbench.getPlatform();
            final DBNProject projectNode = platform.getNavigatorModel().getRoot().getProjectNode(currentProject);
            DBNNode rootNode = projectNode == null ? platform.getNavigatorModel().getRoot() : projectNode.getDatabases();
            dataSourceTree = new DatabaseNavigatorTree(sourceGroup, rootNode, SWT.SINGLE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            dataSourceTree.setLayoutData(gd);

            dataSourceTree.getViewer().addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element)
                {
                    if (element instanceof TreeNodeSpecial) {
                        return true;
                    }
                    if (element instanceof DBNNode) {
                        if (element instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder)element;
                            Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                            return folderItemsClass != null && DBSObjectContainer.class.isAssignableFrom(folderItemsClass);
                        }
                        if (element instanceof DBNLocalFolder ||
                            element instanceof DBNProjectDatabases ||
                            element instanceof DBNDataSource ||
                            (element instanceof DBSWrapper && ((DBSWrapper)element).getObject() instanceof DBSObjectContainer))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            });
            dataSourceTree.getViewer().addSelectionChangedListener(
                event -> {
                    fillObjectTypes();
                    updateEnablement();
                    IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                    Object object = structSel.isEmpty() ? null : structSel.getFirstElement();
                    if (object instanceof DBNNode) {
                        for (DBNNode node = (DBNNode)object; node != null; node = node.getParentNode()) {
                            if (node instanceof DBNDataSource) {
                                DBNDataSource dsNode = (DBNDataSource) node;
                                try {
                                    dsNode.initializeNode(null, status -> {
                                        if (status.isOK()) {
                                            UIUtils.asyncExec(() -> {
                                                if (!dataSourceTree.isDisposed()) {
                                                    fillObjectTypes();
                                                }
                                            });
                                        }
                                    });
                                } catch (DBException e) {
                                    // shouldn't be here
                                    log.error(e);
                                }
                                break;
                            }
                        }
                    }
                }
            );
        }

        {
            Group settingsGroup = UIUtils.createControlGroup(optionsGroup, "Settings", 2, GridData.FILL_BOTH, 0);

            {
                //new Label(searchGroup, SWT.NONE);
                UIUtils.createControlLabel(settingsGroup, UISearchMessages.dialog_search_objects_label_match_type);
                final Combo matchCombo = new Combo(settingsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
                matchCombo.add(UISearchMessages.dialog_search_objects_combo_starts_with, SearchMetadataConstants.MATCH_INDEX_STARTS_WITH);
                matchCombo.add(UISearchMessages.dialog_search_objects_combo_contains, SearchMetadataConstants.MATCH_INDEX_CONTAINS);
                matchCombo.add(UISearchMessages.dialog_search_objects_combo_like, SearchMetadataConstants.MATCH_INDEX_LIKE);
                matchCombo.select(0);
                matchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                if (matchTypeIndex >= 0) {
                    matchCombo.select(matchTypeIndex);
                }
                matchCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        matchTypeIndex = matchCombo.getSelectionIndex();
                    }
                });
                matchCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                if (maxResults <= 0) {
                    maxResults = 100;
                }

                final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(settingsGroup, UISearchMessages.dialog_search_objects_spinner_max_results, maxResults, 1, 10000);
                maxResultsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                maxResultsSpinner.addModifyListener(e -> maxResults = maxResultsSpinner.getSelection());
                maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                Button caseCheckbox = UIUtils.createCheckbox(settingsGroup, UISearchMessages.dialog_search_objects_case_sensitive, null, caseSensitive, 2);
                caseCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        caseSensitive = caseCheckbox.getSelection();
                    }
                });

                searchInCommentsCheckbox = UIUtils.createCheckbox(settingsGroup, UISearchMessages.dialog_search_objects_search_in_comments, null, searchInComments, 2);
                searchInCommentsCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        searchInComments = searchInCommentsCheckbox.getSelection();
                    }
                });
                searchInCommentsCheckbox.setEnabled(false);

                searchInDefinitionsCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    UISearchMessages.dialog_search_objects_search_in_definitions,
                    null,
                    searchInDefinitions,
                    2
                );
                searchInDefinitionsCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        searchInDefinitions = searchInDefinitionsCheckbox.getSelection();
                    }
                });
                searchInDefinitionsCheckbox.setEnabled(false);
            }

            Label otLabel = UIUtils.createControlLabel(settingsGroup, UISearchMessages.dialog_search_objects_group_object_types);
            otLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            typesTable = new Table(settingsGroup, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
            typesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    //checkedTypes.clear();
                    for (TableItem item : typesTable.getItems()) {
                        DBSObjectType objectType = (DBSObjectType) item.getData();
                        if (item.getChecked()) {
                            checkedTypes.add(objectType);
                        } else {
                            checkedTypes.remove(objectType);
                        }
                    }
                    updateEnablement();
                    updateSearchOptionsCheckboxes();
                }
            });
            typesTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    TableItem[] selection = typesTable.getSelection();
                    if (selection.length > 0) {
                        TableItem tableItem = selection[0];
                        tableItem.setChecked(!tableItem.getChecked());
                    }
                }
            });
            typesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createTableColumn(typesTable, SWT.LEFT, UISearchMessages.dialog_search_objects_column_type);
            UIUtils.createTableColumn(typesTable, SWT.LEFT, UISearchMessages.dialog_search_objects_column_description);
        }

        UIUtils.asyncExec(this::loadState);
    }

    private void updateSearchOptionsCheckboxes() {
        DBSStructureAssistant structureAssistant = getSelectedStructureAssistant();
        boolean enableSearchInCommentsCheckbox = false;
        boolean enableSearchInDefinitionsCheckbox = false;
        for (DBSObjectType objectType: checkedTypes) {
            if (!enableSearchInCommentsCheckbox && structureAssistant.supportsSearchInCommentsFor(objectType)) {
                enableSearchInCommentsCheckbox = true;
            }
            if (!enableSearchInDefinitionsCheckbox && structureAssistant.supportsSearchInDefinitionsFor(objectType)) {
                enableSearchInDefinitionsCheckbox = true;
            }
            if (enableSearchInCommentsCheckbox && enableSearchInDefinitionsCheckbox) {
                break;
            }
        }
        searchInCommentsCheckbox.setEnabled(enableSearchInCommentsCheckbox);
        if (!enableSearchInCommentsCheckbox) {
            searchInCommentsCheckbox.setSelection(false);
            searchInComments = false;
        }
        searchInDefinitionsCheckbox.setEnabled(enableSearchInDefinitionsCheckbox);
        if (!enableSearchInDefinitionsCheckbox) {
            searchInDefinitionsCheckbox.setSelection(false);
            searchInDefinitions = false;
        }
    }

    private void loadState() {
        try {
            container.getRunnableContext().run(true, true, monitor -> {
                monitor.beginTask("Load database nodes", 1);
                try {
                    monitor.subTask("Load tree state");
                    sourceNodes = loadTreeState(
                        new DefaultProgressMonitor(monitor),
                        currentProject,
                        DBWorkbench.getPlatform().getPreferenceStore().getString(PROP_SOURCES));
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Data sources load", "Error loading settings", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }

        if (!sourceNodes.isEmpty()) {
            dataSourceTree.getViewer().setSelection(
                new StructuredSelection(sourceNodes));
            DBNDataSource node = DBNDataSource.getDataSourceNode(sourceNodes.get(0));
            if (node != null) {
                dataSourceTree.getViewer().reveal(node);
            }
        }
        updateEnablement();
    }

    @Nullable
    private DBNNode getSelectedNode()
    {
        IStructuredSelection selection = (IStructuredSelection) dataSourceTree.getViewer().getSelection();
        if (!selection.isEmpty()) {
            return (DBNNode) selection.getFirstElement();
        }
        return null;
    }

    private DBPDataSource getSelectedDataSource()
    {
        DBNNode node = getSelectedNode();
        if (node instanceof DBSWrapper) {
            DBSObject object = ((DBSWrapper)node).getObject();
            if (object != null && object.getDataSource() != null) {
                return object.getDataSource();
            }
        }
        return null;
    }

    private DBSStructureAssistant getSelectedStructureAssistant()
    {
        return DBUtils.getAdapter(DBSStructureAssistant.class, getSelectedDataSource());
    }

    private void fillObjectTypes()
    {
        DBSStructureAssistant assistant = getSelectedStructureAssistant();
        typesTable.removeAll();
        if (assistant == null) {
            // No structure assistant - no object types
        } else {
            for (DBSObjectType objectType : assistant.getSupportedObjectTypes()) {
                TableItem item = new TableItem(typesTable, SWT.NONE);
                item.setText(objectType.getTypeName());
                if (objectType.getImage() != null) {
                    item.setImage(0, DBeaverIcons.getImage(objectType.getImage()));
                }
                if (!CommonUtils.isEmpty(objectType.getDescription())) {
                    item.setText(1, objectType.getDescription());
                }
                item.setData(objectType);
                if (checkedTypes.contains(objectType)) {
                    item.setChecked(true);
                } else if (savedTypeNames.contains(objectType.getTypeName())) {
                    item.setChecked(true);
                    checkedTypes.add(objectType);
                    savedTypeNames.remove(objectType.getTypeName());
                }
            }
            updateSearchOptionsCheckboxes();
        }
        for (TableColumn column : typesTable.getColumns()) {
            column.pack();
        }
        updateEnablement();
    }

    @Override
    public SearchMetadataQuery createQuery() {
        DBSObject parentObject = null;
        for (DBNNode node = getSelectedNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) node).getObject();
                if (object instanceof DBSStructContainer || object instanceof DBPDataSourceContainer) {
                    parentObject = object;
                    break;
                }
            }
        }

        DBPDataSource dataSource = getSelectedDataSource();
        DBSStructureAssistant assistant = getSelectedStructureAssistant();
        if (dataSource == null || assistant == null) {
            throw new IllegalStateException("No active datasource");
        }
        java.util.List<DBSObjectType> objectTypes = new ArrayList<>();
        for (TableItem item : typesTable.getItems()) {
            if (item.getChecked()) {
                objectTypes.add((DBSObjectType) item.getData());
            }
        }
        String objectNameMask = nameMask;

        // Save search query
        if (!searchHistory.contains(objectNameMask)) {
            searchHistory.add(objectNameMask);
            searchText.add(objectNameMask);
        }

        if (matchTypeIndex == SearchMetadataConstants.MATCH_INDEX_STARTS_WITH) {
            if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
            }
        } else if (matchTypeIndex == SearchMetadataConstants.MATCH_INDEX_CONTAINS) {
            if (!objectNameMask.startsWith("%")) { //$NON-NLS-1$
                objectNameMask = "%" + objectNameMask; //$NON-NLS-1$
            }
            if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
            }
        }

        DBSStructureAssistant.ObjectsSearchParams params = new DBSStructureAssistant.ObjectsSearchParams(
                objectTypes.toArray(new DBSObjectType[0]),
                objectNameMask
        );
        params.setParentObject(parentObject);
        params.setCaseSensitive(caseSensitive);
        params.setSearchInComments(searchInComments);
        params.setMaxResults(maxResults);
        params.setSearchInDefinitions(searchInDefinitions);
        params.setGlobalSearch(true);

        return new SearchMetadataQuery(dataSource, assistant, params);
    }

    @Override
    public void loadState(DBPPreferenceStore store)
    {
        nameMask = store.getString(PROP_MASK);
        caseSensitive = store.getBoolean(PROP_CASE_SENSITIVE);
        searchInComments = store.getBoolean(PROP_SEARCH_IN_COMMENTS);
        searchInDefinitions = store.getBoolean(PROP_SEARCH_IN_DEFINITIONS);
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
            String type = store.getString(PROP_OBJECT_TYPE);
            if (!CommonUtils.isEmpty(type)) {
                StringTokenizer st = new StringTokenizer(type, "|"); //$NON-NLS-1$
                while (st.hasMoreTokens()) {
                    savedTypeNames.add(st.nextToken());
                }
            }
        }
    }

    @Override
    public void saveState(DBPPreferenceStore store)
    {
        store.setValue(PROP_MASK, nameMask);
        store.setValue(PROP_CASE_SENSITIVE, caseSensitive);
        store.setValue(PROP_SEARCH_IN_COMMENTS, searchInComments);
        store.setValue(PROP_SEARCH_IN_DEFINITIONS, searchInDefinitions);
        store.setValue(PROP_MAX_RESULT, maxResults);
        store.setValue(PROP_MATCH_INDEX, matchTypeIndex);
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
        {
            // Object types
            StringBuilder typesString = new StringBuilder();
            for (DBSObjectType type : checkedTypes) {
                if (typesString.length() > 0) {
                    typesString.append("|"); //$NON-NLS-1$
                }
                typesString.append(type.getTypeName());
            }
            store.setValue(PROP_OBJECT_TYPE, typesString.toString());
        }
    }

    protected void updateEnablement()
    {
        boolean enabled = false;
        if (getSelectedDataSource() != null) {
            enabled = !checkedTypes.isEmpty();
        }
        if (CommonUtils.isEmpty(nameMask)) {
            enabled = false;
        }

        container.setPerformActionEnabled(enabled);
    }

    protected static void saveTreeState(DBPPreferenceStore store, String propName, DatabaseNavigatorTree tree)
    {
        // Object sources
        StringBuilder sourcesString = new StringBuilder();
        Object[] nodes = ((IStructuredSelection)tree.getViewer().getSelection()).toArray();
        for (Object obj : nodes) {
            DBNNode node = (DBNNode) obj;
            if (sourcesString.length() > 0) {
                sourcesString.append("|"); //$NON-NLS-1$
            }
            sourcesString.append(node.getNodeItemPath());
        }
        store.setValue(propName, sourcesString.toString());
    }
}
