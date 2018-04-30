/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public class PostgreDebugController extends DBGBaseController {

    public PostgreDebugController(DBPDataSourceContainer dataSourceDescriptor) {
        super(dataSourceDescriptor);
    }

    @Override
    public PostgreDebugSession createSession(DBRProgressMonitor monitor, Map<String, Object> configuration)
            throws DBGException
    {
        try {
            PostgreDebugSession pgSession = new PostgreDebugSession(monitor,this);

            int oid = Integer.parseInt(String.valueOf(configuration.get(PROCEDURE_OID)));
            int pid = Integer.parseInt(String.valueOf(configuration.get(ATTACH_PROCESS)));
            String kind = String.valueOf(configuration.get(ATTACH_KIND));
            boolean global = DBGController.ATTACH_KIND_GLOBAL.equals(kind);
            String call = (String) configuration.get(SCRIPT_TEXT);
            pgSession.attach(monitor, oid, pid, global, call);

            return pgSession;
        } catch (DBException e) {
            throw new DBGException("Error attaching debug session", e);
        }
    }

    @Override
    public DBGBreakpointDescriptor describeBreakpoint(Map<String, Object> attributes) {
        Object oid = attributes.get(DBGController.PROCEDURE_OID);
        Object lineNumber = attributes.get(DBGController.BREAKPOINT_LINE_NUMBER);
        long parsed = Long.parseLong(String.valueOf(lineNumber));
        PostgreDebugBreakpointProperties properties = new PostgreDebugBreakpointProperties(parsed, false);
        return new PostgreDebugBreakpointDescriptor(oid, properties );
    }

}
