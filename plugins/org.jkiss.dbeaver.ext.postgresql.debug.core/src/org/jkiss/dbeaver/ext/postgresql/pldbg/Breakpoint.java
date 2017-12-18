package org.jkiss.dbeaver.ext.postgresql.pldbg;

public interface Breakpoint {
	
	DebugObject<?> getObj();
	void activate() throws DebugException;
	void drop() throws DebugException;
	long getLineNo();

}
