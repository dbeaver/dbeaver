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
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.debug.ui.DBGConfigurationPanel;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

public class PostgreDebugPanelFunction implements DBGConfigurationPanel {

    private Button kindLocal;
    private Button kindGlobal;
    private Combo functionText;
    private Text processIdText;

    @Override
    public void createPanel(Composite parent) {
        {
            Group kindGroup = UIUtils.createControlGroup(parent, "Attach type", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, SWT.DEFAULT);
            kindLocal = new Button(kindGroup, SWT.RADIO);
            kindLocal.setText("Local");
            kindGlobal = new Button(kindGroup, SWT.RADIO);
            kindGlobal.setText("Global");
        }
        {
            Group functionGroup = UIUtils.createControlGroup(parent, "Function", 2, GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
            functionText = UIUtils.createLabelCombo(functionGroup, "Function", "", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(functionText) * 40 + 10;
            functionText.setLayoutData(gd);

            processIdText = UIUtils.createLabelText(functionGroup, "Process ID", "", SWT.BORDER, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(functionText) * 10 + 10;
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
    }

    @Override
    public void saveConfiguration(DBPDataSourceContainer dataSource, Map<String, Object> configuration) {

    }

    @Override
    public boolean isValid() {
        return false;
    }
}
