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
package org.jkiss.dbeaver.ui.perspective;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceContainerProviderEx;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * DataSource Toolbar
 */
public class DataSourceManagementToolbar implements DBPRegistryListener, DBPEventListener, IPropertyChangeListener, IPageListener, IPartListener, ISelectionListener {
    static final Log log = Log.getLog(DataSourceManagementToolbar.class);

    public static final String EMPTY_SELECTION_TEXT = CoreMessages.toolbar_datasource_selector_empty;

    private IWorkbenchWindow workbenchWindow;
    private IWorkbenchPart activePart;

    private Text resultSetSize;
    private CImageCombo connectionCombo;
    private CImageCombo databaseCombo;

    private SoftReference<DBSDataSourceContainer> curDataSourceContainer = null;

    private final List<DBPDataSourceRegistry> handledRegistries = new ArrayList<DBPDataSourceRegistry>();
    private final List<DatabaseListReader> dbListReads = new ArrayList<DatabaseListReader>();

    private static class DatabaseListReader extends DataSourceJob {
        private final CurrentDatabasesInfo databasesInfo;
        private boolean enabled;

        public DatabaseListReader(DBPDataSource dataSource)
        {
            super(CoreMessages.toolbar_datasource_selector_action_read_databases, null, dataSource);
            setSystem(true);
            this.databasesInfo = new CurrentDatabasesInfo();
            this.enabled = false;
        }

