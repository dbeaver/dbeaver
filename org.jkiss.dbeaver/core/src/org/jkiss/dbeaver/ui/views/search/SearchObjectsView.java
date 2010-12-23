/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.search;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
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

public class SearchObjectsView extends ViewPart implements DBPEventListener {

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.findObjects";

    private static final int MATCH_INDEX_STARTS_WITH = 0;
    private static final int MATCH_INDEX_CONTAINS = 1;
    private static final int MATCH_INDEX_LIKE = 2;

    private Table typesTable;
    private Text searchText;
    private Button searchButton;
    private Combo matchCombo;
    private Spinner maxResultsSpinner;
    private SearchResultsControl itemList;
    private DatabaseNavigatorTree dataSourceTree;
    private Set<DBSObjectType> checkedTypes = new HashSet<DBSObjectType>();

    public SearchObjectsView()
    {
        DataSourceRegistry.getDefault().addDataSourceListener(this);
    }

    @Override
    public void dispose()
    {
        DataSourceRegistry.getDefault().removeDataSourceListener(this);
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

        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Composite searchGroup = new Composite(composite, SWT.NONE);
            searchGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            searchGroup.setLayout(new GridLayout(3, false));
            searchText = UIUtils.createLabelText(searchGroup, "Object Name", "");
            searchText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
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

            Composite optionsGroup = new Composite(searchGroup, SWT.NONE);
            GridLayout layout = new GridLayout(3, true);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            optionsGroup.setLayout(layout);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            optionsGroup.setLayoutData(gd);

            {
                Composite optionsGroup2 = UIUtils.createControlGroup(optionsGroup, "Options", 2, GridData.FILL_BOTH, 0);

                UIUtils.createControlLabel(optionsGroup2, "Name match");
                matchCombo = new Combo(optionsGroup2, SWT.DROP_DOWN | SWT.READ_ONLY);
                matchCombo.add("Starts with");
                matchCombo.add("Contains");
                matchCombo.add("Like");
                matchCombo.select(0);
                matchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                maxResultsSpinner = UIUtils.createLabelSpinner(optionsGroup2, "Max results", 100, 1, 10000);
                maxResultsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }

            {
                Group sourceGroup = UIUtils.createControlGroup(optionsGroup, "Objects Source", 1, GridData.FILL_BOTH, 0);
                dataSourceTree = new DatabaseNavigatorTree(sourceGroup, DBeaverCore.getInstance().getNavigatorModel().getRoot(), SWT.SINGLE);
                gd = new GridData(GridData.FILL_BOTH);
                gd.heightHint = 100;
                dataSourceTree.setLayoutData(gd);

                dataSourceTree.getViewer().addFilter(new ViewerFilter() {
                    @Override
                    public boolean select(Viewer viewer, Object parentElement, Object element)
                    {
                        if (element instanceof TreeLoadNode) {
                            return true;
                        }
                        if (element instanceof DBNNode) {
                            if (element instanceof DBNTreeFolder) {
                                DBNTreeFolder folder = (DBNTreeFolder)element;
                                Class<? extends DBSObject> folderItemsClass = folder.getItemsClass();
                                return folderItemsClass != null && DBSEntityContainer.class.isAssignableFrom(folderItemsClass);
                            }
                            if (element instanceof DBNDataSource || ((DBNNode)element).getObject() instanceof DBSEntityContainer) {
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
                            for (Iterator iter = structSel.iterator(); iter.hasNext(); ) {
                                Object object = iter.next();
                                if (object instanceof DBNDataSource) {
                                    DBNDataSource dsNode = (DBNDataSource)object;
                                    dsNode.initializeNode();
                                }
                            }
                            fillObjectTypes();
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
            Group resultsGroup = UIUtils.createControlGroup(composite, "Results", 1, GridData.FILL_BOTH, 0);
            itemList = new SearchResultsControl(resultsGroup);
            itemList.setInfo("You have to set search criteria");
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 700;
            gd.heightHint = 500;
            itemList.setLayoutData(gd);
            getSite().setSelectionProvider(itemList.getSelectionProvider());
            //itemList.addFocusListener(new ItemsFocusListener());
        }
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
        if (node != null) {
            DBSObject object = node.getObject();
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
        if (CommonUtils.isEmpty(searchText.getText())) {
            enabled = false;
        }

        searchButton.setEnabled(enabled);
    }

    private void performSearch()
    {
        DBNNode selectedNode = getSelectedNode();
        DBSEntityContainer parentObject = null;
        if (selectedNode.getObject() instanceof DBSEntityContainer) {
            parentObject = (DBSEntityContainer) selectedNode.getObject();
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
        String objectNameMask = searchText.getText();
        int maxResults = maxResultsSpinner.getSelection();
        int matchIndex = matchCombo.getSelectionIndex();
        if (matchIndex == MATCH_INDEX_STARTS_WITH) {
            if (!objectNameMask.endsWith("%")) {
                objectNameMask = objectNameMask + "%";
            }
        } else if (matchIndex == MATCH_INDEX_CONTAINS) {
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
            itemList.createVisualizer());
        itemList.loadData(loadingJob);
    }

    @Override
    public void setFocus()
    {
        if (searchText != null && !searchText.isDisposed()) {
            searchText.setFocus();
        }
    }

    public void handleDataSourceEvent(DBPEvent event)
    {
        Display.getDefault().asyncExec(new Runnable() {
            public void run()
            {
                fillObjectTypes();
            }
        });
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
                    workbenchPart.getSite().getWorkbenchWindow().getActivePage().hideView(SearchObjectsView.this);
                }
            });
            return panel;
        }

        public ObjectsLoadVisualizer createVisualizer()
        {
            return new ObjectsLoadVisualizer() {
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
