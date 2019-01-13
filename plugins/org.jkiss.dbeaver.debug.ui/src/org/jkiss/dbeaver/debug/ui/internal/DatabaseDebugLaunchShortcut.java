/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.debug.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.ui.DatabaseLaunchShortcut;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

public class DatabaseDebugLaunchShortcut extends DatabaseLaunchShortcut {

    private static final String CONFIG_TYPE = "org.jkiss.dbeaver.debug.launchConfiguration";

    public DatabaseDebugLaunchShortcut() {
        super(CONFIG_TYPE, "Database Debug");
    }

    @Override
    protected ILaunchConfiguration createConfiguration(DBSObject launchable, Map<String, Object> databaseContext) throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(CONFIG_TYPE);
        ILaunchConfigurationWorkingCopy launchConfig = type.newInstance(null, launchable.getName());

        if (databaseContext != null) {
            DebugUtils.putContextInConfiguration(launchConfig, databaseContext);
        }

        return launchConfig;
    }

    @Override
    protected boolean isCandidate(ILaunchConfiguration config, DBSObject launchable, Map<String, Object> databaseContext) throws CoreException {
        return super.isCandidate(config, launchable, databaseContext);
    }

}
