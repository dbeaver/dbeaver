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

import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.ui.services.UIServiceApplicationVersionUpdater;

public class WorkbenchInitializerUpdateCheck implements IWorkbenchWindowInitializer {
    private static final String UPDATE_TOOLBAR_ID = "dbeaver-version-update";
    private static final String PERSPECTIVE_SPACER_ID = "PerspectiveSpacer";

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        positionUpdateToolbar(configurer);

        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        if (isAutoupdateDisabled(application)) {
            return;
        }
        new DBeaverVersionChecker(false).schedule();
    }

    private static void positionUpdateToolbar(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (!(configurer.getWindow() instanceof WorkbenchWindow window)) {
            return;
        }
        MTrimBar topTrim = window.getTopTrim();
        MTrimElement updateToolbar = null;
        boolean hasSpacer = false;
        for (MTrimElement element : topTrim.getChildren()) {
            if (UPDATE_TOOLBAR_ID.equals(element.getElementId())) {
                updateToolbar = element;
            } else if (PERSPECTIVE_SPACER_ID.equals(element.getElementId())) {
                hasSpacer = true;
            }
        }
        // The workbench adds its right-alignment spacer after applying trim contributions.
        if (hasSpacer && updateToolbar != null && topTrim.getChildren().getLast() != updateToolbar) {
            EModelService modelService = window.getService(EModelService.class);
            modelService.move(updateToolbar, topTrim);
        }
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
