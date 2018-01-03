package org.jkiss.dbeaver.ext.postgresql.pldbg;

import java.util.List;

public interface  DebugSession<SESSIONINFO extends SessionInfo<?>,DEBUGOBJECT extends DebugObject<?>,SESSIONID> {
    SESSIONINFO getSessionInfo();
    String getTitle();
    List<? extends Breakpoint> getBreakpoints();
    Breakpoint setBreakpoint(DEBUGOBJECT obj,BreakpointProperties properties) throws DebugException;
    void removeBreakpoint(Breakpoint bp);
    void execContinue();
    void execStepInto();
    void execStepOver() throws DebugException;
    void abort();
    List<? extends Variable<?>> getVarables(String ctx);
    void setVariableVal(Variable<?> variable, Object value);
    List<? extends StackFrame> getStack() throws DebugException;    
    SESSIONID getSessionId();
    //move Stack
}
