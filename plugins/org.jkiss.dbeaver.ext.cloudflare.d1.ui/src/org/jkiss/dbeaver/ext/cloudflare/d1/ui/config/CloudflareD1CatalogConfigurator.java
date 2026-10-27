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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cloudflare.d1.edit.CloudflareD1CatalogManager;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1Catalog;
import org.jkiss.dbeaver.ext.cloudflare.d1.ui.internal.CloudflareD1UIMessages;
import org.jkiss.dbeaver.ext.cloudflare.d1.ui.views.CloudflareD1CreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

public class CloudflareD1CatalogConfigurator implements DBEObjectConfigurator<CloudflareD1Catalog> {
    @Nullable
    @Override
    public CloudflareD1Catalog configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object parent,
        @NotNull CloudflareD1Catalog catalog,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            if (Boolean.TRUE.equals(options.get(CloudflareD1CatalogManager.CONFIRM_DELETE))) {
                String id = catalog.requireDatabaseId();
                InputDialog confirmation = new InputDialog(UIUtils.getActiveWorkbenchShell(), CloudflareD1UIMessages.delete_title,
                    NLS.bind(CloudflareD1UIMessages.delete_confirm, catalog.getName(), id),
                    "", value -> id.equals(value) ? null : CloudflareD1UIMessages.delete_uuid_required);
                return confirmation.open() == IDialogConstants.OK_ID ? catalog : null;
            }
            if (catalog.isPersisted()) {
                return catalog;
            }
            var dialog = new CloudflareD1CreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(), catalog);
            return dialog.open() == IDialogConstants.OK_ID ? catalog : null;
        });
    }
}
