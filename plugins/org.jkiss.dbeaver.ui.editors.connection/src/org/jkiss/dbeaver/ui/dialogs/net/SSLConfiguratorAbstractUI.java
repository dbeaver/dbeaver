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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;

/**
 * SSL configuration
 */
public abstract class SSLConfiguratorAbstractUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    @Override
    public void resetSettings(DBWHandlerConfiguration configuration) {

    }

    @Override
    public boolean isComplete()
    {
        return false;
    }

    protected void createSSLConfigHint(Composite composite, boolean optional, int hSpan) {
        Label tipLabel = new Label(composite, SWT.WRAP);
        StringBuilder tip = new StringBuilder();
        if (optional) {
            tip.append("All SSL parameters are optional.\n");
        }
        tip.append("You must specify SSL certificates if they are required by your server configuration.\n\n");
        tipLabel.setText(tip.toString());
        if (hSpan > 1) {
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = hSpan;
            tipLabel.setLayoutData(gd);
        }
    }


}
