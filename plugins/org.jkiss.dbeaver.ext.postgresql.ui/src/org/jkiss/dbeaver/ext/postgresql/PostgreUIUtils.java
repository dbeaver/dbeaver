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
package org.jkiss.dbeaver.ext.postgresql;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeToolWizard;
import org.jkiss.dbeaver.ui.UIUtils;

public class PostgreUIUtils {

    public static void addCompatibilityInfoLabelForForks(
        @NotNull Composite composite,
        @NotNull AbstractNativeToolWizard wizard,
        @Nullable PostgreDataSource dataSource
    ) {
        boolean showInfoLabel = false;
        if (dataSource != null && dataSource.getServerType() instanceof PostgreServerPostgreSQL) {
            showInfoLabel = true;
        } else if (!wizard.getSettings().getDatabaseObjects().isEmpty()) {
            Object databaseObject = wizard.getSettings().getDatabaseObjects().get(0);
            if (databaseObject instanceof PostgreObject pObject &&
                !(pObject.getDataSource().getServerType() instanceof PostgreServerPostgreSQL)
                ) {
                showInfoLabel = true;
            }
        }
        if (showInfoLabel) {
            Control infoLabel = UIUtils.createWarningLabel(
                composite,
                PostgreMessages.wizard_info_label_incompatible_tool,
                GridData.FILL_BOTH,
                1);
            GridData gridData = new GridData(SWT.FILL, SWT.END, true, false);
            gridData.horizontalSpan = 1;
            infoLabel.setLayoutData(gridData);
        }
    }
}
