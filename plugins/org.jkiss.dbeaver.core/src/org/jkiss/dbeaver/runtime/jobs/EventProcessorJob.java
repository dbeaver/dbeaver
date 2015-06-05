/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Event processor job
 */
public abstract class EventProcessorJob extends AbstractJob {

    static final Log log = Log.getLog(EventProcessorJob.class);

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

            DBRProcessDescriptor process = processCommand(command, variables);
            if (process != null) {
                container.addChildProcess(process);
            }
        }
    }

    private DBRProcessDescriptor processCommand(DBRShellCommand command, Map<String, Object> variables) {
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        final DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command, variables);
        // Direct execute
        try {
            processDescriptor.execute();
        } catch (DBException e) {
            UIUtils.showErrorDialog(shell, "Execute process", processDescriptor.getName(), e);
        }
        if (command.isShowProcessPanel()) {
            shell.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    try {
                        final ShellProcessView processView =
                            (ShellProcessView) DBeaverUI.getActiveWorkbenchWindow().getActivePage().showView(
                                ShellProcessView.VIEW_ID,
                                ShellProcessView.getNextId(),
                                IWorkbenchPage.VIEW_VISIBLE
                            );
                        processView.initProcess(processDescriptor);
                    } catch (PartInitException e) {
                        log.error(e);
                    }
                }
            });
        }
        if (command.isWaitProcessFinish()) {
            processDescriptor.waitFor();
        }
        return processDescriptor;
    }


}
