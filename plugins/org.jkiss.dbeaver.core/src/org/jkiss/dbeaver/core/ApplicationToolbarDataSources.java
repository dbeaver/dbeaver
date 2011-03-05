/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceContainerProviderEx;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * DataSource Toolbar
 */
class ApplicationToolbarDataSources implements DBPEventListener, IPropertyChangeListener, IPageListener, IPartListener {
    static final Log log = LogFactory.getLog(ApplicationToolbarDataSources.class);

    public static final String EMPTY_SELECTION_TEXT = "<None>";

    private IWorkbenchWindow workbenchWindow;
    private IEditorPart activeEditorPart;

    private Text resultSetSize;
    private CImageCombo connectionCombo;
    private CImageCombo databaseCombo;

    private class CurrentDatabasesInfo {
        Collection<? extends DBSObject> list;
        DBSObject active;
    }

    public ApplicationToolbarDataSources(IWorkbenchWindow workbenchWindow)
    {
        this.workbenchWindow = workbenchWindow;
        this.workbenchWindow.addPageListener(this);
        //workbench.addWindowListener();
    }

    public void dispose()
    {
        setActiveEditor(null);

        UIUtils.dispose(resultSetSize);
        UIUtils.dispose(connectionCombo);
        UIUtils.dispose(databaseCombo);

        this.workbenchWindow.removePageListener(this);
    }

    private IProject getActiveProject()
    {
        if (activeEditorPart == null || activeEditorPart.getEditorInput() == null) {
            return null;
        }
        final IEditorInput editorInput = activeEditorPart.getEditorInput();
        final IFile file = (IFile) editorInput.getAdapter(IFile.class);
        if (file != null) {
            return file.getProject();
        } else {
            return null;
        }
    }

