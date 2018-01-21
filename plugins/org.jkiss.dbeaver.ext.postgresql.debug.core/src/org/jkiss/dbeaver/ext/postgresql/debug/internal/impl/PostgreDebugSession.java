/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGBaseSession;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGEvent;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;

/**
 * Typical scenario for debug session <br/>
 * <br/>
 * 0. create session (now it can only attached to target Procedure)<br/><br/>
 * 1. attach to target this method attaches to a debugging target and listening on the given port - 
 *    waiting for run procedure in other session(s) debugger client should invoke this function after creation
 *    also created implicit breakpoint for target procedure, after this call debug session in <b>WAITING</b> state - 
 *    isDone returns false and is isWaiting returns true<br/><br/>
 * 2. when target procedure will called debug session implicit breakpoint will be reached
 *    and session goes in state <b>READY</b> (isDone - true, isWaiting - true) in this state possible to call
 *    getStack, getVariables, setVariables, setBreakpoint or execStepXXX\continue<br/><br/>
 * 3. when execStepXXX or continue will called session goes in <b>WAITING</b> state until next breakpoint or end of 
 *    procedure will be reached     <br/>
 *
 */
public class PostgreDebugSession extends DBGBaseSession {

    private final DBGSessionInfo sessionInfo;
    private final Object targetId;

    private int sessionId = -1;

    private static final String SQL_ATTACH = "select pldbg_wait_for_target(?sessionid)";


    private static final String SQL_LISTEN = "select pldbg_create_listener() as sessionid";

    private static final String SQL_GET_VARS = "select * from pldbg_get_variables(?sessionid)";
    
    private static final String SQL_SET_VAR = "select pldbg_deposit_value(?,?,?,?)";

    private static final String SQL_GET_STACK = "select * from pldbg_get_stack(?sessionid)";

    private static final String SQL_STEP_OVER = "select pldbg_step_over(?sessionid)";

    private static final String SQL_STEP_INTO = "select pldbg_step_into(?sessionid)";

    private static final String SQL_CONTINUE = "select pldbg_continue(?sessionid)";

    private static final String SQL_ABORT = "select pldbg_abort_target(?sessionid)";

    private static final String SQL_SET_GLOBAL_BREAKPOINT = "select pldbg_set_global_breakpoint(?sessionid, ?obj, ?line, ?target)";
    private static final String SQL_SET_BREAKPOINT = "select pldbg_set_breakpoint(?sessionid, ?obj, ?line)";
    private static final String SQL_DROP_BREAKPOINT = "select pldbg_drop_breakpoint(?sessionid, ?obj, ?line)";
    private static final String SQL_ATTACH_BREAKPOINT = "select pldbg_wait_for_breakpoint(?sessionid)";

    /**
     * Create session with two description 
     * after creation session need to be attached to postgres procedure by attach method
     * 
     * @param sessionManagerInfo - manager (caller connection) description
     * @param sessionDebugInfo - session (debugger client connection) description
     * @throws DBGException
     */
    public PostgreDebugSession(DBGBaseController controller, PostgreDebugSessionInfo sessionInfo, Object targetId) throws DBGException {
        super(controller);
        this.sessionInfo = sessionInfo;
        this.targetId = targetId;
    }

