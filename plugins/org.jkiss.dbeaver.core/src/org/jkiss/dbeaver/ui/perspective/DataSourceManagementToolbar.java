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
package org.jkiss.dbeaver.ui.perspective;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
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
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPRegistryListener;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.dialogs.SelectObjectDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.util.*;

/**
 * DataSource Toolbar
 */
public class DataSourceManagementToolbar implements DBPRegistryListener, DBPEventListener, DBPPreferenceListener, INavigatorListener {
    private static final Log log = Log.getLog(DataSourceManagementToolbar.class);

    public static final String EMPTY_SELECTION_TEXT = CoreMessages.toolbar_datasource_selector_empty;

    private static DataSourceManagementToolbar toolBarInstance;

    private IWorkbenchWindow workbenchWindow;
    private IWorkbenchPart activePart;
    private IPageListener pageListener;
    private IPartListener partListener;

    private Text resultSetSize;
    private CSmartSelector<DBPDataSourceContainer> connectionCombo;
    private CSmartSelector<DBNDatabaseNode> databaseCombo;

    private SoftReference<DBPDataSourceContainer> curDataSourceContainer = null;

    private final List<DBPDataSourceRegistry> handledRegistries = new ArrayList<>();
    private final List<DatabaseListReader> dbListReads = new ArrayList<>();
    private volatile IFile activeFile;

    private static class DatabaseListReader extends DataSourceJob {
        private final List<DBNDatabaseNode> nodeList = new ArrayList<>();
        private DBSObject active;
        private boolean enabled;

        public DatabaseListReader(@NotNull DBCExecutionContext context) {
            super(CoreMessages.toolbar_datasource_selector_action_read_databases, context);
            setSystem(true);
            this.enabled = false;
        }

