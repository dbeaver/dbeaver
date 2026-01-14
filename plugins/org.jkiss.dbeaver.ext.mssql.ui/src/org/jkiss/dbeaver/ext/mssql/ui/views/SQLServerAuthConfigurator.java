/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.ui.views;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.auth.SQLServerAuthModelNTLM;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.utils.CommonUtils;

/**
 * SQL Server auth model config
 */
public class SQLServerAuthConfigurator extends DatabaseNativeAuthModelConfigurator {


    private Button isCheck;

    @Override
    public void createControl(@NotNull Composite parent, DBAAuthModel<?> object, @NotNull Runnable propertyChangeListener) {
        super.createControl(parent, object, propertyChangeListener);
        if (object instanceof SQLServerAuthModelNTLM) {
            isCheck = UIUtils.createCheckbox(
                parent,
                "Use integrated security",
                "Enable integrated security (by default it is required for NTLM)",
                true,
                2);
        }
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);

        if (isCheck != null) {
            isCheck.setSelection(
                CommonUtils.getBoolean(
                    dataSource.getConnectionConfiguration().getProperty(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY),
                    true));
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        if (isCheck != null) {
            dataSource.getConnectionConfiguration().setProperty(
                SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY,
                String.valueOf(isCheck.getSelection()));
        }
    }

    @Override
    public void resetSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.resetSettings(dataSource);

        if (isCheck != null) {
            dataSource.getConnectionConfiguration().removeProperty(
                SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY);
        }
    }

}