    /**
     * This method attach debug session to debug object (procedure) 
     * and wait forever while target or any (depend on targetPID) session will run target procedure  
     * 
     * @param connection - connection for debug session after attach this connection will forever belong to debug
     * @param OID - OID for target procedure
     * @param targetPID - target session PID (-1 for any target)
     * @throws DBGException
     */
    public void attach(JDBCExecutionContext connection,int OID,int targetPID) throws DBGException {

        lock.writeLock().lock();

        try {
            
            setConnection(connection);
            
            try (Statement stmt = getConnection().createStatement();
                    ResultSet rs = stmt.executeQuery(SQL_LISTEN)) {

                if (rs.next()) {
                    sessionId =  rs.getInt("sessionid");
                    getConnection().setClientInfo("ApplicationName", "Debug Mode : " + String.valueOf(sessionId));
                } else {
                    throw new DBGException("Unable to create debug instance");
                }

            } catch (SQLException e) {
                throw new DBGException("SQL error", e);
            } 

            PostgreDebugBreakpointProperties properties = new PostgreDebugBreakpointProperties(true);
            PostgreDebugObjectDescriptor obj = new PostgreDebugObjectDescriptor(OID,"ENTRY","SESSION","THIS","PG"); 
            PostgreDebugBreakpointDescriptor bp = new PostgreDebugBreakpointDescriptor(obj, properties);
            addBreakpoint(bp);
            
            String sessionParam = String.valueOf(getSessionId());
            String taskName = sessionParam + " global attached to " + String.valueOf(targetId);
            runAsync(SQL_ATTACH.replaceAll("\\?sessionid", sessionParam), taskName, new DBGEvent(this, DBGEvent.ATTACH));

            /*if (breakpoint) {
                runAsync(SQL_ATTACH_BREAKPOINT.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                        String.valueOf(sessionId) + " breakpoint attached to "
                                + String.valueOf(sessionManagerInfo.pid));

            } else {
                runAsync(SQL_ATTACH.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                        String.valueOf(sessionId) + " global attached to " + String.valueOf(sessionManagerInfo.pid));
            }*/

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public DBGSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    protected String composeAddBreakpointCommand(DBGBreakpointDescriptor descriptor) {
        PostgreDebugBreakpointDescriptor bp = (PostgreDebugBreakpointDescriptor) descriptor;
        PostgreDebugBreakpointProperties bpd = (PostgreDebugBreakpointProperties) bp.getProperties();
        String sqlPattern = bpd.isGlobal() ? SQL_SET_GLOBAL_BREAKPOINT : SQL_SET_BREAKPOINT;
    
        String sqlCommand = sqlPattern.replaceAll("\\?sessionid", String.valueOf(getSessionId()))
                .replaceAll("\\?obj", String.valueOf(descriptor.getObjectDescriptor().getID()))
                .replaceAll("\\?line", bpd.isOnStart() ? "-1" : String.valueOf(bpd.getLineNo()))
                .replaceAll("\\?target", bpd.isAll() ? "null"
                        : String.valueOf(bpd.getTargetId()));
        return sqlCommand;
    }

    protected String composeRemoveBreakpointCommand(DBGBreakpointDescriptor bp) {
        PostgreDebugBreakpointProperties properties = (PostgreDebugBreakpointProperties) bp.getProperties();
        String sqlCommand = SQL_DROP_BREAKPOINT.replaceAll("\\?sessionid", String.valueOf(getSessionId()))
                .replaceAll("\\?obj", String.valueOf(bp.getObjectDescriptor().getID()))
                .replaceAll("\\?line", properties.isOnStart() ? "-1" : String.valueOf(properties.getLineNo()));
        return sqlCommand;
    }

    @Override
    public void execContinue() throws DBGException {
        execStep(SQL_CONTINUE, " continue for ", DBGEvent.RESUME);
    }

    @Override
    public void execStepInto() throws DBGException {
        execStep(SQL_STEP_INTO, " step into for ", DBGEvent.STEP_INTO);

    }

    @Override
    public void execStepOver() throws DBGException {
        execStep(SQL_STEP_OVER, " step over for ", DBGEvent.STEP_OVER);

    }

    /**
     * Execute step SQL command  asynchronously, set debug session name to 
     * [sessionID] name [managerPID] 
     * 
     * @param commandSQL - SQL command for execute step
     * @param name - session 'name' part
     * @throws DBGException
     */
    public void execStep(String commandSQL, String name, int eventDetail) throws DBGException {

        acquireWriteLock();

        try {
            DBGEvent event = new DBGEvent(this, DBGEvent.RESUME, eventDetail);

            runAsync(commandSQL.replaceAll("\\?sessionid", String.valueOf(sessionId)),
                    String.valueOf(sessionId) + name + String.valueOf(targetId), event);

        } finally {
            lock.writeLock().unlock();
        }

    }

    protected String composeAbortCommand() {
        return SQL_ABORT.replaceAll("\\?sessionid", String.valueOf(sessionId));
    }

    @Override
    public List<DBGVariable<?>> getVariables() throws DBGException {

        acquireReadLock();

        List<DBGVariable<?>> vars = new ArrayList<>();

        String sql = SQL_GET_VARS.replaceAll("\\?sessionid", String.valueOf(sessionId));
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name = rs.getString("name");
                String varclass = rs.getString("varclass");
                int linenumber = rs.getInt("linenumber");
                boolean isunique = rs.getBoolean("isunique");
                boolean isconst = rs.getBoolean("isconst");
                boolean isnotnull = rs.getBoolean("isnotnull");
                int dtype = rs.getInt("dtype");
                String value = rs.getString("value");
                PostgreDebugVariable var = new PostgreDebugVariable(name, varclass, linenumber, isunique, isconst,
                        isnotnull, dtype, value);
                vars.add(var);
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }

        return vars;

    }

    @Override
    public void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException {
        
        acquireReadLock();
            
            try (PreparedStatement stmt = getConnection().prepareStatement(SQL_SET_VAR)) {
                
              if (variable instanceof PostgreDebugVariable){  
                
                if (value instanceof String){
                    
                    PostgreDebugVariable var = (PostgreDebugVariable) variable;
                    
                    stmt.setInt(1,sessionId);
                    stmt.setString(2,var.getName());
                    stmt.setInt(3,var.getLinenumber());
                    stmt.setString(4,(String) value);
                    
                    stmt.execute();

                }else {
                    lock.readLock().unlock();
                    throw new DBGException("Incorrect variable value class");
                }
                
            } else {
                lock.readLock().unlock();
                throw new DBGException("Incorrect variable class");
            }


            } catch (SQLException e) {
                throw new DBGException("SQL error", e);
            } finally {
                lock.readLock().unlock();
            }

    }

    @Override
    public List<DBGStackFrame> getStack() throws DBGException {
        acquireReadLock();

        List<DBGStackFrame> stack = new ArrayList<DBGStackFrame>(1);

        String sql = SQL_GET_STACK.replaceAll("\\?sessionid", String.valueOf(getSessionId()));
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int level = rs.getInt("level");
                String targetname = rs.getString("targetname");
                int func = rs.getInt("func");
                int linenumber = rs.getInt("linenumber");
                String args = rs.getString("args");
                PostgreDebugStackFrame frame = new PostgreDebugStackFrame(level, targetname, func, linenumber, args);
                stack.add(frame);
            }

        } catch (SQLException e) {
            throw new DBGException("SQL error", e);
        } finally {
            lock.readLock().unlock();
        }
        return stack;
    }

    @Override
    public String toString() {
        return "PostgreDebugSession " + (isWaiting() ? "WAITING" : "READY") + " [sessionId=" + sessionId + ", breakpoints=" + getBreakpoints() + "targetId=("
                + targetId + ") Session=(" + sessionInfo.toString() + ") " + "]";
    }

    @Override
    public Integer getSessionId() {
        return sessionId;
    }
    
    /**
     * Return true if debug session up and running on server 
     * 
     * @return boolean
     */
    public boolean isAttached() {
        return super.isAttached() && (sessionId > 0);
    }

}
