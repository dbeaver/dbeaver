/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Event processor job
 */
public abstract class EventProcessorJob extends AbstractJob {

    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_PORT = "port";
    public static final String VARIABLE_SERVER = "server";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_USER = "user";
    public static final String VARIABLE_PASSWORD = "password";
    public static final String VARIABLE_URL = "url";

    protected final DataSourceDescriptor container;

    protected EventProcessorJob(String name, DataSourceDescriptor container)
    {
        super(name);
        this.container = container;
    }

    protected void processEvents(DBPConnectionEventType eventType)
    {
        DBPConnectionInfo info = container.getActualConnectionInfo();
        DBRShellCommand command = info.getEvent(eventType);
        if (command != null && command.isEnabled()) {
            Map<String, Object> variables = new HashMap<String, Object>();
            for (Map.Entry<Object, Object> entry : info.getProperties().entrySet()) {
                variables.put(CommonUtils.toString(entry.getKey()), entry.getValue());
            }
            variables.put(VARIABLE_HOST, info.getHostName());
            variables.put(VARIABLE_PORT, info.getHostPort());
            variables.put(VARIABLE_SERVER, info.getServerName());
            variables.put(VARIABLE_DATABASE, info.getDatabaseName());
            variables.put(VARIABLE_USER, info.getUserName());
            variables.put(VARIABLE_PASSWORD, info.getUserPassword());
            variables.put(VARIABLE_URL, info.getUrl());

            DBRProcessDescriptor process = RuntimeUtils.processCommand(command, variables);
            if (process != null) {
                container.addChildProcess(process);
            }
        }
    }

}
