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

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDDesktopConnectionService;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

import java.util.concurrent.atomic.AtomicBoolean;

public class DDDesktopConnectionInitializer implements IWorkbenchWindowInitializer {
    private static final Log log = Log.getLog(DDDesktopConnectionInitializer.class);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        try {
            DDDesktopConnectionService connection = DDDesktopConnectionService.getInstance();
            connection.start();
            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
                @Override
                public boolean preShutdown(@NotNull IWorkbench workbench, boolean forced) {
                    return true;
                }

                @Override
                public void postShutdown(@NotNull IWorkbench workbench) {
                    connection.close();
                }
            });
        } catch (DBException e) {
            log.warn("DataDam desktop connection could not be initialized", e);
        }
    }
}
