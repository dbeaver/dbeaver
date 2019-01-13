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
package org.jkiss.dbeaver.debug.jdbc;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class DBGJDBCSession implements DBGSession {

    private static final Log log = Log.getLog(DBGJDBCSession.class);

    private final DBGBaseController controller;

    protected volatile DBGJDBCWorker workerJob;

    private final List<DBGBreakpointDescriptor> breakpoints = new ArrayList<>(1);

    protected DBGJDBCSession(DBGBaseController controller) {
        this.controller = controller;
    }

    protected DBGBaseController getController() {
        return controller;
    }

    public abstract JDBCExecutionContext getControllerConnection();


    /**
     * Return true if debug session up and running on server
     * 
     * @return boolean
     */
    public abstract boolean isAttached();

    /**
     * Return true if session up and running debug thread
     * 
     * @return boolean
     */
    public boolean isWaiting() {
        DBGJDBCWorker job = this.workerJob;
        return (job != null && !job.isFinished());
    }

    public abstract boolean isDone();

    /**
     * Start thread for SQL command
     */
    protected void runAsync(String commandSQL, String name, DBGEvent begin, DBGEvent end) throws DBGException {
        workerJob = new DBGJDBCWorker(this, name, commandSQL, begin, end);
        workerJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                workerJob = null;
            }
        });
        workerJob.schedule();
    }

    public void closeSession(DBRProgressMonitor monitor) throws DBGException {
        if (!isAttached()) {
            return;
        }
        doDetach(monitor);
        if (!isDone() && workerJob != null) {
            workerJob.cancel();
            workerJob = null;
        }
    }

    protected abstract void doDetach(DBRProgressMonitor monitor) throws DBGException;

    protected abstract String composeAbortCommand();

    @Override
    public List<? extends DBGBreakpointDescriptor> getBreakpoints() {
        return new ArrayList<>(breakpoints);
    }

    @Override
    public void addBreakpoint(DBRProgressMonitor monitor, DBGBreakpointDescriptor descriptor) throws DBGException {
        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Add breakpoint")) {
            try (Statement stmt = session.createStatement()) {
                String sqlQuery = composeAddBreakpointCommand(descriptor);
                stmt.execute(sqlQuery);
            } catch (SQLException e) {
                throw new DBGException("SQL error", e);
            }
            breakpoints.add(descriptor);
        }
    }

    protected abstract String composeAddBreakpointCommand(DBGBreakpointDescriptor descriptor);

    @Override
    public void removeBreakpoint(DBRProgressMonitor monitor, DBGBreakpointDescriptor bp) throws DBGException {
        try (JDBCSession session = getControllerConnection().openSession(monitor, DBCExecutionPurpose.UTIL, "Remove breakpoint")) {
            try (Statement stmt = session.createStatement()) {
                String sqlCommand = composeRemoveBreakpointCommand(bp);
                stmt.execute(sqlCommand);
            } catch (SQLException e) {
                throw new DBGException("SQL error", e);
            }
            breakpoints.remove(bp);
        }
    }

    protected abstract String composeRemoveBreakpointCommand(DBGBreakpointDescriptor descriptor);

    protected void fireEvent(DBGEvent event) {
        controller.fireEvent(event);
    }

}
