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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.DBGProcedureController;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;
import org.jkiss.dbeaver.debug.core.model.ProcedureDebugTarget;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

public class ProcedureLaunchDelegate extends DatabaseLaunchDelegate<DBGProcedureController> {

    @Override
    protected DBGProcedureController createController(DataSourceDescriptor datasourceDescriptor, String databaseName,
            Map<String, Object> attributes) throws CoreException {
        String providerId = DebugCore.extractProviderId(datasourceDescriptor);
        if (providerId == null) {
            String message = NLS.bind("Unable to setup procedure debug for {0}", datasourceDescriptor.getName());
            throw new CoreException(DebugCore.newErrorStatus(message));
        }
        DBGProcedureController procedureController = DebugCore.findProcedureController(datasourceDescriptor);
        if (procedureController == null) {
            String message = NLS.bind("Procedure debug is not supported for {0}", datasourceDescriptor.getName());
            throw new CoreException(DebugCore.newErrorStatus(message));
        }
        procedureController.init(datasourceDescriptor, databaseName, attributes);
        return procedureController;
    }

    @Override
    protected DatabaseProcess createProcess(ILaunch launch, String name) {
        return new DatabaseProcess(launch, name);
    }

    @Override
    protected ProcedureDebugTarget createDebugTarget(ILaunch launch, DBGProcedureController controller,
            DatabaseProcess process) {
        return new ProcedureDebugTarget(launch, process, controller);
    }

}
