package org.jkiss.dbeaver.ext.postgresql.pldbg;

public interface SessionInfo<SESSIONID> {
	
    SESSIONID getID();
    boolean isDebugWait();

}