    private DBSDataSourceContainer getDataSourceContainer()
    {
        if (activeEditorPart == null || activeEditorPart.getEditorInput() == null) {
            return null;
        }
        final IEditorInput editorInput = activeEditorPart.getEditorInput();
        if (editorInput instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider)editorInput).getDataSourceContainer();
        } else if (editorInput instanceof IDataSourceProvider) {
            return ((IDataSourceProvider)editorInput).getDataSource().getContainer();
        } else {
            return null;
        }
    }

    private IDataSourceContainerProviderEx getActiveDataSourceUpdater()
    {
        if (activeEditorPart instanceof IDataSourceContainerProviderEx) {
            return (IDataSourceContainerProviderEx) activeEditorPart;
        } else {
            final IEditorInput editorInput = activeEditorPart == null ? null : activeEditorPart.getEditorInput();
            return editorInput instanceof IDataSourceContainerProviderEx ? (IDataSourceContainerProviderEx)editorInput : null;
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
                return DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project).getDataSources();
            }
        }
        return null;
    }

    public void setActiveEditor(IEditorPart targetEditor)
    {
        if (activeEditorPart == targetEditor) {
            return;
        }

        // Update previous statuses
        DBSDataSourceContainer container = getDataSourceContainer();
        if (container != null) {
            container.getPreferenceStore().removePropertyChangeListener(this);
            container.getRegistry().removeDataSourceListener(this);
        }
        activeEditorPart = targetEditor;
        container = getDataSourceContainer();

        if (container != null) {
            // Update editor actions
            container.getPreferenceStore().addPropertyChangeListener(this);
            container.getRegistry().addDataSourceListener(this);
        }

        // Update controls and actions
        updateControls();
    }

    public void fillToolBar(IToolBarManager manager)
    {
        manager.add(new ControlContribution("ResultSet Size") {
            protected Control createControl(Composite parent)
            {
                Composite editGroup = new Composite(parent, SWT.NONE);
                GridLayout gl = new GridLayout(1, true);
                gl.marginWidth = 5;
                gl.marginHeight = 0;
                editGroup.setLayout(gl);

                resultSetSize = new Text(editGroup, SWT.BORDER);
                resultSetSize.setTextLimit(10);
                GridData gd = new GridData();
                gd.widthHint = 30;

                resultSetSize.setToolTipText("Maximum result-set size");
                final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
                if (dataSourceContainer != null) {
                    resultSetSize.setText(String.valueOf(dataSourceContainer.getPreferenceStore().getInt(PrefConstants.RESULT_SET_MAX_ROWS)));
                }
                //resultSetSize.setDigits(7);
                resultSetSize.setLayoutData(gd);
                resultSetSize.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
                resultSetSize.addFocusListener(new FocusListener() {
                    public void focusGained(FocusEvent e)
                    {
                    }

                    public void focusLost(FocusEvent e)
                    {
                        changeResultSetSize();
                    }
                });
                return editGroup;
            }
        });

        // Connection related actions
        manager.add(new Separator());
        manager.add(new ControlContribution("DataSources")
        {
            protected Control createControl(Composite parent)
            {
                Composite comboGroup = new Composite(parent, SWT.NONE);
                GridLayout gl = new GridLayout(1, true);
                gl.marginWidth = 5;
                gl.marginHeight = 0;
                comboGroup.setLayout(gl);

                connectionCombo = new CImageCombo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
                GridData gd = new GridData();
                gd.widthHint = 120;
                connectionCombo.setLayoutData(gd);
                connectionCombo.setVisibleItemCount(15);
                connectionCombo.setToolTipText("Active datasource");
                fillDataSourceList();
                connectionCombo.addSelectionListener(new SelectionListener()
                {
                    public void widgetSelected(SelectionEvent e)
                    {
                        changeDataSourceSelection();
                    }

                    public void widgetDefaultSelected(SelectionEvent e)
                    {
                        widgetSelected(e);
                    }
                });
                return comboGroup;
            }
        });
        manager.add(new ControlContribution("DataBases") {
            protected Control createControl(Composite parent)
            {
                Composite comboGroup = new Composite(parent, SWT.NONE);
                GridLayout gl = new GridLayout(1, true);
                gl.marginWidth = 5;
                gl.marginHeight = 0;
                comboGroup.setLayout(gl);

                databaseCombo = new CImageCombo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
                GridData gd = new GridData();
                gd.widthHint = 120;
                databaseCombo.setLayoutData(gd);
                databaseCombo.setVisibleItemCount(15);
                databaseCombo.setToolTipText("Active database");
                databaseCombo.add(DBIcon.TREE_CATALOG.getImage(), EMPTY_SELECTION_TEXT);
                databaseCombo.select(0);
                databaseCombo.addSelectionListener(new SelectionListener() {
                    public void widgetSelected(SelectionEvent e)
                    {
                        changeDataBaseSelection();
                    }

                    public void widgetDefaultSelected(SelectionEvent e)
                    {
                        widgetSelected(e);
                    }
                });
                updateDatabaseList();
                return comboGroup;
            }
        });
    }

    private void fillDataSourceList() {
        connectionCombo.setRedraw(false);
        try {
            connectionCombo.removeAll();

            connectionCombo.add(DBIcon.TREE_DATABASE.getImage(), EMPTY_SELECTION_TEXT);
            int selectionIndex = 0;
            if (activeEditorPart != null) {
                final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
                List<? extends DBSDataSourceContainer> dataSources = getAvailableDataSources();
                if (!CommonUtils.isEmpty(dataSources)) {
                    DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                    for (int i = 0; i < dataSources.size(); i++) {
                        DBSDataSourceContainer ds = dataSources.get(i);
                        DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(ds);
                        connectionCombo.add(
                            dsNode == null ? DBIcon.TREE_DATABASE.getImage() : dsNode.getNodeIconDefault(),
                            ds.getName());
                        if (dataSourceContainer == ds) {
                            selectionIndex = i + 1;
                        }
                    }
                }
            }
            connectionCombo.select(selectionIndex);
        } finally {
            connectionCombo.setRedraw(true);
        }
    }

    public void handleDataSourceEvent(DBPEvent event)
    {
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            if (event.getObject() == dataSourceContainer &&
                event.getAction() == DBPEvent.Action.OBJECT_UPDATE &&
                event.getEnabled() != null)
            {
                Display.getDefault().asyncExec(
                    new Runnable() {
                        public void run()
                        {
                            updateControls();
                        }
                    }
                );
            }
        }
    }

    private void updateControls()
    {
        final DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();

        // Update resultset max size
        if (resultSetSize != null && !resultSetSize.isDisposed()) {
            if (dataSourceContainer == null) {
                resultSetSize.setEnabled(false);
            } else {
                resultSetSize.setEnabled(true);
                resultSetSize.setText(String.valueOf(dataSourceContainer.getPreferenceStore().getInt(PrefConstants.RESULT_SET_MAX_ROWS)));
            }
        }

        // Update datasources combo
        updateDataSourceList();
        updateDatabaseList();
    }

    private void changeResultSetSize()
    {
        DBSDataSourceContainer dsContainer = getDataSourceContainer();
        if (dsContainer != null) {
            String rsSize = resultSetSize.getText();
            if (rsSize.length() == 0) {
                rsSize = "1";
            }
            IPreferenceStore store = dsContainer.getPreferenceStore();
            store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, rsSize);
            RuntimeUtils.savePreferenceStore(store);
        }
    }

    private void updateDataSourceList()
    {
        if (connectionCombo != null && !connectionCombo.isDisposed()) {
            IDataSourceContainerProviderEx containerProvider = getActiveDataSourceUpdater();
            if (containerProvider == null) {
                connectionCombo.setEnabled(false);
            } else {
                connectionCombo.setEnabled(true);
            }
            fillDataSourceList();
        }
    }

    private void updateDatabaseList()
    {
        // Update databases combo
        DBeaverCore.runUIJob("Populate current database list", new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                fillDatabaseCombo(monitor);
            }
        });
    }

    private void fillDatabaseCombo(DBRProgressMonitor monitor)
    {
        if (databaseCombo != null && !databaseCombo.isDisposed()) {
            databaseCombo.setRedraw(false);
            try {
                databaseCombo.removeAll();
                boolean isEnabled = false;
                DBSDataSourceContainer dsContainer = getDataSourceContainer();
                if (dsContainer != null && dsContainer.isConnected()) {
                    final DBPDataSource dataSource = dsContainer.getDataSource();

                    if (dataSource instanceof DBSEntityContainer &&
                        dataSource instanceof DBSEntitySelector &&
                        ((DBSEntitySelector)dataSource).supportsActiveChildChange())
                    {
                        DBSEntityContainer entityContainer = (DBSEntityContainer) dataSource;
                        final CurrentDatabasesInfo databasesInfo = new CurrentDatabasesInfo();
                        try {
                            Class<? extends DBSEntity> childType = entityContainer.getChildType(monitor);
                            if (childType == null || !DBSEntityContainer.class.isAssignableFrom(childType)) {
                                isEnabled = false;
                            } else {
                                isEnabled = true;
                                databasesInfo.list = entityContainer.getChildren(monitor);
                                databasesInfo.active = ((DBSEntitySelector)dataSource).getActiveChild(monitor);
                            }
                        }
                        catch (DBException e) {
                            log.error(e);
                        }
                        if (isEnabled) {
                            if (databasesInfo.list != null && !databasesInfo.list.isEmpty()) {
                                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                                for (DBSObject database : databasesInfo.list) {
                                    if (database instanceof DBSEntityContainer) {
                                        DBNDatabaseNode dbNode = navigatorModel.getNodeByObject(monitor, database, true);
                                        databaseCombo.add(dbNode == null ? DBIcon.TREE_CATALOG.getImage() : dbNode.getNodeIconDefault(), database.getName());
                                        databaseCombo.setData(database.getName(), database);
                                    }
                                }
                            }
                            if (databasesInfo.active != null) {
                                int dbCount = databaseCombo.getItemCount();
                                for (int i = 0; i < dbCount; i++) {
                                    String dbName = databaseCombo.getItem(i);
                                    if (dbName.equals(databasesInfo.active.getName())) {
                                        databaseCombo.select(i);
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        databaseCombo.add(DBIcon.TREE_CATALOG.getImage(), dsContainer.getConnectionInfo().getDatabaseName());
                        databaseCombo.select(0);
                    }
                }
                if (databaseCombo.getItemCount() == 0) {
                    databaseCombo.add(DBIcon.TREE_CATALOG.getImage(), EMPTY_SELECTION_TEXT);
                    databaseCombo.select(0);
                }
                databaseCombo.setEnabled(isEnabled);
            }
            finally {
                databaseCombo.setRedraw(true);
            }
        }
    }

    private void changeDataSourceSelection()
    {
        if (connectionCombo == null || connectionCombo.isDisposed()) {
            return;
        }
        if (activeEditorPart == null || activeEditorPart.getEditorInput() == null) {
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
                log.warn("Connection combo index out of bounds (" + curIndex + ")");
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
        updateControls();
    }

    private void changeDataBaseSelection()
    {
        if (databaseCombo == null || databaseCombo.isDisposed() || databaseCombo.getSelectionIndex() < 0) {
            return;
        }
        DBSDataSourceContainer dsContainer = getDataSourceContainer();
        final String newName = databaseCombo.getItem(databaseCombo.getSelectionIndex());
        if (dsContainer != null && dsContainer.isConnected()) {
            final DBPDataSource dataSource = dsContainer.getDataSource();
            try {
                if (dataSource instanceof DBSEntityContainer &&
                    dataSource instanceof DBSEntitySelector &&
                    ((DBSEntitySelector)dataSource).supportsActiveChildChange())
                {
                    DBeaverCore.getInstance().runAndWait(new DBRRunnableWithProgress() {
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException
                        {
                            try {
                                DBSObject newChild = ((DBSEntityContainer) dataSource).getChild(monitor, newName);
                                if (newChild != null) {
                                    ((DBSEntitySelector)dataSource).setActiveChild(monitor, newChild);
                                } else {
                                    throw new DBException("Could not find database '" + newName + "'");
                                }
                            }
                            catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                } else {
                    throw new DBException("Active database change is not supported");
                }
            } catch (DBException e) {
                log.error("Error changing active database" + e.getMessage());
            }
        }
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(PrefConstants.RESULT_SET_MAX_ROWS) && !resultSetSize.isDisposed()) {
            if (event.getNewValue() != null) {
                resultSetSize.setText(event.getNewValue().toString());
            }
        }
    }

    // IPageListener

    public void pageActivated(IWorkbenchPage page)
    {
        // do nothing
    }

    public void pageClosed(IWorkbenchPage page)
    {
        page.removePartListener(this);
    }

    public void pageOpened(IWorkbenchPage page)
    {
        page.addPartListener(this);
    }

    // IPartListener

    public void partActivated(IWorkbenchPart part)
    {
        if (part instanceof IEditorPart) {
            setActiveEditor((IEditorPart) part);
        }
    }

    public void partBroughtToTop(IWorkbenchPart part)
    {
    }

    public void partClosed(IWorkbenchPart part)
    {
    }

    public void partDeactivated(IWorkbenchPart part)
    {
        if (part == activeEditorPart) {
            setActiveEditor(null);
        }
    }

    public void partOpened(IWorkbenchPart part)
    {
    }

}
