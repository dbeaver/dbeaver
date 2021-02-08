/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreForeignDataWrapper;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.fdw.FDWConfigDescriptor;
import org.jkiss.dbeaver.ext.postgresql.model.fdw.FDWConfigRegistry;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


class PostgreFDWConfigWizardPageConfig extends ActiveWizardPage<PostgreFDWConfigWizard> {

    private static final Log log = Log.getLog(PostgreFDWConfigWizardPageConfig.class);

    private boolean activated;
    private Table entityTable;
    private Combo fdwCombo;
    private Text fdwServerText;
    private Combo schemaCombo;
    private PropertyTreeViewer propsEditor;
    private Text targetDataSourceText;
    private Text targetDriverText;

    private List<PostgreSchema> schemaList;
    private List<PostgreFDWConfigWizard.FDWInfo> fdwList;

    protected PostgreFDWConfigWizardPageConfig(PostgreFDWConfigWizard wizard)
    {
        super("Configuration");
        setTitle("Configure foreign data wrappers");
        setDescription("Choose foreign wrapper and set option");
        setWizard(wizard);
    }

    @Override
    public boolean isPageComplete()
    {
        PostgreFDWConfigWizard wizard = getWizard();
        return activated &&
            !CommonUtils.isEmpty(wizard.getFdwServerId()) &&
            wizard.getSelectedFDW() != null &&
            wizard.getSelectedSchema() != null &&
            !wizard.getSelectedEntities().isEmpty();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Composite fdwGroup = UIUtils.createComposite(composite, 2);
            fdwGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            fdwCombo = UIUtils.createLabelCombo(fdwGroup, "Wrapper", SWT.DROP_DOWN | SWT.READ_ONLY);
            fdwCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().setSelectedFDW(fdwList.get(fdwCombo.getSelectionIndex()));
                    refreshFDWProperties();
                }
            });
            UIUtils.createEmptyLabel(fdwGroup, 1, 1);
            UIUtils.createLink(fdwGroup, "If you don't see right data wrapper in the list then try to <a>install it</a>", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                }
            });
        }

        SashForm sashForm = new SashForm(composite, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        {
            Group settingsGroup = UIUtils.createControlGroup(sashForm, "Settings", 2, GridData.FILL_BOTH, 0);

            fdwServerText = UIUtils.createLabelText(settingsGroup, "Server ID", "", SWT.BORDER);
            fdwServerText.addModifyListener(e -> getWizard().setFdwServerId(fdwServerText.getText()));

            schemaCombo = UIUtils.createLabelCombo(settingsGroup, "Target schema", SWT.DROP_DOWN | SWT.READ_ONLY);
            schemaCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().setSelectedSchema(schemaList.get(schemaCombo.getSelectionIndex()));
                    updatePageCompletion();
                }
            });

            //UIUtils.createControlLabel(settingsGroup, "Options", 2);
            propsEditor = new PropertyTreeViewer(settingsGroup, SWT.BORDER);
            propsEditor.setNamesEditable(true);
            propsEditor.setNewPropertiesAllowed(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            propsEditor.getControl().setLayoutData(gd);
        }
        {
            Group tablesGroup = UIUtils.createControlGroup(sashForm, "Tables", 2, GridData.FILL_BOTH, 0);

            targetDataSourceText = UIUtils.createLabelText(tablesGroup, "Data source", "", SWT.BORDER | SWT.READ_ONLY);
            targetDriverText = UIUtils.createLabelText(tablesGroup, "Driver", "", SWT.BORDER | SWT.READ_ONLY);

            entityTable = new Table(tablesGroup, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            entityTable.setHeaderVisible(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            entityTable.setLayoutData(gd);
            UIUtils.createTableColumn(entityTable, SWT.LEFT, "Table");
            //UIUtils.createTableColumn(entityTable, SWT.LEFT, "Data source");
            //UIUtils.createTableColumn(entityTable, SWT.LEFT, "Driver");
        }


        setControl(composite);
    }

    private void refreshFDWProperties() {
        {
            // Fill options
            DBPDataSourceContainer targetDataSource = getWizard().getSelectedDataSource();
            PostgreFDWConfigWizard.FDWInfo selectedFDW = getWizard().getSelectedFDW();

            PropertySourceCustom propertySource = getWizard().getFdwPropertySource();
            propertySource.setDefValueResolver(targetDataSource.getVariablesResolver(false));
            propertySource.removeAll();
            if (selectedFDW != null && selectedFDW.fdwDescriptor != null) {
                propertySource.addProperties(selectedFDW.fdwDescriptor.getProperties());
            } else if (selectedFDW != null) {
                // Add some default props
                propertySource.addProperty(new PropertyDescriptor(null, "host", "host", "Remote database host", false, String.class, "${host}", null));
                propertySource.addProperty(new PropertyDescriptor(null, "port", "port", "Remote database port", false, String.class, "${port}", null));
                propertySource.addProperty(new PropertyDescriptor(null, "dbname", "dbname", "Remote database name", false, String.class, "${database}", null));
            }
            propsEditor.loadProperties(propertySource);
        }
    }

    @Override
    public void activatePage() {
        if (!activated) {
            activated = true;
        }
        loadSettings();
        super.activatePage();
    }

    private void loadSettings() {
        DBPDataSourceContainer targetDataSource = getWizard().getSelectedDataSource();
        if (targetDataSource == null) {
            setErrorMessage("No target data source");
            return;
        }

        // Fill FDW list
        try {
            getWizard().getRunnableContext().run(false, true, monitor -> {
                try {
                    schemaList = new ArrayList<>();
                    schemaList.addAll(getWizard().getDatabase().getSchemas(monitor));

                    // Fill from both installed FDW and pre-configured FDW
                    fdwList = new ArrayList<>();
                    for (PostgreForeignDataWrapper fdw : CommonUtils.safeCollection(getWizard().getDatabase().getForeignDataWrappers(monitor))) {
                        PostgreFDWConfigWizard.FDWInfo fdwInfo = new PostgreFDWConfigWizard.FDWInfo();
                        fdwInfo.installedFDW = fdw;
                        fdwList.add(fdwInfo);
                    }
                    for (FDWConfigDescriptor fdw : FDWConfigRegistry.getInstance().getConfigDescriptors()) {
                        boolean found = false;
                        for (PostgreFDWConfigWizard.FDWInfo fdwInfo : fdwList) {
                            if (fdwInfo.getId().equals(fdw.getFdwId())) {
                                fdwInfo.fdwDescriptor = fdw;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            PostgreFDWConfigWizard.FDWInfo fdwInfo = new PostgreFDWConfigWizard.FDWInfo();
                            fdwInfo.fdwDescriptor = fdw;
                            fdwList.add(fdwInfo);
                        }
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            log.debug(e.getTargetException());
            setErrorMessage(e.getTargetException().getMessage());
            return;
        } catch (InterruptedException e) {
            return;
        }
        setErrorMessage(null);

        // Detect FDW from target container
        PostgreFDWConfigWizard.FDWInfo fdwInfo = getWizard().getSelectedFDW();
        if (fdwInfo == null) {
            FDWConfigDescriptor fdwConfig = FDWConfigRegistry.getInstance().findFirstMatch(targetDataSource);
            if (fdwConfig != null) {
                for (PostgreFDWConfigWizard.FDWInfo fdw : fdwList) {
                    if (fdw.fdwDescriptor == fdwConfig) {
                        fdwInfo = fdw;
                        break;
                    }
                }
            }
        }

        fdwCombo.removeAll();
        for (PostgreFDWConfigWizard.FDWInfo fdw : fdwList) {
            String fdwName = fdw.getId();
            if (!CommonUtils.isEmpty(fdw.getDescription())) {
                fdwName += " (" + fdw.getDescription() + ")";
            }
            fdwCombo.add(fdwName);
        }
        if (fdwInfo != null) {
            getWizard().setSelectedFDW(fdwInfo);
            fdwCombo.setText(fdwInfo.getId());
        }

        schemaCombo.removeAll();
        for (PostgreSchema schema : schemaList) {
            schemaCombo.add(schema.getName());
        }
        PostgreSchema selectedSchema = getWizard().getSelectedSchema();
        if (selectedSchema != null) {
            schemaCombo.setText(selectedSchema.getName());
        } else {
            PostgreSchema publicSchema = DBUtils.findObject(schemaList, PostgreConstants.PUBLIC_SCHEMA_NAME);
            if (publicSchema != null) {
                schemaCombo.setText(publicSchema.getName());
                getWizard().setSelectedSchema(publicSchema);
            }
        }

        refreshFDWProperties();

        if (CommonUtils.isEmpty(fdwServerText.getText())) {
            String fdwServerId = (fdwInfo == null ? targetDataSource.getDriver().getId() : fdwInfo.getId()) + "_srv";
            getWizard().setFdwServerId(fdwServerId);
            fdwServerText.setText(fdwServerId);
        }

        // Fill entities
        targetDataSourceText.setText(targetDataSource.getName());
        targetDriverText.setText(targetDataSource.getDriver().getName());

        entityTable.removeAll();
        for (DBNDatabaseNode entityNode : getWizard().getSelectedEntities()) {
            TableItem item = new TableItem(entityTable, SWT.NONE);
            item.setImage(0, DBeaverIcons.getImage(entityNode.getNodeIconDefault()));
            item.setText(0, entityNode.getNodeFullName());
        }
        UIUtils.packColumns(entityTable, false);
        propsEditor.repackColumns();
        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
        propsEditor.saveEditorValues();
        super.deactivatePage();
    }
}
