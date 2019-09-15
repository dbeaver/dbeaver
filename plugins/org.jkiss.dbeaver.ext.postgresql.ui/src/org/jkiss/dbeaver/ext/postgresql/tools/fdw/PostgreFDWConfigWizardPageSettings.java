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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;


class PostgreFDWConfigWizardPageSettings extends ActiveWizardPage<PostgreFDWConfigWizard> {
    private DatabaseObjectsSelectorPanel selectorPanel;

    protected PostgreFDWConfigWizardPageSettings()
    {
        super("Settings");
        setTitle("Configure foreign data wrappers");
        setDescription("Choose which databases/tables you need to configure");
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Group databasesGroup = UIUtils.createControlGroup(composite, "Foreign databases", 1, GridData.FILL_BOTH, 0);

            selectorPanel = new DatabaseObjectsSelectorPanel(
                databasesGroup,
                getWizard().getRunnableContext()) {

                @Override
                protected boolean isObjectVisible(DBSObject obj) {
                    return super.isObjectVisible(obj);
                }

                @Override
                protected void onSelectionChange() {
                    updateState();
                }

                @Override
                protected boolean isFolderVisible(DBNLocalFolder folder) {
                    return super.isFolderVisible(folder);
                }

                @Override
                protected boolean isDataSourceVisible(DBNDataSource dataSource) {
                    if (dataSource.getDataSourceContainer() == getWizard().getDatabase().getDataSource().getContainer()) {
                        // Do not show own datasource
                        return false;
                    }
                    return super.isDataSourceVisible(dataSource);
                }
            };
        }

        setControl(composite);
    }

    protected void updateState()
    {
        getContainer().updateButtons();
    }

}
