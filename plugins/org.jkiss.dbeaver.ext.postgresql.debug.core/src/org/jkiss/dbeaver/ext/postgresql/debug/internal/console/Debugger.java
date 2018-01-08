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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.console;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSession;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugBreakpoint;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugBreakpointProperties;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugObject;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugSession;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugSessionInfo;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugSessionManager;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugVariable;

@SuppressWarnings("nls")
public class Debugger {

    public static final String PROMPT = ">";
    public static final String COMMAND_ATTACH = "A";
    public static final String COMMAND_ABORT = "T";
    public static final String COMMAND_CLOSE = "X";
    public static final String COMMAND_STACK = "S";
    public static final String COMMAND_FRAME = "F";
    public static final String COMMAND_VARIABLES = "V";
    public static final String COMMAND_VARIABLE_SET = "=";
    public static final String COMMAND_BREAKPOINT = "B";
    public static final String COMMAND_BREAKPOINT_REMOVE = "R";
    public static final String COMMAND_BREAKPOINT_LIST = "L";
    public static final String COMMAND_CONTINUE = "C";
    public static final String COMMAND_INTO = "I";
    public static final String COMMAND_OVER = "O";
    public static final String COMMAND_TERMINATE = "E";
    public static final String COMMAND_SESSIONS = "W";
    public static final String COMMAND_OBJ = "D";
    public static final String COMMAND_NEW = "N";
    public static final String COMMAND_DEBUG_LIST = "Q";
    public static final String COMMAND_HELP = "?";

    public static final String ANY_ARG = "*";
    
    
    public static DBGVariable<?> chooseVariable(Scanner sc, PostgreDebugSessionManager pgDbgManager,
            PostgreDebugSession session) throws DBGException {

        DBGVariable<?> v = null;

        List<DBGVariable<?>> vars = session.getVarables();

        Scanner scArg;

        if (vars.size() == 1) {

            v = (PostgreDebugVariable) vars.get(0);

        } else {

            System.out.println("Choose variable (0 for quit) :");

            int varNo = 1;

            for (DBGVariable<?> cv : vars) {
                System.out.println(String.format(" (%d) %s", varNo++, cv.toString()));
            }

            int varId = -1;

            while (varId < 0) {

                String argc = sc.nextLine();

                String strvarid = "";

                scArg = new Scanner(argc);

                if (scArg.hasNext()) {

                    strvarid = scArg.next();

                    if (strvarid.trim().length() > 0) {

                        try {

                            varId = Integer.valueOf(strvarid);

                        } catch (Exception e) {
                            System.out.println(String.format("Incorrect var ID %s", strvarid));
                            varId = -1;
                        }

                        if (varId == 0) {
                            break;
                        }

                        if (varId > vars.size()) {
                            System.out.println(String.format("Incorrect var ID %s", strvarid));
                            varId = -1;
                        } else {
                            v = vars.get(varId - 1);
                            break;
                        }
                    }

                }
                scArg.close();

            }

        }

        return v;

    }

    public static PostgreDebugBreakpoint chooseBreakpoint(Scanner sc, PostgreDebugSessionManager pgDbgManager,
                                                          PostgreDebugSession session) throws DBGException {

        PostgreDebugBreakpoint bp = null;

        List<PostgreDebugBreakpoint> bps = session.getBreakpoints();

        Scanner scArg;

        if (bps.size() == 1) {

            bp = (PostgreDebugBreakpoint) bps.get(0);

        } else {

            System.out.println("Choose breakpoint (0 for quit) :");

            int bpNo = 1;

            for (PostgreDebugBreakpoint b : bps) {
                System.out.println(String.format(" (%d) %s", bpNo++, b.toString()));
            }

            int bpId = -1;

            while (bpId < 0) {

                String argc = sc.nextLine();

                String strBpid = "";

                scArg = new Scanner(argc);

                if (scArg.hasNext()) {

                    strBpid = scArg.next();

                    if (strBpid.trim().length() > 0) {

                        try {

                            bpId = Integer.valueOf(strBpid);

                        } catch (Exception e) {
                            System.out.println(String.format("Incorrect session ID %s", strBpid));
                            bpId = -1;
                        }

                        if (bpId == 0) {
                            break;
                        }

                        if (bpId > bps.size()) {
                            System.out.println(String.format("Incorrect breakpoint ID %s", strBpid));
                            bpId = -1;
                        } else {
                            bp = bps.get(bpId - 1);
                            break;
                        }
                    }

                }
                scArg.close();

            }

        }

        return bp;

    }

