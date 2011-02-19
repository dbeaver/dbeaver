/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.search;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.NodeListControl;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SearchObjectsView extends ViewPart {

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.findObjects";

    private static final int MATCH_INDEX_STARTS_WITH = 0;
    private static final int MATCH_INDEX_CONTAINS = 1;
    private static final int MATCH_INDEX_LIKE = 2;

    private static final String PROP_MASK = "search-view.mask";
    private static final String PROP_MAX_RESULT = "search-view.max-results";
    private static final String PROP_MATCH_INDEX = "search-view.match-index";
    private static final String PROP_HISTORY = "search-view.history";
    private static final String PROP_OBJECT_TYPE = "search-view.object-type";

    private Composite searchGroup;
    private Table typesTable;
    private Combo searchText;
    private Button searchButton;
    private SearchResultsControl itemList;
    private DatabaseNavigatorTree dataSourceTree;

    private String nameMask;
    private int maxResults;
    private int matchTypeIndex;
    private Set<DBSObjectType> checkedTypes = new HashSet<DBSObjectType>();
    private Set<String> searchHistory = new LinkedHashSet<String>();
    private Set<String> savedTypeNames = new HashSet<String>();

    public SearchObjectsView()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        nameMask = store.getString(PROP_MASK);
        maxResults = store.getInt(PROP_MAX_RESULT);
        matchTypeIndex = store.getInt(PROP_MATCH_INDEX);
        for (int i = 0; ;i++) {
            String history = store.getString(PROP_HISTORY + "." + i);
            if (CommonUtils.isEmpty(history)) {
                break;
            }
            searchHistory.add(history);
        }
        {
            String type = store.getString(PROP_OBJECT_TYPE);
            if (!CommonUtils.isEmpty(type)) {
                StringTokenizer st = new StringTokenizer(type, "|");
                while (st.hasMoreTokens()) {
                    savedTypeNames.add(st.nextToken());
                }
            }
        }
    }

    @Override
    public void dispose()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        store.setValue(PROP_MASK, nameMask);
        store.setValue(PROP_MAX_RESULT, maxResults);
        store.setValue(PROP_MATCH_INDEX, matchTypeIndex);
        {
            int historyIndex = 0;
            for (String history : searchHistory) {
                if (historyIndex >= 20) {
                    break;
                }
                store.setValue(PROP_HISTORY + "." + historyIndex, history);
                historyIndex++;
            }
        }
        {
            StringBuilder typesString = new StringBuilder();
            for (DBSObjectType type : checkedTypes) {
                if (typesString.length() > 0) {
                    typesString.append("|");
                }
                typesString.append(type.getTypeClass().getName());
            }
            store.setValue(PROP_OBJECT_TYPE, typesString.toString());
        }

        super.dispose();
    }

    public void setCurrentDataSource(DBSDataSourceContainer currentDataSource)
    {
        DBNNode selNode = DBeaverCore.getInstance().getNavigatorModel().findNode(currentDataSource);
        if (selNode != null) {
            dataSourceTree.getViewer().setSelection(new StructuredSelection(selNode), true);
        } else {
            dataSourceTree.getViewer().setSelection(new StructuredSelection());
        }
        fillObjectTypes();
    }

    public void createPartControl(Composite parent)
    {
        setPartName("Find database objects");
        setTitleImage(DBIcon.FIND.getImage());

        //Composite divider = UIUtils.createPlaceholder(parent, 1, 5);
        SashForm divider = UIUtils.createPartDivider(this, parent, SWT.VERTICAL | SWT.SMOOTH);

        {
            searchGroup = new Composite(divider, SWT.NONE);
            searchGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            searchGroup.setLayout(new GridLayout(3, false));
            UIUtils.createControlLabel(searchGroup, "Object Name");
            searchText = new Combo(searchGroup, SWT.DROP_DOWN);
            searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (nameMask != null) {
                searchText.setText(nameMask);
            }
            for (String history : searchHistory) {
                searchText.add(history);
            }
            searchText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
                    nameMask = searchText.getText();
                    checkSearchEnabled();
                }
            });

            searchButton = new Button(searchGroup, SWT.PUSH);
            searchButton.setText("Search");
            searchButton.setImage(DBIcon.FIND.getImage());
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            //gd.horizontalSpan = 2;
            searchButton.setLayoutData(gd);
            searchButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    performSearch();
                }
            });

            {
                //new Label(searchGroup, SWT.NONE);
                Composite optionsGroup2 = UIUtils.createPlaceholder(searchGroup, 4, 5);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.horizontalSpan = 3;
                optionsGroup2.setLayoutData(gd);

                UIUtils.createControlLabel(optionsGroup2, "Name match");
                final Combo matchCombo = new Combo(optionsGroup2, SWT.DROP_DOWN | SWT.READ_ONLY);
                matchCombo.add("Starts with");
                matchCombo.add("Contains");
                matchCombo.add("Like");
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

                if (maxResults <= 0) {
                    maxResults = 100;
                }
                final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(optionsGroup2, "Max results", maxResults, 1, 10000);
                maxResultsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                maxResultsSpinner.addModifyListener(new ModifyListener() {
                    public void modifyText(ModifyEvent e)
                    {
                        maxResults = maxResultsSpinner.getSelection();
                    }
                });
            }

            Composite optionsGroup = new Composite(searchGroup, SWT.NONE);
            GridLayout layout = new GridLayout(2, true);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            optionsGroup.setLayout(layout);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 3;
            optionsGroup.setLayoutData(gd);

            {
                final DBeaverCore core = DBeaverCore.getInstance();

                Group sourceGroup = UIUtils.createControlGroup(optionsGroup, "Objects Source", 1, GridData.FILL_BOTH, 0);
                final DBNProject rootNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());
                dataSourceTree = new DatabaseNavigatorTree(sourceGroup, rootNode.getDatabases(), SWT.SINGLE);
                gd = new GridData(GridData.FILL_BOTH);
                //gd.heightHint = 100;
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
                                Class<? extends DBSObject> folderItemsClass = folder.getItemsClass();
                                return folderItemsClass != null && DBSEntityContainer.class.isAssignableFrom(folderItemsClass);
                            }
                            if (element instanceof DBNProjectDatabases || element instanceof DBNDataSource || (element instanceof DBSWrapper && ((DBSWrapper)element).getObject() instanceof DBSEntityContainer)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
                dataSourceTree.getViewer().addSelectionChangedListener(
                    new ISelectionChangedListener()
                    {
                        public void selectionChanged(SelectionChangedEvent event)
                        {
                            IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
                            for (Iterator<?> iter = structSel.iterator(); iter.hasNext(); ) {
                                Object object = iter.next();
                                if (object instanceof DBNDataSource) {
                                    DBNDataSource dsNode = (DBNDataSource)object;
                                    dsNode.initializeNode(new Runnable() {
                                        public void run()
                                        {
                                            Display.getDefault().asyncExec(new Runnable() {
                                                public void run()
                                                {
                                                    fillObjectTypes();
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    }
                );
            }

            {
                Group typesGroup = UIUtils.createControlGroup(optionsGroup, "Object Types", 1, GridData.FILL_BOTH, 0);
                typesTable = new Table(typesGroup, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
                typesTable.addSelectionListener(new SelectionAdapter() {
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
                        checkSearchEnabled();
                    }
                });
                typesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

                TableColumn typeColumn = new TableColumn(typesTable, SWT.LEFT);
                typeColumn.setText("Type");
                TableColumn descColumn = new TableColumn(typesTable, SWT.LEFT);
                descColumn.setText("Description");
            }
        }

        {
            itemList = new SearchResultsControl(divider);
            itemList.setInfo("You have to set search criteria");
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 700;
            gd.heightHint = 500;
            itemList.setLayoutData(gd);
            getSite().setSelectionProvider(itemList.getSelectionProvider());
            //itemList.addFocusListener(new ItemsFocusListener());
        }

        divider.setWeights(new int[]{30, 70});
    }

    public void afterCreate()
    {
        Shell shell = getSite().getShell();
        shell.addShellListener(new ShellListener());
        shell.setDefaultButton(searchButton);
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
                    item.setImage(0, objectType.getImage());
                }
                item.setText(1, objectType.getDescription());
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
        checkSearchEnabled();
    }

    private void checkSearchEnabled()
    {
        boolean enabled = false;
        for (TableItem item : typesTable.getItems()) {
            if (item.getChecked()) {
                enabled = true;
            }
        }
        if (CommonUtils.isEmpty(nameMask)) {
            enabled = false;
        }

        searchButton.setEnabled(enabled);
    }

    private void performSearch()
    {
        DBNNode selectedNode = getSelectedNode();
        DBSEntityContainer parentObject = null;
        if (selectedNode instanceof DBSWrapper && ((DBSWrapper)selectedNode).getObject() instanceof DBSEntityContainer) {
            parentObject = (DBSEntityContainer) ((DBSWrapper)selectedNode).getObject();
        }

        DBPDataSource dataSource = getSelectedDataSource();
        DBSStructureAssistant assistant = getSelectedStructureAssistant();
        if (dataSource == null || assistant == null) {
            return;
        }
        java.util.List<DBSObjectType> objectTypes = new ArrayList<DBSObjectType>();
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

        if (matchTypeIndex == MATCH_INDEX_STARTS_WITH) {
            if (!objectNameMask.endsWith("%")) {
                objectNameMask = objectNameMask + "%";
            }
        } else if (matchTypeIndex == MATCH_INDEX_CONTAINS) {
            if (!objectNameMask.startsWith("%")) {
                objectNameMask = "%" + objectNameMask;
            }
            if (!objectNameMask.endsWith("%")) {
                objectNameMask = objectNameMask + "%";
            }
        }

        // Start separate service for each data source
        LoadingJob<Collection<DBNNode>> loadingJob = LoadingUtils.createService(
            new ObjectSearchService(dataSource, assistant, parentObject, objectTypes, objectNameMask, maxResults),
            itemList.createVisualizer(ControlEnableState.disable(searchGroup)));
        itemList.loadData(loadingJob);
    }

    @Override
    public void setFocus()
    {
        if (searchText != null && !searchText.isDisposed()) {
            searchText.setFocus();
        }
    }

    private class SearchResultsControl extends NodeListControl {
        public SearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup, SWT.BORDER, SearchObjectsView.this, DBeaverCore.getInstance().getNavigatorModel().getRoot());
        }

        @Override
        protected Composite createProgressPanel(Composite container)
        {
            Composite panel = super.createProgressPanel(container);

            Button closeButton = new Button(panel, SWT.PUSH);
            closeButton.setText("Close");
            closeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SearchObjectsView.this.getSite().getWorkbenchWindow().getActivePage().hideView(SearchObjectsView.this);
                }
            });
            return panel;
        }

        public ObjectsLoadVisualizer createVisualizer(final ControlEnableState blockEnableState)
        {
            return new ObjectsLoadVisualizer() {
                @Override
                public void completeLoading(Collection<DBNNode> items)
                {
                    super.completeLoading(items);
                    blockEnableState.restore();
                }

                protected String getItemsLoadMessage(int count)
                {
                    if (count == 0) {
                        return "No objects like '" + searchText.getText() + "' in '" + getSelectedNode().getNodeName() + "'";
                    } else {
                        return count + " objects found";
                    }
                }
            };
        }
    }

    private class ObjectSearchService extends DatabaseLoadService<Collection<DBNNode>> {

        private final DBSStructureAssistant structureAssistant;
        private final DBSObject parentObject;
        private final java.util.List<DBSObjectType> objectTypes;
        private final String objectNameMask;
        private final int maxResults;

        private ObjectSearchService(
            DBPDataSource dataSource,
            DBSStructureAssistant structureAssistant,
            DBSObject parentObject,
            java.util.List<DBSObjectType> objectTypes,
            String objectNameMask,
            int maxResults)
        {
            super("Find objects", dataSource);
            this.structureAssistant = structureAssistant;
            this.parentObject = parentObject;
            this.objectTypes = objectTypes;
            this.objectNameMask = objectNameMask;
            this.maxResults = maxResults;
        }

        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                java.util.List<DBNNode> nodes = new ArrayList<DBNNode>();
                Collection<DBSObject> objects = structureAssistant.findObjectsByMask(getProgressMonitor(), parentObject, objectTypes, objectNameMask, maxResults);
                for (DBSObject object : objects) {
                    DBNNode node = navigatorModel.getNodeByObject(getProgressMonitor(), object, true);
                    if (node != null) {
                        nodes.add(node);
                    }
                }
                return nodes;
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }


    private class ShellListener extends ShellAdapter {
        @Override
        public void shellActivated(ShellEvent e)
        {
            if (searchButton != null && !searchButton.isDisposed()) {
                getSite().getShell().setDefaultButton(searchButton);
            }
        }

        @Override
        public void shellDeactivated(ShellEvent e)
        {
        }
    }

}
