/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.debug;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * This interface is expected to be used in synch manner
 */
public interface DBGController {

    DBPDataSourceContainer getDataSourceContainer();

    Map<String, Object> getDebugConfiguration();

    /**
     * 
     * @return key to use for <code>detach</code>
     */
    DBGSession openSession(DBRProgressMonitor monitor) throws DBGException;

    DBGBreakpointDescriptor describeBreakpoint(Map<String, Object> attributes);

    /*
     * Events
     */
    void registerEventHandler(DBGEventHandler eventHandler);

    void unregisterEventHandler(DBGEventHandler eventHandler);

    void dispose();

}
