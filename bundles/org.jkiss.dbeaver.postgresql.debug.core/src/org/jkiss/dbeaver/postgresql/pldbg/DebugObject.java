package org.jkiss.dbeaver.postgresql.pldbg;

public interface DebugObject<OBJECTID> {
	
	OBJECTID getID();
	String getName();

}