        @Override
        public IStatus run(DBRProgressMonitor monitor) {
            final DBPDataSource dataSource = getExecutionContext().getDataSource();
            DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
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
                    DBSObject defObject = objectSelector.getDefaultObject();
                    if (defObject instanceof DBSObjectContainer) {
                        // Default object can be object container + object selector (e.g. in PG)
                        objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                        if (objectSelector != null && objectSelector.supportsDefaultChange()) {
                            objectContainer = (DBSObjectContainer) defObject;
                            defObject = objectSelector.getDefaultObject();
                        }
                    }
                    Collection<? extends DBSObject> children = objectContainer.getChildren(monitor);
                    active = defObject;
                    // Cache navigator nodes
                    if (children != null) {
                        DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                        for (DBSObject child : children) {
                            if (DBUtils.getAdapter(DBSObjectContainer.class, child) != null) {
                                DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, child, false);
                                if (node != null) {
                                    nodeList.add(node);
                                }
                            }
                        }
                    }
                }
            } catch (DBException e) {
                return GeneralUtils.makeExceptionStatus(e);
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }

    public static DataSourceManagementToolbar getInstance() {
        return toolBarInstance;
    }

    public DataSourceManagementToolbar(IWorkbenchWindow workbenchWindow) {
        toolBarInstance = this;
        this.workbenchWindow = workbenchWindow;
        DBeaverCore.getInstance().getNavigatorModel().addListener(this);

        final ISelectionListener selectionListener = new ISelectionListener() {
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection) {
                if (part == activePart && selection instanceof IStructuredSelection) {
                    final Object element = ((IStructuredSelection) selection).getFirstElement();
                    if (element != null) {
                        if (RuntimeUtils.getObjectAdapter(element, DBSObject.class) != null) {
                            updateControls(false);
                        }
                    }
                }
            }

        };
        pageListener = new AbstractPageListener() {
            @Override
            public void pageClosed(IWorkbenchPage page) {
                page.removePartListener(partListener);
                page.removeSelectionListener(selectionListener);
            }

            @Override
            public void pageOpened(IWorkbenchPage page) {
                page.addPartListener(partListener);
                page.addSelectionListener(selectionListener);
            }
        };
        partListener = new AbstractPartListener() {
            @Override
            public void partActivated(IWorkbenchPart part) {
                setActivePart(part);
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if (part == activePart) {
                    setActivePart(null);
                }
            }
        };
    }

    private void dispose() {
        DBeaverCore.getInstance().getNavigatorModel().removeListener(this);

        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage != null) {
            pageListener.pageClosed(activePage);
        }

        DataSourceProviderRegistry.getInstance().removeDataSourceRegistryListener(this);
        for (DBPDataSourceRegistry registry : handledRegistries) {
            registry.removeDataSourceListener(this);
        }

        setActivePart(null);

        this.workbenchWindow.removePageListener(pageListener);
    }

    @Override
    public void handleRegistryLoad(DBPDataSourceRegistry registry) {
        registry.addDataSourceListener(this);
        handledRegistries.add(registry);
    }

    @Override
    public void handleRegistryUnload(DBPDataSourceRegistry registry) {
        handledRegistries.remove(registry);
        registry.removeDataSourceListener(this);
    }

    @Nullable
    private static IAdaptable getActiveObject(IWorkbenchPart activePart) {
        if (activePart instanceof IEditorPart) {
            return ((IEditorPart) activePart).getEditorInput();
        } else if (activePart instanceof IViewPart) {
            return activePart;
        } else {
            return null;
        }
    }

    @Nullable
    private DBPDataSourceContainer getDataSourceContainer() {
        return getDataSourceContainer(activePart);
    }

    @Nullable
    private static DBPDataSourceContainer getDataSourceContainer(IWorkbenchPart part) {
        if (part == null) {
            return null;
        }
        if (part instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) part).getDataSourceContainer();
        }

        final IAdaptable activeObject = getActiveObject(part);
        if (activeObject == null) {
            return null;
        }
        if (activeObject instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activeObject).getDataSourceContainer();
        } else if (activeObject instanceof DBPContextProvider) {
            DBCExecutionContext executionContext = ((DBPContextProvider) activeObject).getExecutionContext();
            if (executionContext != null) {
                return executionContext.getDataSource().getContainer();
            }
        }
        return null;
    }

    @Nullable
    private IDataSourceContainerProviderEx getActiveDataSourceUpdater() {
        if (activePart instanceof IDataSourceContainerProviderEx) {
            return (IDataSourceContainerProviderEx) activePart;
        } else {
            final IAdaptable activeObject = getActiveObject(activePart);
            if (activeObject == null) {
                return null;
            }
            return activeObject instanceof IDataSourceContainerProviderEx ? (IDataSourceContainerProviderEx) activeObject : null;
        }
    }

    private List<? extends DBPDataSourceContainer> getAvailableDataSources() {
        //Get project from active editor
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor != null && activeEditor.getEditorInput() instanceof IFileEditorInput) {
            final IFile curFile = ((IFileEditorInput) activeEditor.getEditorInput()).getFile();
            if (curFile != null) {
                DBPDataSourceContainer fileDataSource = EditorUtils.getFileDataSource(curFile);
                if (fileDataSource != null) {
                    return fileDataSource.getRegistry().getDataSources();
                }
                final DataSourceRegistry dsRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(curFile.getProject());
                if (dsRegistry != null) {
                    return dsRegistry.getDataSources();
                }
            }
        }
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            return dataSourceContainer.getRegistry().getDataSources();
        } else {
            return DataSourceRegistry.getAllDataSources();
        }
    }

    private IProject getActiveProject() {
        //Get project from active editor
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor != null && activeEditor.getEditorInput() instanceof IFileEditorInput) {
            final IFile curFile = ((IFileEditorInput) activeEditor.getEditorInput()).getFile();
            if (curFile != null) {
                return curFile.getProject();
            }
        }
        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            return dataSourceContainer.getRegistry().getProject();
        } else {
            return DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        }
    }

    public void setActivePart(@Nullable IWorkbenchPart part) {
        if (!(part instanceof IEditorPart)) {
            if (part == null || part.getSite() == null || part.getSite().getPage() == null) {
                part = null;
            } else {
                // Toolbar works only with active editor
                // Other parts just doesn't matter
                part = part.getSite().getPage().getActiveEditor();
            }
        }
        if (connectionCombo != null && !connectionCombo.isDisposed()) {
            final int selConnection = connectionCombo.getSelectionIndex();
            DBPDataSourceContainer visibleContainer = null;
            if (selConnection > 0) {
                visibleContainer = connectionCombo.getItem(selConnection);
            }
            DBPDataSourceContainer newContainer = getDataSourceContainer(part);
            if (activePart != part || activePart == null || visibleContainer != newContainer) {
                // Update previous statuses
                DBPDataSourceContainer oldContainer = getDataSourceContainer(activePart);
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
        }
        if (part != null) {
            final IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
            activeFile = EditorUtils.getFileFromInput(editorInput);
        } else {
            activeFile = null;
        }
    }

    private void fillDataSourceList(boolean force) {
        if (connectionCombo.isDisposed()) {
            return;
        }
        final List<? extends DBPDataSourceContainer> dataSources = getAvailableDataSources();

        boolean update = force;
        if (!update) {
            // Check if there are any changes
            final List<DBPDataSourceContainer> oldDataSources = new ArrayList<>(connectionCombo.getItems());
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
                connectionCombo.addItem(null);
            }

            int selectionIndex = 0;
            if (activePart != null) {
                final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
                if (!CommonUtils.isEmpty(dataSources)) {
                    for (int i = 0; i < dataSources.size(); i++) {
                        DBPDataSourceContainer ds = dataSources.get(i);
                        if (ds == null) {
                            continue;
                        }
                        if (update) {
                            connectionCombo.addItem(ds);
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
    public void handleDataSourceEvent(final DBPEvent event) {
        if (PlatformUI.getWorkbench().isClosing()) {
            return;
        }
        if (event.getAction() == DBPEvent.Action.OBJECT_ADD ||
            event.getAction() == DBPEvent.Action.OBJECT_REMOVE ||
            (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getObject() == getDataSourceContainer()) ||
            (event.getAction() == DBPEvent.Action.OBJECT_SELECT && Boolean.TRUE.equals(event.getEnabled()) &&
                DBUtils.getContainer(event.getObject()) == getDataSourceContainer())
            ) {
            DBeaverUI.asyncExec(
                new Runnable() {
                    @Override
                    public void run() {
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
            DBeaverUI.asyncExec(
                new Runnable() {
                    @Override
                    public void run() {
                        IWorkbenchWindow workbenchWindow = DBeaverUI.getActiveWorkbenchWindow();
                        if (workbenchWindow instanceof WorkbenchWindow) {
                            ((WorkbenchWindow) workbenchWindow).updateActionBars();
                        }
                    }
                }
            );
        }

    }

    private void updateControls(boolean force) {
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

    private void changeResultSetSize() {
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

    private void updateDataSourceList(boolean force) {
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

    private void updateDatabaseList(boolean force) {
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

    private void fillDatabaseCombo() {
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
                                DBeaverUI.syncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        fillDatabaseList((DatabaseListReader) event.getJob());
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

    private synchronized void fillDatabaseList(DatabaseListReader reader) {
        synchronized (dbListReads) {
            dbListReads.remove(reader);
        }
        if (databaseCombo.isDisposed()) {
            return;
        }
        databaseCombo.setRedraw(false);
        try {
            databaseCombo.removeAll();
            if (reader.active == null) {
                databaseCombo.addItem(null);
            }
            if (!reader.enabled) {
                databaseCombo.setEnabled(false);
                return;
            }
            Collection<DBNDatabaseNode> dbList = reader.nodeList;
            if (dbList != null && !dbList.isEmpty()) {
                for (DBNDatabaseNode node : dbList) {
                    databaseCombo.addItem(node);
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
        } finally {
            if (!databaseCombo.isDisposed()) {
                databaseCombo.setRedraw(true);
            }
        }
    }

    private void changeDataSourceSelection(final DBPDataSourceContainer selectedDataSource) {
        if (connectionCombo == null || connectionCombo.isDisposed()) {
            return;
        }
        final IDataSourceContainerProviderEx dataSourceUpdater = getActiveDataSourceUpdater();
        if (dataSourceUpdater == null) {
            return;
        }

        final AbstractJob updateJob = new AbstractJob("Change active database") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                dataSourceUpdater.setDataSourceContainer(selectedDataSource);
                return Status.OK_STATUS;
            }
        };
        updateJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                DBeaverUI.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        updateControls(false);
                    }
                });
            }
        });
        updateJob.schedule();
    }

    private void changeDataBaseSelection(DBNDatabaseNode node) {
        DBPDataSourceContainer dsContainer = getDataSourceContainer();
        final String newName = node.getNodeName();
        if (dsContainer != null && dsContainer.isConnected()) {
            final DBPDataSource dataSource = dsContainer.getDataSource();
            new AbstractJob("Change active database") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        DBSObjectContainer oc = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
                        DBSObjectSelector os = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
                        if (os != null) {
                            final DBSObject defObject = os.getDefaultObject();
                            if (defObject instanceof DBSObjectContainer) {
                                // USe seconds level of active object
                                DBSObjectSelector os2 = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                                if (os2 != null && os2.supportsDefaultChange()) {
                                    oc = (DBSObjectContainer) defObject;
                                    os = os2;
                                }
                            }
                        }

                        if (oc != null && os != null && os.supportsDefaultChange()) {
                            DBSObject newChild = oc.getChild(monitor, newName);
                            if (newChild != null) {
                                os.setDefaultObject(monitor, newChild);
                            } else {
                                throw new DBException(MessageFormat.format(CoreMessages.toolbar_datasource_selector_error_database_not_found, newName));
                            }
                        } else {
                            throw new DBException(CoreMessages.toolbar_datasource_selector_error_database_change_not_supported);
                        }
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (event.getProperty().equals(DBeaverPreferences.RESULT_SET_MAX_ROWS) && !resultSetSize.isDisposed()) {
            if (event.getNewValue() != null) {
                resultSetSize.setText(event.getNewValue().toString());
            }
        }
    }

    Control createControl(Composite parent) {
        workbenchWindow.addPageListener(pageListener);
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage != null) {
            pageListener.pageOpened(activePage);
        }

        // Register as datasource listener in all datasources
        // We need it because at this moment there could be come already loaded registries (on startup)
        DataSourceProviderRegistry.getInstance().addDataSourceRegistryListener(DataSourceManagementToolbar.this);
        for (DataSourceRegistry registry : DataSourceRegistry.getAllRegistries()) {
            handleRegistryLoad(registry);
        }

        Composite comboGroup = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginWidth = 5;
        layout.marginHeight = 0;

        comboGroup.setLayout(layout);

        final int fontHeight = UIUtils.getFontHeight(parent);
        int comboWidth = fontHeight * 20;

        connectionCombo = new CSmartSelector<DBPDataSourceContainer>(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER, new ConnectionLabelProvider()) {
            @Override
            protected void dropDown(boolean drop) {
                if (!drop) {
                    return;
                }
                showConnectionSelector();
            }
        };
        RowData rd = new RowData();
        rd.width = comboWidth;
        connectionCombo.setLayoutData(rd);
        connectionCombo.setVisibleItemCount(15);
        connectionCombo.setWidthHint(comboWidth);
        connectionCombo.setToolTipText(CoreMessages.toolbar_datasource_selector_combo_datasource_tooltip);
        connectionCombo.addItem(null);
        connectionCombo.select(0);

        comboWidth = fontHeight * 16;
        databaseCombo = new CSmartSelector<DBNDatabaseNode>(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER, new DatabaseLabelProvider()) {
            @Override
            protected void dropDown(boolean drop) {
                if (!drop) {
                    return;
                }
                showDatabaseSelector();
            }
        };
        rd = new RowData();
        rd.width = comboWidth;
        databaseCombo.setLayoutData(rd);
        databaseCombo.setVisibleItemCount(15);
        databaseCombo.setWidthHint(comboWidth);
        databaseCombo.setToolTipText(CoreMessages.toolbar_datasource_selector_combo_database_tooltip);
        databaseCombo.addItem(null);
        databaseCombo.select(0);

        resultSetSize = new Text(comboGroup, SWT.BORDER);
        resultSetSize.setTextLimit(10);
        rd = new RowData();
        rd.width = fontHeight * 4;
        resultSetSize.setLayoutData(rd);
        resultSetSize.setToolTipText(CoreMessages.toolbar_datasource_selector_resultset_segment_size);

        final DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            resultSetSize.setText(String.valueOf(dataSourceContainer.getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS)));
        }
        //resultSetSize.setDigits(7);
        resultSetSize.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        resultSetSize.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                changeResultSetSize();
            }
        });
        comboGroup.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                DataSourceManagementToolbar.this.dispose();
            }
        });

        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (workbenchWindow != null && workbenchWindow.getActivePage() != null) {
                    setActivePart(workbenchWindow.getActivePage().getActivePart());
                }
            }
        });

        return comboGroup;
    }

    void showConnectionSelector() {
        SelectDataSourceDialog dialog = new SelectDataSourceDialog(connectionCombo.getShell(), getActiveProject(), connectionCombo.getSelectedItem());
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return;
        }
        DBPDataSourceContainer dataSource = dialog.getDataSource();
        connectionCombo.select(dataSource);
        changeDataSourceSelection(dataSource);
    }

    void showDatabaseSelector() {
        DBNDatabaseNode selectedDB = databaseCombo.getSelectedItem();
        List<DBNDatabaseNode> items = new ArrayList<>(databaseCombo.getItems());
        items.removeIf(Objects::isNull);
        if (items.isEmpty()) {
            return;
        }
        SelectObjectDialog<DBNDatabaseNode> dialog = new SelectObjectDialog<>(databaseCombo.getShell(),
            "Choose catalog/schema",
            true,
            "SchemaSelector",
            items,
            selectedDB == null ? null : Collections.singletonList(selectedDB));
        dialog.setModeless(true);
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return;
        }
        DBNDatabaseNode node = dialog.getSelectedObject();
        if (node != null) {
            databaseCombo.select(node);
        }
        changeDataBaseSelection(node);
    }

    @Override
    public void nodeChanged(DBNEvent event) {
        if (activeFile == null) {
            return;
        }
        final DBNNode node = event.getNode();
        if (node instanceof DBNResource && activeFile.equals(((DBNResource) node).getResource())) {
            final int selConnection = connectionCombo.getSelectionIndex();
            if (selConnection > 0 && activeFile != null) {
                DBPDataSourceContainer visibleContainer = connectionCombo.getItem(selConnection);
                DBPDataSourceContainer newContainer = EditorUtils.getFileDataSource(activeFile);
                if (newContainer != visibleContainer) {
                    updateControls(true);
                }
            }
        }
    }

    public static class ToolbarContribution extends WorkbenchWindowControlContribution {
        public ToolbarContribution() {
            super(IActionConstants.TOOLBAR_DATASOURCE);
        }

        @Override
        protected Control createControl(Composite parent) {
            DataSourceManagementToolbar toolbar = new DataSourceManagementToolbar(DBeaverUI.getActiveWorkbenchWindow());
            return toolbar.createControl(parent);
        }
    }

    private static class ConnectionLabelProvider extends LabelProvider implements IColorProvider {
        @Override
        public Image getImage(Object element) {
            if (element == null) {
                return DBeaverIcons.getImage(DBIcon.TREE_DATABASE);
            }
            DBNModel nm = DBeaverCore.getInstance().getNavigatorModel();
            nm.ensureProjectLoaded(((DBPDataSourceContainer) element).getRegistry().getProject());
            final DBNDatabaseNode node = nm.findNode((DBPDataSourceContainer) element);
            return node == null ? null : DBeaverIcons.getImage(node.getNodeIcon());
        }

        @Override
        public String getText(Object element) {
            if (element == null) {
                return EMPTY_SELECTION_TEXT;
            }
            return ((DBPDataSourceContainer) element).getName();
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            return element == null ? null : UIUtils.getConnectionColor(((DBPDataSourceContainer) element).getConnectionConfiguration());
        }
    }

    private static class DatabaseLabelProvider extends LabelProvider implements IColorProvider {
        @Override
        public Image getImage(Object element) {
            if (element == null) {
                return DBeaverIcons.getImage(DBIcon.TREE_DATABASE);
            }
            return DBeaverIcons.getImage(((DBNDatabaseNode)element).getNodeIconDefault());
        }

        @Override
        public String getText(Object element) {
            if (element == null) {
                return EMPTY_SELECTION_TEXT;
            }
            return ((DBNDatabaseNode)element).getNodeName();
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            if (element instanceof DBNDatabaseNode) {
                final DBPDataSourceContainer container = ((DBNDatabaseNode) element).getDataSourceContainer();
                if (container != null) {
                    return UIUtils.getConnectionColor((container.getConnectionConfiguration()));
                }
            }
            return null;
        }
    }

}
