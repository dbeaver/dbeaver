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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
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
    private Table objectsTable;
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

        Group objectsPanel = UIUtils.createControlGroup(settingsPanel, "Objects", 1, GridData.FILL_BOTH, 0);
        objectsTable = new Table(objectsPanel, SWT.BORDER);
        objectsTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group optionsPanel = UIUtils.createControlGroup(settingsPanel, "Settings", 1, GridData.FILL_BOTH, 0);

        taskOptionsViewer = new PropertyTreeViewer(optionsPanel, SWT.BORDER);

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
        List<String> scriptLines = new ArrayList<>();
        SQLToolExecuteHandler taskHandler = sqlWizard.getTaskHandler();
        try {
            taskHandler.generateScript(new LoggingProgressMonitor(), sqlWizard.getSettings());
        } catch (DBCException e) {
            log.error(e);
        }

        return String.join(";\n", scriptLines);
    }

    public void loadSettings() {
        {
            // Load objects
            objectsTable.removeAll();
            selectedObjects.clear();
            SQLToolExecuteSettings<? extends DBSObject> settings = sqlWizard.getSettings();
            for (DBSObject object : settings.getObjectList()) {
                TableItem item = new TableItem(objectsTable, SWT.LEFT);
                item.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(object)));
                item.setText(DBUtils.getObjectFullName(object, DBPEvaluationContext.UI));
                item.setData(object);
                selectedObjects.add(object);
            }
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

    public void saveSettings() {
        if (sqlWizard == null) {
            return;
        }
/*
        SQLScriptExecuteSettings settings = sqlWizard.getSettings();

        List<String> scriptPaths = new ArrayList<>();
        for (DBNResource resource : selectedScripts) {
            IResource res = resource.getResource();
            if (res instanceof IFile) {
                scriptPaths.add(res.getFullPath().toString());
            }
        }
        settings.setScriptFiles(scriptPaths);
        List<DBPDataSourceContainer> dsList = new ArrayList<>();
        for (DBNDataSource dsNode : selectedDataSources) {
            dsList.add(dsNode.getDataSourceContainer());
        }
        settings.setDataSources(dsList);

        settings.setIgnoreErrors(ignoreErrorsCheck.getSelection());
        settings.setDumpQueryResultsToLog(dumpQueryCheck.getSelection());
        settings.setAutoCommit(autoCommitCheck.getSelection());
*/
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return null;
    }
}