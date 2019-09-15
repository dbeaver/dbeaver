/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;


class PostgreFDWConfigWizardPageConfig extends ActiveWizardPage<PostgreFDWConfigWizard> {

    private static final Log log = Log.getLog(PostgreFDWConfigWizardPageConfig.class);

    private boolean activated;
    private Table entityTable;
    private Combo fdwCombo;
    private PropertyTreeViewer propsEditor;
    private Text targetDataSourceText;
    private Text targetDriverText;

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
        return false;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Composite fdwGroup = UIUtils.createComposite(composite, 2);
            fdwGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            fdwCombo = UIUtils.createLabelCombo(fdwGroup, "Foreign Data Wrapper", SWT.DROP_DOWN | SWT.READ_ONLY);
            UIUtils.createEmptyLabel(fdwGroup, 1, 1);
            UIUtils.createLink(fdwGroup, "If you don't see right data wrapper in the list then try to <a>install it</a>", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                }
            });
        }

        SashForm sashForm = new SashForm(composite, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        {
            Group settingsGroup = UIUtils.createControlGroup(sashForm, "Settings", 2, GridData.FILL_BOTH, 0);

            //UIUtils.createControlLabel(settingsGroup, "Options", 2);
            propsEditor = new PropertyTreeViewer(settingsGroup, SWT.BORDER);
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
                    getWizard().getDatabase().getForeignDataWrappers(monitor);
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

        String hostName = CommonUtils.notEmpty(targetDataSource.getConnectionConfiguration().getHostName());
        String hostPort = CommonUtils.notEmpty(targetDataSource.getConnectionConfiguration().getHostPort());
        String databaseName = CommonUtils.notEmpty(targetDataSource.getConnectionConfiguration().getDatabaseName());

        {
            // Fill options
            PropertySourceCustom propertySource = new PropertySourceCustom();
            propertySource.addProperty(new PropertyDescriptor(null, "host", "host", "Remote database host", false, String.class, hostName, null));
            propertySource.addProperty(new PropertyDescriptor(null, "port", "port", "Remote database port", false, String.class, hostPort, null));
            propertySource.addProperty(new PropertyDescriptor(null, "dbname", "dbname", "Remote database name", false, String.class, databaseName, null));
            propsEditor.loadProperties(propertySource);
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
    }

}
