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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * Postgre sequence configurator
 */
public class PostgreTriggerConfigurator implements DBEObjectConfigurator<PostgreTrigger> {

    @Override
    public PostgreTrigger configureObject(DBRProgressMonitor monitor, Object parent, PostgreTrigger trigger, Map<String, Object> options) {
        return new UITask<PostgreTrigger>() {

            @Override
            protected PostgreTrigger runTask() {
                PostgreTriggerEditPage editPage = new PostgreTriggerEditPage(trigger);
                if (!editPage.edit()) {
                    return null;
                }
                trigger.setName(editPage.getEntityName());
                trigger.setFunction(editPage.selectedFunction);
                return trigger;
            }
        }.execute();
    }
}
