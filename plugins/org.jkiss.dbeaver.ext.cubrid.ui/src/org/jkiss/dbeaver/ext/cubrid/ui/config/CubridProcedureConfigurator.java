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
package org.jkiss.dbeaver.ext.cubrid.ui.config;

import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridProcedure;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

public class CubridProcedureConfigurator implements DBEObjectConfigurator<CubridProcedure> {

    @Override
    public CubridProcedure configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object parent, @NotNull CubridProcedure newProcedure, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            CreateCubridProcedurePage editPage = new CreateCubridProcedurePage(newProcedure);
            if (!editPage.edit()) {
                return null;
            }
            newProcedure.setProcedureType(editPage.getProcedureType());
            newProcedure.setName(editPage.getProcedureName());
            return newProcedure;
        });
    }

}
