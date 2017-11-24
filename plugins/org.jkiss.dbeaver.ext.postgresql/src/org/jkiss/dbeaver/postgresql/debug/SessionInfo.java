package org.jkiss.dbeaver.postgresql.debug;

public interface SessionInfo<SESSIONID> {
	
    SESSIONID getID();
    boolean isDebugWait();

}
