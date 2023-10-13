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

package org.jkiss.dbeaver.ext.dameng.ui.config;

import org.jkiss.dbeaver.ext.dameng.model.DamengSchema;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DamengSchemaConfigurator implements DBEObjectConfigurator<DamengSchema> {
    @Override
    public DamengSchema configureObject(DBRProgressMonitor monitor, Object container, DamengSchema damengSchema, Map<String, Object> options) {
        return UITask.run(() -> {
            String schemaName = EnterNameDialog.chooseName(
                    UIUtils.getActiveWorkbenchShell(),
                    "New Schema",
                    ""
            );
            if (CommonUtils.isEmpty(schemaName)) {
                return null;
            }
            damengSchema.setName(schemaName);
            return damengSchema;
        });
    }
}
