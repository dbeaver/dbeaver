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
package org.jkiss.dbeaver.ui.config.sample.easyConfig;

import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.config.easy.registry.EasyConfigAction;
import org.jkiss.dbeaver.ui.config.sample.SampleDatabaseUtil;

public class EasyConfigCreateSampleDatabaseAction implements EasyConfigAction.OfCheckbox {
    @Override
    public boolean loadState() {
        return false; // the default value
    }

    @Override
    public void applyState(boolean value) {
        if (!value) {
            return;
        }
        var project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            return;
        }
        SampleDatabaseUtil.createSampleDatabase(project.getDataSourceRegistry());
    }

    @Override
    public boolean isApplicable() {
        var project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            return false;
        }
        // Don't show the option to create a sample database if it already exists in the workspace
        return !SampleDatabaseUtil.isSampleDatabaseExists(project.getDataSourceRegistry());
    }
}
