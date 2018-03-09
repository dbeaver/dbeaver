/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.debug.core.PostgreSqlDebugCore;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class PgSqlGlobalLaunchShortcut extends PgSqlBaseLaunchShortcut {

    @Override
    protected ILaunchConfiguration createConfiguration(DBSObject launchable) throws CoreException {
        ILaunchConfigurationWorkingCopy workingCopy = PostgreSqlDebugCore.createConfiguration(launchable);
        workingCopy.setAttribute(DebugCore.ATTR_ATTACH_KIND, DBGController.ATTACH_KIND_GLOBAL);
        return workingCopy.doSave();
    }

}
