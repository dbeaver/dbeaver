/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rcp;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPApplicationDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;

/**
 * DesktopApplicationImpl
 */
public abstract class DesktopApplicationImpl extends BaseApplicationImpl implements DBPApplicationDesktop {

    private boolean isForcedRestart = false;

    @NotNull
    @Override
    public DBPWorkspaceDesktop createWorkspace(@NotNull DBPPlatform platform) {
        return new DesktopWorkspaceImpl(platform, loadEclipseWorkspace());
    }

    @NotNull
    protected IWorkspace loadEclipseWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    @Override
    public boolean isEnvironmentVariablesAccessible() {
        return true;
    }

    // Dirty fix of pro#6833
    // We should keep this flag somewhere in basic UI plugin
    public boolean isForcedRestart() {
        return isForcedRestart;
    }

    public void setIsForcedRestart(boolean isForcedRestart) {
        this.isForcedRestart = isForcedRestart;
    }


}