    public static PostgreDebugSession chooseSession(Scanner sc, PostgreDebugSessionManager pgDbgManager)
            throws DBGException {

        PostgreDebugSession debugSession = null;

        List<DBGSession> sessions = pgDbgManager.getDebugSessions();

        Scanner scArg;

        if (sessions.size() == 1) {

            debugSession = (PostgreDebugSession) sessions.get(0);

        } else {

            System.out.println("Choose debug session (0 for quit) :");

            int sessNo = 1;

            for (DBGSession s : sessions) {
                System.out.println(String.format(" (%d) %s", sessNo++, s.toString()));
            }

            int sessionId = -1;

            while (sessionId < 0) {

                String argc = sc.nextLine();

                String strSessionid = "";

                scArg = new Scanner(argc);

                if (scArg.hasNext()) {

                    strSessionid = scArg.next();

                    if (strSessionid.trim().length() > 0) {

                        try {

                            sessionId = Integer.valueOf(strSessionid);

                        } catch (Exception e) {
                            System.out.println(String.format("Incorrect session ID %s", strSessionid));
                            sessionId = -1;
                        }

                        if (sessionId == 0) {
                            break;
                        }

                        if (sessionId > sessions.size()) {
                            System.out.println(String.format("Incorrect session ID %s", strSessionid));
                            sessionId = -1;
                        } else {
                            debugSession = (PostgreDebugSession) sessions.get(sessionId - 1);
                            break;
                        }
                    }

                }
                scArg.close();

            }

        }

        return debugSession;

    }

