/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.debug.core.model.ProcedureDebugTarget;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

public class ProcedureLaunchDelegate extends DatabaseLaunchDelegate {

    @Override
    protected DBGController createController(DBPDataSourceContainer dataSourceContainer, String databaseName,
                                             Map<String, Object> attributes) throws CoreException
    {
        try {
            return DebugCore.findProcedureController(dataSourceContainer);
        } catch (DBGException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    @Override
    protected DatabaseProcess createProcess(ILaunch launch, String name) {
        return new DatabaseProcess(launch, name);
    }

    @Override
    protected ProcedureDebugTarget createDebugTarget(ILaunch launch, DBGController controller, DatabaseProcess process) {
        return new ProcedureDebugTarget(launch, process, controller);
    }

}
