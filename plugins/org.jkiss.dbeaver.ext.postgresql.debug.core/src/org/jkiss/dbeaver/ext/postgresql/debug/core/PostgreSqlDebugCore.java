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
package org.jkiss.dbeaver.ext.postgresql.debug.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.PostgreDebugCoreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class PostgreSqlDebugCore {
    
    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.ext.postgresql.debug.core"; //$NON-NLS-1$
    
    public static final String CONFIGURATION_TYPE = BUNDLE_SYMBOLIC_NAME + '.' + "pgSQL";//$NON-NLS-1$

    public static ILaunchConfigurationWorkingCopy createConfiguration(DBSObject launchable)
            throws CoreException {
        boolean isInstance = launchable instanceof PostgreProcedure;
        if (!isInstance) {
            throw DebugCore.abort(PostgreDebugCoreMessages.PostgreSqlDebugCore_e_procedure_required);
        }
        PostgreProcedure procedure = (PostgreProcedure) launchable;
        PostgreDataSource dataSource = procedure.getDataSource();
        DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();
        PostgreDatabase database = procedure.getDatabase();
        PostgreSchema schema = procedure.getContainer();
        
        String databaseName = database.getName();
        Object[] bindings = new Object[] {dataSourceContainer.getName(), databaseName,
                procedure.getName(), schema.getName()};
        String name = NLS.bind(PostgreDebugCoreMessages.PostgreSqlDebugCore_launch_configuration_name, bindings);
        //Let's use metadata area for storage
        IContainer container = null;
        ILaunchConfigurationWorkingCopy workingCopy = createConfiguration(container, name);
        workingCopy.setAttribute(DebugCore.ATTR_DRIVER, dataSourceContainer.getDriver().getId());
        workingCopy.setAttribute(DebugCore.ATTR_DATASOURCE, dataSourceContainer.getId());
        workingCopy.setAttribute(DebugCore.ATTR_DATABASE, databaseName);
        workingCopy.setAttribute(DebugCore.ATTR_OID, String.valueOf(procedure.getObjectId()));
        return workingCopy;
    }

    public static ILaunchConfigurationWorkingCopy createConfiguration(IContainer container, String name)
            throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(CONFIGURATION_TYPE);
        ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(container, name);
        return workingCopy;
    }
}
