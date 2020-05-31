/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL task settings page
 */
class SQLToolTaskPageSettings extends ActiveWizardPage<SQLToolTaskConfigurationWizard> implements DBPContextProvider {

    private static final Log log = Log.getLog(SQLToolTaskPageSettings.class);

    private SQLToolTaskConfigurationWizard sqlWizard;

    private List<DBSObject> selectedObjects = new ArrayList<>();
    private PropertyTreeViewer taskOptionsViewer;
    private Object sqlPreviewPanel;
    private TableViewer objectsViewer;
    private UIServiceSQL serviceSQL;

    SQLToolTaskPageSettings(SQLToolTaskConfigurationWizard wizard) {
        super(wizard.getTaskType().getName() + " parameters");
        setTitle(wizard.getTaskType().getName() + " parameters");
        setDescription("Parameters for " + wizard.getTaskType().getName());
        this.sqlWizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm previewSplitter = new SashForm(composite, SWT.VERTICAL);
        previewSplitter.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm settingsPanel = new SashForm(previewSplitter, SWT.HORIZONTAL);

        {
            Group objectsPanel = UIUtils.createControlGroup(settingsPanel, "Objects", 2, GridData.FILL_BOTH, 0);
            objectsViewer = new TableViewer(objectsPanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
            objectsViewer.setContentProvider(new ListContentProvider());
            objectsViewer.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return DBUtils.getObjectFullName((DBPNamedObject) element, DBPEvaluationContext.UI);
                }
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(DBValueFormatting.getObjectImage((DBPObject) element));
                }

            });

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 150;
            gd.widthHint = 200;
            final Table objectTable = objectsViewer.getTable();
            objectTable.setLayoutData(gd);

            ToolBar buttonsToolbar = new ToolBar(objectsPanel, SWT.VERTICAL);
            buttonsToolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createToolItem(buttonsToolbar, "Add script", UIIcon.ROW_ADD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(sqlWizard.getProject());
                    SQLToolTaskObjectSelectorDialog objectListDialog = new SQLToolTaskObjectSelectorDialog(
                        getShell(),
                        projectNode,
                        (TaskTypeDescriptor) sqlWizard.getTaskType());
                    if (objectListDialog.open() == IDialogConstants.OK_ID) {
                        for (DBSObject object : objectListDialog.getSelectedObjects()) {
                            if (!selectedObjects.contains(object)) {
                                selectedObjects.add(object);
                            }
                        }
                        refreshObjects();
                        updatePageCompletion();
                    }
                }
            });
            ToolItem deleteItem = UIUtils.createToolItem(buttonsToolbar, "Remove script", UIIcon.ROW_DELETE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ISelection selection = objectsViewer.getSelection();
                    if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
                        for (Object element : ((IStructuredSelection) selection).toArray()) {
                            if (element instanceof DBSObject) {
                                selectedObjects.remove(element);
                            }
                        }
                        refreshObjects();
                        updatePageCompletion();
                    }
                }
            });
            UIUtils.createToolBarSeparator(buttonsToolbar, SWT.HORIZONTAL);
            ToolItem moveUpItem = UIUtils.createToolItem(buttonsToolbar, "Move script up", UIIcon.ARROW_UP, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = objectTable.getSelectionIndex();
                    if (selectionIndex > 0) {
                        DBSObject prevScript = selectedObjects.get(selectionIndex - 1);
                        selectedObjects.set(selectionIndex - 1, selectedObjects.get(selectionIndex));
                        selectedObjects.set(selectionIndex, prevScript);
                        refreshObjects();
                    }
                }
            });
            ToolItem moveDownItem = UIUtils.createToolItem(buttonsToolbar, "Move script down", UIIcon.ARROW_DOWN, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = objectTable.getSelectionIndex();
                    if (selectionIndex < objectTable.getItemCount() - 1) {
                        DBSObject nextScript = selectedObjects.get(selectionIndex + 1);
                        selectedObjects.set(selectionIndex + 1, selectedObjects.get(selectionIndex));
                        selectedObjects.set(selectionIndex, nextScript);
                        refreshObjects();
                    }
                }
            });
            objectsViewer.addSelectionChangedListener(event -> {
                int selectionIndex = objectTable.getSelectionIndex();
                deleteItem.setEnabled(selectionIndex >= 0);
                moveUpItem.setEnabled(selectionIndex > 0);
                moveDownItem.setEnabled(selectionIndex < objectTable.getItemCount() - 1);
            });
            deleteItem.setEnabled(false);
        }

        {
            Group optionsPanel = UIUtils.createControlGroup(settingsPanel, "Settings", 1, GridData.FILL_BOTH, 0);

            taskOptionsViewer = new PropertyTreeViewer(optionsPanel, SWT.BORDER);
            taskOptionsViewer.addPropertyChangeListener(event -> updateScriptPreview());
        }

        Composite previewPanel = UIUtils.createComposite(previewSplitter, 1);
        previewPanel.setLayout(new FillLayout());
        serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            try {
                sqlPreviewPanel = serviceSQL.createSQLPanel(
                    UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                    previewPanel,
                    this,
                    "SQL Preview",
                    true,
                    "");
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("SQL preview error", "Can't create SQL preview panel", e);
            }
        }

        getWizard().createTaskSaveButtons(composite, true, 1);

        loadSettings();

        setControl(composite);
    }

    private void refreshObjects() {
        objectsViewer.refresh(true, true);
        saveSettings();
        updateScriptPreview();
    }

    @Override
    public void activatePage() {
        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
    }

    @Override
    protected boolean determinePageCompletion() {
        if (selectedObjects.isEmpty()) {
            setErrorMessage("You must select object(s)");
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    private void updateScriptPreview() {
        String sqlText = generateScriptText();
        if (serviceSQL != null) {
            serviceSQL.setSQLPanelText(sqlPreviewPanel, sqlText);
        }
    }

    private String generateScriptText() {
        SQLToolExecuteHandler taskHandler = sqlWizard.getTaskHandler();
        try {
            return taskHandler.generateScript(new LoggingProgressMonitor(), sqlWizard.getSettings());
        } catch (DBCException e) {
            log.error(e);
            return "-- Error: " + e.getMessage();
        }
    }

    private void loadSettings() {
        {
            // Load objects
            selectedObjects.clear();
            SQLToolExecuteSettings<DBSObject> settings = sqlWizard.getSettings();
            selectedObjects.addAll(settings.getObjectList());
            objectsViewer.setInput(selectedObjects);
        }
        {
            // Load options
            PropertySourceEditable propertyCollector = new PropertySourceEditable(sqlWizard.getSettings(), sqlWizard.getSettings());
            propertyCollector.collectProperties();
            taskOptionsViewer.loadProperties(propertyCollector);
            taskOptionsViewer.repackColumns();
        }

        updateScriptPreview();
    }

    void saveSettings() {
        if (sqlWizard == null) {
            return;
        }
        SQLToolExecuteSettings<DBSObject> settings = sqlWizard.getSettings();

        settings.setObjectList(selectedObjects);
        taskOptionsViewer.saveEditorValues();
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        if (!selectedObjects.isEmpty()) {
            DBUtils.getDefaultContext(selectedObjects.get(0), false);
        }
        return null;
    }

}
