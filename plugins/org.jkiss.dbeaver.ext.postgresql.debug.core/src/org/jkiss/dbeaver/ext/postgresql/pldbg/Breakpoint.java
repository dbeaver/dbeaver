package org.jkiss.dbeaver.ext.postgresql.pldbg;

public interface Breakpoint {
	
	DebugObject<?> getObj();	
	BreakpointProperties getProperties();
	void drop() throws DebugException;

}
