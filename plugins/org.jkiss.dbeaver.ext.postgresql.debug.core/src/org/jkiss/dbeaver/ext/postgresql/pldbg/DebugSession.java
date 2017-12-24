package org.jkiss.dbeaver.ext.postgresql.pldbg;

import java.util.List;

public interface  DebugSession<SESSIONINFO extends SessionInfo<?>,DEBUGOBJECT extends DebugObject<?>,SESSIONID> {
    SESSIONINFO getSessionInfo();
    String getTitle();
    List<? extends Breakpoint> getBreakpoints();
    Breakpoint setBreakpoint(DEBUGOBJECT obj,BreakpointProperties properties);
    void removeBreakpoint(Breakpoint bp);
    void execContinue();
    void execStepInto();
    void execStepOver();
    void abort();
    List<? extends Variable<?>> getVarables(String ctx);
    void setVariableVal(Variable<?> variable, Object value);
    List<? extends StackFrame> getStack();    
    SESSIONID getSessionId();
    //move Stack
}
