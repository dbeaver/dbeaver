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
package org.jkiss.dbeaver.ext.cloudflare.d1.ui.config;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cloudflare.d1.ui.internal.CloudflareD1UIMessages;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;

public final class CloudflareD1AuthConfigurator extends DatabaseNativeAuthModelConfigurator {
    @Override
    public void createControl(@NotNull Composite parent, @NotNull DBAAuthModel<?> model, @NotNull Runnable onChange) {
        super.createControl(parent, model, onChange);
        usernameLabel.setText(CloudflareD1UIMessages.connection_account_id);
    }

    @NotNull
    @Override
    protected String getPasswordFieldLabel() {
        return CloudflareD1UIMessages.connection_api_token;
    }

    public void copyCredentialsTo(@NotNull DBPConnectionConfiguration configuration) {
        configuration.setUserName(usernameText.getText().trim());
        configuration.setUserPassword(passwordText.getText().trim());
    }
}
