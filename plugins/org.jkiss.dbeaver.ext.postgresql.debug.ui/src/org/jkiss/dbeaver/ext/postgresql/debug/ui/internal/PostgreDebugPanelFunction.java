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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

public class PostgreDebugPanelFunction implements DBGConfigurationPanel {

    private Button kindLocal;
    private Button kindGlobal;

    @Override
    public void createPanel(Composite parent) {
        {
            UIUtils.createControlGroup(parent, "Attach type", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
            kindLocal = new Button(parent, SWT.RADIO);
            kindLocal.setText("Local");
            kindGlobal = new Button(parent, SWT.RADIO);
            kindGlobal.setText("Global");
        }
        {
            Group functionGroup = UIUtils.createControlGroup(parent, "Function", 2, GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
            UIUtils.createLabelText(functionGroup, "Function", "", SWT.BORDER | SWT.READ_ONLY, new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createLabelText(functionGroup, "Process ID", "", SWT.BORDER | SWT.READ_ONLY, new GridData(GridData.FILL_HORIZONTAL));
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
    }

    @Override
    public void saveConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {

    }

    @Override
    public boolean isValid() {
        return false;
    }
}
