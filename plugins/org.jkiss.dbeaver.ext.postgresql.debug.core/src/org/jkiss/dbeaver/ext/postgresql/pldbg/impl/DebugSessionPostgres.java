/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jkiss.dbeaver.ext.postgresql.pldbg.Breakpoint;
import org.jkiss.dbeaver.ext.postgresql.pldbg.BreakpointProperties;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugException;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugSession;
import org.jkiss.dbeaver.ext.postgresql.pldbg.StackFrame;
import org.jkiss.dbeaver.ext.postgresql.pldbg.Variable;

@SuppressWarnings("nls")
public class DebugSessionPostgres implements DebugSession<SessionInfoPostgres, DebugObjectPostgres, Integer> {

    private final SessionInfoPostgres sessionManagerInfo;

    private final SessionInfoPostgres sessionDebugInfo;

    private final Connection connection;

    private final String title;

    private final int sessionId;

    private static final String SQL_ATTACH = "select pldbg_wait_for_target(?sessionid)";

    private static final String SQL_ATTACH_BREAKPOINT = "select pldbg_wait_for_breakpoint(?sessionid)";

    private static final String SQL_LISTEN = "select pldbg_create_listener() as sessionid";

    private static final String SQL_GET_VARS = "select * from pldbg_get_variables(?sessionid)";

    private static final String SQL_GET_STACK = "select * from pldbg_get_stack(?sessionid)";

    private static final String SQL_STEP_OVER = "select pldbg_step_over(?sessionid)";

    private static final String SQL_STEP_INTO = "select pldbg_step_into(?sessionid)";

    private static final String SQL_CONTINUE = "select pldbg_continue(?sessionid)";

    private static final String SQL_ABORT = "select pldbg_abort_target(?sessionid)";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private List<PostgresBreakpoint> breakpoints = new ArrayList<PostgresBreakpoint>(1);

    private DebugSessionWorker worker;

    private FutureTask<DebugSessionResult> task;

    private Thread workerThread = null;

