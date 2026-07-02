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
package org.jkiss.dbeaver.ui.config.sample;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class WorkbenchInitializerCreateSampleDatabase implements IWorkbenchWindowInitializer {

    private static final String PROP_SAMPLE_DB_CANCELED = "sample.database.canceled";

    private static final Log log = Log.getLog(WorkbenchInitializerCreateSampleDatabase.class);

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PROP_SAMPLE_DB_CANCELED)) {
            // Create was canceled
            return;
        }
        if (DataSourceRegistry.getAllDataSources().size() > 1) {
            // Seems to be experienced user - no need in sample db
            return;
        }
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject == null || !activeProject.isRegistryLoaded()) {
            // No active project
            return;
        }
        DBPDataSourceRegistry registry = activeProject.getDataSourceRegistry();
        if (SampleDatabaseUtil.isSampleDatabaseExists(registry)) {
            // Already exist
            return;
        }
        if (!showCreateSampleDatabasePrompt(configurer.getWindow().getShell())) {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_SAMPLE_DB_CANCELED, true);
        } else {
            SampleDatabaseUtil.createSampleDatabase(registry);
        }
    }

    static boolean showCreateSampleDatabasePrompt(Shell shell) {
        return UIUtils.confirmAction(
                shell,
                SampleDatabaseMessages.dialog_create_title,
                NLS.bind(SampleDatabaseMessages.dialog_create_description, GeneralUtils.getProductName())
        );
    }

}

