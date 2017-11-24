package org.jkiss.dbeaver.postgresql.debug;

public interface Breakpoint {
	
	DebugObject<?> getObj();
	void activate() throws DebugException;
	void drop() throws DebugException;
	long getLineNo();

}
