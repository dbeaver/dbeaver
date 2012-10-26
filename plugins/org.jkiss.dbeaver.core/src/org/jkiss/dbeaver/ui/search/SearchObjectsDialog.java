/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.NodeListControl;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SearchObjectsDialog extends HelpEnabledDialog {

    static final Log log = LogFactory.getLog(SearchObjectsDialog.class);

    private static final int MATCH_INDEX_STARTS_WITH = 0;
    private static final int MATCH_INDEX_CONTAINS = 1;
    private static final int MATCH_INDEX_LIKE = 2;

    private static final String PROP_MASK = "search-view.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search-view.case-sensitive"; //$NON-NLS-1$ 
    private static final String PROP_MAX_RESULT = "search-view.max-results"; //$NON-NLS-1$
    private static final String PROP_MATCH_INDEX = "search-view.match-index"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search-view.history"; //$NON-NLS-1$
    private static final String PROP_OBJECT_TYPE = "search-view.object-type"; //$NON-NLS-1$

    private volatile static SearchObjectsDialog instance;

    private DBSDataSourceContainer currentDataSource;
    private Composite searchGroup;
    private Table typesTable;
    private Combo searchText;
    private Button searchButton;
    private SearchResultsControl itemList;
    private DatabaseNavigatorTree dataSourceTree;

    private String nameMask;
    private boolean caseSensitive;
    private int maxResults;
    private int matchTypeIndex;
    private Set<DBSObjectType> checkedTypes = new HashSet<DBSObjectType>();
    private Set<String> searchHistory = new LinkedHashSet<String>();
    private Set<String> savedTypeNames = new HashSet<String>();

    private SearchObjectsDialog(Shell shell, DBSDataSourceContainer currentDataSource)
    {
        super(shell, IHelpContextIds.CTX_SQL_EDITOR);
        setShellStyle(SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
        this.currentDataSource = currentDataSource;
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

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

    public void saveState()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        store.setValue(PROP_MASK, nameMask);
        store.setValue(PROP_CASE_SENSITIVE, caseSensitive);
        store.setValue(PROP_MAX_RESULT, maxResults);
        store.setValue(PROP_MATCH_INDEX, matchTypeIndex);
        {
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

    @Override
    protected Control createButtonBar(Composite parent)
    {
        return null;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Shell shell = getShell();

        shell.setText(CoreMessages.dialog_search_objects_title);
        shell.setImage(DBIcon.FIND.getImage());

        //Composite divider = UIUtils.createPlaceholder(parent, 1, 5);
        SashForm divider = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 600;
        divider.setLayoutData(gd);

        {
            searchGroup = new Composite(divider, SWT.NONE);
            searchGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            searchGroup.setLayout(new GridLayout(3, false));
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
                    checkSearchEnabled();
                }
            });

            searchButton = new Button(searchGroup, SWT.PUSH);
            searchButton.setText(CoreMessages.dialog_search_objects_button_search);
            searchButton.setImage(DBIcon.FIND.getImage());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
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
                Composite optionsGroup2 = UIUtils.createPlaceholder(searchGroup, 5, 5);
                gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.horizontalSpan = 3;
                optionsGroup2.setLayoutData(gd);

                UIUtils.createControlLabel(optionsGroup2, CoreMessages.dialog_search_objects_label_name_match);
                final Combo matchCombo = new Combo(optionsGroup2, SWT.DROP_DOWN | SWT.READ_ONLY);
                matchCombo.add(CoreMessages.dialog_search_objects_combo_starts_with, MATCH_INDEX_STARTS_WITH);
                matchCombo.add(CoreMessages.dialog_search_objects_combo_contains, MATCH_INDEX_CONTAINS);
                matchCombo.add(CoreMessages.dialog_search_objects_combo_like, MATCH_INDEX_LIKE);
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
            GridLayout layout = new GridLayout(2, true);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            optionsGroup.setLayout(layout);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 3;
            optionsGroup.setLayoutData(gd);

            {
                final DBeaverCore core = DBeaverCore.getInstance();

                Group sourceGroup = UIUtils.createControlGroup(optionsGroup, CoreMessages.dialog_search_objects_group_objects_source, 1, GridData.FILL_BOTH, 0);
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
                                Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                                return folderItemsClass != null && DBSObjectContainer.class.isAssignableFrom(folderItemsClass);
                            }
                            if (element instanceof DBNProjectDatabases || element instanceof DBNDataSource || (element instanceof DBSWrapper && ((DBSWrapper)element).getObject() instanceof DBSObjectContainer)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
                dataSourceTree.getViewer().addSelectionChangedListener(
                    new ISelectionChangedListener()
                    {
                        @Override
                        public void selectionChanged(SelectionChangedEvent event)
                        {
                            IStructuredSelection structSel = (IStructuredSelection)event.getSelection();
                            for (Iterator<?> iter = structSel.iterator(); iter.hasNext(); ) {
                                Object object = iter.next();
                                if (object instanceof DBNDataSource) {
                                    DBNDataSource dsNode = (DBNDataSource)object;
                                    dsNode.initializeNode(null, new DBRProcessListener() {
                                        @Override
                                        public void onProcessFinish(IStatus status)
                                        {
                                            if (status.isOK()) {
                                                Display.getDefault().asyncExec(new Runnable() {
                                                    @Override
                                                    public void run()
                                                    {
                                                        if (!dataSourceTree.isDisposed()) {
                                                            fillObjectTypes();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                );
            }

            {
                Group typesGroup = UIUtils.createControlGroup(optionsGroup, CoreMessages.dialog_search_objects_group_object_types, 1, GridData.FILL_BOTH, 0);
                typesTable = new Table(typesGroup, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
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
                        checkSearchEnabled();
                    }
                });
                typesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

                TableColumn typeColumn = new TableColumn(typesTable, SWT.LEFT);
                typeColumn.setText(CoreMessages.dialog_search_objects_column_type);
                TableColumn descColumn = new TableColumn(typesTable, SWT.LEFT);
                descColumn.setText(CoreMessages.dialog_search_objects_column_description);
            }
        }

        {
            itemList = new SearchResultsControl(divider);
            itemList.createProgressPanel();
            itemList.setInfo(CoreMessages.dialog_search_objects_item_list_info);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 700;
            gd.heightHint = 500;
            itemList.setLayoutData(gd);
            //itemList.addFocusListener(new ItemsFocusListener());
        }

        divider.setWeights(new int[]{40, 60});

        shell.addShellListener(new ShellListener());
        shell.setDefaultButton(searchButton);

        if (currentDataSource != null) {
            // Set active datasource
            DBNNode selNode = DBeaverCore.getInstance().getNavigatorModel().findNode(currentDataSource);
            if (selNode != null) {
                dataSourceTree.getViewer().setSelection(new StructuredSelection(selNode), true);
            } else {
                dataSourceTree.getViewer().setSelection(new StructuredSelection());
            }
            fillObjectTypes();
        }

        return divider;
    }

    public static void open(Shell shell, DBSDataSourceContainer currentDataSource)
    {
        if (instance != null) {
            instance.getShell().setActive();
            return;
        }
        SearchObjectsDialog dialog = new SearchObjectsDialog(shell, currentDataSource);
        instance = dialog;
        try {
            dialog.open();
        } finally {
            instance = null;
        }
    }

    @Override
    public boolean close()
    {
        saveState();
        return super.close();
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
        itemList.clearListData();
        itemList.loadData();
    }

    private class SearchResultsControl extends NodeListControl {
        public SearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup, SWT.BORDER, null, DBeaverCore.getInstance().getNavigatorModel().getRoot(), null);
        }

        @Override
        protected void fillCustomToolbar(ToolBarManager toolbarManager) {
            toolbarManager.add(new Action(CoreMessages.dialog_search_objects_button_close, Action.AS_PUSH_BUTTON) {
                @Override
                public void run() {
                    SearchObjectsDialog.this.close();
                }
            });
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
                        return NLS.bind(CoreMessages.dialog_search_objects_message_no_objects_like_, new Object[] {searchText.getText(), getSelectedNode().getNodeName()});
                    } else {
                        return count + CoreMessages.dialog_search_objects_message_objects_found;
                    }
                }
            };
        }

        @Override
        protected LoadingJob<Collection<DBNNode>> createLoadService()
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
                if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                    objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                }
            } else if (matchTypeIndex == MATCH_INDEX_CONTAINS) {
                if (!objectNameMask.startsWith("%")) { //$NON-NLS-1$
                    objectNameMask = "%" + objectNameMask; //$NON-NLS-1$
                }
                if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                    objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                }
            }

            return LoadingUtils.createService(
                new ObjectSearchService(dataSource, assistant, parentObject, objectTypes, objectNameMask, caseSensitive, maxResults),
                itemList.createVisualizer(ControlEnableState.disable(searchGroup)));
        }
    }

    private class ObjectSearchService extends DatabaseLoadService<Collection<DBNNode>> {

        private final DBSStructureAssistant structureAssistant;
        private final DBSObject parentObject;
        private final java.util.List<DBSObjectType> objectTypes;
        private final String objectNameMask;
        private final boolean caseSensitive;
        private final int maxResults;

        private ObjectSearchService(
            DBPDataSource dataSource,
            DBSStructureAssistant structureAssistant,
            DBSObject parentObject,
            java.util.List<DBSObjectType> objectTypes,
            String objectNameMask,
            boolean caseSensitive,
            int maxResults)
        {
            super("Find objects", dataSource);
            this.structureAssistant = structureAssistant;
            this.parentObject = parentObject;
            this.objectTypes = objectTypes;
            this.objectNameMask = objectNameMask;
            this.caseSensitive = caseSensitive;
            this.maxResults = maxResults;
        }

        @Override
        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                java.util.List<DBNNode> nodes = new ArrayList<DBNNode>();
                Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(
                    getProgressMonitor(),
                    parentObject,
                    objectTypes.toArray(new DBSObjectType[objectTypes.size()]),
                    objectNameMask,
                    caseSensitive,
                    maxResults);
                for (DBSObjectReference reference : objects) {
                    try {
                        DBSObject object = reference.resolveObject(getProgressMonitor());
                        if (object != null) {
                            DBNNode node = navigatorModel.getNodeByObject(getProgressMonitor(), object, true);
                            if (node != null) {
                                nodes.add(node);
                            }
                        }
                    } catch (DBException e) {
                        log.error(e);
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
                getShell().setDefaultButton(searchButton);
            }
        }

        @Override
        public void shellDeactivated(ShellEvent e)
        {
        }
    }

}
