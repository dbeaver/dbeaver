/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.databend.ui.internal.DatabendMessages;
import org.jkiss.dbeaver.ext.databend.ui.internal.DatabendMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorTrustStoreUI;
import org.jkiss.utils.CommonUtils;

public class DatabendSSLConfigurator extends SSLConfiguratorTrustStoreUI {
    private static final Log log = Log.getLog(DatabendSSLConfigurator.class);

    @Nullable
    private Combo sslModeCombo;

    enum SSLModes {
        STRICT,
        NONE
    }

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        createSSLConfigHint(parent, true, 0);
        createTrustStoreConfigGroup(parent);
        Group advancedSettingsGroup = UIUtils.createControlGroup(parent,
                DatabendMessages.dialog_connection_page_advanced_settings,
                1,
                GridData.FILL_HORIZONTAL,
                0
        );
        Composite sslComposite = UIUtils.createPlaceholder(advancedSettingsGroup, 2);
        sslComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        sslModeCombo = UIUtils.createLabelCombo(sslComposite,
                DatabendMessages.dialog_connection_page_text_ssl_mode,
                DatabendMessages.dialog_connection_page_text_ssl_mode_tip,
                SWT.READ_ONLY
        );
        sslModeCombo.add(SSLModes.STRICT.name()); //$NON-NLS-1$
        sslModeCombo.add(SSLModes.NONE.name()); //$NON-NLS-1$
        sslModeCombo.select(0);
        sslModeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

    }


}
