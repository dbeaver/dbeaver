/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.preferences.PrefPageSQLEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * SQL Editor contributor
 */
public class SQLEditorContributor extends BasicTextEditorActionContributor implements DBPEventListener, IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(SQLEditorContributor.class);

    public static final String EMPTY_SELECTION_TEXT = "<None>";

    private SQLEditor activeEditorPart;

    private Text resultSetSize;
    private CImageCombo connectionCombo;
    private CImageCombo databaseCombo;
    //private MenuContributionItem txnMenu;

    private RetargetTextEditorAction contentAssistProposal;
    private RetargetTextEditorAction contentAssistTip;
    private RetargetTextEditorAction contentFormatProposal;

    public SQLEditorContributor()
    {
        super();

        createActions();
    }

    private void createActions()
    {
        // Init custom actions
        ResourceBundle bundle = DBeaverCore.getInstance().getPlugin().getResourceBundle();
        contentAssistProposal = new RetargetTextEditorAction(bundle, "ContentAssistProposal.");
        contentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        contentFormatProposal = new RetargetTextEditorAction(bundle, "ContentFormatProposal.");
        contentFormatProposal.setActionDefinitionId(ICommandIds.CMD_CONTENT_FORMAT);
        contentAssistTip = new RetargetTextEditorAction(bundle, "ContentAssistTip.");
        contentAssistTip.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
    }

    public void dispose()
    {
        setActiveEditor(null);

        UIUtils.dispose(resultSetSize);
        UIUtils.dispose(connectionCombo);
        UIUtils.dispose(databaseCombo);

        if (activeEditorPart != null) {
            activeEditorPart.getDataSourceContainer().getRegistry().removeDataSourceListener(this);
            activeEditorPart = null;
        }
        super.dispose();
    }

    SQLEditor getEditor()
    {
        return activeEditorPart;
    }

    public void setActiveEditor(IEditorPart targetEditor)
    {
        super.setActiveEditor(targetEditor);

        if (activeEditorPart == targetEditor) {
            return;
        }
        // Update previous statuses
        if (activeEditorPart != null) {
            final DBSDataSourceContainer dataSourceContainer = activeEditorPart.getDataSourceContainer();
            if (dataSourceContainer != null) {
                activeEditorPart.getDataSourceContainer().getPreferenceStore().removePropertyChangeListener(this);
                activeEditorPart.getDataSourceContainer().getRegistry().removeDataSourceListener(this);
            }
        }
        activeEditorPart = (SQLEditor)targetEditor;

        if (activeEditorPart != null) {
            // Update editor actions
            contentAssistProposal.setAction(getAction(activeEditorPart, SQLEditor.ACTION_CONTENT_ASSIST_PROPOSAL)); //$NON-NLS-1$
            contentAssistTip.setAction(getAction(activeEditorPart, SQLEditor.ACTION_CONTENT_ASSIST_TIP)); //$NON-NLS-1$
            contentFormatProposal.setAction(getAction(activeEditorPart, SQLEditor.ACTION_CONTENT_FORMAT_PROPOSAL)); //$NON-NLS-1$

            final DBSDataSourceContainer dataSourceContainer = activeEditorPart.getDataSourceContainer();
            if (dataSourceContainer != null) {
                dataSourceContainer.getPreferenceStore().addPropertyChangeListener(this);
                dataSourceContainer.getRegistry().addDataSourceListener(this);
            }
        }

        // Update controls and actions
        updateControls();
    }

    public void init(IActionBars bars)
    {
        super.init(bars);
    }

    public void contributeToMenu(IMenuManager manager)
    {
        super.contributeToMenu(manager);

        IMenuManager editMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            //editMenu.add(new Separator());
            editMenu.add(contentAssistProposal);
            editMenu.add(contentAssistTip);
            editMenu.add(contentFormatProposal);
            //editMenu.add(new Separator());
            //editMenu.add(executeStatementAction);
            //editMenu.add(executeScriptAction);
        }
    }

    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);
        manager.add(new ControlContribution("ResultSet Size")
        {
            protected Control createControl(Composite parent)
            {
                SQLEditor editor = getEditor();

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
                if (editor != null) {
                    DBSDataSourceContainer curDataSource = editor.getDataSourceContainer();
                    if (curDataSource != null) {
                        resultSetSize.setText("" + curDataSource.getPreferenceStore().getInt(PrefConstants.RESULT_SET_MAX_ROWS));
                    }
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
                SQLEditor editor = getEditor();

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
                fillDataSourceList(editor);
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
        manager.add(new ControlContribution("DataBases")
        {
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
                updateDatabaseList(getEditor());
                return comboGroup;
            }
        });
    }

    private void fillDataSourceList(SQLEditor editor) {
        connectionCombo.setRedraw(false);
        try {
            connectionCombo.removeAll();

            connectionCombo.add(DBIcon.TREE_DATABASE.getImage(), EMPTY_SELECTION_TEXT);
            int selectionIndex = 0;
            if (activeEditorPart != null) {
                final DBSDataSourceContainer dataSourceContainer = activeEditorPart.getDataSourceContainer();
                List<? extends DBSDataSourceContainer> dataSources;
                if (dataSourceContainer != null) {
                    dataSources = dataSourceContainer.getRegistry().getDataSources();
                } else {
                    dataSources = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(activeEditorPart.getEditorInput().getProject()).getDataSources();
                }
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                for (int i = 0; i < dataSources.size(); i++) {
                    DBSDataSourceContainer ds = dataSources.get(i);
                    DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(ds);
                    connectionCombo.add(
                        dsNode == null ? DBIcon.TREE_DATABASE.getImage() : dsNode.getNodeIconDefault(),
                        ds.getName());
                    if (editor != null && editor.getDataSourceContainer() == ds) {
                        selectionIndex = i + 1;
                    }
                }
            }
            connectionCombo.select(selectionIndex);
        } finally {
            connectionCombo.setRedraw(true);
        }
    }

    public void contributeToCoolBar(ICoolBarManager manager)
    {
        super.contributeToCoolBar(manager);
    }

    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
    }

    public void handleDataSourceEvent(DBPEvent event)
    {
        SQLEditor editor = getEditor();
        if (editor != null) {
            if (event.getObject() == editor.getDataSourceContainer() &&
                event.getAction() == DBPEvent.Action.OBJECT_UPDATE &&
                event.getEnabled() != null)
            {
                editor.getSite().getShell().getDisplay().asyncExec(
                    new Runnable() {
                        public void run() {
                            updateControls();
                        }
                    }
                );
            }
        }
    }

    private void updateControls()
    {
        final SQLEditor editor = getEditor();

        // Update resultset max size
        if (resultSetSize != null && !resultSetSize.isDisposed()) {
            if (editor == null) {
                resultSetSize.setEnabled(false);
            } else {
                DBSDataSourceContainer curDataSource = editor.getDataSourceContainer();
                if (curDataSource != null) {
                    resultSetSize.setEnabled(true);
                    resultSetSize.setText("" + curDataSource.getPreferenceStore().getInt(PrefConstants.RESULT_SET_MAX_ROWS));
                }
            }
        }

        // Update datasources combo
        updateDataSourceList(editor);
        updateDatabaseList(editor);


    }

    private void changeResultSetSize()
    {
        SQLEditor editor = getEditor();
        if (editor != null) {
            DBSDataSourceContainer dsContainer = editor.getDataSourceContainer();
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
    }

    private void updateDataSourceList(SQLEditor editor)
    {
        if (connectionCombo != null && !connectionCombo.isDisposed()) {
            if (editor == null) {
                connectionCombo.setEnabled(false);
            } else {
                connectionCombo.setEnabled(true);
                fillDataSourceList(editor);
            }
        }
    }

    private void updateDatabaseList(final SQLEditor editor)
    {
        // Update databases combo
        DBeaverCore.runUIJob("Populate current database list", new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                fillDatabaseCombo(monitor, editor);
            }
        });
    }

    private class CurrentDatabasesInfo {
        Collection<? extends DBSObject> list;
        DBSObject active;
    }

    private void fillDatabaseCombo(DBRProgressMonitor monitor, SQLEditor editor)
    {
        if (databaseCombo != null && !databaseCombo.isDisposed()) {
            databaseCombo.setRedraw(false);
            try {
                databaseCombo.removeAll();
                boolean isEnabled = false;
                if (editor != null) {
                    DBSDataSourceContainer dsContainer = editor.getDataSourceContainer();
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
        if (activeEditorPart == null) {
            return;
        }
        DBSDataSourceContainer curDataSource = activeEditorPart.getDataSourceContainer();

        List<? extends DBSDataSourceContainer> dataSources;
        if (curDataSource != null) {
            dataSources = curDataSource.getRegistry().getDataSources();
        } else {
            dataSources = DBeaverCore.getInstance().getProjectRegistry()
                .getDataSourceRegistry(activeEditorPart.getEditorInput().getProject()).getDataSources();
        }
        int curIndex = connectionCombo.getSelectionIndex();
        if (curIndex == 0) {
            if (curDataSource == null) {
                // Nothing changed
                return;
            }
            activeEditorPart.setDataSourceContainer(null);
        } else if (curIndex > dataSources.size()) {
            log.warn("Connection combo index out of bounds (" + curIndex + ")");
            return;
        } else {
            // Change data source
            DBSDataSourceContainer selectedDataSource = dataSources.get(curIndex - 1);
            if (selectedDataSource == curDataSource) {
                return;
            } else {
                activeEditorPart.setDataSourceContainer(selectedDataSource);
            }
        }
        updateControls();
    }

    private void changeDataBaseSelection()
    {
        if (databaseCombo == null || databaseCombo.isDisposed() || databaseCombo.getSelectionIndex() < 0) {
            return;
        }
        SQLEditor editor = getEditor();
        if (editor == null) {
            return;
        }
        final String newName = databaseCombo.getItem(databaseCombo.getSelectionIndex());
        DBSDataSourceContainer dsContainer = editor.getDataSourceContainer();
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

    private Menu createScriptMenu(final Menu parent, final Shell shell, final SQLEditor editor)
    {
        final Menu menu = parent == null ? new Menu(shell, SWT.POP_UP) : new Menu(parent);
        MenuItem autoCommit = new MenuItem(menu, SWT.PUSH);
        autoCommit.setText("Settings ...");
        autoCommit.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                DBNNode dsNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(editor.getDataSourceContainer());
                if (dsNode instanceof IAdaptable) {
                    String pageId = PrefPageSQLEditor.PAGE_ID;
                    PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
                        shell,
                        (IAdaptable)dsNode,
                        pageId,
                        null,//new String[]{pageId},
                        null);
                    if (propDialog != null) {
                        propDialog.open();
                    }
                }
            }
        });
        return menu;
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(PrefConstants.RESULT_SET_MAX_ROWS) && !resultSetSize.isDisposed()) {
            if (event.getNewValue() != null) {
                resultSetSize.setText(event.getNewValue().toString());
            }
        }
    }



}
