/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.runtime.jobs;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Event processor job
 */
public abstract class EventProcessorJob extends AbstractJob {

    private static final Log log = Log.getLog(EventProcessorJob.class);

    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_PORT = "port";
    public static final String VARIABLE_SERVER = "server";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_USER = "user";
    public static final String VARIABLE_PASSWORD = "password";
    public static final String VARIABLE_URL = "url";

    protected final DBPDataSourceContainer container;

    protected EventProcessorJob(String name, DBPDataSourceContainer container)
    {
        super(name);
        this.container = container;
    }

    protected void processEvents(DBPConnectionEventType eventType)
    {
        DBPConnectionConfiguration info = container.getActualConnectionConfiguration();
        DBRShellCommand command = info.getEvent(eventType);
        if (command != null && command.isEnabled()) {
            Map<String, Object> variables = new HashMap<>();
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

            final DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command, variables);
            DBUserInterface.getInstance().executeProcess(processDescriptor);
            if (command.isWaitProcessFinish()) {
                processDescriptor.waitFor();
            }
            if (container instanceof DataSourceDescriptor) {
                ((DataSourceDescriptor)container).addChildProcess(processDescriptor);
            }
        }
    }


}
