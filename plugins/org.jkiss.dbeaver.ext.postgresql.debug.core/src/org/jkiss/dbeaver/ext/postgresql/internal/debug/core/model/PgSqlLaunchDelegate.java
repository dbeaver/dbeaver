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
package org.jkiss.dbeaver.ext.postgresql.internal.debug.core.model;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.jkiss.dbeaver.debug.core.model.DatabaseLaunchDelegate;
import org.jkiss.dbeaver.debug.core.model.DatabaseProcess;

public class PgSqlLaunchDelegate extends DatabaseLaunchDelegate<PgSqlDebugController> {

    @Override
    protected PgSqlDebugController createController(String datasourceId, String databaseName,
            Map<String, Object> attributes)
    {
        return new PgSqlDebugController(datasourceId, databaseName, attributes);
    }

    @Override
    protected DatabaseProcess createProcess(ILaunch launch, String name)
    {
        return new DatabaseProcess(launch, name);
    }

    @Override
    protected PgSqlDebugTarget createDebugTarget(ILaunch launch, PgSqlDebugController controller,
            DatabaseProcess process)
    {
        return new PgSqlDebugTarget(launch, process, controller);
    }

}
