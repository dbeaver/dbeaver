/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public class PostgreDebugController extends DBGBaseController {

    private static final Log log = Log.getLog(PostgreDebugController.class);

    public PostgreDebugController(DBPDataSourceContainer dataSourceContainer, Map<String, Object> configuration) {
        super(dataSourceContainer, configuration);
    }

    @Override
    public PostgreDebugSession createSession(DBRProgressMonitor monitor, Map<String, Object> configuration)
            throws DBGException
    {
        PostgreDebugSession pgSession = null;
        try {
            log.debug("Creating debug session");
            pgSession = new PostgreDebugSession(monitor,this);

            log.debug("Attaching debug session");
            pgSession.attach(monitor, configuration);

            log.debug("Debug session created");
            return pgSession;
        } catch (DBException e) {
            if (pgSession != null) {
                try {
                    pgSession.closeSession(monitor);
                } catch (Exception e1) {
                    log.error(e1);
                }
            }
            if (e instanceof DBGException) {
                throw (DBGException)e;
            }
            log.debug(String.format("Error attaching debug session %s", e.getMessage()));
            throw new DBGException("Error attaching debug session", e);
        }
    }

    @Override
    public DBGBreakpointDescriptor describeBreakpoint(Map<String, Object> attributes) {
        return PostgreDebugBreakpointDescriptor.fromMap(attributes);
    }

}
