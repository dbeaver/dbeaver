/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanelContainer;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.runtime.sql.ProcedureParameterBindDialog;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.dialogs.BrowseObjectDialog;
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
    private Button configParametersButton;

    private PostgreProcedure selectedFunction;
    private Map<DBSProcedureParameter, Object> parameterValues = new HashMap<>();

    @Override
    public void createPanel(Composite parent, DBGConfigurationPanelContainer container) {
        this.container = container;

        {
            Group kindGroup = UIUtils.createControlGroup(parent, "Attach type", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, SWT.DEFAULT);

            SelectionListener listener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    processIdText.setEnabled(kindGlobal.getSelection());
                    configParametersButton.setEnabled(kindLocal.getSelection());
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
        {
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
                        DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                        DBNDatabaseNode dsNode = navigatorModel.getNodeByObject(container.getDataSource());
                        if (dsNode != null) {
                            DBNNode curNode = selectedFunction == null ? null : navigatorModel.getNodeByObject(selectedFunction);
                            DBNNode node = BrowseObjectDialog.selectObject(
                                parent.getShell(),
                                "Select function to debug",
                                dsNode,
                                curNode,
                                new Class[]{DBSInstance.class, DBSObjectContainer.class, PostgreProcedure.class},
                                new Class[]{PostgreProcedure.class});
                            if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof PostgreProcedure) {
                                functionCombo.removeAll();
                                selectedFunction = (PostgreProcedure) ((DBNDatabaseNode) node).getObject();
                                functionCombo.addItem(selectedFunction);
                                functionCombo.select(selectedFunction);
                                container.updateDialogState();
                            }
                            configParametersButton.setEnabled(selectedFunction != null);
                        }
                    }

                }
            };
            functionCombo.addItem(null);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(functionCombo) * 40 + 10;
            functionCombo.setLayoutData(gd);

            configParametersButton = UIUtils.createPushButton(functionGroup, "Configure Parameters...", null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (selectedFunction == null) {
                        return;
                    }
                    ProcedureParameterBindDialog dialog = new ProcedureParameterBindDialog(parent.getShell(), selectedFunction, parameterValues);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        parameterValues.clear();
                        parameterValues.putAll(dialog.getValues());
                    }
                }
            });
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            configParametersButton.setLayoutData(gd);

            processIdText = UIUtils.createLabelText(functionGroup, "Process ID", "", SWT.BORDER, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(processIdText) * 10 + 10;
            processIdText.setLayoutData(gd);
        }
    }

    @Override
    public void loadConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {
        Object kind = configuration.get(PostgreDebugConstants.ATTR_ATTACH_KIND);
        if (PostgreDebugConstants.ATTACH_KIND_GLOBAL.equals(kind)) {
            kindGlobal.setSelection(true);
        } else {
            kindLocal.setSelection(true);
        }
        configParametersButton.setEnabled(kindLocal.getSelection());
        processIdText.setEnabled(kindGlobal.getSelection());

        Object processId = configuration.get(PostgreDebugConstants.ATTR_ATTACH_PROCESS);
        processIdText.setText(processId == null ? "" : processId.toString());

        long functionId = CommonUtils.toLong(configuration.get(PostgreDebugConstants.ATTR_FUNCTION_OID));
        String databaseName = (String)configuration.get(PostgreDebugConstants.ATTR_DATABASE_NAME);
        String schemaName = (String)configuration.get(PostgreDebugConstants.ATTR_SCHEMA_NAME);
        if (functionId != 0 && dataSource != null) {
            try {
                container.getRunnableContext().run(true, true, monitor -> {
                    try {
                        if (!dataSource.isConnected()) {
                            dataSource.connect(monitor, true, true);
                        }
                        PostgreDataSource ds = (PostgreDataSource) dataSource.getDataSource();
                        PostgreDatabase database = ds.getDatabase(databaseName);
                        if (database != null) {
                            PostgreSchema schema = database.getSchema(monitor, schemaName);
                            if (schema != null) {
                                selectedFunction = schema.getProcedure(monitor, functionId);
                            } else {
                                container.setWarningMessage("Schema '" + schemaName + "' not found");
                            }
                        } else {
                            container.setWarningMessage("Database '" + databaseName + "' not found");
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
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
                List<PostgreProcedureParameter> parameters = selectedFunction.getParameters(null);
                if (parameters.size() == paramValues.size()) {
                    for (int i = 0; i < parameters.size(); i++) {
                        PostgreProcedureParameter param = parameters.get(i);
                        parameterValues.put(param, paramValues.get(i));
                    }
                }
            }
        }
        configParametersButton.setEnabled(selectedFunction != null);
        if (selectedFunction != null) {
            functionCombo.addItem(selectedFunction);
            functionCombo.select(selectedFunction);
        } else {
            if (functionId != 0) {
                container.setWarningMessage("Function '" + functionId + "' not found in schema '" + schemaName + "'");
            }
        }
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
            for (PostgreProcedureParameter param : selectedFunction.getParameters(null)) {
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
