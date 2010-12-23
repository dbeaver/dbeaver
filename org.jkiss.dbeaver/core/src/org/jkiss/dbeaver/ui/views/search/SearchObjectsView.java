/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.search;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.NodeListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

public class SearchObjectsView extends ViewPart {

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.findObjects";

    private static final int MATCH_INDEX_STARTS_WITH = 0;
    private static final int MATCH_INDEX_CONTAINS = 1;
    private static final int MATCH_INDEX_LIKE = 2;

    private static ITreeContentProvider CONTENT_PROVIDER = new ITreeContentProvider() {
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        public Object[] getChildren(Object parentElement)
        {
            return null;
        }

        public Object getParent(Object element)
        {
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return false;
        }

        public void dispose()
        {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

    //private final IWorkbenchPart workbenchPart;
    private java.util.List<DataSourceDescriptor> dataSources = new ArrayList<DataSourceDescriptor>();
    private DBSDataSourceContainer currentDataSource;
    private Table typesTable;
    private Text searchText;
    private Button searchButton;
    private Combo matchCombo;
    private Combo dataSourceCombo;
    private Spinner maxResultsSpinner;
    private SearchResultsControl itemList;

    public SearchObjectsView()
    {
        //super();
        //setShellStyle(SWT.CLOSE | SWT.MODELESS| SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX | SWT.MIN);
        //setBlockOnOpen(false);
        this.currentDataSource = currentDataSource;
    }

    public DBSDataSourceContainer getCurrentDataSource()
    {
        return currentDataSource;
    }

    public void setCurrentDataSource(DBSDataSourceContainer currentDataSource)
    {
        this.currentDataSource = currentDataSource;
        refreshDataSources();
        updateDataSourceSelection();
    }

    private void refreshDataSources()
    {
        dataSources.clear();
        dataSourceCombo.removeAll();
        dataSources.addAll(DBeaverCore.getInstance().getDataSourceRegistry().getDataSources());
        for (int i = 0, dataSourcesSize = dataSources.size(); i < dataSourcesSize; i++) {
            DataSourceDescriptor descriptor = dataSources.get(i);
            dataSourceCombo.add(descriptor.getName());
            if (descriptor == currentDataSource) {
                dataSourceCombo.select(i);
            }
        }
    }

    public void createPartControl(Composite parent)
    {
        setPartName("Find database objects");
        setTitleImage(DBIcon.FIND.getImage());

        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group searchGroup = UIUtils.createControlGroup(composite, "Search", 3, GridData.FILL_HORIZONTAL, 0);
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
            GridLayout layout = new GridLayout(3, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            optionsGroup.setLayout(layout);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            optionsGroup.setLayoutData(gd);

            {
                Composite optionsGroup2 = new Composite(optionsGroup, SWT.NONE);
                layout = new GridLayout(2, false);
                layout.marginHeight = 0;
                layout.marginWidth = 0;
                optionsGroup2.setLayout(layout);
                optionsGroup2.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

                UIUtils.createControlLabel(optionsGroup2, "Data Source");
                dataSourceCombo = new Combo(optionsGroup2, SWT.DROP_DOWN | SWT.READ_ONLY);
                dataSourceCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                dataSourceCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        updateDataSourceSelection();
                    }
                });

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
                Table dbTable = new Table(optionsGroup, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
                dbTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            }
            {
                typesTable = new Table(optionsGroup, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
                typesTable.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e)
                    {
                        checkSearchEnabled();
                    }
                });
                gd = new GridData(GridData.FILL_BOTH);
                typesTable.setLayoutData(gd);

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

    private void updateDataSourceSelection()
    {
        int selectionIndex = dataSourceCombo.getSelectionIndex();
        if (selectionIndex < 0) {
            currentDataSource = null;
        } else {
            currentDataSource = dataSources.get(selectionIndex);
        }
        fillObjectTypes();
    }

    private void fillObjectTypes()
    {
        DBSStructureAssistant structureAssistant = null;
        if (currentDataSource != null) {
            structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, currentDataSource);
        }
        typesTable.removeAll();
        if (structureAssistant == null) {
            // No structure assistant - no object types
        } else {
            for (DBSObjectType objectType : structureAssistant.getSupportedObjectTypes()) {
                TableItem item = new TableItem(typesTable, SWT.NONE);
                item.setText(objectType.getTypeName());
                if (objectType.getImage() != null) {
                    item.setImage(0, objectType.getImage());
                }
                item.setText(1, objectType.getDescription());
                item.setData(objectType);
            }
        }
        for (TableColumn column : typesTable.getColumns()) {
            column.pack();
        }
        checkSearchEnabled();
    }

    private void checkSearchEnabled()
    {
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, currentDataSource);
        boolean enabled = false;
        if (structureAssistant != null) {
            for (TableItem item : typesTable.getItems()) {
                if (item.getChecked()) {
                    enabled = true;
                }
            }
            if (CommonUtils.isEmpty(searchText.getText())) {
                enabled = false;
            }
        }
        searchButton.setEnabled(enabled);
    }

    private void performSearch()
    {
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, currentDataSource);
        if (structureAssistant == null) {
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

        LoadingJob<Collection<DBNNode>> loadingJob = LoadingUtils.createService(
            new ObjectSearchService(currentDataSource.getDataSource(), structureAssistant, objectTypes, objectNameMask, maxResults),
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

    private class SearchResultsControl extends NodeListControl {
        public SearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup, SWT.BORDER, SearchObjectsView.this, DBeaverCore.getInstance().getNavigatorModel().getRoot());
        }

        public ObjectsLoadVisualizer createVisualizer()
        {
            return new ObjectsLoadVisualizer() {
                protected String getItemsLoadMessage(int count)
                {
                    if (count == 0) {
                        return "No objects like '" + searchText.getText() + "'";
                    } else {
                        return count + " objects found";
                    }
                }
            };
        }
    }

    private class ObjectSearchService extends DatabaseLoadService<Collection<DBNNode>> {

        private final DBSStructureAssistant structureAssistant;
        private final java.util.List<DBSObjectType> objectTypes;
        private final String objectNameMask;
        private final int maxResults;

        private ObjectSearchService(DBPDataSource dataSource, DBSStructureAssistant structureAssistant, java.util.List<DBSObjectType> objectTypes, String objectNameMask, int maxResults)
        {
            super("Find objects", dataSource);
            this.structureAssistant = structureAssistant;
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
                Collection<DBSObject> objects = structureAssistant.findObjectsByMask(getProgressMonitor(), null, objectTypes, objectNameMask, maxResults);
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