    private int listen() throws DebugException {

        acquireWriteLock();

        try (Statement stmt = connection.createStatement()) {

            ResultSet rs = stmt.executeQuery(SQL_LISTEN);

            if (rs.next()) {

                connection.setClientInfo("ApplicationName", "Debug Mode : " + String.valueOf(sessionId));
                return rs.getInt("sessionid");

            } else {

                throw new DebugException("Unable to create debug instance");

            }

        } catch (SQLException e) {
            throw new DebugException(e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void attach(boolean breakpoint) throws DebugException {

        acquireWriteLock();

        try {

            if (breakpoint) {
                runAsync(SQL_ATTACH_BREAKPOINT.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                        String.valueOf(sessionId) + " breakpoint attached to "
                                + String.valueOf(sessionManagerInfo.pid));

            } else {
                runAsync(SQL_ATTACH.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                        String.valueOf(sessionId) + " global attached to " + String.valueOf(sessionManagerInfo.pid));
            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    public DebugSessionPostgres(SessionInfoPostgres sessionManagerInfo, SessionInfoPostgres sessionDebugInfo,
            Connection connection) throws DebugException {
        this.sessionManagerInfo = sessionManagerInfo;
        this.sessionDebugInfo = sessionDebugInfo;
        this.connection = connection;
        this.title = sessionManagerInfo.application;
        this.worker = new DebugSessionWorker(this.connection);
        sessionId = listen();
    }

    @Override
    public SessionInfoPostgres getSessionInfo() {

        return sessionDebugInfo;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<PostgresBreakpoint> getBreakpoints() {

        return breakpoints;
    }

    @Override
    public Breakpoint setBreakpoint(DebugObjectPostgres obj, BreakpointProperties properties) throws DebugException {

        acquireReadLock();

        PostgresBreakpoint bp = null;

        try {
            bp = new PostgresBreakpoint(this, obj, (BreakpointPropertiesPostgres) properties);
            breakpoints.add(bp);

        } finally {
            lock.readLock().unlock();
        }

        return bp;
    }

    @Override
    public void removeBreakpoint(Breakpoint bp) throws DebugException {

        acquireReadLock();

        try {

            bp.drop();

            breakpoints.remove(bp);

        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void execContinue() throws DebugException {
        execStep(SQL_CONTINUE, " continue for ");

    }

    @Override
    public void execStepInto() throws DebugException {
        execStep(SQL_STEP_INTO, " step into for ");

    }

    @Override
    public void execStepOver() throws DebugException {
        execStep(SQL_STEP_OVER, " step over for ");

    }

    public void execStep(String commandSQL, String name) throws DebugException {

        acquireWriteLock();

        try {

            runAsync(commandSQL.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                    String.valueOf(sessionId) + name + String.valueOf(sessionManagerInfo.pid));

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public void abort() throws DebugException {

        acquireReadLock();

        try (Statement stmt = connection.createStatement()) {

            stmt.execute(SQL_ABORT.replaceAll("\\?sessionid", String.valueOf(sessionId)));

            task = null;

        } catch (SQLException e) {
            throw new DebugException(e);
        } finally {
            lock.readLock().unlock();
        }

    }

    public void close() {

        lock.writeLock().lock();

        try {

            try {
                if (!isDone()) {
                    task.cancel(true);
                }
            } catch (DebugException e1) {
                e1.printStackTrace();
            }

            connection.close();

        } catch (SQLException e) {

            e.printStackTrace();

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public List<Variable<?>> getVarables() throws DebugException {

        acquireReadLock();

        List<Variable<?>> vars = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {

            ResultSet rs = stmt.executeQuery(SQL_GET_VARS.replaceAll("\\?sessionid", String.valueOf(sessionId)));

            while (rs.next()) {

                PostgresVariable var = new PostgresVariable(rs.getString("name"), rs.getString("varclass"),
                        rs.getInt("linenumber"), rs.getBoolean("isunique"), rs.getBoolean("isconst"),
                        rs.getBoolean("isnotnull"), rs.getInt("dtype"), rs.getString("value"));

                vars.add(var);
            }

        } catch (SQLException e) {
            throw new DebugException(e);
        } finally {
            lock.readLock().unlock();
        }

        return vars;

    }

    @Override
    public void setVariableVal(Variable<?> variable, Object value) throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<StackFrame> getStack() throws DebugException {
        acquireReadLock();

        List<StackFrame> stack = new ArrayList<StackFrame>(1);

        try (Statement stmt = connection.createStatement()) {

            ResultSet rs = stmt.executeQuery(SQL_GET_STACK.replaceAll("\\?sessionid", String.valueOf(sessionId)));

            while (rs.next()) {
                PostgresStackFrame frame = new PostgresStackFrame(rs.getInt("level"), rs.getString("targetname"),
                        rs.getInt("func"), rs.getInt("linenumber"), rs.getString("args"));
                stack.add(frame);
            }

        } catch (SQLException e) {
            throw new DebugException(e);
        } finally {
            lock.readLock().unlock();
        }
        return stack;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return "DebugSessionPostgres " + (isWaiting() ? "WAITING" : "READY") + " [connection=" + connection + ", title="
                + title + ", sessionId=" + sessionId + ", breakpoints=" + breakpoints + "ManagerSession=("
                + sessionManagerInfo.toString() + ") Session=(" + sessionDebugInfo.toString() + ") " + "]";
    }

    @Override
    public Integer getSessionId() {
        return sessionId;
    }

    public boolean isWaiting() {
        return (task == null ? false : !task.isDone()) && (workerThread == null ? false : workerThread.isAlive());
    }

    public boolean isDone() throws DebugException {

        if (task == null)
            return true;

        if (task.isDone()) {

            DebugSessionResult res;

            try {
                res = task.get();
            } catch (InterruptedException | ExecutionException e1) {
                throw new DebugException(e1);
            }

            if (res.getException() != null) {
                System.out.println("WARNING " + res.getException().getMessage());
            }

            return res.isResult();

        }

        return false;

    }

    private void runAsync(String commandSQL, String name) throws DebugException {

        try (Statement stmt = connection.createStatement()) {

            connection.setAutoCommit(false);

            worker.execSQL(commandSQL);

            task = new FutureTask<DebugSessionResult>(worker);

            workerThread = new Thread(task);

            workerThread.setName(name);

            workerThread.start();

        } catch (SQLException e) {
            throw new DebugException(e);

        }
    }

    private void acquireReadLock() throws DebugException {

        try {

            lock.readLock().lockInterruptibly();

        } catch (InterruptedException e1) {

            throw new DebugException(e1.getMessage());

        }

        if (isWaiting()) {
            lock.readLock().unlock();
            throw new DebugException("Debug session in waiting state");
        }

        if (!isDone()) {
            lock.readLock().unlock();
            throw new DebugException("Debug session in incorrect state");
        }

    }

    private void acquireWriteLock() throws DebugException {

        try {

            lock.writeLock().lockInterruptibly();

        } catch (InterruptedException e1) {
            throw new DebugException(e1.getMessage());
        }

        if (isWaiting()) {
            lock.writeLock().unlock();
            throw new DebugException("Debug session in waiting state");
        }

        if (!isDone()) {
            lock.writeLock().unlock();
            throw new DebugException("Debug session in incorrect state");
        }

    }

}
