/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    protected final DataSourceDescriptor container;

    protected EventProcessorJob(String name, DataSourceDescriptor container)
    {
        super(name);
        this.container = container;
    }

    protected void processEvents(DBPConnectionEventType eventType)
    {
        DBPConnectionInfo info = container.getConnectionInfo();
        DBRShellCommand command = info.getEvent(eventType);
        if (command != null && command.isEnabled()) {
            Map<String, Object> variables = new HashMap<String, Object>();
            for (Map.Entry<Object, Object> entry : info.getProperties().entrySet()) {
                variables.put(CommonUtils.toString(entry.getKey()), entry.getValue());
            }
            variables.put("host", info.getHostName());
            variables.put("port", info.getHostPort());
            variables.put("server", info.getServerName());
            variables.put("database", info.getDatabaseName());
            variables.put("user", info.getUserName());
            variables.put("password", info.getUserPassword());
            variables.put("url", info.getUrl());

            DBRProcessDescriptor process = RuntimeUtils.processCommand(command, variables);
            if (process != null) {
                container.addChildProcess(process);
            }
        }
    }

}
