package org.jkiss.dbeaver.postgresql.pldbg;

public interface SessionInfo<SESSIONID> {
	
    SESSIONID getID();
    boolean isDebugWait();

}
