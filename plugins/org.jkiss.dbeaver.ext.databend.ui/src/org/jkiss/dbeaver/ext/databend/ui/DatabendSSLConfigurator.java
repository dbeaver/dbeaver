/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.databend.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.databend.ui.internal.DatabendMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorAbstractUI;

public class DatabendSSLConfigurator extends SSLConfiguratorAbstractUI {
    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        createSSLEnabledHint(parent);
        String message = DatabendMessages.dialog_connection_page_text_ssl_no_additional_configuration;
        UIUtils.createWarningLabel(parent, message, SWT.NONE, 0);
    }

    private static void createSSLEnabledHint(@NotNull Composite parent) {
        Composite enabledPanel = UIUtils.createComposite(parent, 1);
        GridLayout layout = (GridLayout) enabledPanel.getLayout();
        layout.marginHeight = 5;
        enabledPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label enabledLabel = new Label(enabledPanel, SWT.WRAP);
        enabledLabel.setText(DatabendMessages.dialog_connection_page_text_ssl_enabled);
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration configuration) {
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
    }
}
