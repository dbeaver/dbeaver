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
package org.jkiss.dbeaver.ui.search.metadata;

import org.eclipse.core.runtime.IStatus;
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
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadNode;
import org.jkiss.dbeaver.ui.search.AbstractSearchPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class SearchMetadataPage extends AbstractSearchPage {

    private static final String PROP_MASK = "search.metadata.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search.metadata.case-sensitive"; //$NON-NLS-1$
    private static final String PROP_MAX_RESULT = "search.metadata.max-results"; //$NON-NLS-1$
    private static final String PROP_MATCH_INDEX = "search.metadata.match-index"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search.metadata.history"; //$NON-NLS-1$
    private static final String PROP_OBJECT_TYPE = "search.metadata.object-type"; //$NON-NLS-1$
    private static final String PROP_SOURCES = "search.metadata.object-source"; //$NON-NLS-1$

    private Table typesTable;
    private Combo searchText;
    private DatabaseNavigatorTree dataSourceTree;

    private String nameMask;
    private boolean caseSensitive;
    private int maxResults;
    private int matchTypeIndex;
    private Set<DBSObjectType> checkedTypes = new HashSet<>();
    private Set<String> searchHistory = new LinkedHashSet<>();
    private Set<String> savedTypeNames = new HashSet<>();
    private List<DBNNode> sourceNodes = new ArrayList<>();

    public SearchMetadataPage() {
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
        UIUtils.createControlLabel(searchGroup, CoreMessages.dialog_search_objects_label_object_name);
        searchText = new Combo(searchGroup, SWT.DROP_DOWN);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (nameMask != null) {
            searchText.setText(nameMask);
        }
        for (String history : searchHistory) {
            searchText.add(history);
        }
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                nameMask = searchText.getText();
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

            Group sourceGroup = UIUtils.createControlGroup(optionsGroup, CoreMessages.dialog_search_objects_group_objects_source, 1, GridData.FILL_BOTH, 0);
            gd = new GridData(GridData.FILL_BOTH);
            //gd.heightHint = 300;
            sourceGroup.setLayoutData(gd);
            final DBNProject projectNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());
            DBNNode rootNode = projectNode == null ? core.getNavigatorModel().getRoot() : projectNode.getDatabases();
            dataSourceTree = new DatabaseNavigatorTree(sourceGroup, rootNode, SWT.SINGLE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            dataSourceTree.setLayoutData(gd);

            dataSourceTree.getViewer().addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element)
                {
                    if (element instanceof TreeLoadNode) {
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
                new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event)
                    {
                        fillObjectTypes();
                        updateEnablement();
                        IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                        Object object = structSel.isEmpty() ? null : structSel.getFirstElement();
                        if (object instanceof DBNNode) {
                            for (DBNNode node = (DBNNode)object; node != null; node = node.getParentNode()) {
                                if (node instanceof DBNDataSource) {
                                    DBNDataSource dsNode = (DBNDataSource) node;
                                    dsNode.initializeNode(null, new DBRProgressListener() {
                                        @Override
                                        public void onTaskFinished(IStatus status)
                                        {
                                            if (status.isOK()) {
                                                DBeaverUI.asyncExec(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (!dataSourceTree.isDisposed()) {
                                                            fillObjectTypes();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    break;
                                }
                            }
                        }
                    }
                }
            );
        }

        {
            Group settingsGroup = UIUtils.createControlGroup(optionsGroup, "Settings", 2, GridData.FILL_BOTH, 0);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            settingsGroup.setLayoutData(gd);


            {
                //new Label(searchGroup, SWT.NONE);
                UIUtils.createControlLabel(settingsGroup, CoreMessages.dialog_search_objects_label_name_match);
                final Combo matchCombo = new Combo(settingsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
                matchCombo.add(CoreMessages.dialog_search_objects_combo_starts_with, SearchMetadataConstants.MATCH_INDEX_STARTS_WITH);
                matchCombo.add(CoreMessages.dialog_search_objects_combo_contains, SearchMetadataConstants.MATCH_INDEX_CONTAINS);
                matchCombo.add(CoreMessages.dialog_search_objects_combo_like, SearchMetadataConstants.MATCH_INDEX_LIKE);
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

                final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(settingsGroup, CoreMessages.dialog_search_objects_spinner_max_results, maxResults, 1, 10000);
                maxResultsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                maxResultsSpinner.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e)
                    {
                        maxResults = maxResultsSpinner.getSelection();
                    }
                });
                maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                final Button caseCheckbox = UIUtils.createLabelCheckbox(settingsGroup, CoreMessages.dialog_search_objects_case_sensitive, caseSensitive);
                caseCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        caseSensitive = caseCheckbox.getSelection();
                    }
                });
                caseCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            }

            Label otLabel = UIUtils.createControlLabel(settingsGroup, CoreMessages.dialog_search_objects_group_object_types);
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
                }
            });
            typesTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    TableItem tableItem = typesTable.getSelection()[0];
                    tableItem.setChecked(!tableItem.getChecked());
                }
            });
            typesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createTableColumn(typesTable, SWT.LEFT, CoreMessages.dialog_search_objects_column_type);
            UIUtils.createTableColumn(typesTable, SWT.LEFT, CoreMessages.dialog_search_objects_column_description);
        }

        try {
            DBeaverUI.runInProgressDialog(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Load database nodes", 1);
                    try {
                        monitor.subTask("Load tree state");
                        sourceNodes = loadTreeState(monitor, DBeaverCore.getGlobalPreferenceStore(), PROP_SOURCES);
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Data sources load", "Error loading settings", e.getTargetException());
        }

        if (!sourceNodes.isEmpty()) {
            dataSourceTree.getViewer().setSelection(
                new StructuredSelection(sourceNodes));
            dataSourceTree.getViewer().reveal(NavigatorUtils.getDataSourceNode(sourceNodes.get(0)));
        } else {
            updateEnablement();
        }
    }

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
                } else if (savedTypeNames.contains(objectType.getTypeClass().getName())) {
                    item.setChecked(true);
                    checkedTypes.add(objectType);
                    savedTypeNames.remove(objectType.getTypeClass().getName());
                }
            }
        }
        for (TableColumn column : typesTable.getColumns()) {
            column.pack();
        }
        updateEnablement();
    }

    @Override
    public SearchMetadataQuery createQuery() throws DBException
    {
        DBNNode selectedNode = getSelectedNode();
        DBSObjectContainer parentObject = null;
        if (selectedNode instanceof DBSWrapper && ((DBSWrapper)selectedNode).getObject() instanceof DBSObjectContainer) {
            parentObject = (DBSObjectContainer) ((DBSWrapper)selectedNode).getObject();
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

        SearchMetadataParams params = new SearchMetadataParams();
        params.setParentObject(parentObject);
        params.setObjectTypes(objectTypes);
        params.setObjectNameMask(objectNameMask);
        params.setCaseSensitive(caseSensitive);
        params.setMaxResults(maxResults);
        return SearchMetadataQuery.createQuery(dataSource, params);

    }

    @Override
    public void loadState(DBPPreferenceStore store)
    {
        nameMask = store.getString(PROP_MASK);
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
                typesString.append(type.getTypeClass().getName());
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