    public static void main(String[] args) throws DBGException {

        String url = "jdbc:postgresql://192.168.229.133/postgres?user=postgres&password=postgres&ssl=false"; // "jdbc:postgresql://localhost/postgres?user=postgres&password=postgres&ssl=false";

        Connection conn;
        PostgreDebugSessionManager pgDbgManager;
        try {

            conn = DriverManager.getConnection(url);

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // TODO: fix connection
        pgDbgManager = new PostgreDebugSessionManager(null);

        Scanner sc = new Scanner(System.in);
        Scanner scArg;
        String command;
        while (true) {
            System.out.print(PROMPT);
            command = sc.next();
            switch (command.toUpperCase()) {

            case COMMAND_HELP:
                System.out.println("W Show sessions");
                System.out.println("N New session");
                System.out.println("D Show debug objects");
                System.out.println("S Stack");
                System.out.println("F Frame");
                System.out.println("V Variables");
                System.out.println("= Set Variables");
                System.out.println("L List breakpoint(s)");
                System.out.println("Q List debug session(s)");
                System.out.println("B Set breakpoint");
                System.out.println("R Remove breakpoint");
                System.out.println("C Continue execution");
                System.out.println("I Step into");
                System.out.println("O Step over");
                System.out.println("X Close session");
                System.out.println("T Abort session");
                System.out.println("E Exit debugger");
                System.out.println("? This help");
                break;

            case COMMAND_CLOSE:

                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionC = chooseSession(sc, pgDbgManager);

                if (debugSessionC == null) {
                    break;
                }

                pgDbgManager.terminateSession(debugSessionC.getSessionId());

                System.out.println("Session closed");

                break;

            case COMMAND_ABORT:

                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionAB = chooseSession(sc, pgDbgManager);

                if (debugSessionAB == null) {
                    break;
                }

                debugSessionAB.abort();

                System.out.println("Aborted.");

                break;

            case COMMAND_STACK:

                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionSL = chooseSession(sc, pgDbgManager);

                if (debugSessionSL == null) {
                    break;
                }

                List<DBGStackFrame> stack = debugSessionSL.getStack();

                if (stack.size() == 0) {
                    System.out.println("No stack defined");
                }

                for (DBGStackFrame s : stack) {
                    System.out.println(s.toString());
                }

                break;

            case COMMAND_FRAME:
                System.out.println("FRAME!!!");
                break;

            case COMMAND_VARIABLES:

                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionVL = chooseSession(sc, pgDbgManager);

                if (debugSessionVL == null) {
                    break;
                }

                List<DBGVariable<?>> vars = debugSessionVL.getVarables();

                if (vars.size() == 0) {
                    System.out.println("No vars defined");
                }

                for (DBGVariable<?> v : vars) {
                    System.out.println(v.toString());
                }

                break;

            case COMMAND_VARIABLE_SET:
                
                String strVal = "";

                String argcV = sc.nextLine();

                if (argcV.length() > 0) {

                    scArg = new Scanner(argcV);

                    if (scArg.hasNext()) {

                        strVal = scArg.next();

                    }
                    scArg.close();

                }
                
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionVS = chooseSession(sc, pgDbgManager);

                if (debugSessionVS == null) {
                    break;
                }

                
                DBGVariable<?> var = chooseVariable(sc, pgDbgManager,debugSessionVS);
                
                if (var == null) {
                    break;
                }

                debugSessionVS.setVariableVal(var, strVal);
                
                System.out.println(String.format("Variable Set %s",strVal));
                break;

            case COMMAND_BREAKPOINT:

                String strObjId = ANY_ARG;
                String strLineNo = ANY_ARG;

                String argc = sc.nextLine();

                if (argc.length() > 0) {

                    scArg = new Scanner(argc);

                    if (scArg.hasNext()) {

                        strObjId = scArg.next();

                        if (scArg.hasNext()) {

                            argc = scArg.nextLine();

                            if (argc.length() > 0) {
                                strLineNo = argc;
                            }

                        }

                    }
                    scArg.close();

                }

                int objId = -1;

                try {

                    objId = Integer.valueOf(strObjId.trim());

                } catch (Exception e) {
                    System.out.println(String.format("Incorrect object ID '%s'", strObjId));
                    break;
                }

                int lineNo = -1;

                if (strLineNo.trim().length() > 0) {

                    try {

                        lineNo = Integer.valueOf(strLineNo.trim());

                    } catch (Exception e) {
                        System.out.println(String.format("Incorrect line number '%s'", strLineNo));
                        break;
                    }

                }

                PostgreDebugObject debugObject = null;

                for (PostgreDebugObject o : pgDbgManager.getObjects("_", "_")) {
                    if (o.getID() == objId) {
                        debugObject = o;
                    }
                }

                if (debugObject == null) {
                    System.out.println(String.format("Object ID '%s' no found", strObjId));
                    break;
                }

                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSession = chooseSession(sc, pgDbgManager);

                if (debugSession == null) {
                    break;
                }

                PostgreDebugBreakpointProperties bpp = lineNo > 0 ? new PostgreDebugBreakpointProperties(lineNo, true)
                        : new PostgreDebugBreakpointProperties(true);

                PostgreDebugBreakpoint bp = (PostgreDebugBreakpoint) debugSession.setBreakpoint(debugObject, bpp);

                System.out.println("Breakpoint set");

                System.out.println(bp.toString());

                break;

            case COMMAND_BREAKPOINT_LIST:
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionBL = chooseSession(sc, pgDbgManager);

                if (debugSessionBL == null) {
                    break;
                }

                if (debugSessionBL.getBreakpoints().size() == 0) {
                    System.out.println("No breakpoints defined");
                }

                for (PostgreDebugBreakpoint bpl : debugSessionBL.getBreakpoints()) {
                    System.out.println(bpl.toString());
                }

                break;

            case COMMAND_BREAKPOINT_REMOVE:
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionBR = chooseSession(sc, pgDbgManager);

                if (debugSessionBR == null) {
                    break;
                }

                if (debugSessionBR.getBreakpoints().size() == 0) {
                    System.out.println("No breakpoints defined");
                }

                PostgreDebugBreakpoint bpr = chooseBreakpoint(sc, pgDbgManager, debugSessionBR);

                debugSessionBR.removeBreakpoint(bpr);

                System.out.println("Breakpoint removed ...");

                break;

            case COMMAND_CONTINUE:
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionSC = chooseSession(sc, pgDbgManager);

                if (debugSessionSC == null) {
                    break;
                }

                debugSessionSC.execContinue();

                System.out.println("Continue ...");

                break;

            case COMMAND_INTO:
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionSI = chooseSession(sc, pgDbgManager);

                if (debugSessionSI == null) {
                    break;
                }

                debugSessionSI.execStepInto();

                System.out.println("Step Into ...");

                break;

            case COMMAND_OVER:

                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionSO = chooseSession(sc, pgDbgManager);

                if (debugSessionSO == null) {
                    break;
                }

                debugSessionSO.execStepOver();

                System.out.println("Step over ...");

                break;

            case COMMAND_SESSIONS:
                for (PostgreDebugSessionInfo s : pgDbgManager.getSessions()) {
                    System.out.println(s);
                }
                break;

            case COMMAND_DEBUG_LIST:
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("no debug sessions");
                    break;
                }
                for (DBGSession s : pgDbgManager.getDebugSessions()) {
                    System.out.println(s);
                }

                break;

            case COMMAND_NEW:
                try {
                    Connection debugConn = DriverManager.getConnection(url);
                    // TODO: fix connection
                    PostgreDebugSession s = pgDbgManager.createDebugSession(null);
                    System.out.println("created");
                    System.out.println(s);

                } catch (SQLException e) {
                    e.printStackTrace();
                    break;
                }
                break;

            case COMMAND_OBJ:
                String proc = ANY_ARG;
                String owner = ANY_ARG;

                String arg = sc.nextLine();

                if (arg.length() > 0) {

                    scArg = new Scanner(arg);

                    if (scArg.hasNext()) {

                        proc = scArg.next();

                        if (scArg.hasNext()) {

                            arg = scArg.nextLine();

                            if (arg.length() > 0) {
                                owner = arg;
                            }

                        }

                    }
                    scArg.close();

                }

                for (PostgreDebugObject o : pgDbgManager.getObjects(owner.equals(ANY_ARG) ? "_" : owner,
                        proc.equals(ANY_ARG) ? "_" : proc)) {
                    System.out.println(o);
                }

                break;

            case COMMAND_ATTACH:
                if (pgDbgManager.getDebugSessions().size() == 0) {
                    System.out.println("Debug sessions not found");
                    break;
                }

                PostgreDebugSession debugSessionA = chooseSession(sc, pgDbgManager);

                if (debugSessionA == null) {
                    break;
                }

                System.out.println("Waiting for target session ...");

//FIXME:AF: Andrew, please migrate to attach(JDBCExecutionContext connection,int OID,int targetPID) API
//                debugSessionA.attach(false);

                break;

            case COMMAND_TERMINATE:
                System.out.println("EXIT.....");
                return;

            default:
                System.out.println(String.format("Unnown command '%s' for command list type ?", command));
                break;
            }

        }

    }

}
