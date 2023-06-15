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
package org.jkiss.dbeaver.model.dpi.app;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.registry.DesktopApplicationImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Path;

/**
 * DPI application
 */
public class DPIApplication extends DesktopApplicationImpl {

    private static final Log log = Log.getLog(DPIApplication.class);

    @Override
    public Object start(IApplicationContext context) {
        Location instanceLoc = Platform.getInstanceLocation();
        log.debug("Starting DPI application at " + instanceLoc.getURL());

        DPIPlatform.createInstance();

        DBPApplication application = DBWorkbench.getPlatform().getApplication();

        log.debug("Exiting DPI application");

        return EXIT_OK;
    }

    @Override
    public void stop() {
        System.out.println("Stopping DPI application");
        super.stop();
    }

    @Override
    public @Nullable Path getDefaultWorkingFolder() {
        return null;
    }

    @Override
    public String getDefaultProjectName() {
        return "default";
    }

}