        @Override
        public IStatus run(DBRProgressMonitor monitor)
        {
            DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, getDataSource());
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, getDataSource());
            if (objectContainer == null || objectSelector == null) {
                return Status.CANCEL_STATUS;
            }
            try {
                monitor.beginTask(CoreMessages.toolbar_datasource_selector_action_read_databases, 1);
                Class<? extends DBSObject> childType = objectContainer.getChildType(monitor);
                if (childType == null || !DBSObjectContainer.class.isAssignableFrom(childType)) {
                    enabled = false;
                } else {
                    enabled = true;
                    Collection<? extends DBSObject> children = objectContainer.getChildren(monitor);
                    databasesInfo.list = CommonUtils.isEmpty(children) ?
                        Collections.<DBSObject>emptyList() :
                        new ArrayList<DBSObject>(children);
                    databasesInfo.active = objectSelector.getSelectedObject();
                    // Cache navigator nodes
                    if (children != null) {
                        for (DBSObject child : children) {
                            DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(monitor, child, false);
                        }
                    }
                }
            }
            catch (DBException e) {
                return RuntimeUtils.makeExceptionStatus(e);
            }
            finally {
                monitor.done();
            }
            if (enabled) {
                // Cache navigator tree
                if (databasesInfo.list != null && !databasesInfo.list.isEmpty()) {
                    DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                    for (DBSObject database : databasesInfo.list) {
                        if (DBUtils.getAdapter(DBSObjectContainer.class, database) != null) {
                            navigatorModel.getNodeByObject(monitor, database, false);
                        }
                    }
                }
            }
            return Status.OK_STATUS;
        }
    }

    private static class CurrentDatabasesInfo {
        Collection<? extends DBSObject> list;
        DBSObject active;
    }

    public DataSourceManagementToolbar(IWorkbenchWindow workbenchWindow)
    {
        this.workbenchWindow = workbenchWindow;
    }

    private void dispose()
    {
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage != null) {
            pageClosed(activePage);
        }

        DataSourceProviderRegistry.getInstance().removeDataSourceRegistryListener(this);
        for (DBPDataSourceRegistry registry : handledRegistries) {
            registry.removeDataSourceListener(this);
        }

        setActivePart(null);

        this.workbenchWindow.removePageListener(this);
    }

    @Override
    public void handleRegistryLoad(DBPDataSourceRegistry registry)
    {
        registry.addDataSourceListener(this);
        handledRegistries.add(registry);
    }

    @Override
    public void handleRegistryUnload(DBPDataSourceRegistry registry)
    {
        handledRegistries.remove(registry);
        registry.removeDataSourceListener(this);
    }

    @Nullable
    private IAdaptable getActiveObject()
    {
        if (activePart instanceof IEditorPart) {
            return ((IEditorPart) activePart).getEditorInput();
        } else if (activePart instanceof IViewPart) {
            return activePart;
        } else {
            return null;
        }
    }

    private IProject getActiveProject()
    {
        final IAdaptable activeObject = getActiveObject();
        if (activeObject instanceof IEditorInput) {
            final IFile file = ContentUtils.getFileFromEditorInput((IEditorInput) activeObject);
            if (file != null) {
                // If this is a content editor file may belong to temp project
                if (file.getProject() != DBeaverCore.getInstance().getTempProject()) {
                    return file.getProject();
                }
            }
        }
        return DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
    }

    @Nullable
    private DBSDataSourceContainer getDataSourceContainer()
    {
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider)activePart).getDataSourceContainer();
        }

        final IAdaptable activeObject = getActiveObject();
        if (activeObject == null) {
            return null;
        }
        if (activeObject instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider)activeObject).getDataSourceContainer();
        } else if (activeObject instanceof IDataSourceProvider) {
            final DBPDataSource dataSource = ((IDataSourceProvider) activeObject).getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
        } else {
            return null;
        }
    }

    @Nullable
    private IDataSourceContainerProviderEx getActiveDataSourceUpdater()
    {
        if (activePart instanceof IDataSourceContainerProviderEx) {
            return (IDataSourceContainerProviderEx) activePart;
        } else {
            final IAdaptable activeObject = getActiveObject();
            if (activeObject == null) {
                return null;
            }
            return activeObject instanceof IDataSourceContainerProviderEx ? (IDataSourceContainerProviderEx)activeObject : null;
        }
    }

    private List<? extends DBSDataSourceContainer> getAvailableDataSources()
    {
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            return dataSourceContainer.getRegistry().getDataSources();
        } else {
            final IProject project = getActiveProject();
            if (project != null) {
                DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
                return dataSourceRegistry == null ? Collections.<DBSDataSourceContainer>emptyList() : dataSourceRegistry.getDataSources();
            }
        }
        return Collections.emptyList();
    }

    public void setActivePart(@Nullable IWorkbenchPart part)
    {
        if (!(part instanceof IEditorPart)) {
            if (part == null || part.getSite() == null || part.getSite().getPage() == null) {
                part = null;
            } else {
                // Toolbar works only with active editor
                // Other parts just doesn't matter
                part = part.getSite().getPage().getActiveEditor();
            }
        }
        if (activePart != part || activePart == null) {
            // Update previous statuses
            DBSDataSourceContainer container = getDataSourceContainer();
            if (container != null) {
                container.getPreferenceStore().removePropertyChangeListener(this);
            }
            activePart = part;
            container = getDataSourceContainer();

            if (container != null) {
                // Update editor actions
                container.getPreferenceStore().addPropertyChangeListener(this);
            }

            // Update controls and actions
            updateControls(true);
        }

        UIUtils.updateMainWindowTitle(workbenchWindow);
    }

    private void fillDataSourceList(boolean force) {
        if (connectionCombo.isDisposed()) {
            return;
        }
        final List<? extends DBSDataSourceContainer> dataSources = getAvailableDataSources();

        boolean update = force;
        if (!update) {
            // Check if there are any changes
            final List<DBSDataSourceContainer> oldDataSources = new ArrayList<DBSDataSourceContainer>();
            for (TableItem item : connectionCombo.getItems()) {
                if (item.getData() instanceof DBSDataSourceContainer) {
                    oldDataSources.add((DBSDataSourceContainer) item.getData());
                }
            }
            if (oldDataSources.size() == dataSources.size()) {
                for (int i = 0; i < dataSources.size(); i++) {
                    if (dataSources.get(i) != oldDataSources.get(i)) {
                        update = true;
                        break;
                    }
                }
            } else {
                update = true;
            }
        }

        if (update) {
            connectionCombo.setRedraw(false);
        }
        try {
            if (update) {
                connectionCombo.removeAll();
                connectionCombo.add(DBIcon.TREE_DATABASE.getImage(), EMPTY_SELECTION_TEXT, null, null);
            }

            int selectionIndex = 0;
            if (activePart != null) {
                final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
                if (!CommonUtils.isEmpty(dataSources)) {
                    DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                    for (int i = 0; i < dataSources.size(); i++) {
                        DBSDataSourceContainer ds = dataSources.get(i);
                        if (ds == null) {
                            continue;
                        }
                        if (update) {
                            DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(ds);
                            connectionCombo.add(
                                dsNode == null ? DBIcon.TREE_DATABASE.getImage() : dsNode.getNodeIconDefault(),
                                ds.getName(),
                                ds.getConnectionInfo().getColor(),
                                ds);
                        } else {
                            TableItem item = connectionCombo.getItem(i + 1);
                            item.setText(ds.getName());
                            item.setBackground(ds.getConnectionInfo().getColor());
                        }
                        if (dataSourceContainer == ds) {
                            selectionIndex = i + 1;
                        }
                    }
                }
            }
            connectionCombo.select(selectionIndex);
        } finally {
            if (update) {
                connectionCombo.setRedraw(true);
            }
        }
    }

    @Override
    public void handleDataSourceEvent(final DBPEvent event)
    {
        if (event.getAction() == DBPEvent.Action.OBJECT_ADD ||
            event.getAction() == DBPEvent.Action.OBJECT_REMOVE ||
            (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getObject() == getDataSourceContainer()) ||
            (event.getAction() == DBPEvent.Action.OBJECT_SELECT && Boolean.TRUE.equals(event.getEnabled()) && event.getObject().getDataSource().getContainer() == getDataSourceContainer())
            )
        {
            Display.getDefault().asyncExec(
                new Runnable() {
                    @Override
                    public void run()
                    {
                        updateControls(true);
                    }
                }
            );
        }
    }

    private void updateControls(boolean force)
    {
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();

        // Update resultset max size
        if (resultSetSize != null && !resultSetSize.isDisposed()) {
            if (dataSourceContainer == null) {
                resultSetSize.setEnabled(false);
                resultSetSize.setText(""); //$NON-NLS-1$
            } else {
                resultSetSize.setEnabled(true);
                resultSetSize.setText(String.valueOf(dataSourceContainer.getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS)));
            }
        }

        // Update datasources combo
        updateDataSourceList(force);
        updateDatabaseList(force);
    }

    private void changeResultSetSize()
    {
        DBSDataSourceContainer dsContainer = getDataSourceContainer();
        if (dsContainer != null) {
            String rsSize = resultSetSize.getText();
            if (rsSize.length() == 0) {
                rsSize = "1"; //$NON-NLS-1$
            }
            IPreferenceStore store = dsContainer.getPreferenceStore();
            store.setValue(DBeaverPreferences.RESULT_SET_MAX_ROWS, rsSize);
            RuntimeUtils.savePreferenceStore(store);
        }
    }

    private void updateDataSourceList(boolean force)
    {
        if (connectionCombo != null && !connectionCombo.isDisposed()) {
            IDataSourceContainerProviderEx containerProvider = getActiveDataSourceUpdater();
            if (containerProvider == null) {
                connectionCombo.removeAll();
                connectionCombo.setEnabled(false);
            } else {
                connectionCombo.setEnabled(true);
            }
            fillDataSourceList(force);
        }
    }

    private void updateDatabaseList(boolean force)
    {
        if (!force) {
            DBSDataSourceContainer dsContainer = getDataSourceContainer();
            if (curDataSourceContainer != null && dsContainer == curDataSourceContainer.get()) {
                // The same DS container - nothing to change in DB list
                return;
            }
        }

        // Update databases combo
        fillDatabaseCombo();
    }

    private void fillDatabaseCombo()
    {
        if (databaseCombo != null && !databaseCombo.isDisposed()) {
            final DBSDataSourceContainer dsContainer = getDataSourceContainer();
            databaseCombo.setEnabled(dsContainer != null);
            if (dsContainer != null && dsContainer.isConnected()) {
                final DBPDataSource dataSource = dsContainer.getDataSource();

                synchronized (dbListReads) {
                    for (DatabaseListReader reader : dbListReads) {
                        if (reader.getDataSource() == dataSource) {
                            return;
                        }
                    }
                    DatabaseListReader databaseReader = new DatabaseListReader(dataSource);
                    databaseReader.addJobChangeListener(new JobChangeAdapter() {
                        @Override
                        public void done(final IJobChangeEvent event)
                        {
                            UIUtils.runInUI(null, new Runnable() {
                                @Override
                                public void run()
                                {
                                    fillDatabaseList((DatabaseListReader) event.getJob(), dsContainer);
                                }
                            });
                        }
                    });
                    dbListReads.add(databaseReader);
                    databaseReader.schedule();
                }

                curDataSourceContainer = new SoftReference<DBSDataSourceContainer>(dsContainer);
            } else {
                curDataSourceContainer = null;
                databaseCombo.removeAll();
            }
        }
    }

    private synchronized void fillDatabaseList(DatabaseListReader reader, DBSDataSourceContainer dsContainer)
    {
        databaseCombo.setRedraw(false);
        try {
            // Remove all but first item (which is constant)
            databaseCombo.removeAll();
            synchronized (dbListReads) {
                dbListReads.remove(reader);
            }
            if (!reader.enabled || dsContainer.getDataSource() != reader.getDataSource()) {
                databaseCombo.setEnabled(reader.enabled);
                return;
            }
            if (databaseCombo.isDisposed()) {
                return;
            }
            if (reader.databasesInfo.list != null && !reader.databasesInfo.list.isEmpty()) {
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                for (DBSObject database : reader.databasesInfo.list) {
                    if (database instanceof DBSObjectContainer) {
                        DBNDatabaseNode dbNode = navigatorModel.getNodeByObject(database);
                        if (dbNode != null) {
                            DBPDataSource dataSource = database.getDataSource();
                            databaseCombo.add(
                                dbNode.getNodeIconDefault(),
                                database.getName(),
                                dataSource.getContainer().getConnectionInfo().getColor(),
                                database);
                        }
                    }
                }
            }
            if (reader.databasesInfo.active != null) {
                int dbCount = databaseCombo.getItemCount();
                for (int i = 0; i < dbCount; i++) {
                    String dbName = databaseCombo.getItemText(i);
                    if (dbName.equals(reader.databasesInfo.active.getName())) {
                        databaseCombo.select(i);
                        break;
                    }
                }
            }
            databaseCombo.setEnabled(reader.enabled);
        }
        finally {
            databaseCombo.setRedraw(true);
        }
    }

    private void changeDataSourceSelection()
    {
        if (connectionCombo == null || connectionCombo.isDisposed()) {
            return;
        }
        IDataSourceContainerProviderEx dataSourceUpdater = getActiveDataSourceUpdater();
        if (dataSourceUpdater == null) {
            return;
        }

        DBSDataSourceContainer curDataSource = dataSourceUpdater.getDataSourceContainer();
        List<? extends DBSDataSourceContainer> dataSources = getAvailableDataSources();
        if (!CommonUtils.isEmpty(dataSources)) {
            int curIndex = connectionCombo.getSelectionIndex();
            if (curIndex == 0) {
                if (curDataSource == null) {
                    // Nothing changed
                    return;
                }
                dataSourceUpdater.setDataSourceContainer(null);
            } else if (curIndex > dataSources.size()) {
                log.warn("Connection combo index out of bounds (" + curIndex + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            } else {
                // Change data source
                DBSDataSourceContainer selectedDataSource = dataSources.get(curIndex - 1);
                if (selectedDataSource == curDataSource) {
                    return;
                } else {
                    dataSourceUpdater.setDataSourceContainer(selectedDataSource);
                }
            }
        }
        updateControls(false);
    }

    private void changeDataBaseSelection()
    {
        if (databaseCombo == null || databaseCombo.isDisposed() || databaseCombo.getSelectionIndex() < 0) {
            return;
        }
        DBSDataSourceContainer dsContainer = getDataSourceContainer();
        final String newName = databaseCombo.getItemText(databaseCombo.getSelectionIndex());
        if (dsContainer != null && dsContainer.isConnected()) {
            final DBPDataSource dataSource = dsContainer.getDataSource();
            try {
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            DBSObjectContainer oc = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
                            DBSObjectSelector os = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
                            if (oc != null && os != null && os.supportsObjectSelect()) {
                                DBSObject newChild = oc.getChild(monitor, newName);
                                if (newChild != null) {
                                    os.selectObject(monitor, newChild);
                                } else {
                                    throw new DBException(MessageFormat.format(CoreMessages.toolbar_datasource_selector_error_database_not_found, newName));
                                }
                            } else {
                                throw new DBException(CoreMessages.toolbar_datasource_selector_error_database_change_not_supported);
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(
                    workbenchWindow.getShell(),
                    CoreMessages.toolbar_datasource_selector_error_change_database_title,
                    CoreMessages.toolbar_datasource_selector_error_change_database_message,
                    e.getTargetException());
            } catch (InterruptedException e) {
                // skip
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(DBeaverPreferences.RESULT_SET_MAX_ROWS) && !resultSetSize.isDisposed()) {
            if (event.getNewValue() != null) {
                resultSetSize.setText(event.getNewValue().toString());
            }
        }
    }

    // IPageListener

    @Override
    public void pageActivated(IWorkbenchPage page)
    {
        // do nothing
    }

    @Override
    public void pageClosed(IWorkbenchPage page)
    {
        page.removePartListener(this);
        page.removeSelectionListener(this);
    }

    @Override
    public void pageOpened(IWorkbenchPage page)
    {
        page.addPartListener(this);
        page.addSelectionListener(this);
    }

    // IPartListener

    @Override
    public void partActivated(IWorkbenchPart part)
    {
        setActivePart(part);
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part)
    {
    }

    @Override
    public void partClosed(IWorkbenchPart part)
    {
        if (part == activePart) {
            setActivePart(null);
        }
    }

    @Override
    public void partDeactivated(IWorkbenchPart part)
    {
        // Do nothing

//        if (part == activePart) {
//            setActivePart(null);
//        }
    }

    @Override
    public void partOpened(IWorkbenchPart part)
    {
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection)
    {
        if (part == activePart && selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element != null) {
                if (RuntimeUtils.getObjectAdapter(element, DBSObject.class) != null) {
                    updateControls(false);
                }
            }
        }
    }

    Control createControl(Composite parent)
    {
        workbenchWindow.addPageListener(DataSourceManagementToolbar.this);
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage != null) {
            pageOpened(activePage);
        }

        // Register as datasource listener in all datasources
        // We need it because at this moment there could be come already loaded registries (on startup)
        final DBeaverCore core = DBeaverCore.getInstance();
        DataSourceProviderRegistry.getInstance().addDataSourceRegistryListener(DataSourceManagementToolbar.this);
        for (IProject project : core.getLiveProjects()) {
            if (project.isOpen()) {
                DataSourceRegistry registry = core.getProjectRegistry().getDataSourceRegistry(project);
                if (registry != null) {
                    handleRegistryLoad(registry);
                }
            }
        }

        Composite comboGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        comboGroup.setLayout(gl);

        connectionCombo = new CImageCombo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 160;
        gd.minimumWidth = 160;
        connectionCombo.setLayoutData(gd);
        connectionCombo.setVisibleItemCount(15);
        connectionCombo.setWidthHint(160);
        connectionCombo.setToolTipText(CoreMessages.toolbar_datasource_selector_combo_datasource_tooltip);
        connectionCombo.add(DBIcon.TREE_DATABASE.getImage(), EMPTY_SELECTION_TEXT, null, null);
        connectionCombo.select(0);
        connectionCombo.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                changeDataSourceSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
            }
        });

        databaseCombo = new CImageCombo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
        gd = new GridData();
        gd.widthHint = 140;
        gd.minimumWidth = 140;
        databaseCombo.setLayoutData(gd);
        databaseCombo.setVisibleItemCount(15);
        databaseCombo.setWidthHint(140);
        databaseCombo.setToolTipText(CoreMessages.toolbar_datasource_selector_combo_database_tooltip);
        databaseCombo.add(DBIcon.TREE_DATABASE.getImage(), EMPTY_SELECTION_TEXT, null, null);
        databaseCombo.select(0);
        databaseCombo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                changeDataBaseSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
            }
        });

        resultSetSize = new Text(comboGroup, SWT.BORDER);
        resultSetSize.setTextLimit(10);
        gd = new GridData();
        gd.widthHint = 30;

        resultSetSize.setToolTipText(CoreMessages.toolbar_datasource_selector_resultset_segment_size);
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            resultSetSize.setText(String.valueOf(dataSourceContainer.getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS)));
        }
        //resultSetSize.setDigits(7);
        resultSetSize.setLayoutData(gd);
        resultSetSize.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        resultSetSize.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e)
            {
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                changeResultSetSize();
            }
        });
        comboGroup.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                DataSourceManagementToolbar.this.dispose();
            }
        });

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (workbenchWindow != null && workbenchWindow.getActivePage() != null) {
                    setActivePart(workbenchWindow.getActivePage().getActivePart());
                }
            }
        });

        return comboGroup;
    }

    public static class ToolbarContribution extends WorkbenchWindowControlContribution {
        public ToolbarContribution()
        {
            super(IActionConstants.TOOLBAR_DATASOURCE);
        }

        @Override
        protected Control createControl(Composite parent)
        {
            DataSourceManagementToolbar toolbar = new DataSourceManagementToolbar(DBeaverUI.getActiveWorkbenchWindow());
            return toolbar.createControl(parent);
        }
    }
}
