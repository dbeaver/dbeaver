/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.perspective;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * DataSource Toolbar
 */
public class DataSourceManagementToolbar implements DBPRegistryListener, DBPEventListener, DBPPreferenceListener, IPageListener, IPartListener, ISelectionListener {
    static final Log log = Log.getLog(DataSourceManagementToolbar.class);

    public static final String EMPTY_SELECTION_TEXT = CoreMessages.toolbar_datasource_selector_empty;

    private IWorkbenchWindow workbenchWindow;
    private IWorkbenchPart activePart;

    private Text resultSetSize;
    private CImageCombo connectionCombo;
    private CImageCombo databaseCombo;

    private SoftReference<DBPDataSourceContainer> curDataSourceContainer = null;

    private final List<DBPDataSourceRegistry> handledRegistries = new ArrayList<>();
    private final List<DatabaseListReader> dbListReads = new ArrayList<>();

    private static class DatabaseListReader extends DataSourceJob {
        private final List<DBNDatabaseNode> nodeList = new ArrayList<>();
        private DBSObject active;
        private boolean enabled;

        public DatabaseListReader(@NotNull DBCExecutionContext context)
        {
            super(CoreMessages.toolbar_datasource_selector_action_read_databases, null, context);
            setSystem(true);
            this.enabled = false;
        }

