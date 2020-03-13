/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.debug.ui.internal;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanelContainer;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.dbeaver.ext.postgresql.debug.core.PostgreSqlDebugCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureParameter;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgreDebugPanelFunction implements DBGConfigurationPanel {

    private DBGConfigurationPanelContainer container;
    private Button kindLocal;
    private Button kindGlobal;
    private CSmartCombo<PostgreProcedure> functionCombo;
    private Text processIdText;

    private PostgreProcedure selectedFunction;
    private Map<DBSProcedureParameter, Object> parameterValues = new HashMap<>();
    private Table parametersTable;

    @Override
    public void createPanel(Composite parent, DBGConfigurationPanelContainer container) {
        this.container = container;

        {
            Group kindGroup = UIUtils.createControlGroup(parent, "Attach type", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, SWT.DEFAULT);

            SelectionListener listener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    processIdText.setEnabled(kindGlobal.getSelection());
                    parametersTable.setEnabled(kindLocal.getSelection());
                    container.updateDialogState();
                }
            };

            kindLocal = new Button(kindGroup, SWT.RADIO);
            kindLocal.setText("Local");
            kindLocal.addSelectionListener(listener);
            kindGlobal = new Button(kindGroup, SWT.RADIO);
            kindGlobal.setText("Global");
            kindGlobal.addSelectionListener(listener);
        }
        createFunctionGroup(parent);
        createParametersGroup(parent);
    }

    private void createFunctionGroup(Composite parent) {
        Group functionGroup = UIUtils.createControlGroup(parent, "Function", 2, GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
        UIUtils.createControlLabel(functionGroup, "Function");
        functionCombo = new CSmartSelector<PostgreProcedure>(functionGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return DBeaverIcons.getImage(DBIcon.TREE_PROCEDURE);
            }

            @Override
            public String getText(Object element) {
                if (element == null) {
                    return "N/A";
                }
                return ((PostgreProcedure)element).getFullQualifiedSignature();
            }
        }) {
            @Override
            protected void dropDown(boolean drop) {
                if (drop) {
                    DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                    DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(container.getDataSource());
                    if (dsNode != null) {
                        DBNNode curNode = selectedFunction == null ? null : navigatorModel.getNodeByObject(selectedFunction);
                        DBNNode node = DBWorkbench.getPlatformUI().selectObject(
                            parent.getShell(),
                            "Select function to debug",
                            dsNode,
                            curNode,
                            new Class[]{DBSInstance.class, DBSObjectContainer.class, PostgreProcedure.class},
                            new Class[]{PostgreProcedure.class}, null);
                        if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof PostgreProcedure) {
                            functionCombo.removeAll();
                            selectedFunction = (PostgreProcedure) ((DBNDatabaseNode) node).getObject();
                            functionCombo.addItem(selectedFunction);
                            functionCombo.select(selectedFunction);
                            updateParametersTable();
                            container.updateDialogState();
                        }
                        parametersTable.setEnabled(selectedFunction != null);
                    }
                }

            }
        };
        functionCombo.addItem(null);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(functionCombo) * 40 + 10;
        functionCombo.setLayoutData(gd);

        processIdText = UIUtils.createLabelText(functionGroup, "Process ID", "", SWT.BORDER, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(processIdText) * 10 + 10;
        processIdText.setLayoutData(gd);
    }

    private void createParametersGroup(Composite parent) {
        Group composite = UIUtils.createControlGroup(parent, "Function parameters", 2, GridData.FILL_BOTH, SWT.DEFAULT);

        parametersTable = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        parametersTable.setLayoutData(gd);
        parametersTable.setHeaderVisible(true);
        parametersTable.setLinesVisible(true);

        final TableColumn nameColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Name");
        nameColumn.setWidth(100);
        final TableColumn valueColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Value");
        valueColumn.setWidth(200);
        final TableColumn typeColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Type");
        typeColumn.setWidth(60);
        final TableColumn kindColumn = UIUtils.createTableColumn(parametersTable, SWT.LEFT, "Kind");
        kindColumn.setWidth(40);

        new CustomTableEditor(parametersTable) {
            {
                firstTraverseIndex = 1;
                lastTraverseIndex = 1;
                editOnEnter = false;
            }
            @Override
            protected Control createEditor(Table table, int index, TableItem item) {
                if (index != 1) {
                    return null;
                }
                DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                Text editor = new Text(table, SWT.BORDER);
                editor.setText(CommonUtils.toString(parameterValues.get(param), ""));
                editor.selectAll();
                return editor;
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                DBSProcedureParameter param = (DBSProcedureParameter) item.getData();
                String newValue = ((Text) control).getText();
                item.setText(1, newValue);

                parameterValues.put(param, newValue);
                container.updateDialogState();
            }
        };
    }

    @Override
    public void loadConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {
        Object kind = configuration.get(PostgreDebugConstants.ATTR_ATTACH_KIND);
        boolean isGlobal = PostgreDebugConstants.ATTACH_KIND_GLOBAL.equals(kind);
        kindGlobal.setSelection(isGlobal);
        kindLocal.setSelection(!isGlobal);

        Object processId = configuration.get(PostgreDebugConstants.ATTR_ATTACH_PROCESS);
        processIdText.setText(processId == null ? "" : processId.toString());

        long functionId = CommonUtils.toLong(configuration.get(PostgreDebugConstants.ATTR_FUNCTION_OID));
        if (functionId != 0 && dataSource != null) {
            try {
                container.getRunnableContext().run(true, true, monitor -> {
                    try {
                        selectedFunction = PostgreSqlDebugCore.resolveFunction(monitor, dataSource, configuration);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                container.setWarningMessage(null);
            } catch (InvocationTargetException e) {
                container.setWarningMessage(e.getTargetException().getMessage());
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (selectedFunction != null) {
            @SuppressWarnings("unchecked")
            List<String> paramValues = (List<String>) configuration.get(PostgreDebugConstants.ATTR_FUNCTION_PARAMETERS);
            if (paramValues != null) {
                List<PostgreProcedureParameter> parameters = selectedFunction.getInputParameters();
                if (parameters.size() == paramValues.size()) {
                    for (int i = 0; i < parameters.size(); i++) {
                        PostgreProcedureParameter param = parameters.get(i);
                        parameterValues.put(param, paramValues.get(i));
                    }
                }
            }

            updateParametersTable();
        }
        parametersTable.setEnabled(selectedFunction != null && !isGlobal);
        processIdText.setEnabled(isGlobal);

        if (selectedFunction != null) {
            functionCombo.addItem(selectedFunction);
            functionCombo.select(selectedFunction);
        }
    }

    private void updateParametersTable() {
        parametersTable.removeAll();
        for (DBSProcedureParameter param : selectedFunction.getInputParameters()) {
            TableItem item = new TableItem(parametersTable, SWT.NONE);
            item.setData(param);
            item.setImage(DBeaverIcons.getImage(DBIcon.TREE_ATTRIBUTE));
            item.setText(0, param.getName());
            Object value = parameterValues.get(param);
            item.setText(1, CommonUtils.toString(value, ""));
            item.setText(2, param.getParameterType().getFullTypeName());
            item.setText(3, param.getParameterKind().getTitle());
        }

        parametersTable.select(0);
    }

    @Override
    public void saveConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {
        configuration.put(PostgreDebugConstants.ATTR_ATTACH_KIND,
            kindGlobal.getSelection() ? PostgreDebugConstants.ATTACH_KIND_GLOBAL : PostgreDebugConstants.ATTACH_KIND_LOCAL);
        configuration.put(PostgreDebugConstants.ATTR_ATTACH_PROCESS, processIdText.getText());

        if (selectedFunction != null) {
            configuration.put(PostgreDebugConstants.ATTR_FUNCTION_OID, selectedFunction.getObjectId());
            configuration.put(PostgreDebugConstants.ATTR_DATABASE_NAME, selectedFunction.getDatabase().getName());
            configuration.put(PostgreDebugConstants.ATTR_SCHEMA_NAME, selectedFunction.getSchema().getName());
            List<String> paramValues = new ArrayList<>();
            for (PostgreProcedureParameter param : selectedFunction.getInputParameters()) {
                Object value = parameterValues.get(param);
                paramValues.add(value == null ? null : value.toString());
            }
            configuration.put(PostgreDebugConstants.ATTR_FUNCTION_PARAMETERS, paramValues);
        } else {
            configuration.remove(PostgreDebugConstants.ATTR_FUNCTION_OID);
            configuration.remove(PostgreDebugConstants.ATTR_DATABASE_NAME);
            configuration.remove(PostgreDebugConstants.ATTR_SCHEMA_NAME);
            configuration.remove(PostgreDebugConstants.ATTR_FUNCTION_PARAMETERS);
        }
    }

    @Override
    public boolean isValid() {
        return selectedFunction != null;
    }
}
