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
package org.jkiss.dbeaver.registry;

import org.eclipse.equinox.app.IApplicationContext;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DesktopPlatform;
import org.jkiss.dbeaver.core.DesktopUI;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rcp.DesktopApplicationImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

import java.nio.file.Path;

/**
 * EclipseApplicationImpl
 */
public abstract class EclipsePluginApplicationImpl extends DesktopApplicationImpl {

    public EclipsePluginApplicationImpl() {
        initializeApplicationServices();
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean isPrimaryInstance() {
        return false;
    }

    @Override
    public boolean isHeadlessMode() {
        return false;
    }

    @Override
    public String getInfoDetails(DBRProgressMonitor monitor) {
        return "Eclipse";
    }

    @Override
    public String getDefaultProjectName() {
        return "DBeaver";
    }

    @Override
    public Object start(IApplicationContext context) throws Exception {
        return null;
    }

    @Nullable
    @Override
    public Path getDefaultWorkingFolder() {
        return null;
    }

    @Override
    public void stop() {

    }

    @NotNull
    @Override
    public Class<? extends DBPPlatform> getPlatformClass() {
        return DesktopPlatform.class;
    }

    @Nullable
    @Override
    public Class<? extends DBPPlatformUI> getPlatformUIClass() {
        return DesktopUI.class;
    }

    @Override
    public long getLastUserActivityTime() {
        return -1;
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return DBeaverActivator.getInstance().getPreferences();

    }

}
