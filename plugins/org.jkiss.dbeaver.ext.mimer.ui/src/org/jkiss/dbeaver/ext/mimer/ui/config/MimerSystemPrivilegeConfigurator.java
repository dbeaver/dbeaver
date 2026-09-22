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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.model.MimerSystemPrivilege;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * Collects the privilege type and Grant Option before a new Mimer SQL system privilege grant is
 * created.
 *
 * @author Mimer Information Technology
 */
public class MimerSystemPrivilegeConfigurator implements DBEObjectConfigurator<MimerSystemPrivilege> {

    @Nullable
    @Override
    public MimerSystemPrivilege configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object container,
        @NotNull MimerSystemPrivilege privilege,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            MimerCreateSystemPrivilegePage page = new MimerCreateSystemPrivilegePage(privilege, (DBSObject) container);
            if (!page.edit()) {
                return null;
            }
            page.applyChanges();
            return privilege;
        });
    }
}
