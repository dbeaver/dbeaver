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
package org.jkiss.dbeaver.ext.sqlite.ui;

import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoading;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetErrorAction;


public class SQLiteExtensionsErrorAction implements IResultSetErrorAction {
    @Override
    public boolean isVisible(@NotNull IResultSetContainer container, @NotNull IStatus status) {
        DBCExecutionContext context = container.getExecutionContext();
        if (context == null || !(context.getDataSource() instanceof SQLiteDataSource)) {
            return false;
        }
        return SQLiteExtensionLoading.isLoadingDenied(status.getException());
    }

    @Override
    public void perform(@NotNull IResultSetContainer container, @NotNull IStatus status) {
        DBCExecutionContext context = container.getExecutionContext();
        if (context != null) {
            SQLiteExtensionsApproval.request(context.getDataSource().getContainer());
        }
    }
}
