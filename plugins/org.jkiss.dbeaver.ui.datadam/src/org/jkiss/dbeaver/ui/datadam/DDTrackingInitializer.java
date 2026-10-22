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
package org.jkiss.dbeaver.ui.datadam;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.sync.tracking.DDDesktopTrackingService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

import java.util.concurrent.atomic.AtomicBoolean;

public class DDTrackingInitializer implements IWorkbenchWindowInitializer {

    private static final Log log = Log.getLog(DDTrackingInitializer.class);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (!DDProductConfig.isIntegrationEnabled() || !INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
            @Override
            public boolean preShutdown(@NotNull IWorkbench workbench, boolean forced) {
                try {
                    DDDesktopTrackingService.getInstance().stop();
                } catch (DBException e) {
                    log.debug("DataDam tracking stop failed", e);
                    DDDesktopTrackingService.getInstance().reset();
                }
                return true;
            }

            @Override
            public void postShutdown(@NotNull IWorkbench workbench) {
                //empty
            }
        });

        AbstractJob startJob = new AbstractJob("DataDam tracking start") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
                        return Status.CANCEL_STATUS;
                    }
                    DDDesktopTrackingService.getInstance().start();
                } catch (DBException e) {
                    log.debug("DataDam tracking start failed", e);
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        startJob.setSystem(true);
        startJob.schedule();
    }
}
