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
package org.jkiss.dbeaver.ui.app.standalone.update;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.core.ui.services.UIServiceApplicationVersionUpdater;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

public class WorkbenchInitializerUpdateCheck implements IWorkbenchWindowInitializer {
    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        if (isAutoupdateDisabled(application)) {
            return;
        }
        new DBeaverVersionChecker(false).schedule();
    }

    private boolean isAutoupdateDisabled(@NotNull DBPApplication application) {
        return application.isDistributed()
            || ApplicationPolicyService.getInstance().isInstallUpdateDisabled()
            || isUpdateJobDisabledByService();
    }

    private boolean isUpdateJobDisabledByService() {
        UIServiceApplicationVersionUpdater service = DBWorkbench.findService(UIServiceApplicationVersionUpdater.class);
        return service != null && !service.isAutoUpdateEnabled();
    }
}
