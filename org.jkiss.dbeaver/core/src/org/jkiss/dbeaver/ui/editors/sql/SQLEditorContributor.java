/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.DBMEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.registry.event.IDataSourceListener;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.sql.*;
import org.jkiss.dbeaver.ui.controls.DefaultMenuCreator;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.preferences.PrefPageSQLEditor;

import java.util.*;
import java.util.List;
import java.lang.reflect.InvocationTargetException;

/**
 * SQL Editor contributor
 */
public class SQLEditorContributor extends TextEditorActionContributor implements IDataSourceListener, IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(SQLEditorContributor.class);

    private static class StatusFieldDef
    {
        private String category;
        private String actionId;
        private boolean visible;
        private int widthInChars;

        private StatusFieldDef(String category, String actionId, boolean visible, int widthInChars)
        {
            Assert.isNotNull(category);
            this.category = category;
            this.actionId = actionId;
            this.visible = visible;
            this.widthInChars = widthInChars;
        }
    }

    private final static StatusFieldDef[] STATUS_FIELD_DEFS = {
        new StatusFieldDef(ITextEditorActionConstants.STATUS_CATEGORY_FIND_FIELD, null, false, 30),
        new StatusFieldDef(ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE, null, true, 15),
        new StatusFieldDef(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_MODE, ITextEditorActionDefinitionIds.TOGGLE_OVERWRITE, true, 15),
        new StatusFieldDef(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION, ITextEditorActionConstants.GOTO_LINE, true, 15)
    };
    private Map<StatusFieldDef, StatusLineContributionItem> statusFields;

    private IEditorPart activeEditorPart;

    private RetargetTextEditorAction editorActionFindNext;
    private RetargetTextEditorAction editorActionFindPrevious;
    private RetargetTextEditorAction editorActionIncrementalFind;
    private RetargetTextEditorAction editorActionIncrementalFindReverse;
    private RetargetTextEditorAction editorActionGotoLine;

    private OpenSQLFileAction openFileAction;
    private ExecuteStatementAction executeStatementAction;
    private ExecuteScriptAction executeScriptAction;
    private ValidateStatementAction validateStatementAction;
    private ExplainPlanAction explainPlanAction;
    private AnalyseStatementAction analyseStatementAction;

    private Text resultSetSize;
    private Combo connectionCombo;
    private Combo databaseCombo;
    //private MenuContributionItem txnMenu;

    private RetargetTextEditorAction contentAssistProposal;
    private RetargetTextEditorAction contentAssistTip;
    private RetargetTextEditorAction contentFormatProposal;

    public SQLEditorContributor()
    {
        super();

        createActions();
        DataSourceRegistry.getDefault().addDataSourceListener(this);
    }

    private void createActions()
    {
        // Init staus line
        statusFields = new HashMap<StatusFieldDef, StatusLineContributionItem>(3);
        for (StatusFieldDef fieldDef : STATUS_FIELD_DEFS) {
            statusFields.put(
                fieldDef,
                new StatusLineContributionItem(
                    fieldDef.category,
                    fieldDef.visible,
                    fieldDef.widthInChars));
        }


        // Init standard actions
        ResourceBundle textEditorBundle = ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages");

        editorActionFindNext= new RetargetTextEditorAction(textEditorBundle, "Editor.FindNext."); //$NON-NLS-1$
        editorActionFindNext.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_NEXT);
        editorActionFindPrevious= new RetargetTextEditorAction(textEditorBundle, "Editor.FindPrevious."); //$NON-NLS-1$
        editorActionFindPrevious.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_PREVIOUS);
        editorActionIncrementalFind= new RetargetTextEditorAction(textEditorBundle, "Editor.FindIncremental."); //$NON-NLS-1$
        editorActionIncrementalFind.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_INCREMENTAL);
        editorActionIncrementalFindReverse= new RetargetTextEditorAction(textEditorBundle, "Editor.FindIncrementalReverse."); //$NON-NLS-1$
        editorActionIncrementalFindReverse.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_INCREMENTAL_REVERSE);
        editorActionGotoLine= new RetargetTextEditorAction(textEditorBundle, "Editor.GotoLine."); //$NON-NLS-1$
        editorActionGotoLine.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);


        // Init custom actions
        ResourceBundle bundle = DBeaverCore.getInstance().getPlugin().getResourceBundle();
        contentAssistProposal = new RetargetTextEditorAction(bundle, "ContentAssistProposal.");
        contentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        contentFormatProposal = new RetargetTextEditorAction(bundle, "ContentFormatProposal.");
        contentAssistTip = new RetargetTextEditorAction(bundle, "ContentAssistTip.");

        // open SQL file
        openFileAction = new OpenSQLFileAction() {
            protected SQLEditor getEditor()
            {
                return SQLEditorContributor.this.getEditor();
            }
        };
        // Execute statement
        executeStatementAction = new ExecuteStatementAction()
        {
            protected SQLEditor getEditor()
            {
                return SQLEditorContributor.this.getEditor();
            }
        };
        // Execute script
        executeScriptAction = new ExecuteScriptAction()
        {
            protected SQLEditor getEditor()
            {
                return SQLEditorContributor.this.getEditor();
            }
        };
        executeScriptAction.setMenuCreator(new DefaultMenuCreator()
        {
            public Menu getMenu(Menu parent)
            {
                return createScriptMenu(parent, parent.getShell(), getEditor());
            }

            public Menu getMenu(Control parent)
            {
                return createScriptMenu(null, parent.getShell(), getEditor());
            }
        });
        validateStatementAction = new ValidateStatementAction();
        explainPlanAction = new ExplainPlanAction();
        analyseStatementAction = new AnalyseStatementAction();
    }

    public void dispose()
    {
        setActiveEditor(null);

        if (resultSetSize != null) {
            resultSetSize.dispose();
        }
        if (connectionCombo != null) {
            connectionCombo.dispose();
        }
        if (databaseCombo != null) {
            databaseCombo.dispose();
        }

        DataSourceRegistry.getDefault().removeDataSourceListener(this);
        super.dispose();
    }

    SQLEditor getEditor()
    {
        if (activeEditorPart instanceof SQLEditor) {
            return ((SQLEditor) activeEditorPart);
        }
        return null;
    }

    public void setActiveEditor(IEditorPart targetEditor)
    {
        // Update previous statuses
        if (activeEditorPart instanceof ITextEditorExtension) {
            ITextEditorExtension extension= (ITextEditorExtension)activeEditorPart;
            for (StatusFieldDef STATUS_FIELD_DEF : STATUS_FIELD_DEFS) {
                extension.setStatusField(null, STATUS_FIELD_DEF.category);
            }
        }
        if (activeEditorPart instanceof SQLEditor) {
            ((SQLEditor)activeEditorPart).getDataSourceContainer().getPreferenceStore().removePropertyChangeListener(this);
        }
        activeEditorPart = targetEditor;

        // Update controls and actions
        updateControls();

        ITextEditor editor = (activeEditorPart instanceof ITextEditor) ? (ITextEditor)activeEditorPart : null;


        // Update editor actions
        editorActionFindNext.setAction(getAction(editor, ITextEditorActionConstants.FIND_NEXT));
        editorActionFindPrevious.setAction(getAction(editor, ITextEditorActionConstants.FIND_PREVIOUS));
        editorActionIncrementalFind.setAction(getAction(editor, ITextEditorActionConstants.FIND_INCREMENTAL));
        editorActionIncrementalFindReverse.setAction(getAction(editor, ITextEditorActionConstants.FIND_INCREMENTAL_REVERSE));
        editorActionGotoLine.setAction(getAction(editor, ITextEditorActionConstants.GOTO_LINE));

        contentAssistProposal.setAction(getAction(editor, SQLEditor.ACTION_CONTENT_ASSIST_PROPOSAL)); //$NON-NLS-1$
        contentAssistTip.setAction(getAction(editor, SQLEditor.ACTION_CONTENT_ASSIST_TIP)); //$NON-NLS-1$

        // Update status line
        if (activeEditorPart instanceof ITextEditorExtension) {
            for (StatusFieldDef STATUS_FIELD_DEF : STATUS_FIELD_DEFS) {
                StatusLineContributionItem statusField = statusFields.get(STATUS_FIELD_DEF);
                statusField.setActionHandler(getAction(editor, STATUS_FIELD_DEF.actionId));
                ITextEditorExtension extension = (ITextEditorExtension) activeEditorPart;
                extension.setStatusField(statusField, STATUS_FIELD_DEF.category);
            }
        }
        if (activeEditorPart instanceof SQLEditor) {
            ((SQLEditor)activeEditorPart).getDataSourceContainer().getPreferenceStore().addPropertyChangeListener(this);
        }
    }

    public void init(IActionBars bars)
    {
        super.init(bars);

        IActionBars actionBars = getActionBars();
        if (actionBars != null) {
/*

            ITextEditor editor = (activeEditorPart instanceof ITextEditor) ? (ITextEditor) activeEditorPart : null;

            actionBars.setGlobalActionHandler(
                ActionFactory.DELETE.getId(),
                getAction(editor, ITextEditorActionConstants.DELETE));
            actionBars.setGlobalActionHandler(
                ActionFactory.UNDO.getId(),
                getAction(editor, ITextEditorActionConstants.UNDO));
            actionBars.setGlobalActionHandler(
                ActionFactory.REDO.getId(),
                getAction(editor, ITextEditorActionConstants.REDO));
            actionBars.setGlobalActionHandler(
                ActionFactory.CUT.getId(),
                getAction(editor, ITextEditorActionConstants.CUT));
            actionBars.setGlobalActionHandler(
                ActionFactory.COPY.getId(),
                getAction(editor, ITextEditorActionConstants.COPY));
            actionBars.setGlobalActionHandler(
                ActionFactory.PASTE.getId(),
                getAction(editor, ITextEditorActionConstants.PASTE));
            actionBars.setGlobalActionHandler(
                ActionFactory.SELECT_ALL.getId(),
                getAction(editor, ITextEditorActionConstants.SELECT_ALL));
            actionBars.setGlobalActionHandler(
                ActionFactory.FIND.getId(),
                getAction(editor, ITextEditorActionConstants.FIND));
            actionBars.setGlobalActionHandler(
                IDEActionFactory.BOOKMARK.getId(),
                getAction(editor, IDEActionFactory.BOOKMARK.getId()));
*/
            actionBars.setGlobalActionHandler(
                ICommandIds.CMD_EXECUTE_STATEMENT,
                executeStatementAction);
            actionBars.setGlobalActionHandler(
                ICommandIds.CMD_EXECUTE_SCRIPT,
                executeScriptAction);

            actionBars.updateActionBars();

            IMenuManager menuManager = actionBars.getMenuManager();
            IMenuManager editMenu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
            if (editMenu != null) {
                editMenu.add(new Separator());
                editMenu.add(contentAssistProposal);
                editMenu.add(contentAssistTip);
                //editMenu.add(new Separator());
                //editMenu.add(executeStatementAction);
                //editMenu.add(executeScriptAction);
            }
        }
    }

    public void contributeToMenu(IMenuManager manager)
    {
        //super.contributeToMenu(manager);

        IMenuManager menu = new MenuManager("S&QL Editor");
        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
        menu.add(openFileAction);
        menu.add(new Separator());
        menu.add(executeStatementAction);
        menu.add(executeScriptAction);
        menu.add(validateStatementAction);
        menu.add(explainPlanAction);
        menu.add(analyseStatementAction);

        IMenuManager editMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            editMenu.prependToGroup(IWorkbenchActionConstants.FIND_EXT, editorActionIncrementalFindReverse);
            editMenu.prependToGroup(IWorkbenchActionConstants.FIND_EXT, editorActionIncrementalFind);
            editMenu.prependToGroup(IWorkbenchActionConstants.FIND_EXT, editorActionFindPrevious);
            editMenu.prependToGroup(IWorkbenchActionConstants.FIND_EXT, editorActionFindNext);

            editMenu.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, editorActionGotoLine);

            editMenu.add(new Separator());
            editMenu.add(contentAssistProposal);
            editMenu.add(contentFormatProposal);
            editMenu.add(contentAssistTip);

        }

    }

    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);
        // Execution
        manager.add(executeStatementAction);
        //manager.add(executeScriptAction);
        manager.add(executeScriptAction);

        manager.add(new Separator());
        manager.add(validateStatementAction);
        manager.add(explainPlanAction);
        manager.add(analyseStatementAction);
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
                    resultSetSize.setText("" + curDataSource.getPreferenceStore().getInt(PrefConstants.RESULT_SET_MAX_ROWS));
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

                connectionCombo = new Combo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd = new GridData();
                gd.widthHint = 100;
                connectionCombo.setLayoutData(gd);
                connectionCombo.setToolTipText("Active datasource");
                connectionCombo.add("<None>");
                List<DataSourceDescriptor> dataSources = DataSourceRegistry.getDefault().getDataSources();
                for (int i = 0; i < dataSources.size(); i++) {
                    DataSourceDescriptor ds = dataSources.get(i);
                    connectionCombo.add(ds.getName(), i + 1);
                    if (editor != null && editor.getDataSourceContainer() == ds) {
                        connectionCombo.select(i + 1);
                    }
                }
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

                databaseCombo = new Combo(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd = new GridData();
                gd.widthHint = 100;
                databaseCombo.setLayoutData(gd);
                databaseCombo.setToolTipText("Active database");
                databaseCombo.addSelectionListener(new SelectionListener()
                {
                    public void widgetSelected(SelectionEvent e)
                    {
                        changeDataBaseSelection();
                    }

                    public void widgetDefaultSelected(SelectionEvent e)
                    {
                        widgetSelected(e);
                    }
                });
                //fillDatabaseCombo(monitor, editor);
                return comboGroup;
            }
        });
    }

    public void contributeToCoolBar(ICoolBarManager manager)
    {
        super.contributeToCoolBar(manager);
    }

    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
        for (StatusFieldDef STATUS_FIELD_DEF : STATUS_FIELD_DEFS) {
            statusLineManager.add(statusFields.get(STATUS_FIELD_DEF));
        }
    }

    public void handleDataSourceEvent(DataSourceEvent event)
    {
        SQLEditor editor = getEditor();
        if (editor != null) {
            if (event.getDataSource() == editor.getDataSourceContainer()) {
                switch (event.getAction()) {
                    case CONNECT:
                    case DISCONNECT:
                        //boolean isConnected = event.getAction() == DataSourceEvent.Action.CONNECT;
                        //enableActions(isConnected);
                        editor.getSite().getShell().getDisplay().asyncExec(
                            new Runnable() {
                                public void run() {
                                    updateControls();
                                }
                            }
                        );
                        break;
                }
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
                resultSetSize.setEnabled(true);
                resultSetSize.setText("" + curDataSource.getPreferenceStore().getInt(PrefConstants.RESULT_SET_MAX_ROWS));
            }
        }

        // Update datasources combo
        updateDataSourceList(editor);

        // Update databases combo
        DBeaverCore.runUIJob("Populate current database list", new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                fillDatabaseCombo(monitor, editor);
            }
        });


        updateActions(editor);
    }

    private void updateActions(SQLEditor editor)
    {
        // Enable actions
        boolean isConnected = editor != null && editor.getDataSourceContainer() != null && editor.getDataSourceContainer().isConnected();
        executeStatementAction.setEnabled(isConnected);
        executeScriptAction.setEnabled(isConnected);
        validateStatementAction.setEnabled(false);
        explainPlanAction.setEnabled(false);
        analyseStatementAction.setEnabled(false);
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
                dsContainer.getPreferenceStore().setValue(PrefConstants.RESULT_SET_MAX_ROWS, rsSize);
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
                DBSDataSourceContainer curDataSource = editor.getDataSourceContainer();
                if (curDataSource == null) {
                    connectionCombo.select(0);
                } else {
                    String[] items = connectionCombo.getItems();
                    for (int i = 0; i < items.length; i++) {
                        String item = items[i];
                        if (item.equals(curDataSource.getName())) {
                            connectionCombo.select(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private class CurrentDatabasesInfo {
        Collection<? extends DBSObject> list;
        DBSObject active;
    }

    private void fillDatabaseCombo(DBRProgressMonitor monitor, SQLEditor editor)
    {
        if (databaseCombo != null && !databaseCombo.isDisposed()) {
            databaseCombo.removeAll();
            boolean isEnabled = false;
            if (editor != null) {
                DBSDataSourceContainer dsContainer = editor.getDataSourceContainer();
                if (dsContainer != null && dsContainer.isConnected()) {
                    final DBPDataSource dataSource = dsContainer.getDataSource();

                    if (dataSource instanceof DBSStructureContainer &&
                        dataSource instanceof DBSStructureContainerActive &&
                        ((DBSStructureContainerActive)dataSource).supportsActiveChildChange())
                    {
                        final CurrentDatabasesInfo databasesInfo = new CurrentDatabasesInfo();

                        try {
                            databasesInfo.list = ((DBSStructureContainer) dataSource).getChildren(
                                monitor);
                            databasesInfo.active = ((DBSStructureContainerActive)dataSource).getActiveChild(monitor);
                        }
                        catch (DBException e) {
                            log.error(e);
                        }

                        isEnabled = true;
                        if (databasesInfo.list != null && !databasesInfo.list.isEmpty()) {
                            for (DBSObject database : databasesInfo.list) {
                                databaseCombo.add(database.getName());
                                databaseCombo.setData(database.getName(), database);
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
                    } else {
                        databaseCombo.add(dsContainer.getConnectionInfo().getDatabaseName());
                        databaseCombo.select(0);
                    }
                }
            }
            databaseCombo.setEnabled(isEnabled);
        }
    }

    private void changeDataSourceSelection()
    {
        if (connectionCombo == null || connectionCombo.isDisposed()) {
            return;
        }
        SQLEditor editor = getEditor();
        if (editor == null) {
            return;
        }
        DBSDataSourceContainer curDataSource = editor.getDataSourceContainer();
        List<DataSourceDescriptor> dataSources = DataSourceRegistry.getDefault().getDataSources();
        int curIndex = connectionCombo.getSelectionIndex();
        if (curIndex == 0) {
            if (curDataSource == null) {
                // Nothing changed
                return;
            }
            editor.setDataSourceContainer(null);
        } else if (curIndex > dataSources.size()) {
            log.warn("Connection combo index out of bounds (" + curIndex + ")");
            return;
        } else {
            // Change data source
            DataSourceDescriptor selectedDataSource = dataSources.get(curIndex - 1);
            if (selectedDataSource == curDataSource) {
                return;
            } else {
                editor.setDataSourceContainer(selectedDataSource);
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
                if (dataSource instanceof DBSStructureContainer &&
                    dataSource instanceof DBSStructureContainerActive &&
                    ((DBSStructureContainerActive)dataSource).supportsActiveChildChange())
                {
                    DBeaverCore.getInstance().runAndWait(true, true, new DBRRunnableWithProgress() {
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException
                        {
                            try {
                                DBSObject newChild = ((DBSStructureContainer) dataSource).getChild(monitor, newName);
                                if (newChild != null) {
                                    ((DBSStructureContainerActive)dataSource).setActiveChild(monitor, newChild);
                                } else {
                                    throw new DBException("Can't find database '" + newName + "'");
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
                log.error("Error changing active databas" + e.getMessage());
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
                String pageId = PrefPageSQLEditor.PAGE_ID;
                PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
                    shell,
                    (DataSourceDescriptor) editor.getDataSourceContainer(),
                    pageId,
                    null,//new String[]{pageId},
                    null);
                if (propDialog != null) {
                    propDialog.open();
                }
            }
        });
        return menu;
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(PrefConstants.RESULT_SET_MAX_ROWS)) {
            resultSetSize.setText(event.getNewValue().toString());
        }
    }



}