        @Override
        public IStatus run(DBRProgressMonitor monitor)
        {
            DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, getExecutionContext().getDataSource());
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, getExecutionContext().getDataSource());
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
                    active = objectSelector.getSelectedObject();
                    // Cache navigator nodes
                    if (children != null) {
                        DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                        for (DBSObject child : children) {
                            if (DBUtils.getAdapter(DBSObjectContainer.class, child) != null) {
                                DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, child, false);
                                if (node != null) {
                                    nodeList.add(node);
                                } else {
                                    log.debug("Can't find node for object " + child);
                                }
                            }
                        }
                    }
                }
            }
            catch (DBException e) {
                return GeneralUtils.makeExceptionStatus(e);
            }
            finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
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
    private static IAdaptable getActiveObject(IWorkbenchPart activePart)
    {
        if (activePart instanceof IEditorPart) {
            return ((IEditorPart) activePart).getEditorInput();
        } else if (activePart instanceof IViewPart) {
            return activePart;
        } else {
            return null;
        }
    }

    @Nullable
    private DBPDataSourceContainer getDataSourceContainer()
    {
        return getDataSourceContainer(activePart);
    }

    @Nullable
    private static DBPDataSourceContainer getDataSourceContainer(IWorkbenchPart part)
    {
        if (part instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider)part).getDataSourceContainer();
        }

        final IAdaptable activeObject = getActiveObject(part);
        if (activeObject == null) {
            return null;
        }
        if (activeObject instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider)activeObject).getDataSourceContainer();
        } else if (activeObject instanceof DBPContextProvider) {
            DBCExecutionContext executionContext = ((DBPContextProvider) activeObject).getExecutionContext();
            if (executionContext != null) {
                return executionContext.getDataSource().getContainer();
            }
        }
        return null;
    }

    @Nullable
    private IDataSourceContainerProviderEx getActiveDataSourceUpdater()
    {
        if (activePart instanceof IDataSourceContainerProviderEx) {
            return (IDataSourceContainerProviderEx) activePart;
        } else {
            final IAdaptable activeObject = getActiveObject(activePart);
            if (activeObject == null) {
                return null;
            }
            return activeObject instanceof IDataSourceContainerProviderEx ? (IDataSourceContainerProviderEx)activeObject : null;
        }
    }

    private List<? extends DBPDataSourceContainer> getAvailableDataSources()
    {
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            return dataSourceContainer.getRegistry().getDataSources();
        } else {
            return DataSourceDescriptor.getActiveDataSources();
        }
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
            DBPDataSourceContainer oldContainer = getDataSourceContainer(activePart);
            DBPDataSourceContainer newContainer = getDataSourceContainer(part);
            activePart = part;
            if (oldContainer != newContainer) {
                if (oldContainer != null) {
                    oldContainer.getPreferenceStore().removePropertyChangeListener(this);
                }
                oldContainer = getDataSourceContainer();

                if (oldContainer != null) {
                    // Update editor actions
                    oldContainer.getPreferenceStore().addPropertyChangeListener(this);
                }
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
        final List<? extends DBPDataSourceContainer> dataSources = getAvailableDataSources();

        boolean update = force;
        if (!update) {
            // Check if there are any changes
            final List<DBPDataSourceContainer> oldDataSources = new ArrayList<>();
            for (TableItem item : connectionCombo.getItems()) {
                if (item.getData() instanceof DBPDataSourceContainer) {
                    oldDataSources.add((DBPDataSourceContainer) item.getData());
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
                connectionCombo.add(DBeaverIcons.getImage(DBIcon.TREE_DATABASE), EMPTY_SELECTION_TEXT, null, null);
            }

            int selectionIndex = 0;
            if (activePart != null) {
                final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
                if (!CommonUtils.isEmpty(dataSources)) {
                    DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                    for (int i = 0; i < dataSources.size(); i++) {
                        DBPDataSourceContainer ds = dataSources.get(i);
                        if (ds == null) {
                            continue;
                        }
                        if (update) {
                            DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(ds);
                            connectionCombo.add(
                                DBeaverIcons.getImage(dsNode == null ? DBIcon.TREE_DATABASE : dsNode.getNodeIconDefault()),
                                ds.getName(),
                                UIUtils.getConnectionColor(ds.getConnectionConfiguration()),
                                ds);
                        } else {
                            TableItem item = connectionCombo.getItem(i + 1);
                            item.setText(ds.getName());
                            item.setBackground(UIUtils.getConnectionColor(ds.getConnectionConfiguration()));
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
        if (PlatformUI.getWorkbench().isClosing()) {
            return;
        }
        if (event.getAction() == DBPEvent.Action.OBJECT_ADD ||
            event.getAction() == DBPEvent.Action.OBJECT_REMOVE ||
            (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getObject() == getDataSourceContainer()) ||
            (event.getAction() == DBPEvent.Action.OBJECT_SELECT && Boolean.TRUE.equals(event.getEnabled()) &&
                event.getObject().getDataSource() != null &&
                event.getObject().getDataSource().getContainer() == getDataSourceContainer())
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
        // This is a hack. We need to update main toolbar. By design toolbar should be updated along with command state
        // but in fact it doesn't. I don't know better way than trigger update explicitly.
        // TODO: replace with something smarter
        if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getEnabled() != null) {
            DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_CONNECTED);
            DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
            Display.getDefault().asyncExec(
                new Runnable() {
                    @Override
                    public void run()
                    {
                        IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
                        if (workbenchWindow instanceof WorkbenchWindow) {
                            ((WorkbenchWindow) workbenchWindow).updateActionBars();
                        }
                    }
                }
            );
        }

    }

    private void updateControls(boolean force)
    {
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();

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
        DBPDataSourceContainer dsContainer = getDataSourceContainer();
        if (dsContainer != null) {
            String rsSize = resultSetSize.getText();
            if (rsSize.length() == 0) {
                rsSize = "1"; //$NON-NLS-1$
            }
            DBPPreferenceStore store = dsContainer.getPreferenceStore();
            store.setValue(DBeaverPreferences.RESULT_SET_MAX_ROWS, rsSize);
            PrefUtils.savePreferenceStore(store);
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
            DBPDataSourceContainer dsContainer = getDataSourceContainer();
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
            final DBPDataSourceContainer dsContainer = getDataSourceContainer();
            databaseCombo.setEnabled(dsContainer != null);
            if (dsContainer != null && dsContainer.isConnected()) {
                final DBPDataSource dataSource = dsContainer.getDataSource();
                if (dataSource != null) {
                    synchronized (dbListReads) {
                        for (DatabaseListReader reader : dbListReads) {
                            if (reader.getExecutionContext().getDataSource() == dataSource) {
                                return;
                            }
                        }
                        DatabaseListReader databaseReader = new DatabaseListReader(dataSource.getDefaultContext(true));
                        databaseReader.addJobChangeListener(new JobChangeAdapter() {
                            @Override
                            public void done(final IJobChangeEvent event) {
                                UIUtils.runInUI(null, new Runnable() {
                                    @Override
                                    public void run() {
                                        fillDatabaseList((DatabaseListReader) event.getJob(), dsContainer);
                                    }
                                });
                            }
                        });
                        dbListReads.add(databaseReader);
                        databaseReader.schedule();
                    }
                }

                curDataSourceContainer = new SoftReference<>(dsContainer);
            } else {
                curDataSourceContainer = null;
                databaseCombo.removeAll();
            }
        }
    }

    private synchronized void fillDatabaseList(DatabaseListReader reader, DBPDataSourceContainer dsContainer)
    {
        synchronized (dbListReads) {
            dbListReads.remove(reader);
        }
        if (databaseCombo.isDisposed()) {
            return;
        }
        databaseCombo.setRedraw(false);
        try {
            databaseCombo.removeAll();
            if (!reader.enabled) {
                databaseCombo.setEnabled(false);
                return;
            }
            Collection<DBNDatabaseNode> dbList = reader.nodeList;
            if (dbList != null && !dbList.isEmpty()) {
                for (DBNDatabaseNode node : dbList) {
                    databaseCombo.add(
                        DBeaverIcons.getImage(node.getNodeIconDefault()),
                        node.getName(),
                        UIUtils.getConnectionColor(node.getObject().getDataSource().getContainer().getConnectionConfiguration()),
                        node);
                }
            }
            if (reader.active != null) {
                int dbCount = databaseCombo.getItemCount();
                for (int i = 0; i < dbCount; i++) {
                    String dbName = databaseCombo.getItemText(i);
                    if (dbName.equals(reader.active.getName())) {
                        databaseCombo.select(i);
                        break;
                    }
                }
            }
            databaseCombo.setEnabled(reader.enabled);
        }
        finally {
            if (!databaseCombo.isDisposed()) {
                databaseCombo.setRedraw(true);
            }
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

        DBPDataSourceContainer curDataSource = dataSourceUpdater.getDataSourceContainer();
        List<? extends DBPDataSourceContainer> dataSources = getAvailableDataSources();
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
                DBPDataSourceContainer selectedDataSource = dataSources.get(curIndex - 1);
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
        DBPDataSourceContainer dsContainer = getDataSourceContainer();
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
    public void preferenceChange(PreferenceChangeEvent event)
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
        gl.marginWidth = 5;
        gl.marginHeight = 0;
        comboGroup.setLayout(gl);

        connectionCombo = new CImageCombo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 160;
        gd.minimumWidth = 160;
        gd.grabExcessVerticalSpace = true;
        connectionCombo.setLayoutData(gd);
        connectionCombo.setVisibleItemCount(15);
        connectionCombo.setWidthHint(160);
        connectionCombo.setToolTipText(CoreMessages.toolbar_datasource_selector_combo_datasource_tooltip);
        connectionCombo.add(DBeaverIcons.getImage(DBIcon.TREE_DATABASE), EMPTY_SELECTION_TEXT, null, null);
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
        gd.grabExcessVerticalSpace = true;
        databaseCombo.setLayoutData(gd);
        databaseCombo.setVisibleItemCount(15);
        databaseCombo.setWidthHint(140);
        databaseCombo.setToolTipText(CoreMessages.toolbar_datasource_selector_combo_database_tooltip);
        databaseCombo.add(DBeaverIcons.getImage(DBIcon.TREE_DATABASE), EMPTY_SELECTION_TEXT, null, null);
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
        gd.grabExcessVerticalSpace = true;
        resultSetSize.setLayoutData(gd);

        resultSetSize.setToolTipText(CoreMessages.toolbar_datasource_selector_resultset_segment_size);
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            resultSetSize.setText(String.valueOf(dataSourceContainer.getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS)));
        }
        //resultSetSize.setDigits(7);
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
